# AI Agent Response Format Specification

This document describes the expected JSON response formats for the Forge MTG AI Agent API.

---

## Standard Action Response

When the game asks for an action decision (playing cards, activating abilities, passing priority):

```json
{
  "decision": {
    "type": "action",
    "index": 0
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `decision.type` | string | Should be `"action"` for normal actions |
| `decision.index` | int | 0-based index into the `actionState.actions` array from the request |

---

## Pass Priority

Either use the index of the `pass_priority` action in the actions array, or explicitly:

```json
{
  "decision": {
    "type": "pass"
  }
}
```

Both `"pass"` and `"pass_priority"` are accepted as type values.

---

## Target Selection Response

When selecting a single target for spells/abilities:

```json
{
  "decision": {
    "type": "target",
    "index": 0
  }
}
```

For **multi-select** (e.g., choosing multiple targets):

```json
{
  "decision": {
    "type": "target",
    "indices": [0, 2, 3]
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `decision.index` | int | Single target selection (0-based index) |
| `decision.indices` | int[] | Multiple target selection (array of 0-based indices) |

---

## Declare Attackers Response

```json
{
  "decision": {
    "type": "declare_attackers",
    "attackers": [
      {"attacker_index": 0, "defender_index": 0},
      {"attacker_index": 2, "defender_index": 0}
    ]
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `attacker_index` | int | Index into `actionState.attackers` array |
| `defender_index` | int | Index into `actionState.defenders` array |

To attack with no creatures, return an empty array:
```json
{
  "decision": {
    "type": "declare_attackers",
    "attackers": []
  }
}
```

---

## Declare Blockers Response

```json
{
  "decision": {
    "type": "declare_blockers",
    "blocks": [
      {"blocker_index": 0, "attacker_index": 1},
      {"blocker_index": 2, "attacker_index": 0}
    ]
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `blocker_index` | int | Index into `actionState.blockers` array |
| `attacker_index` | int | Index into `actionState.attackers` array |

To block with no creatures, return an empty array:
```json
{
  "decision": {
    "type": "declare_blockers",
    "blocks": []
  }
}
```

---

## Key Points Summary

1. ✅ **Top-level field must be `"decision"`** (not `game_decision`)
2. ✅ **Use `"index"`** (not `action_index`)
3. ✅ **Use `"type"`** (not `action_type`)
4. ✅ Index values are **0-based** and reference arrays in `actionState` from the request
5. ✅ The last action in the actions list is typically `pass_priority`

---

## Common Mistakes

❌ **Wrong:**
```json
{
  "game_decision": {
    "action_type": "activate_ability",
    "action_index": 0
  }
}
```

✅ **Correct:**
```json
{
  "decision": {
    "type": "action",
    "index": 0
  }
}
```

---

## Full Example

### Request (from Forge):
```json
{
  "gameId": "abc-123",
  "requestType": "action",
  "actionState": {
    "actions": [
      {"type": "activate_ability", "card_name": "Plains", "card_id": 21},
      {"type": "activate_ability", "card_name": "Plains", "card_id": 17},
      {"type": "pass_priority"}
    ],
    "count": 3
  }
}
```

### Response (to play first Plains):
```json
{
  "decision": {
    "type": "action",
    "index": 0
  }
}
```

### Response (to pass priority):
```json
{
  "decision": {
    "type": "action",
    "index": 2
  }
}
```
or
```json
{
  "decision": {
    "type": "pass"
  }
}
```
