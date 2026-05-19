# Playing with the LLM Sidecar

This guide walks you through playing a Magic: The Gathering game in Forge
while the LLM sidecar analyzes the game in real-time and provides post-game
analysis of the AI's thought process.

## What the sidecar does

The sidecar runs an LLM model to:

1. **Recognize the opponent's deck archetype** — as the opponent plays cards,
   the sidecar guesses what deck they're running (e.g. "Aggro", "Control",
   "Izzet Spells") with a confidence score.

2. **Advise the AI on piloting** — based on the AI's own deck and the current
   board state, the sidecar recommends what the AI should do next.

3. **Track game state** — life totals, board state, graveyards, combat damage,
   and all observed opponent plays.

## Quick start

### 1. Start the sidecar

```bash
cd forge-llm-sidecar
pip install -e .
python -m app.main
```

The sidecar listens on `http://localhost:18970` by default. Verify it's running:

```bash
curl http://localhost:18970/health
# {"status": "ok", "model": "local-model", ...}
```

### 2. Set up your LLM backend

The sidecar needs an OpenAI-compatible API endpoint. Set these environment
variables before starting:

```bash
export LLM_BASE_URL="http://localhost:8080/v1"   # Your LLM server
export LLM_API_KEY="not-needed"                   # Or your actual key
export MODEL_NAME="qwen3.6-27b"                   # Your model name
```

Popular local LLM servers:
- **llama.cpp** (`./server -m your-model.gguf -c 8192`)
- **Ollama** (`ollama run llama3` → `LLM_BASE_URL=http://localhost:11434/v1`)
- **vLLM** (`python -m vllm.entrypoints.api_server --model ...`)

### 3. Play a game in Forge

1. Open Forge and create a game:
   - **Game type**: 1v1 (or FFA, but 1v1 gives the best analysis)
   - **Format**: Any constructed format (Standard, Modern, Commander, etc.)
   - **Players**: You (human) vs. an AI player

2. Enable game logging:
   - In Forge, go to `Options → Game → Enable game log`
   - Note the log file path (usually in `~/.forge/logs/`)

3. Start the game and play normally. The AI will play its turns using
   Forge's built-in heuristic AI.

### 4. Analyze the game

#### Option A: Post-game analysis (easiest)

After the game ends, run:

```bash
python scripts/analyze_forge_log.py /path/to/game.log \
    --sidecar http://localhost:18970 \
    --opponent "YourName" \
    --ai-player "AI Player Name" \
    --output jsonl > analysis.jsonl
```

This sends every checkpoint (turn boundary + opponent action) to the sidecar
and prints the LLM's archetype guesses and piloting advice.

#### Option B: Live tailing (real-time)

While the game is running, watch the log file:

```bash
python scripts/analyze_forge_log.py /path/to/game.log \
    --tail \
    --sidecar http://localhost:18970 \
    --opponent "YourName" \
    --ai-player "AI Player Name"
```

The sidecar will analyze each new event as it appears in the log. You'll see
JSON output showing the AI's evolving understanding of your deck.

#### Option C: Parse only (no LLM needed)

Just extract structured data from the log, without an LLM:

```bash
python scripts/analyze_forge_log.py /path/to/game.log \
    --opponent "YourName" \
    --ai-player "AI Player Name" \
    --events
```

This prints all parsed events and checkpoints to stdout/stderr.

#### Option D: API endpoint

Send a log to the running sidecar directly:

```bash
curl -X POST http://localhost:18970/forge-log/analyze \
    -H "Content-Type: application/json" \
    -d '{
        "log": "$(cat /path/to/game.log)",
        "opponent": "YourName",
        "ai_player": "AI Player Name",
        "format": "Modern"
    }' | python -m json.tool
```

Returns every checkpoint with full LLM responses and training data.

#### Option E: Generate training data

```bash
python scripts/analyze_forge_log.py /path/to/game.log \
    --sidecar http://localhost:18970 \
    --opponent "YourName" \
    --ai-player "AI Player Name" \
    --training training_data.jsonl
```

Creates a JSONL file with `{observations → archetype, confidence, reasoning}`
pairs suitable for fine-tuning.

## Running the sidecar remotely

