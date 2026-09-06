# TEST — Testing Standards

- **Status:** Active
- **Applies to:** all Go code under `crucible/`
- **Rule IDs:** cite as `TEST-n`

Default test level = **module**. Test the package through its public API. Drop lower only when a lower level tests
something the module level cannot reach.

---

## Why module-first

Port work rewrites internals constantly. Java shape ≠ Go shape (see
[02-java-to-go-translation](02-java-to-go-translation.md)). `Card.java` becomes six Go types. `AbilityFactory` becomes a
compile step plus a registry.

Test bound to a function locks that function's existence. Every refactor breaks 40 tests that were testing structure,
not behavior. Team stops refactoring. Port ends up as transliterated Java.

Test bound to package behavior survives every internal rewrite. Refactor freely, suite still answers "does the module
still do its job".

Second reason: rules engine correctness is **emergent**. `checkStateEffects` alone is meaningless. Correct answer only
exists at the level of "creature with 0 toughness dies after this spell resolves". That is a module-level fact.

---

## TEST-1 — Default: external test package

```go
// crucible/internal/carddb/carddb_test.go
package carddb_test   // NOT package carddb

import "github.com/<org>/crucible/internal/carddb"
```

`package x_test` cannot touch unexported identifiers. Compiler enforces module-level testing. No discipline required.

**Bad** — internal test used by default:

```go
package carddb

func TestParseLineSetsName(t *testing.T) {
    r := &reader{}                       // unexported, structural
    r.parseLine("Name:Lightning Bolt")   // locks internal method shape
    if r.faces[0].name != "Lightning Bolt" { ... }
}
```

**Good** — behavior at package boundary:

```go
package carddb_test

func TestLoadCorpus_ParsesNameAndCost(t *testing.T) {
    t.Parallel()
    db := carddb.MustLoad(t, testCorpus)
    got, ok := db.Card("Lightning Bolt")
    if !ok { t.Fatalf("card not found: %q", "Lightning Bolt") }
    if got.ManaCost.String() != "{R}" {
        t.Errorf("Lightning Bolt mana cost = %q, want %q", got.ManaCost, "{R}")
    }
}
```

Reason: second test survives renaming `reader`, merging it into `Loader`, or replacing line parsing with a generated
lexer. First does not.

---

## TEST-2 — When to go below module level

Not a ban. A decision.

| Situation                                                                                                    | Level                                        | Reason                                                                                          |
| ------------------------------------------------------------------------------------------------------------ | -------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| Behavior observable through public API                                                                       | **Module** (`package x_test`)                | Default. Always try this first                                                                  |
| Pure function, text in → value out, huge input space (mana cost, valid string, count expr, cost string)      | **File-level table test** + `testing.F` fuzz | Combinatorics. 400 input rows read fine as a table, unreadable as module scenarios              |
| Invariant not observable from outside (layer timestamp ordering, mana solver search pruning, arena ID reuse) | **Internal test** (`package x`)              | Invariant is internal by definition. Probe it directly                                          |
| Perf characteristic (alloc count, clone cost)                                                                | **Benchmark**, internal if needed            | Public API hides allocation shape                                                               |
| Whole-engine rules behavior                                                                                  | **Scenario fixture** (TEST-5), not a Go func | Data scales to thousands of cases; Go funcs do not                                              |
| Bug fix                                                                                                      | Module-level regression **first**            | Bug is a behavior failure. Only add an internal test if the root cause is an internal algorithm |

Internal test file needs a header comment saying **why** it is internal:

```go
// Internal test: layer application order depends on unexported timestamp
// assignment, which is not observable through the public API. See TEST-2.
package static
```

No comment → reviewer moves it to `_test` package.

---

## TEST-3 — No mirror test files

Ban: `card_test.go` existing only because `card.go` exists.

Test files named for **behavior under test**, not for source file:

**Bad:** `card_test.go`, `state_test.go`, `counters_test.go`, `factory_test.go`

**Good:** `zone_change_test.go`, `layers_test.go`, `combat_damage_test.go`, `mulligan_test.go`

Reason: mirror naming pushes authors toward per-function tests, which is exactly TEST-1's failure mode. Name says
behavior → author writes behavior test.

---

## TEST-4 — Test packages, not the module tree

Go "module" = the whole `crucible/` go.mod. Too coarse for a test unit.

**Test unit = Go package.** One coherent suite per package with a public API:

