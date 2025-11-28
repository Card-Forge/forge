# ForgeHeadless Documentation

## Overview
ForgeHeadless is a headless Magic: The Gathering game engine that supports two modes of operation:

1. **HTTP Server Mode (v0)**: Java hosts an HTTP API and external processes poll for prompts and push actions.
2. **AI Agent Mode (v1)**: Java calls out to YOUR external AI endpoint for every decision.

Use this document as the canonical reference for both architectures.

## Architecture

### HTTP Server Mode (v0 - Default)

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

External automations poll `GET /input` and push `POST /action`/`POST /target`.

### AI Agent Mode (v1 - NEW!)

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

In AI Agent mode, Java sends serialized game state + possible actions to your hosted AI endpoint, waits for the response, and executes it locally. **Your AI is in control.**

## Quick Start

### Building
```bash
mvn clean install -DskipTests
```

### Running in HTTP Server Mode (v0)
```bash
./forge-headless
```
Server starts on **port 8081**.

### Running in AI Agent Mode (v1)
```bash
./forge-headless --ai-endpoint http://your-ai-service:5000/decide --game-id my-game-123
```

When `--ai-endpoint` is provided, the game will call out to your AI service for all decisions instead of waiting for HTTP input.

### Testing the API (HTTP Server Mode)
```bash
# Get game state
curl http://localhost:8081/state

# Get available actions
curl http://localhost:8081/input

# Take an action (pass priority)
curl -X POST -d '{"index": 0}' http://localhost:8081/action
```

## AI Agent Mode

### Command Line Options

| Option | Description |
|--------|-------------|
| `--ai-endpoint <url>` | URL of your AI agent endpoint |
| `--game-id <id>` | Unique game ID for tracking (auto-generated if not provided) |

### Request Format (sent to your AI)

For **action decisions** (choosing spells, abilities, passing priority):
```json
{
  "gameId": "my-game-123",
  "requestType": "action",
  "gameState": {
    "turn": 1,
    "phase": "MAIN1",
    "activePlayerId": 0,
    "priorityPlayerId": 0,
    "stack": [],
    "players": [...]
  },
  "actionState": {
    "actions": [
      {"type": "play_land", "card_id": 7, "card_name": "Mountain"},
      {"type": "cast_spell", "card_id": 34, "card_name": "Shock", "requires_targets": true, ...},
      {"type": "pass_priority"}
    ],
    "count": 3
  },
  "context": {
    "requestType": "action",
    "phase": "MAIN1",
    "turn": 1,
    "playerName": "Player 1"
  }
}
```

For **target selection** (choosing targets for spells/abilities):
```json
{
  "gameId": "my-game-123",
  "requestType": "target",
  "gameState": { ... },
  "actionState": {
    "min": 1,
    "max": 1,
    "title": "Select targets for Shock",
    "targets": [
      {"index": 0, "type": "Player", "name": "Player 1", "id": 0, "life": 20},
      {"index": 1, "type": "Player", "name": "AI Player 2", "id": 1, "life": 20}
    ]
  },
  "context": {
    "requestType": "target",
    "spellName": "Shock",
    "spellDescription": "Shock deals 2 damage to any target."
  }
}
```

### Response Format (expected from your AI)

For **action decisions**:
```json
{
  "decision": {
    "type": "action",
    "index": 0
  }
}
```

To **pass priority**:
```json
{
  "decision": {
    "type": "pass"
  }
}
```

For **target selection** (single target):
```json
{
  "decision": {
    "type": "target",
    "index": 1
  }
}
```

For **target selection** (multiple targets):
```json
{
  "decision": {
    "type": "target",
    "indices": [0, 2]
  }
}
```

### Example AI Endpoint (Python Flask)

```python
from flask import Flask, request, jsonify

app = Flask(__name__)

@app.route('/decide', methods=['POST'])
def decide():
    data = request.json
    game_id = data.get('gameId')
    request_type = data.get('requestType')
    game_state = data.get('gameState')
    action_state = data.get('actionState')
    context = data.get('context')
    
    if request_type == 'action':
        # Your AI logic here
        # For now, always pass priority
        return jsonify({
            "decision": {
                "type": "pass"
            }
        })
    
    elif request_type == 'target':
        # Your AI logic here
        # For now, select first target
        return jsonify({
            "decision": {
                "type": "target",
                "index": 0
            }
        })
    
    return jsonify({"error": "Unknown request type"}), 400

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
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

### Player Options
| Option | Description |
|--------|-------------|
| (default) | Player 1 = Human (HTTP-controlled or AI-agent-controlled), Player 2 = AI |
| `--both-ai` | Both players AI-controlled (simulation mode, uses built-in AI) |
| `--both-human` | Both players HTTP/AI-agent-controlled |
| `--p1-ai` | Player 1 = built-in AI, Player 2 = HTTP/AI-agent-controlled |
| `--p2-human` | Player 2 = HTTP/AI-agent-controlled |

### AI Agent Options
| Option | Description |
|--------|-------------|
| `--ai-endpoint <url>` | URL of your external AI agent endpoint for decision-making |
| `--game-id <id>` | Unique game ID for tracking (auto-generated UUID if not provided) |

### Other Options
| Option | Description |
|--------|-------------|
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

### AI Agent Mode Integration (DONE ✓)

The following features have been implemented:

- [x] Add configuration for outbound AI endpoint (URL, auth, timeout)
- [x] Serialize full decision context (game state + action options) in a single request body
- [x] Fall back to embedded HTTP server when endpoint is unavailable (dev mode)

**Still TODO:**
- [ ] Handle streaming/async responses from the AI service
- [ ] Add authentication token support via command-line

> **Note**: Your custom AI endpoint is owned by you. See the "AI Agent Mode" section above for the complete request/response schema.

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

1. ~~Plug ForgeHeadless into the upcoming AI service (Java acts as HTTP client).~~ ✓ DONE
2. ~~Define and document the external AI endpoint schema (request/response, auth, timeout).~~ ✓ DONE
3. Add manual combat control (attacker/blocker selection) via new endpoints or AI agent calls.
4. Support configurable decks and match formats (deck import, best-of series).
5. Implement optional authentication/rate limiting for the HTTP surface.
6. Add authentication token support for AI agent endpoint.

## Files

| File | Description |
|------|-------------|
| `forge-gui-desktop/src/main/java/forge/view/ForgeHeadless.java` | Main implementation |
| `forge-gui-desktop/src/main/java/forge/view/AIAgentClient.java` | AI Agent HTTP client |
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
