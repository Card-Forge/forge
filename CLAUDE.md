# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

Upstream **Forge** is a Java rules engine for *Magic: The Gathering* (multi-module Maven project, Java 17). This fork adds an **LLM-assisted AI** layer: a Python sidecar service (`forge-llm-sidecar/`) that the Java AI calls over local HTTP to recognize the opponent's deck and recommend plays. Most active work here happens in the sidecar and in `forge-ai/src/main/java/forge/ai/llm/`.

## Java side (Forge engine + AI)

### Build & run
```sh
mvn -U -B clean -P windows-linux -DskipTests install   # full build (profile lives in forge-installer/pom.xml)
mvn -pl forge-ai -am -DskipTests install                # build one module + its deps
./run-forge.sh                                          # launch desktop GUI from the source build
```
`run-forge.sh` wraps the launch: it `cd`s into `forge-gui/` (so the `res/` resource folder resolves), sets `DISPLAY`/`XAUTHORITY` for tmux/SSH sessions, and passes the `--add-opens` flags XStream needs on Java 17+. Build artifacts are picked up from `forge-gui-desktop/target/forge-gui-desktop-*-jar-with-dependencies.jar` (newest jar wins).

### Tests
Tests use **TestNG** and live under `<module>/src/test/java/`. Most modules have few or no tests.
```sh
mvn -pl forge-game test                                 # one module's tests
mvn -pl forge-game test -Dtest=ManaCostBeingPaidTest    # a single test class
```
Per CONTRIBUTING: do **not** add new CI unit/wiring tests unless they catch a real integration regression — agents tend to over-add them.

### Module layout
- **forge-core** — card model, rules primitives, game-state fundamentals.
- **forge-game** — game session, players, turn/phase flow, multiplayer.
- **forge-ai** — computer-opponent decision logic. `forge.ai.AiController` is the heuristic brain; `forge.ai.llm.*` is the sidecar bridge (see below).
- **forge-gui** — shared UI logic + all card-scripting resources under `forge-gui/res/`.
- **forge-gui-desktop** — Swing desktop frontend (entrypoint for `run-forge.sh`).
- **forge-gui-mobile** / **-android** / **-ios** — libgdx mobile frontends.
- Card scripting reference: `docs/Card-scripting-API`; scripts live in `forge-gui/res/`.

### Self-play / sim CLI (`SelfPlayRunner`, `SimulateMatch`)
Headless game runners launched via `java -jar …-jar-with-dependencies.jar selfplay …`. Two recurring gotchas:
- **Needs a `DISPLAY`.** `Main.main` builds the Swing `GuiDesktop` *before* dispatching the CLI mode, so with an empty `DISPLAY` the X11 init fails and the process **exits 1 with zero output** (the error handler swallows it). Run from `forge-gui/` with `DISPLAY` set (e.g. `:1`, check `/tmp/.X11-unix/`), mirroring `run-forge.sh`.
- **Turn counting: per-player vs. global.** `PhaseHandler.getTurn()` is the *global* counter — it ticks on **every** player's turn, so it roughly doubles in a 2-player game (a win on the pilot's turn 9 reads as game turn 18). For "what turn did this deck win on", use `Player.getTurn()` (= `stats.getTurnsPlayed()`), the per-player count. `SelfPlayRunner.playerTurn(...)` does this; don't regress its `win_turn`/`turns` back to the global turn.

## Sidecar bridge (`forge-ai/src/main/java/forge/ai/llm/`)

