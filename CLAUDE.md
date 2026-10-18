# CLAUDE.md — Crucible

@.claude/RTK.md

Fork of [Card-Forge/forge](https://github.com/Card-Forge/forge). Java MTG engine.

**Crucible** = Go port of that engine + automated deck testing / optimization suite. Runs batch simulations of a target
deck vs a meta gauntlet, captures telemetry (dead cards, mana health, per-card impact), emits deck-improvement reports.

Plan: `docs/crucible/00-master-implementation-plan.md`

---

## Read before working

| Doing                                               | Read first                                              |
| --------------------------------------------------- | ------------------------------------------------------- |
| Writing **any** doc, ADR, comment, commit body      | `docs/crucible/guidelines/00-documentation-style.md`    |
| Writing Go                                          | `docs/crucible/guidelines/01-go-coding-standards.md`    |
| Porting a Java file                                 | `docs/crucible/guidelines/02-java-to-go-translation.md` |
| Writing tests                                       | `docs/crucible/guidelines/03-testing-standards.md`      |
| Making an architectural decision                    | `docs/crucible/guidelines/04-adr-process.md`            |
| Writing anything under `architecture/` or `design/` | `docs/crucible/guidelines/06-architecture-docs.md`      |
| Committing / reviewing                              | `docs/crucible/guidelines/05-commit-and-review.md`      |
| Syncing with `Card-Forge/forge`                     | `docs/crucible/runbooks/upstream-sync.md`               |

Index: `docs/crucible/guidelines/README.md`

Rules have stable IDs — `DOC-4`, `GO-2`, `TEST-1`, `PORT-2`, `ADRP-1`, `REV-8`, `ARCH-2`. Cite them in comments and
commit bodies.

---

## Layout — do not deviate

| Path                    | Contents                                                     |
| ----------------------- | ------------------------------------------------------------ |
| `crucible/`             | **All** Go code. Own `go.mod`                                |
| `docs/crucible/`        | **All** Crucible docs                                        |
| `crucible/oracle-java/` | Only place Java may be added — test-scoped dumpers/recorders |
| everything else         | Upstream Forge. **Do not edit**                              |

Unavoidable upstream edit → log it in `docs/crucible/porting/upstream-patches.md`, same commit.
`.claude/hooks/guard-upstream.sh` asks before any unlogged one. Reason: every touched line outside `crucible/` and
`docs/crucible/` is a future rebase conflict (REV-1).

---

## Non-negotiables

Violated most often. Full reasoning in the guideline files.

1. **No package-level mutable state** in `internal/engine/...` or `internal/carddb/...`. Inject `*carddb.DB` and
   `*javarand.Rand` via `*Game`. Engine runs goroutine-per-game. Java's `StaticData` / `FModel` / `MyRandom` singletons
   must not be translated. (GO-2)
2. **No mutex in the engine.** Games share only immutable data. Mutex there = design bug. (GO-3)
3. **No reflection in the engine.** `ApiType` → effect dispatch uses a generated registry, not `reflect`. (GO-4)
4. **No `any`** in engine or carddb packages. Ability params are generated typed structs, not `map[string]string`.
   (GO-8)
5. **Compile card scripts once at load** into an immutable typed AST. Never re-interpret script strings at runtime the
   way Java does. (PORT-2)
6. **Identity by ID**, never pointer. `CardID uint32` into a per-`Game` arena. (GO-9)
7. **Iteration order is load-bearing.** Use `collect.OrderedSet` / `OrderedMap` where Java used `FCollection`. Bare Go
   `map` breaks trigger ordering and replay parity. (GO-12)
8. **`error` for anything a card script can cause. `panic` only on engine invariant breach**, recovered at the game
   boundary. One bad card must not kill a 100k-game batch. (GO-7)
9. **Docs land in the same commit as code.** New package → row in `architecture/module-map.md`. Ported unit →
   `porting/port-log/<unit>.md` note. ADR merges _before_ its implementing PR. (DOC-12, ADRP-4)
10. **Near-zero dependencies.** Non-stdlib import needs an ADR. Currently allowed: `github.com/google/go-cmp`, tests
    only. (GO-14)
11. **A Forge bug is reported, never worked around.** Malformed script, param that never reaches its effect, method that
    cannot do what its name says → stop, name the file and line, fix it upstream. Carry the fix on a branch and log it
    in `porting/upstream-patches.md` if Crucible needs it now. Never Go code that compensates. Reason: a workaround
    makes Crucible disagree with the oracle for a reason no diff can explain. Distinct from PORT-7, where a quirk is
    reproduced because parity depends on it. (PORT-8)

---

## Testing — module first

**Default: test the whole package through its public API.** Use `package x_test`, not `package x`. Compiler then blocks
reaching into internals. (TEST-1)

Reason: port work rewrites internals constantly. Tests bound to functions lock structure and block the refactors the
port needs. Tests bound to package behavior survive rewrites.

Drop below module level only per this table (TEST-2):

| Situation                                                              | Level                                           |
| ---------------------------------------------------------------------- | ----------------------------------------------- |
| Behavior visible through public API                                    | **Module** — `package x_test`. Default          |
| Pure function, huge input space (parsers, mana cost, valid strings)    | File-level table test + `testing.F` fuzz        |
| Invariant not observable from outside (layer ordering, solver pruning) | Internal test — `package x`, with a why-comment |
| Whole-engine rules behavior                                            | **Fixture directory**, not a Go func            |
| Bug fix                                                                | Module-level regression first                   |

Also:

- Rules tests are **data**: `testdata/scenarios/<case>/{setup.state,actions.log,expect.state,expect.events}`. One Go
  test walks the tree. Adding a test = adding a directory. (TEST-5)
- `setup.state` uses Forge's `GameState` text format so the same fixture runs against the Java oracle.
- `t.Parallel()` by default. CI runs `go test -race ./...` every commit. (TEST-7)
- **No mocking library.** Pass in a different DB / RNG / controller. Needing a mock = wrong seam. (TEST-8)
- Test files named for behavior (`layers_test.go`), never mirroring source files (`card_test.go`). (TEST-3)

---

## Doc style

All docs use the compressed style in `guidelines/00-documentation-style.md`:

- Drop articles, filler, hedging, pleasantries. Fragments fine.
- **Never** drop technical content — identifiers, paths with line numbers, numbers with units, error strings stay exact.
  (DOC-2)
- Every rule carries a reason. (DOC-4)
- 3+ parallel items → table. (DOC-5)
- Code blocks and code comments stay normal English, uncompressed. (DOC-7)
- Show Bad → Good pairs. (DOC-8)
- Write in full sentences for destructive steps, ordered procedures, security and licensing. (DOC-9)
- **State, never history.** No `Correction` / `Update` / changelog sections, no "this previously said". Fix the fact in
  place; git holds the diff. Same for ADRs — a new decision supersedes, everything else is edited in place. (DOC-16,
  ADRP-3)
- **Run `prettier --write .` before every commit.** Config `/.prettierrc`, `printWidth: 120`, `proseWrap: always`.
  Prettier owns table alignment and wrapping — do not hand-align. (DOC-14)

Audience is an IT engineer. Compress grammar, not substance.

---

## Commands

Before every commit: `prettier --write .` (DOC-14), `npx markdownlint-cli2 "CLAUDE.md" "docs/crucible/**/*.md"`
(DOC-15); `.claude/` hooks run `crucible/scripts/gates.sh full` (blocking) on every Claude commit. Every other command
(Go gates, `card-smoke`, `landporter`, `merge-porters.sh`, `genregistry`, the Java oracle) is in the `crucible-commands`
skill: load it before running or recommending one.

## Current state

M0-M4 done (ADRs, guidelines, `pkg/collect`, `pkg/javarand`,
`internal/{mana,cardtype,carddb,deck,valid,expr,cost,keyword}`, the tools under `tools/`, the typed AST for all 33,980
cards, `internal/engine` core, `PlayerController`, fixture round-trip). P1 and P2 exit gates green. Detail:
`docs/crucible/00-master-implementation-plan.md`.

M5 in progress (rules kernel): turn/priority loop, zone changes, SBA, combat, mana, casting, stack, triggers,
replacement effects, continuous effects across all eight layers (partial), targeting, LKI, activated abilities.

M6 in progress: 191 of the corpus's 203 script-driven `Effect` APIs resolve (`NewRegistry`, generated into
`registry_gen.go`); the rest return `ErrUnimplemented`, and the other 12 have no real corpus line.
`crucible card-smoke -all` measures how many cards play: 91.7% of 33,616 judged, an upper bound; the refusal causes it
groups are the backlog.

