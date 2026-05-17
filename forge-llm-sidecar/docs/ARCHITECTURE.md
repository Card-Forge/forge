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
            │         └─ deck_recognition node  (app/nodes/deck_recognition.py)│
            │              ├─ metagame loader   (app/knowledge/metagame.py)    │
            │              ├─ curated KB        (app/knowledge/loader.py)      │
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
| `app/main.py` | FastAPI app; `/health`, `/recognize`, `/metagame`; compiles the graph at startup. |
| `app/config.py` | Immutable `Config` built from environment variables. |
| `app/schema.py` | Pydantic request/response models + the `GraphState` TypedDict. |
| `app/graph.py` | Builds and caches the LangGraph `StateGraph`. |
| `app/nodes/deck_recognition.py` | The single graph node: resolve format → gather candidates → prompt the LLM → parse. |
| `app/llm_client.py` | Async client for an OpenAI-compatible LLM server (JSON mode). |
| `app/knowledge/metagame.py` | Runtime loader for the committed metagame JSON. |
| `app/knowledge/loader.py` | Curated archetype knowledge base + merge logic. |
| `app/knowledge/format_detect.py` | Scryfall-backed format inference. |
| `app/knowledge/scraper.py` | MTGGoldfish scraper — used only by the CI script, never at request time. |
| `scripts/scrape_metagame.py` | CLI run weekly by the GitHub Action. |

## Request flow (`POST /recognize`)

1. **Forge** observes the opponent's public plays and POSTs a `RecognitionRequest`.
2. **`main.py`** builds the initial `GraphState` and calls `graph.ainvoke(...)`.
3. **`deck_recognition` node**:
   1. `_resolve_meta_slug` — uses the format Forge reported; if it is generic
      (`Constructed`), infers the precise format from the AI's own decklist via
      Scryfall. Cached per `game_id` so detection runs once per game.
   2. Loads the metagame archetypes for that format (`metagame.get_metagame`)
      and merges in curated `strategy`/`tells` detail (`loader.merge_with_curated`).
   3. Builds a structured prompt (format, candidate archetypes with metagame
      shares, the opponent's chronological plays).
   4. Calls the local LLM via the OpenAI-compatible API in JSON mode.
   5. Parses the result, clamps `confidence` to `[0, 1]`, and caps confidence
      for any archetype outside the known set.
4. **`main.py`** returns a `RecognitionResponse`.

See [API.md](API.md) for the exact wire contract.

## The LangGraph graph

```
START ──▶ deck_recognition ──▶ END
```

Currently one node. `GraphState` (in `app/schema.py`) is a `TypedDict` with
`total=False` and deliberately holds more keys than this node uses, so future
nodes can read/write their own state without changing the HTTP contract. See
[EXTENDING.md](EXTENDING.md) for how to add a node.

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

- **LLM server down** → the node returns `archetype: "Unknown", confidence: 0.0`.
- **Scryfall down** → format detection returns `None`; the node falls back to
  `DEFAULT_META_FORMAT`.
- **Metagame file missing** → the node falls back to the curated knowledge base.
- **Sidecar down entirely** → Forge's client fails soft; the game is unaffected.

Nothing in this service is on the game's critical path.
