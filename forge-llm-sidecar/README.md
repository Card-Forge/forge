# Forge LLM Sidecar

A standalone Python service that runs the LangGraph agent powering Forge's
LLM-assisted AI. The single node — **`game_advisor`** — does two things in one
LLM call:

- **Deck recognition** — given the game format and the opponent's observed
  plays, it guesses which deck/archetype the opponent is playing.
- **Piloting advice** — using a per-archetype piloting guide for the AI's own
  deck, it recommends what the AI should play next (see
  [docs/PILOTING.md](docs/PILOTING.md)).

Forge's Java AI calls this service over local HTTP. The recognition guess is
shown in the game log only — it does not change how the heuristic AI plays.

## Architecture

```
client + adapter --HTTP--> this sidecar (FastAPI + LangGraph) --HTTP--> llama.cpp (local LLM)
                                  |                          \--HTTP--> Scryfall (format detect)
                                  ├─ metagame_data/*.json (refreshed weekly by a GitHub Action)
                                  \─ piloting/*.json      (deck-piloting guides)
```

The LangGraph graph is `START -> game_advisor -> END`. New nodes can be added
later without changing the HTTP contract. The sidecar is client-agnostic;
Forge is the reference *adapter* — see [docs/ADAPTERS.md](docs/ADAPTERS.md).

## Requirements

