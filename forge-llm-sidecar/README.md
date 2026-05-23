# Forge LLM Sidecar

A standalone Python service that runs the LangGraph agent powering Forge's
LLM-assisted AI. The graph is a chain of four nodes that together provide:

- **Deck recognition** (`game_advisor`) — given the game format and the
  opponent's observed plays, it guesses which deck/archetype the opponent is
  playing.
- **Piloting advice** (`game_advisor` + `mulligan_planner`) — using a
  per-archetype piloting guide for the AI's own deck, it recommends mulligan
  decisions and what the AI should play next (see [docs/PILOTING.md](docs/PILOTING.md)).
- **Combo lines** (`combo_strategist`) — for AI combo decks with a combo
  profile, it scores the best line to advance.
- **Opponent reasoning** (`opponent_strategist`) — infers the opponent's hand,
  predicts their next turn, and ranks their threats.

Forge's Java AI calls this service over local HTTP. The recognition guess is
always written to the game log; whether the rest of the response actually
changes how the AI plays is controlled on the Forge side by the
`SIDECAR_INFLUENCE_*` AI properties (see *Connecting Forge* below). With
influence off, the sidecar is purely advisory/log-only.

## Architecture

```
client + adapter --HTTP--> this sidecar (FastAPI + LangGraph) --HTTP--> llama.cpp (local LLM)
                                  |                          \--HTTP--> Scryfall (format detect)
                                  ├─ metagame_data/*.json (refreshed weekly by a GitHub Action)
                                  \─ piloting/*.json      (deck-piloting guides)
```

The LangGraph graph is the linear chain
`START → game_advisor → mulligan_planner → combo_strategist → opponent_strategist → END`.
Only `game_advisor` always runs an LLM call; the later nodes self-gate and fail
soft, so a request makes 1–3 LLM calls. New nodes can be appended without
changing the HTTP contract. The sidecar is client-agnostic; Forge is the
reference *adapter* — see [docs/ADAPTERS.md](docs/ADAPTERS.md).

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
| `LLM_TIMEOUT`          | `300`                         | LLM request timeout (seconds)                         |
| `LLM_DISABLE_THINKING` | `true`                        | Skip the model's `<think>` block (`enable_thinking:false`) — ~20x faster |
| `HOST`                 | `0.0.0.0`                     | Interface the sidecar binds to; set `127.0.0.1` for localhost only |
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
- `POST /recognize` — runs the full graph: opponent recognition + own-deck
  piloting advice + combo/opponent strategy. See `app/schema.py` for the models.
- `POST /mulligan-plan` — opening-hand keep/mulligan decision and early plan.
- `POST /identify-own-archetype` — deterministic (no-LLM) identification of the
  AI's own archetype from its decklist; cached and reused by `/recognize`.
- `POST /forge-log/analyze` — parse a Forge game log into structured events
  (used by tooling and post-game analysis).
- `POST /selfplay/reflect` — summarize a batch of self-play games into learnings.
- `POST /selfplay/record` — persist a finished self-play run into the results store
  (the runner calls this automatically at the end of a run).
- `GET /api/selfplay/trends` — per-deck baseline-vs-latest self-play results over time
  (powers the dashboard's "Self-play Trends" panel; `?archetype=` for one deck's full
  per-run series). Backed by the `selfplay/results.db` store — see below.
- `GET /metagame?format=modern` — debug: shows the loaded metagame breakdown.
- `GET /piloting?format=modern&archetype=...` — debug: shows the resolved
  piloting guide (omit `archetype` to list available guides).
- `GET /` and `GET /dashboard` — live recognition-history dashboard.

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

## Self-play results tracking

The Java `SelfPlayRunner` writes per-seat JSONL (one record per sidecar seat per game) and
records each finished run into a small SQLite store (`selfplay/results.db`, gitignored;
override with `FORGE_SELFPLAY_DB`) so every deck has a baseline and its performance can be
tracked over time.

**Automatic** — at the end of a run the runner POSTs its records to `POST /selfplay/record`
(fail-soft: the JSONL is still written if the sidecar is unreachable). Pass `-format` and an
optional `-label baseline` to tag the run; disable recording with `-record false`:

```sh
forge selfplay -config goldfish -p1 ruby.dck -p2 60-islands.dck -n 50 \
  -out runs/ruby.jsonl -format modern -label baseline
```

**Manual** — ingest existing JSONL files after the fact with the same store:

```sh
python -m scripts.record_run selfplay/runs/ruby.jsonl --format modern --config goldfish --label baseline
```

Read the results back:

```sh
# Per-deck baseline-vs-latest win% and turns-to-win deltas
python -m scripts.selfplay_trends
# Full per-run series for one deck (each run shows its learnings_version)
python -m scripts.selfplay_trends --archetype "Ruby Storm"
```

Each run snapshots the `learnings_version()` token, so win-rate / turns-to-win movements
line up against learnings changes. The same data renders live in the dashboard's
**Self-play Trends** panel via `GET /api/selfplay/trends`. `scripts/selfplay_report.py`
remains the quick one-shot aggregator over raw JSONL files.

## Card bucket profiles

The opponent strategist reads `app/knowledge/archetype_profiles/<format>/*.json`.
Build those profiles from real imported decklists with:

