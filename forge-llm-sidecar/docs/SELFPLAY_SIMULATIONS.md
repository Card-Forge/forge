# Self-Play Simulation Guide

This guide covers the headless simulation harness used to evaluate the LLM
sidecar:

- **Goldfish**: one sidecar-piloted deck tries to win as quickly as possible
  against a passive deck such as `60-islands.dck`.
- **Sidecar vs sidecar**: both seats are sidecar-piloted in a normal matchup.
- **Gauntlet**: a chosen set of opponent decks you run the pilot deck against.

Commands below assume you start from the repo root:

```sh
cd /home/lou/Development/forge-1
```

## 1. Prerequisites

Build the Forge desktop jar if it is not already present:

```sh
mvn -DskipTests install
```

Start the sidecar in another terminal:

```sh
cd forge-llm-sidecar
python -m venv .venv
source .venv/bin/activate
pip install -e .
uvicorn app.main:app --port 18970
```

Check it:

```sh
curl http://localhost:18970/health
```

`./run-forge.sh` is the easiest way to launch the runner because it finds the
built jar and adds the Java 17 module flags Forge needs. One important detail:
`run-forge.sh` changes the working directory to `forge-gui/` before launching
Java, so deck paths in `-p1`, `-p2`, and `-out` should usually be written with
`../` when you are referring to files outside `forge-gui/`.

## 2. Import MTGGoldfish Decks

The importer downloads representative MTGGoldfish archetype decks and converts
them into Forge `.dck` files for self-play.

Run it from the sidecar directory:

```sh
cd forge-llm-sidecar
python scripts/import_goldfish_decks.py modern
```

Generated decks are written here by default:

```text
forge-llm-sidecar/selfplay/decks/<format>/<archetype-slug>.dck
```

Examples:

```sh
# Import the top 8 Modern archetype decks.
python scripts/import_goldfish_decks.py modern --limit 8

# Import only archetypes whose name contains "Boros Energy".
python scripts/import_goldfish_decks.py modern --archetype "Boros Energy"

# Overwrite existing downloaded decks.
python scripts/import_goldfish_decks.py modern --force

# Write to a different deck root.
python scripts/import_goldfish_decks.py pioneer --out selfplay/decks
```

Useful importer flags:

| Flag | Meaning |
|---|---|
| `format` | MTGGoldfish format slug, for example `standard`, `pioneer`, `modern`, `legacy`, or `vintage`. |
| `--archetype` | Imports only archetypes whose MTGGoldfish name contains this substring. |
| `--limit` | Imports only the first N archetypes from the metagame page. |
| `--out` | Output root. Default is `forge-llm-sidecar/selfplay/decks`. |
| `--force` | Replaces existing `.dck` files. |
| `--sleep` | Delay between MTGGoldfish requests. Default is `2.0` seconds. |

## 3. Pick Decks to Evaluate

The runner evaluates exactly one `-p1` deck against exactly one `-p2` deck per
command.

- `-p1` is the deck being evaluated.
- `-p2` is the opponent deck.
- In `goldfish`, only `-p1` is sidecar-piloted; `-p2` is passive and sidecar
  recognition is disabled for that seat.
- In `mirror`, both `-p1` and `-p2` are sidecar-piloted.

The built-in passive goldfish opponent is:

```text
forge-llm-sidecar/selfplay/60-islands.dck
```

Imported MTGGoldfish decks are under:

```text
forge-llm-sidecar/selfplay/decks/<format>/
```

Example pilot deck:

```text
forge-llm-sidecar/selfplay/decks/modern/boros-energy.dck
```

Because `run-forge.sh` runs from `forge-gui/`, pass that path as:

```text
../forge-llm-sidecar/selfplay/decks/modern/boros-energy.dck
```

## 4. Run Goldfish Simulations

Goldfish mode uses `-config goldfish`. The sidecar pilots `-p1` in `solve`
mode against the passive `-p2` deck.