| Package                   | Suite covers                                                     |
| ------------------------- | ---------------------------------------------------------------- |
| `internal/carddb`         | Load corpus → query cards. Not "does `parseLine` split on colon" |
| `internal/carddb/compile` | Script text → typed AST. Not "does `splitParams` handle pipes"   |
| `internal/engine/valid`   | Valid-string + game state → matched entities                     |
| `internal/engine/game`    | Fixture in → post-state + event stream out (TEST-5)              |
| `internal/ai/eval`        | Board position → score, with ordering properties                 |
| `internal/telemetry`      | Event stream in → metric rows out                                |

Package with no public API worth testing = package that should not exist. Merge it.

---

## TEST-5 — Rules tests are fixtures, not Go functions

Engine behavior lives in `testdata/scenarios/`. One directory per case. One Go test walks the tree.

```text
testdata/scenarios/
  cr704-state-based-actions/
    creature-zero-toughness/
      setup.state      # GameState text format — shared with Java oracle
      actions.log      # scripted decisions, no AI
      expect.state     # post-state dump
      expect.events    # normalized event stream
  cr613-layers/
    layer7b-set-pt-then-modify/
  effects/
    DealDamage/
      basic/  zero-damage/  vs-protection/
```

```go
func TestScenarios(t *testing.T) {
    db := testDB(t) // sync.Once shared, immutable
    for _, dir := range scenarioDirs(t, "testdata/scenarios") {
        t.Run(dir.Rel, func(t *testing.T) {
            t.Parallel()
            got := runScenario(t, db, dir)
            if diff := cmp.Diff(dir.Want(t), got); diff != "" {
                t.Errorf("scenario %s mismatch (-want +got):\n%s", dir.Rel, diff)
            }
        })
    }
}
```

Rules:

- Adding a rules test = adding a directory. Never a new Go func.
- Fixture name = what it proves. `creature-zero-toughness`, not `test3`.
- Directory named after Comprehensive Rules section where one applies (`cr704-…`).
- `setup.state` uses Forge's `GameState` text format so the same fixture runs against the Java oracle. That is what
  makes differential testing possible. Do not invent a second format.
- Minimum per implemented API: 3 fixtures — normal, edge, interaction.

---

## TEST-6 — Golden files

`-update` flag regenerates. Regenerated diff **must be reviewed**, never blind-committed.

```go
var update = flag.Bool("update", false, "rewrite golden files")
```

Commit message for a golden update states **why the expected output changed**. "Update goldens" alone is rejected in
review.

Reason: blind golden acceptance turns the suite into a change detector that approves every regression.

---

## TEST-7 — Parallel by default, race always

```go
func TestX(t *testing.T) {
    t.Parallel()
    ...
}
```

CI runs `go test -race ./...` on every commit. Not nightly.

Reason: engine design is goroutine-per-game with a shared immutable card DB
([02-java-to-go-translation](02-java-to-go-translation.md), ADR-0005). Java source it is ported from uses mutable
singletons — `StaticData`, `FModel`, `MyRandom`. Accidental translation of a singleton is the highest-probability
porting bug, and parallel tests under `-race` are the only cheap detector.

Test that fails when parallel = test with hidden shared state. Fix the state, not the test.

---

## TEST-8 — No mocking framework

Java suite needs Mockito + PowerMock because of static singletons. Go design removes the cause.

Test needing a different card DB, RNG, clock, or controller **passes one in**:

```go
g := game.New(game.Config{
    DB:   testDB(t),
    Rand: javarand.New(seed),   // deterministic
    P1:   control.Scripted(actions),
    P2:   control.Scripted(actions),
})
```

Go test wanting a mocking library = seam is wrong. Fix the seam.

---

## TEST-9 — Failure messages name input, want, got

```go
t.Errorf("%s: net power = %d, want %d", cardName, got, want)
```

Not `t.Errorf("wrong power")`. Not bare `t.Fail()`.

`t.Fatalf` when the test cannot continue (setup failed). `t.Errorf` otherwise — one run should report every failure, not
the first.

Reason: batch runs and CI logs are the only thing anyone reads. Message must be diagnosable without rerunning.

---

## TEST-10 — Fuzz every parser

Targets, seeded from real card scripts:

`FuzzCardScript` · `FuzzParamMap` · `FuzzValidString` · `FuzzCountExpr` · `FuzzManaCost` · `FuzzCostString`

