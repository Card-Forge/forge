# AGENTS.md — Forge LLM Sidecar

## Quick start

```bash
# This repo lives inside the larger Forge monorepo at ../forge-llm-sidecar/
pip install -e ".[dev]"      # install package + dev deps
uvicorn app.main:app --port 18970   # dev server
pytest                       # fully offline tests (LLM + Scryfall stubbed)
```

## Verify commands (run in this order)

```bash
ruff check .        # lint
black --check .     # format check
pytest              # tests
```

CI runs `ruff check .`, `black --check .`, then `pytest -q` (see `.github/workflows/sidecar-ci.yml`).

## Architecture

FastAPI + LangGraph service. Single graph node `game_advisor` does opponent deck recognition + own-deck piloting advice in one LLM call.

```
client --HTTP--> FastAPI (app/main.py) --> LangGraph (app/graph.py)
                                    --> app/nodes/game_advisor.py (LLM call)
                                    --> app/knowledge/ (offline JSON data)
```

- **Entrypoint**: `app/main.py` — FastAPI routes (`/health`, `/recognize`, `/metagame`, `/piloting`, `/`)
- **Graph**: `app/graph.py` — `get_graph()` returns compiled LangGraph
- **Node**: `app/nodes/game_advisor.py` — LLM prompt assembly + call
- **Knowledge**: `app/knowledge/` — metagame JSON, archetype JSON, piloting guides, Scryfall format detection
- **Config**: `app/config.py` — env-driven (`LLM_BASE_URL`, `MODEL_NAME`, `PORT`, etc.)
- **Schema**: `app/schema.py` — Pydantic request/response models + `GraphState`
- **Forge log adapter**: `app/forge_log/` — parses Forge game logs into structured events
- **Dashboard**: `app/static/dashboard.html` + `app/store.py` — in-memory request store

## Package structure

Monorepo sub-project. `pyproject.toml` defines the package as `app` with subpackages:
`app.nodes`, `app.knowledge`, `app.forge_log`, `app.static`.

Package data (JSON files) is bundled via `[tool.setuptools.package-data]`:
- `app/knowledge/archetypes/*.json` — curated archetype strategy/tells
- `app/knowledge/metagame_data/*.json` — scraped metagame (refreshed weekly by CI)
- `app/knowledge/piloting/*/*.json` — generated piloting guides

## Testing quirks

- All tests are **fully offline**. LLM calls and Scryfall lookups are monkeypatched.
- Graph tests (`tests/test_graph.py`) pre-seed `_resolved_format` and `_own_archetype` caches via `_offline` fixture to avoid network calls.
- `pytest.ini_options.asyncio_mode = "auto"` — no manual event loop needed.
- Dashboard tests use `httpx.AsyncClient` with `ASGITransport` for in-process HTTP testing.

## Scripts (offline, not runtime)

- `scripts/scrape_metagame.py` — scrapes MTGGoldfish, writes `app/knowledge/metagame_data/*.json`
- `scripts/build_piloting_guides.py` — generates piloting guide JSON from archetype data
- `scripts/analyze_forge_log.py` — CLI for parsing Forge game logs via `app.forge_log`

These use separate LLM env vars (`BUILDER_LLM_BASE_URL`, etc.) defaulting to the main `LLM_*` values.

## Environment variables

| Variable | Default | Notes |
|---|---|---|
| `LLM_BASE_URL` | `http://localhost:8080/v1` | OpenAI-compatible endpoint |
| `MODEL_NAME` | `local-model` | Model name in LLM requests |
| `PORT` | `18970` | uvicorn listen port |
| `METAGAME_ENABLE` | `true` | Score guesses against metagame data |
| `FORMAT_DETECT_ENABLE` | `true` | Scryfall-based format detection |
| `DEFAULT_META_FORMAT` | `standard` | Fallback when detection fails |

## Docker

Build: `docker build -t forge-llm-sidecar .`
Run: `docker run -p 18970:18970 -e LLM_BASE_URL=http://host.docker.internal:8080/v1 forge-llm-sidecar`

CI publishes to GHCR on `master` pushes only.

## Docs

See `docs/` for detailed reference: `ARCHITECTURE.md`, `API.md`, `PILOTING.md`, `EXTENDING.md`, `ADAPTERS.md`, `DECK_IDENTIFICATION.md`.
