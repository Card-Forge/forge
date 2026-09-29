# Testing Mana Table

Build the adapter first with `mvn -pl forge-api -am verify` from the repository
root. Run npm commands below from `forge-desktop` after `npm ci`.

| Check | Command | What it covers |
| --- | --- | --- |
| Java compilation, style, tests and engine JAR | `mvn -pl forge-api -am verify` (root) | Adapter and required upstream modules |
| JavaScript syntax | `npm run check` | Host, renderer, tooling and test sources |
| Fast unit tests | `npm run test:unit` | Phase guidance, external deck URL rules, runtime resolution |
| Real-engine integration | `npm run test:engine` | Persistence/revisions, matches, Commander, multiplayer, presets, response availability |
| UI smoke | `npm run test:smoke` | Cold startup, renderer reload, deck workflow, casting, match lifecycle, conditional response skipping |
| Complete UI suite | `npm run test:ui` | All `tests/*.spec.cjs` interaction scenarios |
| Reusable encounter suite | `npm run test:encounters` | Shared automated scenarios, participant handoff, notes and failure artifacts |

`npm test` retains its existing meaning: the Playwright UI suite. It does not
include the Node engine or unit suites. `test:engine` runs files serially because
each starts a Java process with up to 2 GB of heap. The UI suite also uses one
worker. These are integration tests with real resource scans; allow minutes for
the larger suites and keep their timeouts meaningful.

Match pointer tests should follow the visible interaction: approach the hand
from the table's bottom edge and hover an exposed card strip; approach a covered
battlefield card from the playmat so the fan opens a gap. Keyboard focus is also
a supported way to lift a card. Direct center clicks can hit an overlapping card;
keep real hit testing enabled instead of bypassing it with forced clicks.

## Run a focused regression

See [reusable encounters](Mana-Table-Encounters.md) to run the same setup as an
automated regression or a guided human playtest, with optional video recording.

```sh
npm run test:ui -- tests/hand-gestures.spec.cjs tests/hand-readability.spec.cjs
npm run test:ui -- tests/card-selection.spec.cjs tests/land-play.spec.cjs
node --test tests/commander.test.cjs
```

| Change | Useful existing coverage |
| --- | --- |
| Startup/readiness and engine failures | `startup.spec.cjs`, `engine-client.test.cjs` |
| Deck editing/import/export, discovery and review | `engine.test.cjs`, `desktop.spec.cjs`, `deck-workshop.spec.cjs`, `presets.*` |
| Prompts, turn guidance and stale actions | `match.*`, `priority.spec.cjs`, `card-selection.spec.cjs`, `land-play.spec.cjs` |
| Commander/multiplayer | `commander.*`, `multiplayer.*` |
| Hand and battlefield layout | `hand-gestures.spec.cjs`, `hand-readability.spec.cjs`, `battlefield-fit.spec.cjs` |
| Combat | `combat.spec.cjs`, `match.test.cjs`, `multiplayer.test.cjs` |
| Card visibility/inspection | `card-preview.spec.cjs`, `card-faces.spec.cjs`, `library-search.spec.cjs` |
| Animation/event correlation | `animation-feedback.spec.cjs` |
| 3D continuity, idle rendering and graphics fallback | `table-scene.spec.cjs` (real WebGL and engine); animation-feedback retains the 2D fallback check |
| Anchored controls and independent panel scrolling | `rail-layout.spec.cjs` |
| Casting, cancelling, the stack and revealed hand portraits | `casting-reveal.spec.cjs` |
| Source-aware artifact mana choices | `mana-choice.spec.cjs` |

## Profiles and artifacts

Shared helpers live in `tests/support/engine.cjs` and `tests/support/desktop.cjs`.
They create unique profiles under ignored `test-results/`, respect configured
Java, and never use your normal `.data` or packaged `UserData`. UI tests disable
remote artwork by default and launch hidden Electron windows. The face-image
test uses a stubbed image service to exercise the cache without remote requests.

The UI helper supports both source and packaged builds, removes
`ELECTRON_RUN_AS_NODE`, and disables background throttling. Test bodies retain
their own assertions and close the application in `finally`. Engine tests wait
for ready/error with a bounded timeout and close their child process in `finally`.

Failures can leave `engine.log`, Playwright error context, and screenshots in
`test-results`. Use a separate output directory when comparing runs:

```sh
npm run test:ui -- --output=test-results/my-change --reporter=line tests/match.spec.cjs
```

## Packaged verification

The desktop test launcher seeds **Full control** in its isolated profile so
encounters retain deterministic response pauses. Pass `preferences: null` to
`launchDesktop` to exercise production defaults, or supply a preference object.
`response-skip.spec.cjs` checks Auto across real turns and verifies cancellation,
phase stops, temporary holds, stale prompts, and persistence. Its opening-land
scenario must advance automatically after the only available play, then wait
for the next turn's land play. `response-skip.test.cjs` also protects affordable
instants and commanders from an automatic pass. `preferences.test.cjs`
covers validation, disk round trips, and corrupt-file recovery.

After `npm run package`, run a fresh test profile against the manifest's build:

```powershell
$env:MANA_TEST_PACKAGED = '1'
npm run test:smoke
Remove-Item Env:MANA_TEST_PACKAGED
```

All UI tests use the shared launcher and support packaged mode. Use
`MANA_TEST_EXECUTABLE` for a particular executable instead; packaged mode takes
precedence when both variables are set. Neither mode uses the player's profile.
Visual-review screenshots are captured in source runs; packaged tests skip those
captures because the hidden-window compositor can stall, while retaining layout
and interaction assertions.
The optional `node scripts/smoke-package.cjs` also captures preview images and
checks bundled resources; unlike the normal UI suite, it permits artwork fetches.

## CI and review

The `Mana Table` workflow builds the focused Java reactor on Windows, checks
JavaScript, runs unit and real-engine tests, then the UI smoke and encounter suites.
Encounter artifacts are retained for 14 days. The full UI suite is a local/release
check. Inherited Forge workflows remain separate and may
also run. A local successful command does not mean a hosted CI run has completed.

Add regression tests for changed behavior, especially hidden information,
persistence, stale prompts, and click/drag boundaries. Reuse existing scenarios
when possible. Do not replace engine-backed assertions with UI-only mocks for
rules behavior. For a flaky failure, retain the profile/log and reproduction
before changing the assertion or adding a retry.
