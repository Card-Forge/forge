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

FastAPI + LangGraph service. The graph is a linear chain of four self-gating, fail-soft nodes:

```
client --HTTP--> FastAPI (app/main.py) --> LangGraph (app/graph.py):
    game_advisor -> mulligan_planner -> combo_strategist -> opponent_strategist
                                    --> app/knowledge/ (offline JSON data)
```

- `game_advisor` — opponent deck recognition (1 LLM call) + own-deck piloting advice derived locally from the guide.
- `mulligan_planner` — deterministic keep/mulligan + rolling early-game plan.
- `combo_strategist` — refines action scores for AI combo decks (gated on a `combo_profile`; 1 LLM call when it runs).
- `opponent_strategist` — deeper opponent reasoning: hand inference, next-turn prediction, threat ranking (gated on an archetype profile + decision importance; 1 LLM call when it runs).

So a request makes 1–3 LLM calls depending on the deck and decision point. Each later node only enriches the state and never regresses it.

- **Entrypoint**: `app/main.py` — FastAPI routes (`/health`, `/recognize`, `/identify-own-archetype`, `/mulligan-plan`, `/metagame`, `/piloting`, `/forge-log/analyze`, `/selfplay/reflect`, `/selfplay/record`, `/api/selfplay/trends`, `/` + `/dashboard`)
- **Graph**: `app/graph.py` — `get_graph()` returns the compiled LangGraph (cached for the process)
- **Nodes**: `app/nodes/` — `game_advisor.py`, `mulligan_planner.py`, `combo_strategist.py`, `opponent_strategist.py`
- **Knowledge**: `app/knowledge/` — metagame JSON, archetype JSON, piloting guides, Scryfall format detection, self-play learnings
- **Supporting modules**: `app/advice.py`, `app/early_plan.py`, `app/combo.py`, `app/opponent_hand_probability.py`
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
- `scripts/selfplay_report.py` — one-shot aggregation of runner JSONL (win-rate / turns table)
- `scripts/record_run.py` — ingest a runner JSONL batch into the persistent results DB,
  capturing timestamp + format + config + `learnings_version()` + git sha
- `scripts/selfplay_trends.py` — read the results DB: per-deck baseline-vs-latest and the
  per-run series, so win-rate / turns-to-win track over time against learnings changes

These use separate LLM env vars (`BUILDER_LLM_BASE_URL`, etc.) defaulting to the main `LLM_*` values.

### Self-play results store

`app/selfplay_store.py` is a SQLite store (stdlib, no deps) at `selfplay/results.db`
(gitignored; override with `FORGE_SELFPLAY_DB`). Flow: `SelfPlayRunner → runs/*.jsonl`
(raw artifact) **and** `POST /selfplay/record` (auto, end of run) → store →
`selfplay_trends.py` / `GET /api/selfplay/trends` (dashboard "Self-play Trends" panel).
`record_run.py` does the same ingest manually for existing JSONL. A run = one runner
invocation; a game = one seat-record. The server snapshots `learnings_version()` (and git
sha) on record so the self-improvement signal (did win-rate/turns move after learnings
changed?) is recoverable. Baseline = the run labelled `baseline` (else the earliest run for
that deck). The runner's `-record false` disables auto-record; `-format`/`-label` tag it.

## Environment variables

| Variable | Default | Notes |
|---|---|---|
| `LLM_BASE_URL` | `http://localhost:8080/v1` | OpenAI-compatible endpoint |
| `MODEL_NAME` | `local-model` | Model name in LLM requests |
| `LLM_DISABLE_THINKING` | `true` | Send `enable_thinking:false` — skip the `<think>` block (~20x faster) |
| `HOST` | `127.0.0.1` | uvicorn bind interface; `0.0.0.0` for remote access |
| `PORT` | `18970` | uvicorn listen port |
| `METAGAME_ENABLE` | `true` | Score guesses against metagame data |
| `FORMAT_DETECT_ENABLE` | `true` | Scryfall-based format detection |
| `DEFAULT_META_FORMAT` | `standard` | Fallback when detection fails |
| `FORGE_SELFPLAY_DB` | `selfplay/results.db` | SQLite path for the self-play results store |

## Docker

Build: `docker build -t forge-llm-sidecar .`
Run: `docker run -p 18970:18970 -e LLM_BASE_URL=http://host.docker.internal:8080/v1 forge-llm-sidecar`

CI publishes to GHCR on `master` pushes only.

## Docs

See `docs/` for detailed reference: `ARCHITECTURE.md`, `API.md`, `PILOTING.md`, `EXTENDING.md`, `ADAPTERS.md`, `DECK_IDENTIFICATION.md`.
