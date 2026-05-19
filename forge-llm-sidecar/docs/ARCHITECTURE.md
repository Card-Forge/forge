# Architecture

The Forge LLM Sidecar is a standalone Python service that runs a [LangGraph](https://langchain-ai.github.io/langgraph/)
agent on behalf of Forge's AI. Forge is a Java application; the sidecar is kept
as a separate process so the agent can use the Python LangGraph ecosystem
without embedding a Python runtime in the game.

## High-level picture

```
            ┌────────────────────────── Forge (JVM) ──────────────────────────┐
            │  AiController                                                    │
            │    └─ DeckRecognitionManager  (attaches the feature, fail-soft)   │
            │         ├─ DeckRecognitionObserver  (Guava EventBus subscriber)   │
            │         └─ DeckRecognitionClient    (HttpURLConnection, async)    │
            └───────────────────────────────┬──────────────────────────────────┘
                                            │  HTTP  (POST /recognize)
                                            ▼
            ┌──────────────────── forge-llm-sidecar (this app) ────────────────┐
            │  FastAPI  (app/main.py)                                          │
            │    └─ LangGraph graph  (app/graph.py)                            │
            │         └─ game_advisor node  (app/nodes/game_advisor.py)        │
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
| `app/main.py` | FastAPI app; `/health`, `/recognize`, `/metagame`, `/piloting`; compiles the graph at startup. |
| `app/config.py` | Immutable `Config` built from environment variables. |
| `app/schema.py` | Pydantic request/response models + the `GraphState` TypedDict. |
| `app/graph.py` | Builds and caches the LangGraph `StateGraph`. |
| `app/nodes/game_advisor.py` | The single graph node: opponent recognition + own-deck piloting advice, in one LLM call. |
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
4. **`main.py`** returns a `RecognitionResponse` carrying both.

See [API.md](API.md) for the exact wire contract and [PILOTING.md](PILOTING.md)
for the piloting layer.

## The LangGraph graph

```
START ──▶ game_advisor ──▶ END
```

One node, by design. Deck recognition and piloting advice are merged into a
single node and a single LLM call because the AI re-runs the graph on every
action — a second call would add per-action lag. `GraphState` (in
`app/schema.py`) is a `TypedDict` with `total=False` and holds more keys than
this node strictly needs, so future nodes can read/write their own state
without changing the HTTP contract. See [EXTENDING.md](EXTENDING.md).

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