```sh
python scripts/import_goldfish_decks.py standard --limit 8
python scripts/build_card_buckets.py standard --refresh-scryfall
```

`build_card_buckets.py` parses `selfplay/decks/<format>/*.dck`, writes normalized
decklists to `app/knowledge/decklists/<format>/`, refreshes the local Scryfall
cache at `app/knowledge/card_cache/cards.json`, regenerates bucketed archetype
profiles, and writes `reports/card_bucket_coverage_<format>.md`.

## Connecting Forge

Forge integrates the sidecar through `forge.ai.llm.*` (see Forge's
[docs/AI.md](../docs/AI.md) for the Java-side design). There are two layers, each
gated by AI properties (set in an `.ai` profile or via system property):

**1. Deck recognition** — fires `/recognize` and writes the guess to the game log.

- `DECK_RECOGNITION_ENABLE=true` (off by default), or launch with
  `-Dforge.ai.deckRecognition=true`
- `DECK_RECOGNITION_SIDECAR_URL=http://localhost:18970`

**2. Sidecar influence** — lets the sidecar's response actually change AI play
(piloting, mulligan, targeting, combat). On by default *once recognition is
enabled*:

- `SIDECAR_INFLUENCE_ENABLE=true`, `SIDECAR_INFLUENCE_WEIGHT=0..100`
  (0 = advisory/log-only, 100 = force legal sidecar choices)
- finer-grained `SIDECAR_BIAS_*`, `SIDECAR_*_ENABLE`, and `SIDECAR_WAIT_MS_*`
  knobs — see `AiProps` in forge-ai

Everything is **fail-soft**: if the sidecar is not running or influence is off,
Forge logs one line and plays exactly as stock Forge.

The desktop GUI shows a transient "AI is thinking…" indicator while a decision
blocks on the sidecar (via `SidecarStatusBus`).

## Remote access over Tailscale

To run Forge from anywhere — laptop, another machine — while keeping the sidecar
and the LLM on a home server, expose the sidecar over a
[Tailscale](https://tailscale.com) tailnet. Tailscale provides the encryption
and access control (WireGuard), so the sidecar itself stays auth-free.

```
[ Forge, any device ]                  [ home server ]
  Forge desktop  --Tailscale-->  sidecar :18970  --localhost-->  llama.cpp :8080
  browser /dashboard --Tailscale--^
```

**On the home server:**

1. Install Tailscale and `tailscale up`. Enable **MagicDNS** in the admin
   console so the server has a stable name (`home-server.tailnet-name.ts.net`).
2. Run the sidecar with `HOST=0.0.0.0` so it binds the Tailscale interface.
   The provided systemd unit does this and keeps it running:

   ```sh
   mkdir -p ~/.config/systemd/user
   cp scripts/forge-sidecar.service ~/.config/systemd/user/
   # edit WorkingDirectory / ExecStart to match your install path
   systemctl --user daemon-reload
   systemctl --user enable --now forge-sidecar
   loginctl enable-linger "$USER"
   ```

3. Do **not** port-forward `18970` on your router — Tailscale is the only
   ingress path. Optionally restrict `:18970` to specific devices with a
   Tailscale ACL.

**On each device running Forge:** install Tailscale, join the same tailnet,
then launch Forge pointing at the home server:

```sh
FORGE_SIDECAR_URL=http://home-server.tailnet-name.ts.net:18970 ./run-forge.sh
```

The `/dashboard` view (live recognition history) is then reachable from any
tailnet device's browser at
`http://home-server.tailnet-name.ts.net:18970/dashboard`.

## Project layout

```
forge-llm-sidecar/
├─ app/
│  ├─ main.py                 FastAPI app: /recognize, /mulligan-plan, /piloting, ...
│  ├─ config.py               Environment-driven configuration
│  ├─ schema.py               Request/response models + GraphState
│  ├─ graph.py                LangGraph graph definition (the four-node chain)
│  ├─ llm_client.py           OpenAI-compatible LLM client (llama.cpp)
│  ├─ advice.py / early_plan.py / combo.py / opponent_hand_probability.py
│  │                          Deterministic helpers backing the nodes
│  ├─ nodes/
│  │  ├─ game_advisor.py      Recognition + locally-derived piloting advice
│  │  ├─ mulligan_planner.py  Keep/mulligan decision + early-game plan
│  │  ├─ combo_strategist.py  Combo-line scoring for AI combo decks
│  │  └─ opponent_strategist.py  Opponent hand inference / threat ranking
│  └─ knowledge/
│     ├─ metagame.py          Runtime loader for scraped metagame data
│     ├─ scraper.py           MTGGoldfish scraper (CI/builder only)
│     ├─ format_detect.py     Scryfall-based format detection
│     ├─ loader.py            Curated archetype knowledge base
│     ├─ piloting.py          Piloting-guide loader + own-archetype id
│     ├─ piloting_schema.py   Pydantic models for piloting guides
│     ├─ builder_llm.py       Offline LLM client for the guide builder
│     ├─ learnings.py         Self-play learnings loader (baseline + promoted notes)
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

- [docs/PLAYING_WITH_SIDECAR.md](docs/PLAYING_WITH_SIDECAR.md) — end-to-end
  walkthrough of playing a game with the sidecar attached.
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