Properties:

1. Never panic.
2. Parse → serialize → parse is a fixed point.
3. Error return, never a partially-populated result.

Runs nightly and on any parser change. Crashers commit to `testdata/fuzz/` as permanent regressions.

---

## TEST-11 — Forbidden in tests

| Forbidden                                    | Use instead                         |
| -------------------------------------------- | ----------------------------------- |
| `time.Sleep`                                 | Deterministic step, or a channel    |
| Wall clock, `time.Now()` in assertions       | Injected clock                      |
| Network, real filesystem outside `testdata/` | `testdata/`, `t.TempDir()`          |
| Order dependence between tests               | Independent setup per test          |
| Global mutable state in `_test.go`           | `t.Cleanup`, per-test fixtures      |
| `math/rand` default source                   | `javarand.New(seed)` — reproducible |

Reason: every one of these produces a flake. Flaky suite gets muted, muted suite is not a suite.

---

## TEST-12 — Coverage gates

| Package group                                                     | Line coverage floor                               |
| ----------------------------------------------------------------- | ------------------------------------------------- |
| Parsers (`carddb/…`, `mana`, `cardtype`, `valid`, `expr`, `cost`) | 90%                                               |
| Engine core (`engine/game`, `engine/zone`, `engine/static`)       | 80%                                               |
| Effects (`engine/effect`)                                         | measured by **fixture count per API**, not line % |
| AI (`ai/…`)                                                       | 60% + statistical parity dashboard                |
| CLI, report rendering                                             | 50%                                               |

Effects use fixture count because line coverage on a 20-line effect is trivially 100% and proves nothing about rule
correctness.

---

## TEST-13 — Layers, and what gates what

| Layer                | What                                                                   | Runs                             |
| -------------------- | ---------------------------------------------------------------------- | -------------------------------- |
| **L1** Unit / table  | Parsers, value types, `javarand`                                       | Every commit                     |
| **L2** Corpus golden | All 33,682 scripts → canonical JSON, diffed against Java dump          | Every commit                     |
| **L3** Scenario      | TEST-5 fixtures                                                        | Every commit                     |
| **L4** Differential  | Scenario + replay parity vs Java oracle. Build tag `//go:build oracle` | Nightly                          |
| **L5** Fuzz + soak   | TEST-10, plus randomized-deck games with invariant checks              | Nightly                          |
| **L6** Bench + race  | Hot-path benchmarks, `-race`                                           | Race every commit, bench nightly |

L1–L3 must pass before merge. L4–L6 failures open a bug, do not block the merge queue.

---

## TEST-14 — Test helper package

`crucible/internal/engine/enginetest` holds shared helpers. Ported from Forge's `AITest` — those helpers are good:

```go
func NewGame(t *testing.T, opts ...Option) *game.Game
func AddCard(t *testing.T, g *game.Game, name string, p game.PlayerID) card.CardID
func AddCards(t *testing.T, g *game.Game, name string, n int, p game.PlayerID)
func FindSAWithPrefix(t *testing.T, c *card.Card, prefix string) *sa.SpellAbility
func CountCardsWithName(g *game.Game, name string, z zone.Type) int
func AdvanceToPhase(t *testing.T, g *game.Game, p phase.Type, active game.PlayerID)
```

All take `*testing.T` and call `t.Helper()`. Failure points at the caller, not the helper.

Card DB loads once per test binary via `TestMain` + `sync.Once`. Replaces Java's `@BeforeMethod initializeModel()` and
its load-outside-the-timeout-window workaround.

---

## Checklist before merging a test

- [ ] `package x_test` unless TEST-2 justifies otherwise, with a why-comment
- [ ] File named for behavior, not for source file (TEST-3)
- [ ] `t.Parallel()` present
- [ ] Rules behavior went into a fixture directory, not a Go func (TEST-5)
- [ ] Failure message names input, want, got (TEST-9)
- [ ] No sleep, no wall clock, no cross-test ordering (TEST-11)
- [ ] New API? ≥3 fixtures — normal, edge, interaction
- [ ] Golden change? Commit body says why output changed (TEST-6)

## Related

- [00-documentation-style](00-documentation-style.md)
- [02-java-to-go-translation](02-java-to-go-translation.md)
- `docs/crucible/porting/test-port-matrix.md` — status of all 456 ported Java tests
