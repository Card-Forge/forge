# ForgeHeadless Documentation

## Overview
ForgeHeadless is a headless Magic: The Gathering game engine. The current build exposes an embedded HTTP server so external tooling can drive the match loop, but the **long-term plan is for Java to become the HTTP client** that calls out to a custom AI service (owned by you) for every decision. Use this document as the canonical reference while we migrate to that architecture.

- **Today**: Java hosts an HTTP API (`GET /state`, `GET /input`, etc.) so that a separate process can poll for prompts and push actions.
- **Planned**: Java will call a configurable AI endpoint with the full game history and possible actions (PA) and expect back a chosen action/target.
- **Goal**: Replace the old CLI/Scanner bridge with a network-friendly decision surface that lets us swap in your bespoke AI without touching the core rules engine.

## Architecture

### Current (v0)

```
┌─────────────────┐       HTTP        ┌──────────────────┐
│   Your AI/LLM   │ ◄──────────────► │  ForgeHeadless   │
│   (Python, etc) │   JSON API        │  (Java Server)   │
└─────────────────┘                   └──────────────────┘
                │
                ▼
              ┌──────────────────┐
              │   Forge Engine   │
              │  (Game Rules)    │
              └──────────────────┘
```

External automations poll `GET /input` and push `POST /action`/`POST /target`. This is the bridge we just landed.

### Roadmap (v1)

```
┌──────────────────┐   HTTPS (client)   ┌────────────────────┐
│  ForgeHeadless   │ ─────────────────► │  Your AI Endpoint  │
│   (Java Engine)  │   game_state+PA    │  (LLM/RL service)  │
└──────────────────┘ ◄───────────────── └────────────────────┘
    │                  action/targets
    ▼
  ┌──────────────────┐
  │   Forge Engine   │
  │  (Game Rules)    │
  └──────────────────┘
```

In v1, Java will send serialized game state + possible actions to your hosted AI, wait for the response, and then execute it locally. The outbound call can reuse the same JSON contracts documented below; we simply flip the client/server ownership. Until that lands, keep using the embedded HTTP server for local testing.

## Quick Start

### Building
```bash
mvn clean install -DskipTests
```

### Running the Server
```bash
./forge-headless
```
Server starts on **port 8081**.

### Testing the API
```bash
# Get game state
curl http://localhost:8081/state

# Get available actions
curl http://localhost:8081/input

# Take an action (pass priority)
curl -X POST -d '{"index": 0}' http://localhost:8081/action
```

## HTTP API Reference

### `GET /state`
Returns the complete game state as JSON.

**Response:**
```json
{
  "turn": 1,
  "phase": "MAIN1",
  "activePlayerId": 0,
  "priorityPlayerId": 0,
  "stack": [],
  "stack_size": 0,
  "players": [
    {
      "id": 0,
      "name": "Player 1",
      "life": 20,
      "libraryCount": 53,
      "hand": [{"name": "Mountain", "id": 7, "zone": "Hand"}, ...],
      "graveyard": [],
      "battlefield": [],
      "exile": []
    },
    ...
  ]
}
```

### `GET /input`
Returns the current prompt type and available options.

**Response (when action needed):**
```json
{
  "type": "action",
  "data": {
    "actions": [
      {
        "type": "play_land",
        "card_id": 7,
        "card_name": "Mountain"
      },
      {
        "type": "cast_spell",
        "card_id": 34,
        "card_name": "Shock",
        "ability_description": "CARDNAME deals 2 damage to any target.",
        "mana_cost": "{R}",
        "requires_targets": true,
        "target_min": 1,
        "target_max": 1
      },
      {
        "type": "pass_priority"
      }
    ],
    "count": 3
  }
}
```

**Response (when target selection needed):**
```json
{
  "type": "target",
  "data": {
    "min": 1,
    "max": 1,
    "title": "Select targets for Shock",
    "targets": [
      {"index": 0, "type": "Player", "name": "Player 1", "id": 0, "life": 20},
      {"index": 1, "type": "Player", "name": "AI Player 2", "id": 1, "life": 20}
    ]
  }
}
```

