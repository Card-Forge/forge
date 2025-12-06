# ForgeHeadlessServer API Reference

A comprehensive guide for interacting with the ForgeHeadlessServer HTTP API for Magic: The Gathering game automation.

## Quick Start

```bash
# Start the server
java -cp forge-gui-desktop/target/forge-gui-desktop-2.0.08-SNAPSHOT-jar-with-dependencies.jar forge.view.ForgeHeadlessServer

# The server runs on port 8080
```

---

## API Endpoints

### `POST /api/reset`

Starts a new game. Optionally loads a specific scenario or puzzle.

**Request Body (JSON):**

```json
{}
```

**Response:** Full game state JSON (see Game State Format below)

#### Options

| Option           | Type       | Description                                         |
| ---------------- | ---------- | --------------------------------------------------- |
| `forced_hand`    | `string[]` | Card names to place in player's starting hand       |
| `forced_library` | `string[]` | Card names to stack on top of library (first = top) |
| `puzzle_file`    | `string`   | Path to a `.pzl` puzzle file for mid-game state     |

**Example - Force specific hand:**

```json
{
  "forced_hand": ["Lightning Bolt", "Mountain", "Goblin Guide"],
  "forced_library": ["Lava Spike", "Rift Bolt"]
}
```

**Example - Load puzzle (mid-game state):**

```json
{
  "puzzle_file": "res/puzzle/INQ01.pzl"
}
```

---

### `POST /api/step`

Execute an action in the game.

**Request Body:** Raw string (not JSON)

```
play_action <index>
```

Where `<index>` is the 0-based index from `possible_actions.actions[]`.

**Example:**

```bash
curl -X POST http://localhost:8080/api/step -d "play_action 0"
```

**Response:** Updated game state JSON

---

### `GET /api/state`

Get current game state without performing any action.

**Response:** Current game state JSON

---

## Game State Format

```json
{
  "turn": 1,
  "phase": "MAIN1",
  "game_over": false,
  "possible_actions": {
    "actions": [
      {
        "type": "play_land",
        "card_id": 123,
        "card_name": "Mountain"
      },
      {
        "type": "cast_spell",
        "card_id": 456,
        "card_name": "Lightning Bolt",
        "mana_cost": "{R}",
        "requires_targets": true
      },
      {
        "type": "pass_priority"
      }
    ],
    "count": 3
  },
  "hand": [
    {
      "id": 123,
      "name": "Mountain",
      "type": "Basic Land - Mountain"
    }
  ],
  "library_count": 53,
  "battlefield": {
    "player1_creatures": [],
    "player2_creatures": []
  }
}
```

### Action Types

| Type               | Description                           |
| ------------------ | ------------------------------------- |
| `play_land`        | Play a land from hand (once per turn) |
| `cast_spell`       | Cast a spell (requires mana payment)  |
| `activate_ability` | Activate an ability on a permanent    |
| `pass_priority`    | Pass priority to opponent/next phase  |

### Phases

```
UNTAP → UPKEEP → DRAW → MAIN1 → COMBAT_BEGIN →
COMBAT_DECLARE_ATTACKERS → COMBAT_DECLARE_BLOCKERS →
COMBAT_DAMAGE → COMBAT_END → MAIN2 → END → CLEANUP
```

---

## Puzzle Files (.pzl)

Forge includes 217 built-in puzzle files in `res/puzzle/`. These define complete mid-game states:

```
res/puzzle/INQ01.pzl    # Inquest Gamer puzzle
res/puzzle/MTGP_01.pzl  # Official MTG puzzle
res/puzzle/PC_*.pzl     # PureMTGO puzzles
res/puzzle/PS_*.pzl     # Various puzzles
```

### Puzzle File Format

```ini
[metadata]
Name:Puzzle Name
Goal:Win
Turns:1
Difficulty:Hard
Description:Win this turn.

[state]
humanlife=20
ailife=5
activeplayer=human
activephase=MAIN1
humanhand=Lightning Bolt;Mountain
humanbattlefield=Goblin Guide|Tapped
aibattlefield=Wall of Omens
```

---

## Example: Basic Game Loop

```python
import urllib.request
import json

BASE_URL = "http://localhost:8080"

def reset_game():
    req = urllib.request.Request(f"{BASE_URL}/api/reset", method="POST")
    req.data = b"{}"
    req.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(req) as resp:
        return json.load(resp)

def step(action_index):
    req = urllib.request.Request(f"{BASE_URL}/api/step", method="POST")
    req.data = f"play_action {action_index}".encode()
    with urllib.request.urlopen(req) as resp:
        return json.load(resp)

# Start game
state = reset_game()

# Game loop
while not state.get("game_over"):
    actions = state["possible_actions"]["actions"]

    # Find a land to play, otherwise pass
    action_idx = next(
        (i for i, a in enumerate(actions) if a["type"] == "play_land"),
        next(i for i, a in enumerate(actions) if a["type"] == "pass_priority")
    )

    state = step(action_idx)
```

---

## Example: Load Puzzle and Solve

```python
def load_puzzle(puzzle_path):
    req = urllib.request.Request(f"{BASE_URL}/api/reset", method="POST")
    req.data = json.dumps({"puzzle_file": puzzle_path}).encode()
    req.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(req) as resp:
        return json.load(resp)

# Load the Inquest Gamer puzzle #1
state = load_puzzle("res/puzzle/INQ01.pzl")

# The puzzle state is now loaded with:
# - Specific life totals
# - Cards on battlefield
# - Cards in hand
# - Specific phase/turn
print(f"Your life: check state for player life")
print(f"Actions available: {state['possible_actions']['count']}")
```

---

## Notes for LLM Agents

1. **Action Selection**: Always use the index from `possible_actions.actions[]`, not card IDs
2. **Targeting**: Some spells require targets - check `requires_targets` field
3. **Priority**: The game pauses at each priority point waiting for `play_action`
4. **Pass to Advance**: Use `pass_priority` action to move through phases
5. **Mana Payment**: Mana is auto-paid from available lands when casting

---

## Server Startup

The server requires being run from the project root directory:

```bash
cd /path/to/forge
java -cp forge-gui-desktop/target/forge-gui-desktop-2.0.08-SNAPSHOT-jar-with-dependencies.jar forge.view.ForgeHeadlessServer
```

The `res/` symlink must exist pointing to `forge-gui/res/` for card data to load.
