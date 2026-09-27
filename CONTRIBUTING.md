# Contributing to Mana Table

Start with the [desktop setup guide](forge-desktop/README.md), then read the
[architecture map](docs/Development/Mana-Table-Architecture.md). The working
integration branch is **`feature/desktop-beta`**; base feature branches and pull
requests on it until the repository adopts another integration branch.

## Pick the right layer

- **UI and interaction:** `forge-desktop/renderer`. Keep game decisions in the
  engine; the renderer displays snapshots and answers current prompts.
- **Desktop services:** `forge-desktop/main.cjs`, `preload.cjs`, and the small
  CommonJS modules beside them. These own process startup, files, and permitted IPC.
- **API and visibility:** `forge-api/src/main/java/forge/api`. Extend the adapter
  when a UI needs new projected state or an engine-controlled interaction.
- **Card rules and AI:** the existing Forge modules and `forge-gui/res`. Consult
  the [upstream notes](docs/upstream/CONTRIBUTING.md) and card-scripting guides.

Keep unrelated upstream formatting and resource changes out of focused PRs.
Follow the style of the file being changed: Java 17 and the Maven checks for Java;
plain JavaScript/CSS for the renderer; CommonJS for Electron and Node tools.
Do not add a framework or broad formatting migration as incidental cleanup.

## Work on a change

```sh
git switch feature/desktop-beta
git pull --ff-only
git switch -c feature/describe-your-change
```

Describe the behavior being improved. For a game regression, retain a small deck
list and reproduction sequence with the format, table size, relevant cards, phase,
and pending choice. For visual changes, capture the relevant state at 1000×740
and a larger window; check keyboard access and reduced motion when affected.

Run checks appropriate to the change. The [testing guide](docs/Development/Mana-Table-Testing.md)
separates quick checks, real-engine tests, and Electron interactions. New tests
should catch a meaningful behavior or regression, rather than restating the
implementation. Use the shared helpers under `forge-desktop/tests/support` so
tests use isolated data and the same Java resolution as the app.

Never commit generated packages, local profiles, caches, logs, or test output.
They are already ignored. Use `FORGE_USER_DATA` for a disposable manual-test
profile instead of experimenting on your saved decks.

## Review expectations

A PR should explain the concrete problem, resulting behavior, and checks run.
Include screenshots for visible changes and mention remaining limitations.
Update the relevant documentation when changing setup, IPC, environment variables,
saved data, or packaging. Do not change persisted schemas or the action protocol
without describing compatibility and migration behavior.

Keep prompt/session checks and hidden-information filtering intact. A visual ID
is not an action key; a card entering a hidden zone must not remain inspectable.
See the [API contract](forge-api/README.md) for the full integration rules.

If an AI coding assistant substantially helped produce a contribution, identify
that in the PR body or a co-author attribution, following the upstream policy.
The contributor remains responsible for understanding and verifying the change.

## Report an issue

Use this fork's issue tracker for Mana Table UI, host, and API issues. Include the
beta version/commit, OS, window size, deck format, number of players, reproduction,
and expected versus actual behavior. Engine diagnostics are in `engine.log`
inside the active data directory; share the relevant excerpt and a minimal deck.
See [data locations and troubleshooting](forge-desktop/README.md#data-and-troubleshooting).

## Keep upstream changes manageable

`origin` is this fork (or your personal fork). An `upstream` remote may point to
`https://github.com/Card-Forge/forge.git`. Keep shared-engine patches small and
document why the adapter needs them. Do not rewrite shared branch history during
upstream integration. The module map and existing adapter touchpoints are in the
architecture guide. Preserve the [license](LICENSE), Forge attribution, and
third-party notices.