Everything left, easiest first: `docs/crucible/00-master-implementation-plan-open.md`. Full per-gap list:
`docs/crucible/porting/port-log/game-state.md`, "Not ported yet". **M5 exit gate:** test-port matrix green (met). **P4
exit gate:** fixture-count half met (≥300 scenarios), qualitative half not.

Keep this section short — it loads every session. Every primitive, corpus count, design decision and Java citation goes
in `docs/crucible/00-master-implementation-plan-in-progress.md` items 24-32 and
`docs/crucible/porting/port-log/game-state.md`, never here.

---

## Subagents and skills

| Kind     | Name                 | Use for                                                                                |
| -------- | -------------------- | -------------------------------------------------------------------------------------- |
| subagent | `forge-oracle`       | Java semantics of an API before porting. Keeps `forge-game/` reads out of context      |
| subagent | `rules-reviewer`     | Diff vs non-negotiables, before commit                                                 |
| subagent | `gate-runner`        | Gates or one test, failures only                                                       |
| subagent | `effect-porter`      | Batch of routine M6 effects (Sonnet), one worktree per batch, run in parallel          |
| subagent | `effect-porter-hard` | M6 effects needing real design (stack/casting, Layer 1, Command zone) - `effort: high` |
| skill    | `port-effect`        | Any M6 `ApiType` effect: file, registry, enginelint, test, docs, counts                |
| skill    | `port-batch`         | Big M6 batch: split across parallel porters, merge, reconcile, clean up worktrees      |
| skill    | `port-java-unit`     | Any other Java unit, PORT-3 order                                                      |
| skill    | `add-scenario`       | Rules test as a `testdata/scenarios/` fixture                                          |
| skill    | `sync-upstream`      | Merge `Card-Forge/forge`, verify the port still agrees with what moved                 |
| skill    | `check-card`         | Audit a card script against Oracle text and the Java that runs it; upstream fix branch |

Engine port-log lives in `docs/crucible/porting/port-log/game-state/<topic>.md`; `game-state.md` is the index plus
`Not ported yet`. Each new M6 batch gets its own `game-state/effects-<batch>.md`; existing `effects-*.md` files are
closed, so parallel porters never append to one file.

Worktree subagents cannot run `git`: the user's global `rtk` hook rewrites it to `rtk git`, which the isolation guard
refuses, and no variant passes. Never bypass it with `/usr/bin/git`. Subagents leave work uncommitted; the main session
commits with `crucible/scripts/merge-porters.sh commit BRANCH MSG`. Any worktree-subagent prompt carries the prompt
block from `docs/crucible/runbooks/subagent-worktrees.md` (failures seen and their rules).

Hooks resolve the checkout from the tool call (hook input `cwd`, the edited path, `git -C`/`cd`), never
`$CLAUDE_PROJECT_DIR` alone: a porter's worktree is gated and REV-1-guarded as its own checkout.
