# Deck piloting guidance

The sidecar advises the AI on how to play *its own* deck. Per-archetype JSON
**piloting guides** carry mulligan rules, a game plan, win conditions, key
cards, matchup notes, and meta threats. The `game_advisor` node feeds the
relevant guide into the LLM prompt so the model's play recommendation is
grounded in archetype-specific strategy.

## Where guides live

```
app/knowledge/piloting/
├─ generic/                 hand-authored fallbacks, one per strategy
│  ├─ aggro.json
│  ├─ tempo.json
│  ├─ midrange.json
│  ├─ control.json
│  ├─ combo.json
│  └─ ramp.json
└─ <format>/                archetype-specific guides (LLM-generated)
   └─ <archetype-slug>.json  e.g. modern/boros-energy.json
```

## Schema

Every guide is validated against `PilotingGuide` in
`app/knowledge/piloting_schema.py` — both when the builder writes it and when
the loader reads it. An invalid file is logged and skipped (fail-soft).

| Field | Type | Meaning |
|---|---|---|
| `archetype` | string | Archetype (or generic strategy) name. |
| `format` | string | Format slug; empty for generic guides. |
| `strategy_type` | enum | `aggro` / `tempo` / `midrange` / `control` / `combo` / `ramp`. |
| `overview` | string | The game plan in 2–4 sentences. |
| `win_conditions` | string[] | How the deck actually wins. |
| `mulligan` | object | `keep_criteria`, `mulligan_criteria`, and worked `examples`. |
| `game_plan` | object | `early_game` / `mid_game` / `late_game` priority lists. |
| `key_cards` | object[] | `{name, role, notes}` for cards central to the plan. |
| `sequencing_tips` | string[] | Play-order advice. |
| `matchups` | object[] | `{opponent_archetype, advice, watch_for}` per matchup. |
| `common_threats` | string[] | Cards/effects from the meta to watch for. |
| `metadata` | object | `{source, generated_at, model, schema_version}`. |

## How a guide is chosen

The `game_advisor` node identifies the AI's own archetype once per game
(`identify_own_archetype` in `app/knowledge/piloting.py` — a deterministic
signature-card match, no LLM) and then resolves a guide via this fallback
chain:

```
piloting/<format>/<archetype-slug>.json     specific guide
        ↓ (missing or invalid)
piloting/generic/<strategy_type>.json        generic guide for the strategy
        ↓ (missing or invalid)
piloting/generic/midrange.json               last-resort fallback
```

Inspect the resolved guide with `GET /piloting?format=modern&archetype=...`.

## Generating guides — `scripts/build_piloting_guides.py`

Hand-authoring a guide per metagame archetype does not scale, so guides are
generated **offline** with an LLM. No site exposes structured piloting data;
the builder feeds an LLM the archetype's signature cards (and a representative
decklist scraped from MTGGoldfish, when reachable) and has it emit schema-valid
JSON. Output is committed and human-reviewable.

```sh
python scripts/build_piloting_guides.py                 # all formats
python scripts/build_piloting_guides.py modern          # one format
python scripts/build_piloting_guides.py modern --archetype "Boros Energy"
python scripts/build_piloting_guides.py modern --force  # regenerate existing
```

Existing files are skipped unless `--force` is passed. Generic fallback guides
are **hand-authored** and never touched by the script.

### Builder LLM

Guide generation runs offline and may use a stronger model than the runtime
local llama.cpp server. `app/knowledge/builder_llm.py` is configured by:

| Variable | Default | Meaning |
|---|---|---|
| `BUILDER_LLM_BASE_URL` | `LLM_BASE_URL` | OpenAI-compatible API base URL. |
| `BUILDER_LLM_API_KEY` | `LLM_API_KEY` | Bearer token. |
| `BUILDER_MODEL_NAME` | `MODEL_NAME` | Model name sent in the request. |
| `BUILDER_LLM_TIMEOUT` | `180` | Request timeout (seconds). |

The runtime sidecar never imports the builder — it stays fully offline.

## Why piloting shares the `game_advisor` node

Deck recognition and piloting advice are produced by **one LLM call** in the
`game_advisor` node, not two nodes. The AI re-runs the graph on every action; a
second call would add noticeable per-action lag. This intentionally trades away
the "one concern per node" convention in [EXTENDING.md](EXTENDING.md) — latency
is the priority for an interactive game.
