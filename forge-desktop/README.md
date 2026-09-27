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
node --test tests/engine.test.cjs tests/match.test.cjs tests/commander.test.cjs
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
`matchOpponents`, `matchSetup`, `matchStart`, `matchState`, `matchAction`, `matchConcede`.
Edits require the current deck revision. Saved decks use opaque UUID filenames,
schema version 1, printing IDs, quantities, and explicit sections. Writes use
temporary files and atomic replacement where supported. Failed writes remain
visible and block switching decks until saved.

`MatchSession` hosts one human with one AI in Constructed or 1–5 AIs in Commander.
`matchStart` uses the saved current deck and an `opponents` array of IDs from
`matchOpponents` (or legacy singular `opponent`). `matchState` returns
a cached snapshot with a revision, viewer-filtered zones, stack, and current
prompt. `matchAction` requires `sessionId` and the current `promptId`; stale or
duplicate answers are rejected. Card handles belong to that prompt only.
Synchronous dialogs use response futures so replies cannot deadlock behind the
waiting engine thread. Returning to the workshop keeps the match active; closing
the application ends it. See [the API integration guide](../forge-api/README.md).

`match-feedback.js` presents turn ownership, phases, recent actions, and optional
animations. `activity` is a bounded event history copied by the host. A separate
`boardRevision` keeps event-only updates from rebuilding the table; `visualId`
correlates visible cards across stable snapshots without replacing prompt-scoped
action keys. Motion defaults to the system preference and can be toggled locally.

The renderer retains card nodes across changes to prompt handles and highlights
when visible board data is unchanged. It acknowledges clicks synchronously and
polls immediately after actions, then every 50 ms while resolving and 350 ms at
a waiting prompt. Polls never overlap, and responses from an old session cannot
replace a newer table. Cached artwork bypasses the network download queue.

`node --test tests/multiplayer.test.cjs` exercises real four- and six-player
Commander setup, private zones, attacks against a selected defender, concession,
and human elimination. `npm test -- tests/multiplayer.spec.cjs` covers seat setup,
viewing any opponent, resizing to 1000/1120/1540 pixels, immediate acknowledgment,
and retaining the same card DOM nodes through a response pause.

`turn-guide.js` describes all engine phase keys, names each priority action, and
distinguishes optional responses from required combat, cost, and card-selection
prompts. Guidance never dispatches an action. The renderer retains the engine's
message for required choices and provides a read-only expandable turn guide.
Run `node --test tests/turn-guide.test.cjs` for the phase/prompt matrix and
`npm test -- tests/priority.spec.cjs` for a real match through upkeep, draw,
main phases, end step, spell responses, and mandatory cleanup discards.

`matchSetup` returns the current deck ID/revision, format, starting life, opponents,
`maxPlayers`, and a validated Commander preview. A Commander list without a Cmd section can
select `commanderId` from its main-deck candidates; the host validates a detached
99+1 copy and never edits the saved list. A unique valid candidate is preselected.
Pass `commanderId`, `deckId`, and `revision` to `matchStart` to launch that preview.
Commander matches use the engine variant, 40 life, singleton AI decks, command-zone
casting, commander tax, and commander damage. Existing Cmd sections are honored.

Catalog searches include ordinary and supplemental card databases, all faces,
and scripted casual cards. The UI starts without a query or color/type/mana filter.
`search` returns both the matching `total` and unfiltered `catalogTotal` (with
printing grouping applied consistently). `card.deckSection` routes supplemental
cards to sections such as Planes or Schemes; it is not a legality assertion.
