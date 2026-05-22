# Architecture

The Forge LLM Sidecar is a standalone Python service that runs a [LangGraph](https://langchain-ai.github.io/langgraph/)
agent on behalf of Forge's AI. Forge is a Java application; the sidecar is kept
as a separate process so the agent can use the Python LangGraph ecosystem
without embedding a Python runtime in the game.

## High-level picture

```
            ┌────────────────────────── Forge (JVM) ──────────────────────────┐
            │  AiController                                                    │
            │    ├─ DeckRecognitionManager  (attaches the feature, fail-soft)   │
            │    │    ├─ DeckRecognitionObserver (Guava EventBus subscriber)    │
            │    │    └─ DeckRecognitionClient   (HttpURLConnection, async)     │
            │    └─ SidecarInfluence  (applies the response to AI decisions,    │
            │                          gated by SIDECAR_INFLUENCE_* AiProps)    │
            └───────────────────────────────┬──────────────────────────────────┘
                                  │  HTTP  (POST /recognize, /mulligan-plan, ...)
                                            ▼
            ┌──────────────────── forge-llm-sidecar (this app) ────────────────┐
            │  FastAPI  (app/main.py)                                          │
            │    └─ LangGraph graph  (app/graph.py): linear node chain         │
            │         game_advisor → mulligan_planner → combo_strategist       │
            │                                        → opponent_strategist     │
            │              ├─ metagame loader   (app/knowledge/metagame.py)    │
            │              ├─ curated KB        (app/knowledge/loader.py)      │
            │              ├─ piloting guides   (app/knowledge/piloting.py)    │
            │              ├─ format detector   (app/knowledge/format_detect)  │
            │              └─ LLM client        (app/llm_client.py)            │
            └────────┬─────────────────────┬────────────────────┬──────────────┘
                     │ HTTP                │ HTTP               │ files
                     ▼                     ▼                    ▼
              llama.cpp (local LLM,   Scryfall API        metagame_data/*.json
              OpenAI-compatible)   (card legalities)   (refreshed weekly by CI)
```

## Why a separate process

LangGraph is a Python library; Forge is Java. Rather than port LangGraph or
shell out per call, the agent runs as a long-lived HTTP service. This keeps a
clean boundary: Forge sends observations, the sidecar returns a guess. The
sidecar can be restarted, scaled, or developed independently of the game.

## Components

| Module | Responsibility |
|---|---|
| `app/main.py` | FastAPI app; `/health`, `/recognize`, `/identify-own-archetype`, `/mulligan-plan`, `/metagame`, `/piloting`, `/forge-log/analyze`, `/selfplay/reflect`, `/dashboard`; compiles the graph at startup. |
| `app/config.py` | Immutable `Config` built from environment variables. |
| `app/schema.py` | Pydantic request/response models + the `GraphState` TypedDict. |
| `app/graph.py` | Builds and caches the LangGraph `StateGraph` (the four-node chain). |
| `app/nodes/game_advisor.py` | First node: opponent recognition + locally-derived own-deck piloting advice (1 LLM call per trigger). |
| `app/nodes/mulligan_planner.py` | Deterministic keep/mulligan decision + rolling early-game plan. |
| `app/nodes/combo_strategist.py` | Refines action scores for AI combo decks (gated on a `combo_profile`). |
| `app/nodes/opponent_strategist.py` | Deeper opponent reasoning — hand inference, next-turn prediction, threat ranking (gated on an archetype profile + decision importance). |
| `app/advice.py`, `app/early_plan.py`, `app/combo.py`, `app/opponent_hand_probability.py` | Deterministic helpers backing the nodes (piloting advice, early plan, combo readiness, Bayesian opponent-hand prior). |
| `app/llm_client.py` | Async client for an OpenAI-compatible LLM server (JSON mode). |
| `app/knowledge/metagame.py` | Runtime loader for the committed metagame JSON. |
| `app/knowledge/loader.py` | Curated archetype knowledge base + merge logic. |
| `app/knowledge/piloting.py` | Piloting-guide loader + deterministic own-archetype identification. |
| `app/knowledge/piloting_schema.py` | Pydantic models for piloting guides. |
| `app/knowledge/format_detect.py` | Scryfall-backed format inference. |
| `app/knowledge/scraper.py` | MTGGoldfish scraper — used only by the CI/builder scripts, never at request time. |
| `app/knowledge/builder_llm.py` | Offline LLM client for the piloting-guide builder. |
| `scripts/scrape_metagame.py` | CLI run weekly by the GitHub Action. |
| `scripts/build_piloting_guides.py` | Offline CLI that generates piloting guides with an LLM. |

## Request flow (`POST /recognize`)

1. **Forge** observes the opponent's public plays and POSTs a `RecognitionRequest`.
2. **`main.py`** builds the initial `GraphState` and calls `graph.ainvoke(...)`.
3. **`game_advisor` node**:
   1. `_resolve_meta_slug` — uses the format Forge reported; if it is generic
      (`Constructed`), infers the precise format from the AI's own decklist via
      Scryfall. Cached per `game_id` so detection runs once per game.
   2. Loads the metagame archetypes for that format (`metagame.get_metagame`)
      and merges in curated `strategy`/`tells` detail (`loader.merge_with_curated`).
   3. Identifies the AI's *own* archetype from `deck_cards`
      (`piloting.identify_own_archetype` — deterministic, no LLM; cached per
      `game_id`) and loads the matching piloting guide.
   4. Builds one structured prompt (candidate archetypes with metagame shares,
      the opponent's plays, the AI's own piloting guide, the live board state).
   5. Makes a **single** LLM call that returns both the opponent guess and the
      piloting advice.
   6. Parses the result, clamps `confidence` to `[0, 1]`, and caps confidence
      for any archetype outside the known set.
4. The state then flows through the remaining nodes (each self-gating and
   fail-soft): **`mulligan_planner`** adds a keep/mulligan decision and a rolling
   early-game plan; **`combo_strategist`** refines action scores for AI combo
   decks; **`opponent_strategist`** does deeper opponent reasoning. See
   *The LangGraph graph* below.
5. **`main.py`** returns a `RecognitionResponse` carrying the merged result.

See [API.md](API.md) for the exact wire contract and [PILOTING.md](PILOTING.md)
for the piloting layer.

## The LangGraph graph

```
START ─▶ game_advisor ─▶ mulligan_planner ─▶ combo_strategist ─▶ opponent_strategist ─▶ END
```

A linear chain. `game_advisor` is the only node that always runs an LLM call;
the later nodes **self-gate** (combo only on a `combo_profile`, the opponent
strategist only on a recognized archetype profile and an important decision)
and **fail soft** — they enrich the state and never regress it. So a single
`/recognize` makes 1–3 LLM calls depending on the deck and the decision point.

Recognition and piloting advice stay merged into `game_advisor`'s single call
because the AI re-runs the graph on every action — splitting them would add
per-action lag. `GraphState` (in `app/schema.py`) is a `TypedDict` with
`total=False`, so each node reads/writes only the keys it needs and new nodes
can be appended without changing the HTTP contract. See [EXTENDING.md](EXTENDING.md).

## Metagame data pipeline

The sidecar performs **no web scraping on the request path**. Instead:

- `scripts/scrape_metagame.py` (using `app/knowledge/scraper.py`) scrapes
  MTGGoldfish and writes `app/knowledge/metagame_data/<format>.json`.
- `.github/workflows/update-metagame.yml` runs that script **weekly** and
  commits any changes.
- At runtime, `app/knowledge/metagame.py` just loads those committed files.

This removes a runtime network dependency, makes the data version-controlled
and reviewable, and isolates the brittle scraping logic in CI.

## Failure behavior

Every external dependency is treated as optional:

- **LLM server down** → the node returns `archetype: "Unknown", confidence: 0.0`
  and empty piloting advice.
- **Scryfall down** → format detection returns `None`; the node falls back to
  `DEFAULT_META_FORMAT`.
- **Metagame file missing** → the node falls back to the curated knowledge base.
- **Piloting guide missing/invalid** → the loader falls back to a generic guide
  for the strategy, then to `generic/midrange`.
- **Sidecar down entirely** → Forge's client fails soft; the game is unaffected.

Nothing in this service is on the game's critical path.
