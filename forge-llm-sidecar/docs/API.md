# HTTP API reference

The sidecar listens on `http://127.0.0.1:18970` by default (configurable via
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
  "model": "local-model",
  "llm_reachable": true,
  "metagame_enabled": true
}
```

| Field | Meaning |
|---|---|
| `status` | Always `"ok"` when the service is up. |
| `model` | The model name the sidecar is configured to use. |
| `llm_reachable` | Whether the LLM server responded to a probe. |
| `metagame_enabled` | Whether scraped metagame data is being used. |

## `POST /recognize`

Run the game-advisor graph for one opponent: it identifies the opponent's
archetype **and** advises the AI on piloting its own deck — both in a single
LLM call.

**Request** — `RecognitionRequest`

```json
{
  "client": "forge",
  "game_id": "12345",
  "format": "Constructed",
  "opponent_seat": 1,
  "turn": 4,
  "observations": [
    {"turn": 1, "event": "land",  "card": "Steam Vents",          "cmc": 0, "colors": ["U","R"], "types": ["Land"]},
    {"turn": 2, "event": "spell", "card": "Monastery Swiftspear",  "cmc": 1, "colors": ["R"],     "types": ["Creature"]},
    {"turn": 3, "event": "spell", "card": "Lightning Bolt",        "cmc": 1, "colors": ["R"],     "types": ["Instant"]}
  ],
  "deck_cards": ["Ragavan, Nimble Pilferer", "Lightning Bolt", "Monastery Swiftspear"],
  "hand": ["Lightning Bolt", "Mountain"],
  "own_board": ["Ragavan, Nimble Pilferer", "Mountain", "Mountain"],
  "opponent_board": ["Monastery Swiftspear"],
  "your_graveyard": [],
  "opponent_graveyard": ["Lightning Bolt"],
  "life_totals": {"ai": 18, "opponent": 14}
}
```

| Field | Type | Notes |
|---|---|---|
| `client` | string | Identifies the calling client/adapter (e.g. `"forge"`). See [ADAPTERS.md](ADAPTERS.md). |
| `game_id` | string | Identifies the game; caches the detected format and the AI's own archetype. |
| `format` | string | The client's game type (e.g. `Constructed`, `Commander`). Often generic. |
| `opponent_seat` | int | The opponent's seat/id. Informational. |
| `turn` | int | Current game turn. `0` requests mulligan advice instead of a play. |
| `observations` | array | The opponent's public plays so far, chronological. |
| `deck_cards` | array of string | The AI's own decklist (card names). Detects the precise format and identifies the AI's own archetype. Optional. |
| `hand` | array of string | The AI's current hand. Optional — feeds piloting advice. |
| `own_board` / `opponent_board` | array of string | Battlefield card names. Optional. |
| `your_graveyard` / `opponent_graveyard` | array of string | Graveyard card names. Optional. |
| `life_totals` | object | Player name/seat → life total. Optional. |

The `hand` / board / graveyard / `life_totals` fields are optional: when
omitted the sidecar still returns archetype-level piloting advice from the
guide. See [ADAPTERS.md](ADAPTERS.md) on capturing them.

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
  "piloting": {
    "own_archetype": "Boros Energy",
    "guide_source": "modern/boros-energy",
    "recommended_play": "Cast Lightning Bolt on Monastery Swiftspear to keep the race ahead.",
    "reasoning": "Removing their only threat preserves our clock; we are the beatdown here.",
    "alternatives": ["Attack with Ragavan, then pass"],
    "mulligan_advice": ""
  },
  "schema_version": 2
}
```

| Field | Type | Notes |
|---|---|---|
| `archetype` | string | Best-guess opponent archetype, or `"Unknown"`. |
| `confidence` | number | `0.0`–`1.0`. Capped low for off-metagame guesses. |
| `reasoning` | string | One or two sentences explaining the guess. |
| `alternatives` | array of string | Up to three other plausible archetypes. |
| `piloting` | object | Advice on piloting the AI's own deck (see below). May be `null`. |
| `schema_version` | int | Response contract version (currently `2`). |

**`piloting` object** — `PilotingAdvice`

| Field | Type | Notes |
|---|---|---|
| `own_archetype` | string | The AI's own archetype, identified from `deck_cards`. |
| `guide_source` | string | Which guide answered: `<format>/<slug>` or `generic/<strategy>`. |
| `recommended_play` | string | The single best play this turn (empty on turn `0`). |
| `reasoning` | string | One or two sentences explaining the recommendation. |
| `alternatives` | array of string | Up to three other reasonable plays. |
| `mulligan_advice` | string | Keep/mulligan advice; populated instead of `recommended_play` on turn `0`. |

The endpoint never returns a 5xx for a model/data failure — it degrades to
`archetype: "Unknown", confidence: 0.0` and empty piloting advice.

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

## `GET /piloting`

Debug endpoint: shows the piloting guide resolved for an archetype.

**Query parameters**

| Param | Default | Notes |
|---|---|---|
| `format` | `modern` | A Forge format name or a metagame slug. |
| `archetype` | `""` | Archetype name. When empty, lists the available guides instead. |

**Response** (with `archetype`)

```json
{
  "requested": "modern",
  "meta_slug": "modern",
  "archetype": "Boros Energy",
  "guide": {
    "archetype": "Boros Energy",
    "strategy_type": "aggro",
    "overview": "...",
    "win_conditions": ["..."],
    "mulligan": {"keep_criteria": ["..."], "mulligan_criteria": ["..."], "examples": []},
    "game_plan": {"early_game": ["..."], "mid_game": ["..."], "late_game": ["..."]},
    "key_cards": [{"name": "...", "role": "...", "notes": "..."}],
    "sequencing_tips": ["..."],
    "matchups": [{"opponent_archetype": "...", "advice": "...", "watch_for": ["..."]}],
    "common_threats": ["..."],
    "metadata": {"source": "...", "generated_at": "...", "model": "...", "schema_version": 1}
  }
}
```

When the archetype has no specific guide, the loader falls back to a generic
guide (`generic/<strategy>` → `generic/midrange`). See
[PILOTING.md](PILOTING.md).
