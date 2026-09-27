# Reusable encounters and UX playtests

An encounter defines a deck, a starting checkpoint, participant tasks, automated
UI actions, and objective outcome checks. Automation and human sessions use the
same setup and checks. All play goes through the real engine and desktop UI.

The first encounter, **`land-play`**, covers playing a Forest, keeping priority,
seeing the board/hand update, and tapping that land through the overlapping hand.
It runs in Constructed and Commander. Its all-Forest library makes the relevant
opening hand repeatable. Turn order and opponent shuffles still vary; there is
no engine seed, arbitrary board-state loader, or durable game replay here.

## Automated regression runs

Use the [development setup](../../forge-desktop/README.md), then run these from
`forge-desktop`:

```sh
npm run test:encounters
npm run encounter -- --list
npm run encounter -- land-play --format Commander
npm run encounter -- land-play --format Constructed --repeat 5
```

The Playwright suite covers both formats, a simulated participant completing the
tasks, an incomplete task retaining failure evidence, and participant cancellation.
CLI repetitions create
independent profiles and stop at the first failure. They do not replay old card
handles. Existing `tests/match.test.cjs` also plays a full game through casting,
targets, payments, combat, and a terminal result; that engine bot is separate from
the guided encounter runner.

To exercise a packaged beta, add `--packaged` to the CLI command (or set
`MANA_TEST_PACKAGED=1` for the Playwright suite). This
uses `dist/latest-beta.json` and the bundled Java. `MANA_TEST_EXECUTABLE` selects
a particular binary. Each run gets a disposable profile; no player decks or
active player sessions are used. Profiles remain under `test-results/` for diagnosis.

## Guided human sessions

In an interactive terminal:

```sh
npm run encounter -- land-play --format Commander --mode ux
```

For the local beta with bundled Java, add `--packaged`; you still need the
desktop npm dependencies, but do not need a system JDK for this mode.

The runner prepares a game while hidden, stops at the human's first main phase,
then shows the window. Read the task in the terminal, perform it in the app, pause,
and return to enter observations. It checks the outcome and presents the next
task. It does not click or answer prompts for the participant after handoff.
Closing the playtest window or pressing Ctrl+C aborts the session. Completing the
last task closes the disposable app and saves the report.

Use the questions in `tester-guide.md` to observe turn comprehension, feedback,
and reachability. An objective task check passing does **not** establish that the
UI was understandable or pleasant. Record hesitation, wrong turns, and verbal
feedback. Reported durations include the time spent typing observations and are
not calibrated task-completion metrics. The automated UX test uses a simulated
participant; it is not a substitute for testing with people.

Artwork is enabled in UX mode, disabled in automated runs. Pass `--offline` for
a human session without image requests. The app remains fully playable offline.

## Recordings and review artifacts

Every run saves a directory containing:

- `report.md` and `report.json`: outcome, step durations, observations, build
  identity, failures, and any artifact-capture problems.
- `tester-guide.md` and `deck.txt`: the shared tasks and fixture deck.
- Numbered `.json` and `.png` checkpoints, plus a failure checkpoint when possible.
  Snapshots use the viewer-filtered API; opponent hands and libraries stay hidden.
- `trace.zip`: UI operations and DOM snapshots. Checkpoint PNGs and optional
  video capture visuals without recording a second frame stream during pauses.
- `desktop.log`: Electron diagnostics, including rejected engine calls. The raw
  Java log stays in the disposable profile and is not included in CI artifacts.

For a video, install Playwright's recording dependency once if it is missing:

```sh
npx playwright install ffmpeg
npm run encounter -- land-play --format Commander --video
npm run encounter -- land-play --format Commander --mode ux --video
```

`playthrough.webm` records the app window. Add `--visible` to watch an automated
run on screen. Open `report.md` to review checkpoint images or use:

```sh
npx playwright show-trace path/to/trace.zip
```

The trace is a review artifact, not an engine replay. It does not independently
prove outcome assertions; those results and failures are in `report.json`.
`checkoutRevision` identifies the tooling checkout; it may differ from the
packaged app. Packaged runs also record the app archive and engine JAR hashes.
Review observations, local paths, and artwork before sharing artifacts.

The Windows CI workflow runs the encounter suite and retains its artifact folder
for 14 days. Local CLI runs print their unique output path under
`forge-desktop/test-results/encounters`.

## Add another encounter

1. Add a module under `forge-desktop/encounters` with `id`, `title`, `description`,
   `formats`, `decks`, `prepare(page, format)`, and `steps`.
2. Each step supplies a participant `task`, research `questions`, an automated
   `perform(page, context)`, and a shared `verify(page, context)`.
3. Register it in `encounters/run.cjs` and add a thin Playwright wrapper like
   `tests/land-play.spec.cjs`. Include it in `test:encounters` for CI coverage.
4. Choose a small real deck/setup that reliably reaches the behavior. Resolve
   current prompt handles immediately before actions; never replay stored keys.
   Treat unexpected required choices as failures, not arbitrary selections.
5. Check that human mode accepts equivalent legal choices (such as another copy
   of the same land), waits for the participant, and retains useful failure data.

Useful next scenarios are instant-speed responses, library searches, alternate
card faces, combat assignments, and multiplayer defender selection. Existing UI
regressions already cover these behaviors and can be extracted without changing
the engine's rules or visibility boundaries.