How the Java AI talks to the Python sidecar:
- **`DeckRecognitionManager.attach(ai, player, game)`** — called from the `AiController` constructor. Fully **fail-soft and self-gating**: if the feature is off (via the *"Enable AI Deck Recognition"* preference checkbox) or the sidecar is unreachable, nothing attaches and the game plays exactly as stock Forge.
- **`DeckRecognitionObserver`** — watches the game, builds `RecognitionRequest`s, and fires async `/recognize` calls to the sidecar.
- **`DeckRecognitionClient`** — HTTP client; base URL from `-Dforge.ai.deckRecognition.url` (set by `run-forge.sh` from `$FORGE_SIDECAR_URL`, default `http://localhost:18970`).
- **`SidecarInfluence`** — stores the latest structured `RecognitionResult` and applies a second-pass **personality weighting** to action percentages, which `AiController` reads at decision points. NOTE: the sidecar README still says recognition "does not change how the AI plays" — that is outdated; `SidecarInfluence` does feed back into play decisions, gated by `AiProps` personality factors.

Toggle gotcha (documented in `run-forge.sh`): the on/off switch is the UI preference, **not** a system property — `GamePlayerUtil` overwrites `-Dforge.ai.deckRecognition` at game start from the UI setting. Only the sidecar *URL* property is honored from the CLI. For verbose tracing use `FORGE_LOG_LEVEL=debug ./run-forge.sh` (it sets `-Dtinylog.writerdefault.level`, since `tinylog.properties` pins the per-writer level).

## Python sidecar (`forge-llm-sidecar/`)

FastAPI + LangGraph service. See `forge-llm-sidecar/AGENTS.md` for the authoritative, up-to-date guide.

### Dev commands (run from `forge-llm-sidecar/`)
```sh
pip install -e ".[dev]"
uvicorn app.main:app --port 18970     # dev server
ruff check .                          # lint   (CI order:)
black --check .                       # format check
pytest                                # tests (fully offline — LLM + Scryfall are stubbed)
```
CI (`.github/workflows/sidecar-ci.yml`) runs ruff → black → `pytest -q`. Line length is 100 for both ruff and black.

### Graph & nodes
The LangGraph chain is `START → game_advisor → mulligan_planner → combo_strategist → opponent_strategist → END`, assembled in `app/graph.py`. (The README's "single node" description predates the multi-node chain.) Nodes are in `app/nodes/`. Adding a node = `add_node` + `add_edge` in `graph.py`; the HTTP contract stays stable.

- **Entrypoint/routes**: `app/main.py` — `/health`, `/recognize`, `/identify-own-archetype`, `/mulligan-plan`, `/metagame`, `/piloting`, `/forge-log/analyze`, `/selfplay/reflect`, `/` (dashboard).
- **Schema**: `app/schema.py` — Pydantic request/response models + `GraphState`.
- **Knowledge** (`app/knowledge/`, all **offline at request time**): scraped `metagame_data/<format>.json`, curated `archetypes/*.json`, generated `piloting/*/*.json`, plus Scryfall-based `format_detect.py`. JSON is bundled as package-data (see `pyproject.toml`).
- **Forge log adapter**: `app/forge_log/` — parses Forge game logs into structured events.

### Key behaviors
- **No network on the request path.** Metagame data is committed JSON, refreshed weekly by the `update-metagame` GitHub Action. Format detection (Scryfall) results are cached per game and fail soft to `DEFAULT_META_FORMAT`.
- **Offline scripts** (`scripts/`, not on the runtime path): `scrape_metagame.py`, `build_piloting_guides.py`, `analyze_forge_log.py`, etc. These use separate `BUILDER_LLM_*` env vars defaulting to the runtime `LLM_*` values.
- Config is env-driven via `app/config.py`: `LLM_BASE_URL` (OpenAI-compatible, default llama.cpp at `http://localhost:8080/v1`), `MODEL_NAME`, `PORT` (18970), `HOST` (`0.0.0.0` for remote), `LLM_DISABLE_THINKING` (skip `<think>` block, ~20x faster).

### Testing quirks
Tests are fully offline: LLM calls and Scryfall lookups are monkeypatched. `tests/test_graph.py` pre-seeds the `_resolved_format`/`_own_archetype` caches via the `_offline` fixture. `asyncio_mode = "auto"`. Dashboard tests use `httpx.AsyncClient` + `ASGITransport` for in-process HTTP.
