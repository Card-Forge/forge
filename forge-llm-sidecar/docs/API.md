# HTTP API reference

The sidecar listens on `http://127.0.0.1:8000` by default (configurable via
`PORT`). All payloads are JSON. The authoritative models live in
`app/schema.py`.

## `GET /health`

Lightweight availability check. Forge calls this once when attaching the
feature; if it does not return `200`, the feature is silently disabled for the
game.

**Response**

```json
{
  "status": "ok",
  "model": "llama3.1:8b",
  "ollama_reachable": true,
  "metagame_enabled": true
}
```

| Field | Meaning |
|---|---|
| `status` | Always `"ok"` when the service is up. |
| `model` | The Ollama model the sidecar is configured to use. |
| `ollama_reachable` | Whether the Ollama server responded to a probe. |
| `metagame_enabled` | Whether scraped metagame data is being used. |

## `POST /recognize`

Run the deck-recognition graph for one opponent.

**Request** — `RecognitionRequest`

```json
{
  "game_id": "12345",
  "format": "Constructed",
  "opponent_seat": 1,
  "turn": 4,
  "observations": [
    {"turn": 1, "event": "land",  "card": "Steam Vents",          "cmc": 0, "colors": ["U","R"], "types": ["Land"]},
    {"turn": 2, "event": "spell", "card": "Monastery Swiftspear",  "cmc": 1, "colors": ["R"],     "types": ["Creature"]},
    {"turn": 3, "event": "spell", "card": "Lightning Bolt",        "cmc": 1, "colors": ["R"],     "types": ["Instant"]}
  ],
  "deck_cards": ["Ragavan, Nimble Pilferer", "Lightning Bolt", "Monastery Swiftspear"]
}
```

| Field | Type | Notes |
|---|---|---|
| `game_id` | string | Identifies the game; used to cache the detected format. |
| `format` | string | Forge's game type (e.g. `Constructed`, `Commander`). Often generic. |
| `opponent_seat` | int | The opponent's seat/id. Informational. |
| `turn` | int | Current game turn. |
| `observations` | array | The opponent's public plays so far, chronological. |
| `deck_cards` | array of string | The **AI's own** decklist (card names). Used to detect the precise format. Optional. |

**`Observation` object**

| Field | Type | Notes |
|---|---|---|
| `turn` | int | Turn the play happened. |
| `event` | string | `"spell"` or `"land"`. |
| `card` | string | Card name. |
| `cmc` | int | Mana value. |
| `colors` | array of string | Single-letter color codes (`W U B R G`). |
| `types` | array of string | Card types (`Creature`, `Instant`, …). |

**Response** — `RecognitionResponse`

```json
{
  "archetype": "Izzet Prowess",
  "confidence": 0.72,
  "reasoning": "Low-curve red spells plus a prowess one-drop and a UR dual land.",
  "alternatives": ["Mono-Red Aggro", "Izzet Murktide"],
  "schema_version": 1
}
```

| Field | Type | Notes |
|---|---|---|
| `archetype` | string | Best-guess archetype, or `"Unknown"`. |
| `confidence` | number | `0.0`–`1.0`. Capped low for off-metagame guesses. |
| `reasoning` | string | One or two sentences explaining the guess. |
| `alternatives` | array of string | Up to three other plausible archetypes. |
| `schema_version` | int | Response contract version (currently `1`). |

The endpoint never returns a 5xx for a model/data failure — it degrades to
`archetype: "Unknown", confidence: 0.0`.

## `GET /metagame`

Debug endpoint: shows the metagame data currently loaded for a format.

**Query parameters**

| Param | Default | Notes |
|---|---|---|
| `format` | `modern` | A Forge format name or a metagame slug. |

**Response**

```json
{
  "requested": "modern",
  "meta_slug": "modern",
  "enabled": true,
  "info": {"format": "modern", "source": "mtggoldfish", "updated_at": "...", "archetype_count": 60},
  "count": 60,
  "archetypes": [
    {"name": "Boros Energy", "meta_share": 18.2, "colors": ["W","R"], "signature_cards": ["..."]}
  ]
}
```
