# Mana Table

Mana Table is a desktop card workshop and playable Magic: The Gathering table,
built with a web UI on the Forge rules engine. It is an independent community
fork, with active development on **`feature/desktop-beta`**.

The beta includes the full bundled card catalog, deck imports and exports,
autosave and undo, Commander presets, opening-hand practice, two-player
Constructed, and Commander with **2–6 players** (one local human and AI opponents).
The engine handles spells, mana, targets, phases, and combat. The UI presents a
tabletop battlefield, a persistent card fan, card inspection, and combat assignments.

## Start here

| I want to… | Guide |
| --- | --- |
| Play the current beta | [Player guide](forge-desktop/BETA.md) |
| Build and run from source | [Desktop development](forge-desktop/README.md) |
| Make a contribution | [Contributing](CONTRIBUTING.md) |
| Understand the components | [Architecture](docs/Development/Mana-Table-Architecture.md) |
| Run checks or diagnose a regression | [Testing](docs/Development/Mana-Table-Testing.md) |
| Build a distributable | [Packaging and releases](docs/Development/Mana-Table-Releases.md) |
| Extend the engine adapter | [Engine API contract](forge-api/README.md) |

## Run from source

Install **JDK 17+**, **Maven 3.8.1+**, and **Node.js 22.12+**. JDK 17 and Node 24
are the contributor CI baseline. Set `JAVA_HOME` to your JDK directory and put
Maven on `PATH`.

```sh
git clone --branch feature/desktop-beta https://github.com/proflayton/Mana-Table.git
cd Mana-Table
mvn -pl forge-api -am verify
cd forge-desktop
npm ci
npm run doctor
npm start
```

The focused Maven command builds the engine adapter and its dependencies; the
Android/iOS toolchains are not needed for Mana Table. The first startup scans the
card scripts and can take a moment. See the development guide for PowerShell and
Java configuration details.

**Windows x64 is the tested desktop/package target.** Java discovery supports
other development platforms, but their Electron UI and packaging need validation.
After a local package has been built, [Launch Mana Table.cmd](Launch%20Mana%20Table.cmd)
opens the build recorded in `dist/latest-beta.json`. `Launch Workshop.cmd` is a
compatibility alias. These launchers do not build the app on a fresh clone.

## Beta scope

Matches run locally against AI. Online human play, durable match saves, and
complete format legality checks are not implemented. Closing the app ends the
current game; saved decks persist. Deck validation checks structure, not rotating
set legality or ban lists. Unusual card interactions still need broader testing.

Four Commander precons are bundled for offline use. Moxfield discovery opens in
your browser; arbitrary deck imports use exported text lists. Card art is fetched
from Scryfall when available and cached locally. See the player guide for details.

## Forge and licensing

Mana Table reuses [Card-Forge/forge](https://github.com/Card-Forge/forge), including
its rules, AI, card scripts, and shared human controller. Existing `forge-*`
module names are retained for compatibility and upstream maintenance.

The repository is [GPL-3.0-or-later](LICENSE). Preserve Forge attribution and
runtime notices when distributing builds. Magic: The Gathering and card artwork
belong to their respective owners; this project is not affiliated with Wizards
of the Coast. The [upstream overview](docs/upstream/README.md) and
[upstream contribution notes](docs/upstream/CONTRIBUTING.md) are retained separately.