```sh
./run-forge.sh selfplay \
  -config goldfish \
  -p1 ../forge-llm-sidecar/selfplay/decks/modern/boros-energy.dck \
  -p2 ../forge-llm-sidecar/selfplay/60-islands.dck \
  -n 50 \
  -out ../forge-llm-sidecar/selfplay/runs/boros-energy-goldfish.jsonl \
  -format modern \
  -label baseline \
  -url http://localhost:18970
```

Important flags:

| Flag | Meaning |
|---|---|
| `-config goldfish` | P1 sidecar in solve mode vs passive P2. |
| `-p1` | Deck being evaluated. |
| `-p2` | Passive opponent deck, usually `60-islands.dck`. |
| `-n` | Number of games. |
| `-out` | Raw JSONL output file. |
| `-format` | Metadata tag stored with the run, for example `modern`. |
| `-label baseline` | Pins this run as the baseline for trend comparison. |
| `-record false` | Optional: skip automatic recording to SQLite. JSONL is still written. |
| `-c` | Optional per-game timeout in seconds. |

## 5. Run Sidecar vs Sidecar Simulations

Sidecar-vs-sidecar mode uses `-config mirror`. Both seats are sidecar-piloted
in normal mode, and the JSONL contains two records per game: one from each
seat's perspective.

Single sidecar instance:

```sh
./run-forge.sh selfplay \
  -config mirror \
  -p1 ../forge-llm-sidecar/selfplay/decks/modern/boros-energy.dck \
  -p2 ../forge-llm-sidecar/selfplay/decks/modern/ruby-storm.dck \
  -n 25 \
  -out ../forge-llm-sidecar/selfplay/runs/boros-vs-ruby-mirror.jsonl \
  -format modern \
  -url http://localhost:18970
```

Two sidecar instances, useful when you want separate dashboards or different
model/config experiments per seat:

```sh
# Terminal 1
cd forge-llm-sidecar
uvicorn app.main:app --port 18970

# Terminal 2
cd forge-llm-sidecar
PORT=18971 uvicorn app.main:app --port 18971

# Terminal 3, repo root
./run-forge.sh selfplay \
  -config mirror \
  -p1 ../forge-llm-sidecar/selfplay/decks/modern/boros-energy.dck \
  -p2 ../forge-llm-sidecar/selfplay/decks/modern/ruby-storm.dck \
  -n 25 \
  -out ../forge-llm-sidecar/selfplay/runs/boros-vs-ruby-two-sidecars.jsonl \
  -format modern \
  -url1 http://localhost:18970 \
  -url2 http://localhost:18971
```

## 6. Build and Run a Gauntlet

There is no separate `-gauntlet` flag. A gauntlet is the list of decks you
choose to pass as `-p2` across multiple runner invocations.

Example: evaluate one pilot deck against every imported Modern deck:

```sh
PILOT="forge-llm-sidecar/selfplay/decks/modern/boros-energy.dck"
GAUNTLET_DIR="forge-llm-sidecar/selfplay/decks/modern"
RUN_DIR="forge-llm-sidecar/selfplay/runs"

for OPPONENT in "$GAUNTLET_DIR"/*.dck; do
  NAME=$(basename "$OPPONENT" .dck)
  ./run-forge.sh selfplay \
    -config mirror \
    -p1 "../$PILOT" \
    -p2 "../$OPPONENT" \
    -n 20 \
    -out "../$RUN_DIR/boros-energy-vs-$NAME.jsonl" \
    -format modern \
    -label "gauntlet"
done
```

Example: define a smaller explicit gauntlet:

```sh
PILOT="forge-llm-sidecar/selfplay/decks/modern/boros-energy.dck"
RUN_DIR="forge-llm-sidecar/selfplay/runs"

for OPPONENT in \
  forge-llm-sidecar/selfplay/decks/modern/ruby-storm.dck \
  forge-llm-sidecar/selfplay/decks/modern/amulet-titan.dck \
  forge-llm-sidecar/selfplay/decks/modern/dimir-murktide.dck
do
  NAME=$(basename "$OPPONENT" .dck)
  ./run-forge.sh selfplay \
    -config mirror \
    -p1 "../$PILOT" \
    -p2 "../$OPPONENT" \
    -n 20 \
    -out "../$RUN_DIR/boros-energy-vs-$NAME.jsonl" \
    -format modern \
    -label "gauntlet"
done
```

