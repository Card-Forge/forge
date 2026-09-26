# Mana Table desktop beta

A local Electron renderer connected to the Forge Java engine through private
stdin/stdout pipes. No HTTP server is started. The renderer has no Node access;
the preload exposes an allowlisted API. The Java process owns deck edits,
validation, persistence, practice-hand shuffling, and human-versus-AI games.

## Development

Requirements: JDK 17, Maven 3.8.1+, Node 22.12+, Windows x64 for the supplied package script.

```sh
# From repository root
mvn -pl forge-api -am verify
cd forge-desktop
npm ci
npm start
```

The engine jar is `forge-api/target/forge-engine.jar`. Set `FORGE_JAVA` to a Java
executable or set `JAVA_HOME`. Development deck data lives in `forge-desktop/.data`.
`FORGE_USER_DATA` overrides the data folder; `FORGE_OFFLINE=1` disables image fetches.

```sh
node --test tests/engine.test.cjs tests/match.test.cjs
npm test
npm run package
```

Packaging copies only the required Forge resources, builds a Java runtime using
`jlink`, and writes a new timestamped folder under `dist`. Existing packages and
their user data are preserved. The package script copies decks and cached art
from the previous beta and verifies deck file hashes before updating
`dist/latest-beta.json` to identify the new build.
The packaged app needs neither Maven, Node, nor a system Java installation.

Read [BETA.md](BETA.md) for user instructions and the current scope.

## Protocol

Requests are newline-delimited UTF-8 JSON: `{id, method, params}`. Responses are
`{id, result}` or `{id, error}`. Startup emits `{event: "loading"}` and
`{event: "ready", printings}`. Forge diagnostics go to stderr and the desktop log.

Commands: `search`, `list`, `new`, `open`, `snapshot`, `edit`, `rename`, `undo`,
`redo`, `format`, `save`, `importPreview`, `import`, `export`, `practice`,
`matchOpponents`, `matchStart`, `matchState`, `matchAction`, `matchConcede`.
Edits require the current deck revision. Saved decks use opaque UUID filenames,
schema version 1, printing IDs, quantities, and explicit sections. Writes use
temporary files and atomic replacement where supported. Failed writes remain
visible and block switching decks until saved.

`MatchSession` hosts one human-versus-AI Constructed game. `matchStart` uses the
saved current deck and an opponent ID from `matchOpponents`. `matchState` returns
a cached snapshot with a revision, viewer-filtered zones, stack, and current
prompt. `matchAction` requires `sessionId` and the current `promptId`; stale or
duplicate answers are rejected. Card handles belong to that prompt only.
Synchronous dialogs use response futures so replies cannot deadlock behind the
waiting engine thread. Returning to the workshop keeps the match active; closing
the application ends it. See [the API integration guide](../forge-api/README.md).

Catalog searches include ordinary and supplemental card databases, all faces,
and scripted casual cards. The UI starts without a query or color/type/mana filter.
`search` returns both the matching `total` and unfiltered `catalogTotal` (with
printing grouping applied consistently). `card.deckSection` routes supplemental
cards to sections such as Planes or Schemes; it is not a legality assertion.
