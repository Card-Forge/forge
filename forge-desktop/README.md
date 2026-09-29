# Mana Table desktop development

Electron hosts the plain HTML/CSS/JavaScript renderer and a Java child process.
The Java process owns the card catalog, deck operations, persistence, rules, AI,
and matches. Communication uses private stdin/stdout JSON pipes; there is no HTTP
server. Read the [architecture](../docs/Development/Mana-Table-Architecture.md)
and [engine API](../forge-api/README.md) before extending that boundary.

## Prerequisites

- Git, **JDK 17+**, **Maven 3.8.1+**, **Node.js 22.12+** with npm.
- Contributor CI uses JDK 17, Node 24, and Windows. Windows x64 is the tested
  desktop target and the only target supported by the package script.
- Set `JAVA_HOME` to the JDK directory. Java discovery works on Windows, Linux,
  and macOS, but non-Windows desktop behavior is not yet validated by this fork.

The repository is large because it contains Forge resources. A first build needs
network access for Maven/npm dependencies and Electron; a running test suite uses
the local card scripts and normally disables remote artwork.

## Build and launch

From the repository root:

```sh
mvn -pl forge-api -am verify
cd forge-desktop
npm ci
npm run doctor
npm start
```

This produces `forge-api/target/forge-engine.jar` and builds only its required
Maven modules. Rebuild the JAR after Java changes. Restart Electron after changes
to `main.cjs`, the preload, or renderer code. No frontend bundle step is required.

In PowerShell, configure your own installed JDK, for example:

```powershell
$env:JAVA_HOME = 'C:\path\to\your\jdk-17'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version
mvn -version
```

Use `npm.cmd` if your PowerShell policy prevents execution of `npm.ps1`.
Ignored `.tools` and `.m2` directories on someone else's workstation are not
prerequisites. To opt into a repository-local Maven cache, pass
`-Dmaven.repo.local=.m2` while building from the root.

`npm run doctor` reports the resolved Java command, Node requirement, engine JAR,
resources, and installed desktop dependencies. It exits nonzero for missing items.

## Common commands

Run these in `forge-desktop`:

| Command | Purpose |
| --- | --- |
| `npm run check` | Parse host, renderer, scripts, and tests without launching the app |
| `npm run test:unit` | Fast Node tests; no Java process or Electron window |
| `npm run test:engine` | Real Java protocol, persistence, game, Commander, and preset tests |
| `npm run test:smoke` | Electron deck-workshop and playable-match checks |
| `npm run test:ui` | All Electron/Playwright checks (`npm test` remains an alias) |
| `npm run test:encounters` | Reusable encounter regressions and UX handoff checks |
| `npm run encounter -- --help` | Automated or guided human playtests with review artifacts |
| `npm run package` | Build a new Windows x64 package with Java and card resources |

See [testing](../docs/Development/Mana-Table-Testing.md) for subsets, packaged
tests, and failure artifacts; see [releases](../docs/Development/Mana-Table-Releases.md)
for versioning, data preservation, and distribution.
For reusable scenarios and participant sessions, see the
[encounter guide](../docs/Development/Mana-Table-Encounters.md).

The deck-building regression is `tests/deck-workshop.spec.cjs`. It covers rapid
copy edits across imported printings, section moves and undo, deck search/grouping,
color identity, explained suggestions, and wide/compact screenshots. Run it with
`npm run test:ui -- tests/deck-workshop.spec.cjs`. The engine transport regression
also checks that review is read-only and role/identity filters use real card data.

## Environment variables

| Variable | Meaning |
| --- | --- |
| `JAVA_HOME` | JDK directory for development; required by packaging for `jlink` |
| `FORGE_JAVA` | Explicit Java executable override, including for packaged tests |
| `FORGE_USER_DATA` | Override the complete data directory for a manual or automated test |
| `FORGE_OFFLINE=1` | Disable artwork retrieval; the engine/catalog still use bundled scripts |
| `FORGE_TEST=1` | Start the Electron window hidden for automation |
| `MANA_TEST_PACKAGED=1` | UI tests use the executable in `dist/latest-beta.json` |
| `MANA_TEST_EXECUTABLE` | UI-test executable override when packaged mode is not selected |

Java resolution is shared in `runtime.cjs`: `FORGE_JAVA` first, then the packaged
runtime (if packaged), then `JAVA_HOME/bin/java[.exe]`, then `java` on `PATH`.
Legacy `FORGE_*` variable and module names are kept for compatibility.

## Data and troubleshooting

Development data lives in `forge-desktop/.data`. A packaged build uses `UserData`
beside its executable. Either can be overridden with `FORGE_USER_DATA`.
Each profile contains `decks/` (UUID JSON files), `art/` (cached illustrations),
`preferences.json` (response mode, own-turn stops, and card inspector), `engine.log`,
and Electron preferences/cache files. Play preferences carry forward when packaging
the next beta. Deck saves use schema version
1 and atomic file replacement where supported. Match state is not a saved deck
and does not survive closing the app.

- **Engine does not start:** run `npm run doctor`; check Java and the JAR, then
  read `engine.log` in the profile being used.
- **Library appears to stall:** the first resource scan takes time. The engine
  requests up to 2 GB of heap; avoid launching many integration tests at once.
- **Electron runs as a Node CLI:** unset `ELECTRON_RUN_AS_NODE` before `npm start`.
  Automated tests remove it in their shared launcher.
- **Launcher cannot find a beta:** build a package first. The root `.cmd` launchers
  read a generated manifest; they do not start the development source tree.
- **Artwork is absent:** offline tests intentionally omit it. The card catalog
  and rules do not depend on downloaded images.

The [player guide](BETA.md) describes current features and limitations. The
[contribution guide](../CONTRIBUTING.md) explains branch and review conventions.
