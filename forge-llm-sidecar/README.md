# Forge LLM Sidecar

A standalone Python service that runs the LangGraph agent powering Forge's
LLM-assisted AI. The first (and currently only) node is **deck recognition**:
given the game format and the opponent's observed plays, it guesses which
deck/archetype the opponent is playing.

Forge's Java AI calls this service over local HTTP. The guess is shown in the
game log only — it does not change how the heuristic AI plays.

## Architecture

```
Forge (Java) --HTTP--> this sidecar (FastAPI + LangGraph) --HTTP--> Ollama (local LLM)
                              |                          \--HTTP--> Scryfall (format detect)
                              \-- metagame_data/*.json (refreshed weekly by a GitHub Action)
```

The LangGraph graph is `START -> deck_recognition -> END`. New nodes can be
added later without changing the HTTP contract.

## Requirements

- Python 3.10+
- A running [Ollama](https://ollama.com/) instance with a model pulled:

  ```sh
  ollama pull llama3.1:8b
  ```

## Install & run

```sh
cd forge-llm-sidecar
python -m venv .venv && source .venv/bin/activate
pip install -e .
uvicorn app.main:app --host 127.0.0.1 --port 8000
```

## Configuration (environment variables)

| Variable               | Default                  | Meaning                                               |
|------------------------|--------------------------|-------------------------------------------------------|
| `OLLAMA_URL`           | `http://localhost:11434` | Base URL of the Ollama server                         |
| `MODEL_NAME`           | `llama3.1:8b`            | Ollama model to use                                   |
| `PORT`                 | `8000`                   | Port the sidecar listens on                           |
| `METAGAME_ENABLE`      | `true`                   | Score guesses against the scraped metagame data       |
| `FORMAT_DETECT_ENABLE` | `true`                   | Detect the precise format via Scryfall when ambiguous |
| `DEFAULT_META_FORMAT`  | `standard`               | Fallback format when detection fails                  |

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
- `POST /recognize` — see `app/schema.py` for the request/response models.
- `GET /metagame?format=modern` — debug: shows the loaded metagame breakdown.

### Quick manual test

```sh
curl http://localhost:8000/health

curl -X POST http://localhost:8000/recognize \
  -H 'Content-Type: application/json' \
  -d '{
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
- `DECK_RECOGNITION_SIDECAR_URL=http://localhost:8000`

or launch Forge with `-Dforge.ai.deckRecognition=true`.