To play Forge from anywhere while the sidecar and LLM stay on a home server,
expose the sidecar over a [Tailscale](https://tailscale.com) tailnet:

1. On the home server, run the sidecar with `HOST=0.0.0.0` (the
   `scripts/forge-sidecar.service` systemd unit does this) and join a tailnet.
2. On the device running Forge, join the same tailnet and launch with
   `FORGE_SIDECAR_URL=http://home-server.tailnet-name.ts.net:18970 ./run-forge.sh`.

The live `/dashboard` view is then reachable from any tailnet device's browser.
See the "Remote access over Tailscale" section in the sidecar `README.md` for
full setup steps.

## Understanding the output

Each checkpoint JSON contains:

```json
{
    "turn": 5,
    "archetype": "Izzet Spells",
    "confidence": 0.72,
    "reasoning": "Opponent cast multiple red/blue instants and sorceries...",
    "alternatives": ["Rakdos Midrange", "Mono Red Aggro"],
    "piloting": {
        "own_archetype": "Blue Control",
        "recommended_play": "Hold your counterspell for their next spell...",
        "reasoning": "They likely have a finisher in hand...",
        "alternatives": ["Play your land and pass", "Tap lands for defense"]
    }
}
```

## Seeing the AI's thought process

To follow along during a game, use live tailing and pipe output to a viewer:

```bash
# Terminal 1: Play your game in Forge

# Terminal 2: Watch real-time analysis
python scripts/analyze_forge_log.py ~/.forge/logs/game.log \
    --tail --sidecar http://localhost:18970 \
    --opponent "You" --ai-player "Computer" | jq -r
    '.archetype + " (conf: " + (.confidence|tostring) + "): " + .reasoning'
```

You'll see output like:

```
Turn 1: Unknown (conf: 0.0): No plays observed yet
Turn 2: Mono Red Aggro (conf: 0.45): Opponent played three basic Mountains...
Turn 3: Izzet Spells (conf: 0.62): Casting red and blue instants at low CMC...
Turn 5: Izzet Spells (conf: 0.78): Consistent pattern of cheap spells...
Turn 8: Izzet Spells (conf: 0.91): Confident: opponent went wide with spells...
```

## Advanced usage

### Using a deck list

If the AI is using a specific deck, provide it for better format detection and
piloting advice:

```bash
python scripts/analyze_forge_log.py /path/to/game.log \
    --sidecar http://localhost:18970 \
    --deck-cards "Lightning Bolt" "Counterspell" "Island" "Mountain" ...
```

Or in the API:

```json
{
    "log": "...",
    "deck_cards": ["Lightning Bolt", "Counterspell", "Island", ...]
}
```

### Multiple format support

The sidecar supports all standard formats: `Standard`, `Modern`, `Legacy`,
`Pioneer`, `Vintage`, `Pauper`, `Commander`, `Constructed`. Specify with
`--format`:

```bash
python scripts/analyze_forge_log.py game.log --format Modern
```

### Performance tips

- **Local LLM**: Use a model with at least 8GB VRAM for responsive analysis
- **Batch size**: For post-game analysis, the default rate is fine. For live
  tailing, add `--delay 0.5` to avoid overwhelming the LLM
- **Offline parsing**: Run `--events` mode first to verify the log parses
  correctly before sending to the LLM

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `ModuleNotFoundError: aiohttp` | `pip install aiohttp` |
| Sidecar returns `Unknown` with 0 confidence | Check LLM is reachable: `curl $LLM_BASE_URL/v1/models` |
| No checkpoints generated | Verify `--opponent` and `--ai-player` match the names in the log |
| Wrong turn order | Forge logs are reverse-chronological; the adapter handles this automatically |
| Slow analysis | Use a faster model or increase `--delay` between calls |
| Parser misses events | Check stderr for unrecognized lines; file an issue with the log snippet |

## Architecture overview

```
┌─────────────────────────────────────────────────────────────┐
│  Forge (game client)                                        │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  Game log file (~/.forge/logs/game.log)               │  │
│  │  Turn: Turn 1 (Player)                                │  │
│  │  Land: Player played Mountain (54)                    │  │
│  │  Add To Stack: Player cast Lightning Bolt             │  │
│  │  ...                                                  │  │
│  └───────────────────────┬───────────────────────────────┘  │
└──────────────────────────┼──────────────────────────────────┘
                           │ file tail / read
┌──────────────────────────▼──────────────────────────────────┐
│  forge_log adapter (this module)                            │
│                                                             │
│  parser.py    →  Regex line parser → typed events           │
│  state.py     →  GameSessionState → checkpoints             │
│  __init__.py  →  ForgeLogAdapter → RecognitionRequests      │
└──────────────────────────┬──────────────────────────────────┘
                           │ POST /recognize
┌──────────────────────────▼──────────────────────────────────┐
│  LLM sidecar (FastAPI)                                      │
│                                                             │
│  graph.py     →  LangGraph pipeline                         │
│  game_advisor →  LLM call (archetype + piloting)            │
│  knowledge/   →  Metagame data, archetype KB, piloting guides│
└─────────────────────────────────────────────────────────────┘
```
