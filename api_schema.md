`# ForgeHeadless AI Agent API Schema

This document defines the JSON API contract between `ForgeHeadless` (the client) and your external AI Agent (the server).

## Overview

-   **Protocol**: HTTP/1.1
-   **Method**: POST
-   **Content-Type**: application/json

## Request Format

`ForgeHeadless` sends a POST request to your configured endpoint with the following body:

```json
{
  "gameId": "string",         // Unique identifier for the current game
  "requestType": "string",    // "action" or "target"
  "gameState": {              // Current state of the game
    "turn": "integer",
    "phase": "string",        // e.g., "MAIN1", "COMBAT_ATTACKERS"
    "activePlayerId": "integer",
    "priorityPlayerId": "integer",
    "stack": [                // Objects on the stack
      {
        "card_name": "string",
        "card_id": "integer",
        "description": "string",
        "controller": "string"
      }
    ],
    "players": [              // List of players
      {
        "id": "integer",
        "name": "string",
        "life": "integer",
        "libraryCount": "integer",
        "hand": [             // Cards in hand (only visible for the AI player)
          {
            "name": "string",
            "id": "integer",
            "zone": "Hand"
          }
        ],
        "graveyard": [],
        "battlefield": [],
        "exile": []
      }
    ]
  },
  "actionState": {            // Options available to the AI
    "actions": [              // List of available actions (for requestType="action")
      {
        "type": "string",     // "play_land", "cast_spell", "activate_ability", "pass_priority"
        "card_id": "integer", // Optional
        "card_name": "string",// Optional
        "ability_description": "string", // Optional
        "mana_cost": "string",// Optional
        "requires_targets": "boolean" // Optional
      }
    ],
    "targets": [              // List of valid targets (for requestType="target")
      {
        "index": "integer",
        "type": "string",     // e.g., "Player", "Card"
        "name": "string",
        "id": "integer"
      }
    ],
    "min": "integer",         // Min targets required (for requestType="target")
    "max": "integer"          // Max targets allowed (for requestType="target")
  },
  "context": {                // Additional context
    "requestType": "string",
    "phase": "string",
    "turn": "integer",
    "playerName": "string"
  }
}
```

## Response Format

Your AI Agent must return a JSON response with a `decision` object.

### 1. Select an Action or Target

To select an action or a single target, return the `index` corresponding to the option in the `actionState` list.

```json
{
  "decision": {
    "type": "action",  // or "target"
    "index": 0         // Integer index of the selected option
  }
}
```

### 2. Pass Priority (Explicit)

To explicitly pass priority (equivalent to selecting the "pass_priority" action), you can use the `pass` type.

```json
{
  "decision": {
    "type": "pass"
  }
}
```

> **Note**: You can also simply select the index of the "pass_priority" action using the standard "action" type response. `ForgeHeadless` will automatically interpret it as a pass.

### 3. Select Multiple Targets

For targeting requests that allow multiple targets:

```json
{
  "decision": {
    "type": "target",
    "indices": [0, 1]  // List of indices
  }
}
```

## Optimizations

-   **Auto-Pass**: If the only available action is "pass_priority", `ForgeHeadless` will automatically take it without calling your agent.
-   **Translation**: If you return an `index` that points to a "pass_priority" action, `ForgeHeadless` handles it correctly as a pass.

## Combat Requests

### Declare Attackers
**Request Type**: `declare_attackers`

**Action State**:
```json
{
  "attackers": [
    { "index": 0, "id": 123, "name": "Goblin Guide", "power": 2, "toughness": 2 }
  ],
  "defenders": [
    { "index": 0, "id": 456, "name": "Player 2", "type": "Player" }
  ]
}
```

**Response**:
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

### Declare Blockers
**Request Type**: `declare_blockers`

**Action State**:
```json
{
  "attackers": [
    { "index": 0, "id": 123, "name": "Goblin Guide", "attacking": "Player 1" }
  ],
  "blockers": [
    { "index": 0, "id": 789, "name": "Wall of Omens", "power": 0, "toughness": 4 }
  ]
}
```

**Response**:
```json
{
  "decision": {
    "type": "declare_blockers",
    "blocks": [
      { "blocker_index": 0, "attacker_index": 0 },
      { "blocker_index": 1, "attacker_index": 0 } 
    ]
  }
}
```
*Note: To assign multiple blockers to a single attacker, simply include multiple entries with the same `attacker_index` and different `blocker_index`s.*