**Response (when no input needed):**
```json
{
  "type": "none",
  "data": {}
}
```

### `POST /action`
Submit an action by index from the `/input` response.

**Request:**
```json
{"index": 0}
```

**Response:**
```
Action queued
```

### `POST /target`
Submit a target selection by index.

**Request:**
```json
{"index": 1}
```

**Response:**
```
Target selection queued
```

### `POST /control`
Send control commands.

**Request:**
```json
{"command": "pass_priority"}
```
or
```json
{"command": "concede"}
```

**Response:**
```
Command queued
```

## Command Line Options

```bash
./forge-headless [options]
```

| Option | Description |
|--------|-------------|
| (default) | Player 1 = Human (HTTP-controlled), Player 2 = AI |
| `--both-ai` | Both players AI-controlled (simulation mode) |
| `--both-human` | Both players HTTP-controlled |
| `--p1-ai` | Player 1 = AI, Player 2 = HTTP-controlled |
| `--p2-human` | Player 2 = HTTP-controlled |
| `--verbose` | Enable detailed game event logging |
| `--help` | Show help message |

## AI/LLM Integration

### Recommended Flow

```python
import requests
import time

BASE_URL = "http://localhost:8081"

def play_game():
    while True:
        # 1. Check what input is needed
        input_resp = requests.get(f"{BASE_URL}/input").json()
        
        if input_resp["type"] == "none":
            time.sleep(0.1)  # Wait for game to need input
            continue
            
        elif input_resp["type"] == "action":
            # 2. Get game state for context
            state = requests.get(f"{BASE_URL}/state").json()
            
            # 3. Your AI decides which action to take
            action_index = your_llm_decides(state, input_resp["data"])
            
            # 4. Submit the action
            requests.post(f"{BASE_URL}/action", json={"index": action_index})
            
        elif input_resp["type"] == "target":
            # Handle target selection
            target_index = your_llm_selects_target(input_resp["data"])
            requests.post(f"{BASE_URL}/target", json={"index": target_index})
```

### TODO: LLM Endpoint Integration

- [ ] Add configuration for outbound AI endpoint (URL, auth, timeout)
- [ ] Serialize full decision context (game history + PA payload) in a single request body
- [ ] Handle streaming/async responses from the AI service
- [ ] Fall back to embedded HTTP server when endpoint is unavailable (dev mode)

> **Heads-up**: The custom AI is owned by you. Please drop the endpoint contract (request/response schema, auth expectations) into this README once finalized so we can wire Java directly to it.

## Logging

### Verbose Mode
Enable with `--verbose` flag. Logs written to `headless_game.log`.

**Logged Events:**
- Turn/phase transitions
- Land plays and spell casts
- Combat declarations
- Damage and life changes
- Game outcomes

## Current Limitations

1. **Combat is AI-controlled**: Declaring attackers/blockers uses AI logic
   - TODO: Add `POST /attackers` and `POST /blockers` endpoints
2. **Fixed test decks**: Currently uses hardcoded test decks
3. **Single game**: No match/sideboard support yet
4. **No authentication**: API is open (intended for local use)

## Ongoing TODOs

1. Plug ForgeHeadless into the upcoming AI service (Java acts as HTTP client).
2. Define and document the external AI endpoint schema (request/response, auth, timeout).
3. Add manual combat control (attacker/blocker selection) via new endpoints.
4. Support configurable decks and match formats (deck import, best-of series).
5. Implement optional authentication/rate limiting for the HTTP surface.

## Files

| File | Description |
|------|-------------|
| `forge-gui-desktop/src/main/java/forge/view/ForgeHeadless.java` | Main implementation |
| `forge-headless` | Launch script |
| `test_http_endpoints.sh` | API test script |
| `headless_game.log` | Game event log (when verbose) |

## Development

### Testing
```bash
# Build
mvn clean install -DskipTests

# Run test script
./test_http_endpoints.sh
```

### Extending the API
1. Add new endpoint in `startHttpServer()` method
2. Add handler logic
3. Update this README

## License

Part of the Forge MTG project. See main project README for license information.

---
*Generated by Copilot*
