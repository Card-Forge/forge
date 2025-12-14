# Forge AI Agent API Specification

This document defines the HTTP JSON interface between the Forge Game Engine and an external AI Agent.

## Overview

When a player slot is configured as an "HTTP Agent" (or when running in Headless AI mode), Forge sends HTTP POST requests to the configured endpoint for every game decision. The engine waits for a JSON response before proceeding.

**Endpoint:** User-configurable (e.g., `http://localhost:5005`)
**Method:** `POST`
**Content-Type:** `application/json`

---

## 1. Request Structure

The request payload provides the complete game context and the specific decision required.

```json
{
  "gameId": "string (UUID)",
  "requestType": "string",
  "context": { ... },
  "gameState": { ... },
  "actionState": { ... }
}
```

### Top-Level Fields

| Field | Type | Description |
|-------|------|-------------|
| `gameId` | String | Unique UUID for the current game session. |
| `requestType` | String | The type of decision needed. Values: `possible_actions`, `declare_attackers`, `declare_blockers`, `target`. |
| `context` | Object | Metadata about the request (phase, turn, player name). |
| `gameState` | Object | Snapshot of the board, hands, stack, and players. |
| `actionState` | Object | The specific options available for this decision. |

---

## 2. Game State Object (`gameState`)

Included in every request. Contains all public information.

```json
{
  "turn": 1,
  "phase": "MAIN1",
  "activePlayerId": 100,
  "priorityPlayerId": 100,
  "stack_size": 0,
  "stack": [
    {
      "card_name": "Lightning Bolt",
      "card_id": 55,
      "description": "Lightning Bolt deals 3 damage...",
      "controller": "Opponent"
    }
  ],
  "players": [
    {
      "id": 100,
      "name": "AI Agent",
      "life": 20,
      "libraryCount": 50,
      "hand": ["Mountain", "Shock"],
      "graveyard": [],
      "battlefield": [
        { "name": "Forest", "id": 201, "zone": "Battlefield" }
      ],
      "exile": []
    }
  ],
  "card_definitions": {
    "Lightning Bolt": {
      "name": "Lightning Bolt",
      "mana_cost": "{R}",
      "type": "Instant",
      "oracle_text": "Lightning Bolt deals 3 damage to any target."
    },
    "Goblin Guide": {
      "name": "Goblin Guide",
      "mana_cost": "{R}",
      "type": "Creature — Goblin Scout",
      "oracle_text": "Haste\nWhenever Goblin Guide attacks, defending player reveals the top card of their library. If it's a land card, that player puts it into their hand.",
      "power": "2",
      "toughness": "2"
    }
  },
  "combat": {
    "attackers": [],
    "blockers": []
  }
}
```

*Note: The `hand` array for the AI player contains card names. Opponent hands will be masked (e.g., containing "Unknown") or empty depending on visibility rules.*

---

## 3. Request Types & Action States

### A. `possible_actions` (Main Gameplay)
Used during Main phases, Combat steps, etc., when the player has priority.

**`actionState` Structure:**
```json
{
  "possible_actions": [
    {
      "index": 0,
      "type": "play_land",
      "card": "Mountain",
      "card_id": 201
    },
    {
      "index": 1,
      "type": "cast_spell",
      "card": "Shock",
      "card_id": 202,
      "mana_cost": "{R}",
      "requires_targets": true
    },
    {
      "index": 2,
      "type": "pass_priority"
    }
  ]
}
```

**Expected Response:**
```json
{
  "decision": {
    "type": "action",
    "index": 1
  }
}
```
*To pass priority, select the index corresponding to the `pass_priority` action.*

### B. `declare_attackers`
Used during the Declare Attackers step.

**`actionState` Structure:**
```json
{
  "attackers": [
    { "index": 0, "id": 301, "name": "Goblin Guide", "power": 2, "toughness": 2 }
  ],
  "defenders": [
    { "index": 0, "id": 900, "name": "Opponent", "type": "Player" }
  ]
}
```

**Expected Response:**
```json
{
  "decision": {
    "type": "declare_attackers",
    "attackers": [
      { "attacker_index": 0, "defender_index": 0 }
    ]
  }
}
```
*Send an empty `attackers` list to declare no attacks.*

### C. `declare_blockers`
Used during the Declare Blockers step.

**`actionState` Structure:**
```json
{
  "attackers": [
    { "index": 0, "id": 301, "name": "Goblin Guide", "attacking": "AI Agent" }
  ],
  "blockers": [
    { "index": 0, "id": 401, "name": "Wall of Omens", "power": 0, "toughness": 4 }
  ]
}
```

**Expected Response:**
```json
{
  "decision": {
    "type": "declare_blockers",
    "blocks": [
      { "blocker_index": 0, "attacker_index": 0 }
    ]
  }
}
```

### D. `target`
Used when a spell or ability requires target selection.

**`actionState` Structure:**
```json
{
  "title": "Select targets for Shock",
  "min": 1,
  "max": 1,
  "targets": [
    { "index": 0, "type": "Player", "name": "Opponent", "id": 900, "life": 20 },
    { "index": 1, "type": "Creature", "name": "Goblin Guide", "id": 301 }
  ]
}
```

**Expected Response:**
```json
{
  "decision": {
    "type": "target",
    "index": 1
  }
}
```
*For multi-target selections, use `"indices": [0, 1]` instead of `"index"`.*

---

## 4. Response Codes

- **200 OK**: Decision accepted.
- **400 Bad Request**: Invalid JSON or invalid index.
- **500 Internal Error**: Logic error in AI agent.

If the AI agent returns a 404 or fails to connect, Forge will pause and retry every 5 seconds.
