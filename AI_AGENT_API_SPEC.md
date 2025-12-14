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
    "attacker_count": 2,
    "attackers": [
      {
        "card_id": 301,
        "card_name": "Goblin Guide",
        "power": 2,
        "toughness": 2,
        "controller": "AI Agent",
        "attacking_id": 900,
        "attacking_name": "Opponent",
        "attacking_type": "player",
        "blockers": []
      },
      {
        "card_id": 302,
        "card_name": "Monastery Swiftspear",
        "power": 2,
        "toughness": 3,
        "controller": "AI Agent",
        "attacking_id": 900,
        "attacking_name": "Opponent",
        "attacking_type": "player",
        "blockers": [
          {
            "card_id": 401,
            "card_name": "Wall of Omens",
            "power": 0,
            "toughness": 4,
            "controller": "Opponent"
          }
        ]
      }
    ]
  }
}
```

### Combat State Fields

The `combat` object is **only present during combat phases** when there are attackers declared.

| Field | Type | Description |
|-------|------|-------------|
| `combat.attacker_count` | Integer | Number of attacking creatures |
| `combat.attackers` | Array | List of attacking creatures |
| `combat.attackers[].card_id` | Integer | Unique ID of the attacking creature |
| `combat.attackers[].card_name` | String | Name of the attacking creature |
| `combat.attackers[].power` | Integer | Current power (after modifications) |
| `combat.attackers[].toughness` | Integer | Current toughness (after modifications) |
| `combat.attackers[].controller` | String | Name of the creature's controller |
| `combat.attackers[].attacking_id` | Integer | ID of the entity being attacked |
| `combat.attackers[].attacking_name` | String | Name of the entity being attacked |
| `combat.attackers[].attacking_type` | String | `"player"` or `"planeswalker"` |
| `combat.attackers[].blockers` | Array | List of creatures blocking this attacker |
| `combat.attackers[].blockers[].card_id` | Integer | Unique ID of the blocking creature |
| `combat.attackers[].blockers[].card_name` | String | Name of the blocking creature |
| `combat.attackers[].blockers[].power` | Integer | Current power of the blocker |
| `combat.attackers[].blockers[].toughness` | Integer | Current toughness of the blocker |
| `combat.attackers[].blockers[].controller` | String | Name of the blocker's controller |

*Note: The `hand` array for the AI player contains card names. Opponent hands will be masked (e.g., containing "Unknown") or empty depending on visibility rules.*

---

## 3. Request Types & Action States

### A. `possible_actions` (Main Gameplay)
Used during Main phases, Combat steps, etc., when the player has priority.

**`actionState` Structure:**
```json
{
  "actions": [
    {
      "type": "play_land",
      "card_name": "Mountain",
      "card_id": 201,
      "card_zone": "Hand"
    },
    {
      "type": "cast_spell",
      "card_name": "Shock",
      "card_id": 202,
      "card_zone": "Hand",
      "mana_cost": "{R}",
      "ability_description": "Shock deals 2 damage to any target.",
      "requires_targets": true,
      "target_min": 1,
      "target_max": 1,
      "target_zone": "any"
    },
    {
      "type": "activate_ability",
      "card_name": "Goblin Guide",
      "card_id": 301,
      "card_zone": "Battlefield",
      "ability_description": "{T}: Deal 1 damage to any target.",
      "mana_cost": "no cost",
      "requires_targets": true
    },
    {
      "type": "pass_priority"
    }
  ],
  "count": 4
}
```

### Action Fields

| Field | Type | Description |
|-------|------|-------------|
| `type` | String | Action type: `play_land`, `cast_spell`, `activate_ability`, or `pass_priority` |
| `card_name` | String | Name of the card (not present for `pass_priority`) |
| `card_id` | Integer | Unique ID of the card |
| `card_zone` | String | Zone where the card is located: `Hand`, `Battlefield`, `Graveyard`, etc. |
| `mana_cost` | String | Mana cost to perform this action (e.g., `{2}{R}`) |
| `ability_description` | String | Text description of the ability |
| `requires_targets` | Boolean | Whether this action requires target selection |
| `target_min` | Integer | Minimum number of targets (if targeting) |
| `target_max` | Integer | Maximum number of targets (if targeting) |
| `target_zone` | String | Zone(s) where targets can be selected from |

**Note:** Mana abilities (tapping lands for mana) are NOT included in the action list. The game engine handles mana payment automatically when you cast spells.

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