Use `goldfish` instead of `mirror` when the gauntlet is a set of passive or
scripted benchmark opponents and you only want one sidecar-piloted seat.

## 7. Reports and Output Files

Every run writes a raw JSONL artifact to the path passed with `-out`.

Recommended location:

```text
forge-llm-sidecar/selfplay/runs/*.jsonl
```

Each JSONL line is one sidecar seat record:

```json
{
  "game_id": "123",
  "archetype": "Boros Energy",
  "opponent": "60 Islands",
  "pilot_mode": "solve",
  "won": true,
  "win_turn": 4,
  "turns": 4
}
```

Goldfish runs produce one record per game. Mirror runs produce two records per
game.

If the sidecar is reachable and `-record` is not set to `false`, the runner also
POSTs the finished run to:

```text
POST http://localhost:18970/selfplay/record
```

That stores run history in:

```text
forge-llm-sidecar/selfplay/results.db
```

Override the DB path with:

```sh
export FORGE_SELFPLAY_DB=/path/to/results.db
```

## 8. Generate Reports

One-shot report from raw JSONL:

```sh
cd forge-llm-sidecar
python -m scripts.selfplay_report selfplay/runs/*.jsonl
```

Group only by archetype:

```sh
python -m scripts.selfplay_report selfplay/runs/*.jsonl --by archetype
```

Emit JSON:

```sh
python -m scripts.selfplay_report selfplay/runs/*.jsonl --json
```

If a run was not recorded automatically, ingest it manually into SQLite:

```sh
python -m scripts.record_run selfplay/runs/boros-energy-goldfish.jsonl \
  --format modern \
  --config goldfish \
  --label baseline
```

Trend report from SQLite:

```sh
python -m scripts.selfplay_trends
```

Full per-run series for one deck:

```sh
python -m scripts.selfplay_trends --archetype "Boros Energy"
```

The dashboard reads the same SQLite store. With the sidecar running, open:

```text
http://localhost:18970/dashboard
```

The relevant panel is **Self-play Trends**.

## 9. Quick Recipes

Import decks, then goldfish one deck:

```sh
cd forge-llm-sidecar
python scripts/import_goldfish_decks.py modern --limit 8
cd ..

./run-forge.sh selfplay \
  -config goldfish \
  -p1 ../forge-llm-sidecar/selfplay/decks/modern/boros-energy.dck \
  -p2 ../forge-llm-sidecar/selfplay/60-islands.dck \
  -n 50 \
  -out ../forge-llm-sidecar/selfplay/runs/boros-energy-goldfish.jsonl \
  -format modern
```

Run a baseline, make changes to learnings/guidance, then compare:

```sh
./run-forge.sh selfplay \
  -config goldfish \
  -p1 ../forge-llm-sidecar/selfplay/decks/modern/boros-energy.dck \
  -p2 ../forge-llm-sidecar/selfplay/60-islands.dck \
  -n 50 \
  -out ../forge-llm-sidecar/selfplay/runs/boros-baseline.jsonl \
  -format modern \
  -label baseline

# After changing guidance/learnings, run another batch.
./run-forge.sh selfplay \
  -config goldfish \
  -p1 ../forge-llm-sidecar/selfplay/decks/modern/boros-energy.dck \
  -p2 ../forge-llm-sidecar/selfplay/60-islands.dck \
  -n 50 \
  -out ../forge-llm-sidecar/selfplay/runs/boros-after-learnings.jsonl \
  -format modern \
  -label after-learnings

cd forge-llm-sidecar
python -m scripts.selfplay_trends --archetype "Boros Energy"
```
