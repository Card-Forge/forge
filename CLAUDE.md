# CLAUDE.md — Crucible

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

```bash
# Markdown — both required before every commit (DOC-14, DOC-15)
prettier --write .            # format; .prettierignore excludes all upstream Forge files
prettier --check .            # CI gate
npx markdownlint-cli2 "CLAUDE.md" "docs/crucible/**/*.md"   # semantic lint
```

```bash
# Go — every line below is a CI gate (.github/workflows/crucible-go.yml)
cd crucible && gofmt -l . && go vet ./...
cd crucible && go test -race ./...
cd crucible && go test -race -coverprofile=cover.out ./... && go run ./tools/covergate -profile cover.out   # TEST-12 floors
cd crucible && go run ./tools/enginelint -config internal/engine/enginelint.json   # new engine file → new group + allow-list
cd crucible && go run ./tools/docgate -module . -docs ../docs/crucible            # DOC-12 docs land with code
cd crucible && go run ./tools/apiscan -check && go run ./tools/apiscan -check -api
cd crucible && golangci-lint run   # v2.13.2, same as CI; scripts/ensure-golangci.sh installs one built with go.mod's Go

# All of the above in CI order. .claude/ hooks run full before every Claude commit (blocking) and fast on Stop (report
# only), both with GATES_AUTO_SKIP=1: gates whose inputs are unchanged since HEAD are skipped, and go test uses its cache
# (CI keeps -count=1). Measured: ~25 s with tests cached; a changed engine adds its own ~45 s test run. New .claude/ files need `git add -f`: upstream
# .gitignore ignores .claude
crucible/scripts/gates.sh fast|full

# What to port next: unregistered APIs by corpus line count, deferred ones marked (port-effect step 0)
crucible/scripts/unported-apis.sh 30

# Parallel porters (port-batch skill): list agent worktrees, merge their branches, remove merged ones
crucible/scripts/merge-porters.sh list|merge|cleanup [BRANCH...]

# After editing .claude/hooks: their decision logic (what counts as a commit, what REV-1 guards)
.claude/hooks/hooks_test.sh

# Regenerate NewRegistry after adding an effect (ADR-0017); CI fails on a stale registry_gen.go
cd crucible && go generate -run genregistry ./internal/engine

# Regenerate the golden AST fingerprints, then review the diff
cd crucible && go test ./internal/carddb/compile -run TestCorpusAST -update

# Java oracle
mvn -pl crucible/oracle-java -am test

# Upstream Java suite: TestNG, needs a display
mvn -U -B clean test          # CI runs this under Xvfb
```

---

## Current state

M0 done — 16 ADRs `Accepted`, 7 guidelines, 4 design docs, 6 DSL grammars.

M1 done — `pkg/collect`, `pkg/javarand` (bit-matches Java over the committed golden), `internal/mana` (round-trips every
cost in the corpus), `internal/cardtype`, `tools/javacycles`, `tools/enginelint`, `tools/docgate`, `tools/covergate`,
`oracle-java`.

M2 done — `internal/carddb`, `internal/deck`, `tools/carddump`, `crucible corpus-coverage`. P1 gate green: the canonical
dump is byte-identical to Forge's own `CardRules.Reader` across the whole corpus, and no script key is exempt from the
parser.

M3 done — `internal/carddb/compile` compiles all 33,978 cards with no exemption; `internal/valid`, `expr`, `cost`,
`keyword` port the value grammars; `tools/apiscan` gates the param vocabulary two ways, both at zero; typed param
structs generated (`compile/params_gen.go`); the valid property vocabulary gate is green
(`TestEveryPropertyIsAccountedFor`, M3 item 19, `port-log/valid-strings.md`). **P2 exit gate green:** no unknowns,
allowlist empty, golden AST diff clean.

M4 done — `internal/engine/{game,card,player,zone,event,control}`; `PlayerController` (eleven decision methods) with
`ScriptedController`; `GameState` fixture load/dump, byte-identical round-trip (`internal/fixture`); event schema
(ADR-0013); `Effect`/`Registry` dispatch, effects inside `internal/engine`, registry generated (ADR-0017).

M5 in progress (rules kernel): turn/priority loop and turn driver (`Game.Step`/`Game.Run`, ADR-0026), zone changes and
state-based actions, combat, mulligans, the valid-string evaluator, mana pool and payment, casting permanents, Auras and
Instants/Sorceries through the stack (ADR-0018), trigger firing, replacement effects, block legality, continuous effects
across all eight layers (partial), targeting, SubAbility chaining, last-known information, activated abilities.

M6 in progress: 176 of the corpus's 203 script-driven `Effect` APIs resolve (`NewRegistry`, generated into
`registry_gen.go`); the rest return `ErrUnimplemented`. Of the other 27, 13 have real corpus lines and are listed by
`scripts/unported-apis.sh` as deliberately deferred (Planechase/Archenemy/Un-set/Alchemy shapes, plus
`ChangeText`/`ControlPlayer`/`Meld`); the rest (`Mutate`, `Haunt`, `Bond`, `Encode`,
`InternalIgnoreEffect`/`InternalLegendaryRule`/`InternalRadiation`, ...) have zero real `AB$`/`SP$`/`DB$` lines under
that exact name and never surface in `unported-apis.sh`'s corpus-driven listing at all. Largest gaps (corpus lines,
`scripts/unported-apis.sh`): `ChangeText` (17), `ControlPlayer` (11), `Meld` (7).

Thin or missing: Layer 1 past `Clone`'s own "enters as a copy" (`entersascopy.go` resolves it now) — a copy replacement
other than `Clone` itself, CR 616.1's choice among several, Mystic Reflection's batch "next time"; most of Layers 3-8
past their literal shapes; Ward's own non-mana cost shapes (`PayLife`/`Discard`/`Sac`/`Ward:X`) and
ability-source/retargeted Ward — the mana-cost shape against a directly cast spell is ported (ADR-0028), alongside
Hexproof/Shroud/Protection. Full list: `port-log/game-state.md`, "Not ported yet".

**P4 exit gate:** fixture-count half met (≥300 scenarios, `testdata/scenarios/`); qualitative half ("every layer, every
SBA," Plan Section 3.2) not.

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

Engine port-log lives in `docs/crucible/porting/port-log/game-state/<topic>.md`; `game-state.md` is the index plus
`Not ported yet`. Each new M6 batch gets its own `game-state/effects-<batch>.md`; existing `effects-*.md` files are
closed, so parallel porters never append to one file.

Hooks resolve the checkout from the tool call (hook input `cwd`, the edited path, `git -C`/`cd`), never
`$CLAUDE_PROJECT_DIR` alone: a porter's worktree is gated and REV-1-guarded as its own checkout.