- Python 3.10+
- An OpenAI-compatible LLM server. The default is a local
  [llama.cpp](https://github.com/ggml-org/llama.cpp) server:

  ```sh
  llama-server -m model.gguf --host 0.0.0.0 --port 8080
  ```

  Any OpenAI-compatible endpoint works — point `LLM_BASE_URL` at it.

## Install & run

```sh
cd forge-llm-sidecar
python -m venv .venv && source .venv/bin/activate
pip install -e .
uvicorn app.main:app --port 18970
```

### Run with Docker

```sh
docker build -t forge-llm-sidecar .
docker run -p 18970:18970 \
  -e LLM_BASE_URL=http://host.docker.internal:8080/v1 \
  forge-llm-sidecar
```

The image is also published to GHCR by CI on every push to `master`:
`ghcr.io/<owner>/forge/forge-llm-sidecar:latest`.

## Configuration (environment variables)

| Variable               | Default                       | Meaning                                               |
|------------------------|-------------------------------|-------------------------------------------------------|
| `LLM_BASE_URL`         | `http://localhost:8080/v1`    | OpenAI-compatible API base URL (llama.cpp server)     |
| `LLM_API_KEY`          | `not-needed`                  | Bearer token; llama.cpp ignores it                    |
| `MODEL_NAME`           | `local-model`                 | Model name sent in the request                        |
| `LLM_TIMEOUT`          | `60`                          | LLM request timeout (seconds)                         |
| `PORT`                 | `18970`                       | Port the sidecar listens on                           |
| `METAGAME_ENABLE`      | `true`                        | Score guesses against the scraped metagame data       |
| `FORMAT_DETECT_ENABLE` | `true`                        | Detect the precise format via Scryfall when ambiguous |
| `DEFAULT_META_FORMAT`  | `standard`                    | Fallback format when detection fails                  |

The offline piloting-guide builder (`scripts/build_piloting_guides.py`) has its
own optional LLM config — `BUILDER_LLM_BASE_URL`, `BUILDER_LLM_API_KEY`,
`BUILDER_MODEL_NAME`, `BUILDER_LLM_TIMEOUT` — each defaulting to the matching
`LLM_*` value. See [docs/PILOTING.md](docs/PILOTING.md). The runtime sidecar
never uses these.

### Metagame knowledge (offline at runtime)

The sidecar does **not** scrape the internet on the request path. The metagame
breakdown lives in committed JSON files under `app/knowledge/metagame_data/`
(`<format>.json` — name, meta share %, colors, signature cards per archetype).

Those files are refreshed weekly by the **`update-metagame` GitHub Action**
(`.github/workflows/update-metagame.yml`), which runs `scripts/scrape_metagame.py`
and commits any changes. To refresh by hand:

```sh
python scripts/scrape_metagame.py            # all formats
python scripts/scrape_metagame.py modern     # one format
```

The scraper/parser lives in `app/knowledge/scraper.py` and targets MTGGoldfish;
if the site's markup changes, only that module needs adjusting. Curated detail
files in `app/knowledge/archetypes/` still supply `strategy`/`tells` enrichment.

### Piloting guides (offline at runtime)

Per-archetype JSON **piloting guides** (`app/knowledge/piloting/`) tell the LLM
how to play the AI's own deck — mulligan rules, game plan, win conditions, key
cards, matchup notes. Specific guides fall back to hand-authored generic guides
(`piloting/generic/<strategy>.json`). They are generated offline by
`scripts/build_piloting_guides.py` and validated against a Pydantic schema. See
[docs/PILOTING.md](docs/PILOTING.md).

### Format detection

Forge reports a generic `Constructed` game type without naming the format. The
sidecar therefore detects the precise format (Standard/Pioneer/Modern/Legacy/
Vintage) from the AI's own decklist — Forge sends it in `deck_cards` — by
looking up card legalities on the [Scryfall](https://scryfall.com/docs/api) API
and picking the narrowest format all cards are legal in. Result is cached per
game. Fully fail-soft: on failure it falls back to `DEFAULT_META_FORMAT`.

## Endpoints

- `GET /health` — `{"status":"ok","model":"...","metagame_enabled":...}`. Used
  by Forge for a fail-soft availability check.
- `POST /recognize` — opponent recognition + own-deck piloting advice. See
  `app/schema.py` for the request/response models.
- `GET /metagame?format=modern` — debug: shows the loaded metagame breakdown.
- `GET /piloting?format=modern&archetype=...` — debug: shows the resolved
  piloting guide (omit `archetype` to list available guides).

### Quick manual test

```sh
curl http://localhost:18970/health

curl -X POST http://localhost:18970/recognize \
  -H 'Content-Type: application/json' \
  -d '{
    "client": "forge",
    "game_id": "test",
    "format": "Constructed",
    "opponent_seat": 1,
    "turn": 3,
    "observations": [
      {"turn":1,"event":"land","card":"Steam Vents","cmc":0,"colors":["U","R"],"types":["Land"]},
      {"turn":2,"event":"spell","card":"Monastery Swiftspear","cmc":1,"colors":["R"],"types":["Creature"]},
      {"turn":3,"event":"spell","card":"Lightning Bolt","cmc":1,"colors":["R"],"types":["Instant"]}
    ],
    "deck_cards": ["Ragavan, Nimble Pilferer","Lightning Bolt","Monastery Swiftspear"]
  }'
```

## Connecting Forge

In an AI profile (`.ai` file) or via system property, enable the feature:

- `DECK_RECOGNITION_ENABLE=true`
- `DECK_RECOGNITION_SIDECAR_URL=http://localhost:18970`

or launch Forge with `-Dforge.ai.deckRecognition=true`.

The feature is **off by default** and **fail-soft**: if the sidecar is not
running, Forge logs one debug line and plays normally.

## Project layout

```
forge-llm-sidecar/
├─ app/
│  ├─ main.py                 FastAPI app: /health, /recognize, /metagame, /piloting
│  ├─ config.py               Environment-driven configuration
│  ├─ schema.py               Request/response models + GraphState
│  ├─ graph.py                LangGraph graph definition
│  ├─ llm_client.py           OpenAI-compatible LLM client (llama.cpp)
│  ├─ nodes/
│  │  └─ game_advisor.py      The graph node: recognition + piloting advice
│  └─ knowledge/
│     ├─ metagame.py          Runtime loader for scraped metagame data
│     ├─ scraper.py           MTGGoldfish scraper (CI/builder only)
│     ├─ format_detect.py     Scryfall-based format detection
│     ├─ loader.py            Curated archetype knowledge base
│     ├─ piloting.py          Piloting-guide loader + own-archetype id
│     ├─ piloting_schema.py   Pydantic models for piloting guides
│     ├─ builder_llm.py       Offline LLM client for the guide builder
│     ├─ archetypes/          Hand-curated archetype detail (strategy/tells)
│     ├─ metagame_data/       Scraped metagame JSON (refreshed weekly by CI)
│     └─ piloting/            Piloting guides (generic/ + per-format/)
├─ scripts/
│  ├─ scrape_metagame.py      CLI run by the update-metagame GitHub Action
│  └─ build_piloting_guides.py  Offline CLI that generates piloting guides
├─ tests/
└─ docs/
```

## Documentation

- [docs/DECK_IDENTIFICATION.md](docs/DECK_IDENTIFICATION.md) — a guided
  walkthrough of how the AI identifies a deck, including the LLM prompt.
- [docs/PILOTING.md](docs/PILOTING.md) — the piloting-guidance layer: the
  guide schema, the fallback chain, and the offline builder script.
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — components, request flow, the
  graph, the metagame pipeline, failure behavior.
- [docs/API.md](docs/API.md) — full HTTP contract for every endpoint.
- [docs/ADAPTERS.md](docs/ADAPTERS.md) — the client-agnostic adapter model.
- [docs/EXTENDING.md](docs/EXTENDING.md) — how to add a new graph node.

## Development

```sh
pip install -e ".[dev]"

ruff check .        # lint
black --check .     # formatting check  (drop --check to auto-format)
pytest              # tests (LLM + Scryfall calls are stubbed — fully offline)
```

Lint, formatting, and tooling config live in `pyproject.toml`
(`[tool.ruff]`, `[tool.black]`, `[tool.pytest.ini_options]`).

## CI/CD

Two GitHub Actions workflows cover the sidecar:

- **`sidecar-ci.yml`** — on every push / PR touching `forge-llm-sidecar/**`:
  runs ruff, `black --check`, and pytest, then builds the Docker image.
  On `master` the image is published to GHCR. (PR/branch builds do not push.)
- **`update-metagame.yml`** — weekly: re-scrapes the metagame and commits
  refreshed `metagame_data/*.json`.

## Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| No deck guesses appear in Forge's game log | Feature not enabled, or sidecar unreachable — check `GET /health`. |
| `/health` shows `"llm_reachable": false` | The LLM server is not running, or `LLM_BASE_URL` is wrong. |
| Guesses are always `"Unknown"` | The model call is failing — check the sidecar log and that the LLM server has a model loaded. |
| `/metagame` returns `count: 0` | Metagame data missing — run `python scripts/scrape_metagame.py`. |
| `/piloting` always returns a `generic/*` guide | No specific guide for that archetype yet — run `python scripts/build_piloting_guides.py <format>`. |
| Wrong format detected | Scryfall lookup failed or the deck is off-meta — falls back to `DEFAULT_META_FORMAT`. |
