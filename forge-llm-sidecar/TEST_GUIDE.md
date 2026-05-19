# Test Guide: Running the Sidecar Against a Forge Game Log

This guide walks you through testing the LLM sidecar with a real Forge game log, from zero to full analysis.

---

## Prerequisites

- **Python 3.10+** installed
- **Forge game log file** (`.txt` file from `~/.forge/logs/` or wherever Forge saves logs)
- **LLM server** (optional for offline parsing, required for analysis)

### Install the sidecar

```bash
cd forge-llm-sidecar
python -m venv .venv && source .venv/bin/activate
pip install -e ".[dev]"
```

### Start the LLM server (optional)

If you want LLM-powered analysis (deck recognition + piloting advice), start an OpenAI-compatible server:

```bash
# llama.cpp example
llama-server -m your-model.gguf --host 0.0.0.0 --port 8080

# Or set env vars for any compatible server
export LLM_BASE_URL="http://localhost:8080/v1"
export MODEL_NAME="your-model-name"
```

If you skip this step, you can still parse logs offline (Mode 1 below).

### Start the sidecar

```bash
uvicorn app.main:app --host 127.0.0.1 --port 8000
```

Verify it's running:

```bash
curl http://localhost:8000/health
```

---

## Quick Test: Manual API Call

Test the `/recognize` endpoint with a sample request:

```bash
curl -X POST http://localhost:8000/recognize \
  -H 'Content-Type: application/json' \
  -d '{
    "client": "forge",
    "game_id": "test-1",
    "format": "Constructed",
    "turn": 3,
    "observations": [
      {"turn":1,"event":"land","card":"Steam Vents","cmc":0,"colors":["U","R"]},
      {"turn":2,"event":"spell","card":"Monastery Swiftspear","cmc":1,"colors":["R"]},
      {"turn":3,"event":"spell","card":"Lightning Bolt","cmc":1,"colors":["R"]}
    ],
    "deck_cards": ["Ragavan, Nimble Pilferer","Lightning Bolt","Monastery Swiftspear"]
  }' | python -m json.tool
```

Expected response: archetype guess, confidence score, reasoning, and piloting advice.

---

## Mode 1: Offline Parse (No LLM Needed)

Extract structured events and checkpoints from a log file. This validates the parser works on your log.

```bash
python scripts/analyze_forge_log.py /path/to/game.log \
    --opponent "OpponentName" \
    --ai-player "AIPlayerName"
```

**Flags:**
- `--events` — also print raw parsed events to stderr
- `--output jsonl` — compact one-line-per-checkpoint output (pipe-friendly)
- `--format Modern` — override format detection

**Example output (stderr):**
```
# Parsed 148 events from 512 lines
# Generated 25 checkpoints
```

**Example output (stdout):** JSON objects per checkpoint with observations, board state, life totals, etc.

---

## Mode 2: Full Analysis (With Sidecar)

Parse the log and send every checkpoint to the running sidecar for LLM analysis:

```bash
python scripts/analyze_forge_log.py /path/to/game.log \
    --sidecar http://localhost:8000 \
    --opponent "OpponentName" \
    --ai-player "AIPlayerName"
```

This prints the LLM's archetype guess and piloting advice for each checkpoint. Use `--delay 0.5` to avoid overwhelming your LLM server.

**Filtered output with jq:**
```bash
python scripts/analyze_forge_log.py game.log \
    --sidecar http://localhost:8000 \
    --opponent "You" --ai-player "AI" | \
    jq -r '"Turn \(.turn): \(.archetype) (conf: \(.confidence))"'
```

---

## Mode 3: Live Tailing (Real-Time During a Game)

Watch a live game log and get real-time analysis as events are appended:

```bash
python scripts/analyze_forge_log.py /path/to/game.log \
    --tail \
    --sidecar http://localhost:8000 \
    --opponent "YourName" \
    --ai-player "AIPlayerName"
```

Play a game in Forge in another terminal. The script will stream JSON analysis as each new event appears.

---

## Mode 4: API Endpoint (Direct HTTP)

Send a full log to the sidecar's `/forge-log/analyze` endpoint:

```bash
curl -X POST http://localhost:8000/forge-log/analyze \
    -H "Content-Type: application/json" \
    -d '{
        "log": "$(cat /path/to/game.log)",
        "opponent": "OpponentName",
        "ai_player": "AIPlayerName",
        "format": "Modern"
    }' | python -m json.tool
```

Returns all checkpoints with full LLM responses plus training data.

---

## Mode 5: Generate Training Data

Create a JSONL file of `{observations → archetype, confidence, reasoning}` pairs for fine-tuning:

```bash
python scripts/analyze_forge_log.py /path/to/game.log \
    --sidecar http://localhost:8000 \
    --opponent "OpponentName" \
    --ai-player "AIPlayerName" \
    --training training_data.jsonl
```

---

## Python API (Programmatic Usage)

```python
from app.forge_log import ForgeLogAdapter

adapter = ForgeLogAdapter(game_id="my-game", format="Standard")
adapter.set_opponent("Atlin")
adapter.set_ai_player("Rogist")

# Parse offline
checkpoints = adapter.parse(open("/path/to/game.log").read())

# Or tail live (async)
# async for req in adapter.tail("/path/to/game.log", "http://localhost:8000"):
#     ...
```

---

## Debugging Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /health` | Sidecar status and LLM reachability |
| `GET /metagame?format=modern` | Loaded metagame data for a format |
| `GET /piloting?format=modern` | Available piloting guides (add `&archetype=...` for a specific guide) |

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| No checkpoints generated | Verify `--opponent` and `--ai-player` match the names in the log exactly |
| Parser misses events | Run with `--events` flag to see what's being parsed; unrecognized lines go to stderr |
| Sidecar returns `Unknown` with 0 confidence | Check LLM is running: `curl $LLM_BASE_URL/v1/models` |
| Wrong turn order | The adapter handles Forge's reverse-chronological logs automatically |
| Slow analysis | Use a faster model or add `--delay` between LLM calls |
| `ModuleNotFoundError: aiohttp` | `pip install aiohttp` |
