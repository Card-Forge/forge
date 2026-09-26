# Forge Workshop desktop beta

A local Electron renderer connected to the Forge Java engine through private
stdin/stdout pipes. No HTTP server is started. The renderer has no Node access;
the preload exposes an allowlisted API. The Java process owns deck edits,
validation, persistence, and practice-hand shuffling.

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
node --test tests/engine.test.cjs
npm test
npm run package
```

Packaging copies only the required Forge resources, builds a Java runtime using
`jlink`, and writes a new timestamped folder under `dist`. Existing packages and
their user data are preserved. `dist/latest-beta.json` identifies the latest build.
The packaged app needs neither Maven, Node, nor a system Java installation.

Read [BETA.md](BETA.md) for user instructions and the current scope.

## Protocol

Requests are newline-delimited UTF-8 JSON: `{id, method, params}`. Responses are
`{id, result}` or `{id, error}`. Startup emits `{event: "loading"}` and
`{event: "ready", printings}`. Forge diagnostics go to stderr and the desktop log.

Commands: `search`, `list`, `new`, `open`, `snapshot`, `edit`, `rename`, `undo`,
`redo`, `format`, `save`, `importPreview`, `import`, `export`, `practice`.
Edits require the current deck revision. Saved decks use opaque UUID filenames,
schema version 1, printing IDs, quantities, and explicit sections. Writes use
temporary files and atomic replacement where supported. Failed writes remain
visible and block switching decks until saved.

The beta does not implement a match-session host. The existing game projection
and observation hooks remain available in `forge-api` for the next gameplay work.
See [the API integration guide](../forge-api/README.md).
