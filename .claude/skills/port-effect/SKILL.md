---
name: port-effect
description:
  Port one or more Forge ApiType effects (M6) to crucible/internal/engine - effect file, registry wiring, enginelint
  group, engine_test test, port-log section and API counts, in one commit. Use when asked to port, implement or land an
  effect such as "Play", "CopySpellAbility", "Clone", or "the next M6 effects".
---

# Port an M6 effect

Every step is enforced by a gate or has broken a commit before. Do them all, in order, for each API. Batch several APIs
per commit (memory: port in bigger packs), but every API gets every step.

## 0. Pick, then research

Pick by corpus use, among effects nobody has already ruled out:

```bash
crucible/scripts/unported-apis.sh 30   # "<lines> <Api>[  deferred]", unregistered only, most-used first
```

Skip any API the script marks `deferred` unless you are taking on the blocker its `**Researched and deferred.**` table
row names (`grep -rn '<Api>' docs/crucible/porting/port-log/game-state/effects-*.md`). Reason: each row cost a full
`forge-oracle` run to establish. An API you research and defer gets a row, with its blocker, in your own batch file's
table (step 5); the script picks it up from there.

Routine or hard: routine = the dominant shape is a state change over pieces the engine already has. Hard = needs a new
stack/casting mechanic, a Layer 1 copy, synthetic Command-zone cards with their own triggers, a new trigger mode, a new
`PlayerController` decision loop, or changes a documented contract (e.g. `DeclareCombatBlockers`' "not re-checked") --
that last one needs an ADR first (ADRP-4). Do them inline; ADR first when a contract changes.

Then delegate to the `forge-oracle` subagent: "forge-oracle: <Api>". Its answer gives the params the Java reads,
resolution order, controller decisions, corpus shapes and PORT-8 bugs. Do not read `forge-game/` inline unless it left a
gap.

Pick which params to resolve by corpus count. Everything else is rejected with an `error` (step 1), never silently
ignored.

## 1. Write `crucible/internal/engine/<api>effect.go`

File name is the API name lowercased plus `effect.go`. Shape, from `healdamageeffect.go`:

```go
package engine

//enginelint:allow card game ability defined condition control parts

import "fmt"

// <api>Effect is <Api>Effect.java: <one-line semantics, CR rule if any>.
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/<Api>Effect.java's resolve.
type <api>Effect struct{}

func (<api>Effect) Resolve(g *Game, a *Ability, c PlayerController) error {
	// Reject every param this port does not resolve, before acting (PORT-8, GO-7).
	for _, key := range [...]string{"<Unresolved1>", "<Unresolved2>"} {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: <Api>: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	// ... resolve. Script-reachable failure -> error, never panic (GO-7).
	return nil
}
```

Rules that bite here:

| Rule   | Do                                                                                                                                                                                               |
| ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| GO-12  | Iterate `collect.OrderedSet`/`OrderedMap` or slices, never a bare `map`, where order is visible                                                                                                  |
| GO-9   | Hold `CardID`/`PlayerID`, never `*Card`                                                                                                                                                          |
| GO-8   | No `any`; read params through `a.Params`                                                                                                                                                         |
| PORT-8 | Forge bug found -> stop, report file:line, do not compensate in Go                                                                                                                               |
| GO-7   | A valid-string property `Matches` has no case for reads false for every card, silently. Check each `Choices$`/`Valid*$` property the dominant shape uses in `valid.go`; reject the unported ones |

Reuse helpers before writing new ones: `targetedOrDefinedCards`, `targetedOrDefinedPlayers` (`defined.go`),
`resolveNamedAmount` (`amount.go`), `optionalAmount`, `checkChoice` (`effecthelpers.go`), `moveByEffect`
(`zonemove.go`).

## 2. Regenerate the registry

`NewRegistry()` is generated (ADR-0017): type `<api>Effect` with a value-receiver `Resolve` registers as `API<Api>`
automatically. Only a type serving several APIs, another API, or needing field values adds doc-comment lines:

```go
//crucible:register Manifest manifestEffect{api: "Manifest", remember: "RememberManifested"}
```

Then regenerate; never edit `registry_gen.go` by hand:

```bash
cd crucible && go generate -run genregistry ./internal/engine
```

On a merge conflict in `registry_gen.go`, take either side and rerun `go generate`.

## 3. `enginelint` group, in the effect file

The file declares its own group (named after the file) right after the package clause. Copy a similar effect's list,
e.g. `healdamageeffect.go`:

```go
package engine

//enginelint:allow card game ability defined condition control parts
```

Do not touch `enginelint.json` for an effect. A violation names the file a symbol lives in
(`Memory is declared in memory.go`); the allow list takes that file's group name, which can differ -- look it up under
`"groups"` in `internal/engine/enginelint.json` (`memory.go` is group `parts`, `emitCounterChanged`'s `event.go` is
`event`). Run until clean:

```bash
cd crucible && go run ./tools/enginelint -config internal/engine/enginelint.json
```

## 4. Test in `package engine_test`

At least two players: a one-player game ends at the first SBA check (CR 104.2a) and later stack items never resolve. Put
tests in a file named for behavior (TEST-3), e.g. an existing `pack*effects_test.go` or a new one. Check every returned
`error` (errcheck is a CI gate).

```go
func Test<Api>Effect<WhatItProves>(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	host := resolveLine(t, g, p, engine.NewScriptedController(), "DB$ <Api> | <Param>$ <Value>")
	// assert on g / g.Card(host) / g.Player(other)
}
```

Helpers: `newTwoPlayerGame`, `resolveLine` (fails the test on error), `resolveNow` (returns the error; use it to assert
a rejected param), `creatureDefPT`, `libraryCards`, `etbChainDef` + `castETBChain` for a `SubAbility$` chain. Queue
controller answers on `engine.NewScriptedController()` (`QueueEntityChoice`, `QueueAbilityChoice`, ...). `newTokenGame`
is a two-player game whose DB holds `testTokens` (`tokens_test.go`; add a script there). In `pack4effects_test.go`:
`offerRecorder` records every card pool `ChooseCardsForEffect` is offered (assert on pool contents and order),
`firstPicker` picks the first cards offered (for cards the effect itself creates), and `pushStackAbility` leaves an
ability waiting on the stack under the one being tested.

Add one test per rejected param shape that matters: assert `resolveNow` returns the `not resolvable yet` error.

Write a test for every resolved param branch now, not after the covergate fails. `internal/engine` has a 90% floor
(TEST-12) and a batch of effects tested only on their main shape lands below it. Check the new files before moving on to
the docs:

```bash
cd crucible && go test -count=1 -coverprofile=/tmp/eff.out ./internal/engine \
  && go tool cover -func=/tmp/eff.out | grep -E '/(<api1>|<api2>)effect\.go:' | awk '$3+0 < 90'
```

Every function it prints is under 90%; cover its missing branches (`go tool cover -html=/tmp/eff.out` shows which).

## 5. Docs, same commit (DOC-12)

| File                                                             | Change                                                                                                                                                                                  |
| ---------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `docs/crucible/porting/port-log/game-state/effects-<batch>.md`   | New file, one per batch (`<batch>` = kebab-case of its lead APIs). A `## <Apis> land` section: what resolves, what is rejected and why, shared engine pieces, Java `path:line`; then a `**Researched and deferred.**` table for APIs researched and not ported |
| `docs/crucible/porting/port-log/game-state.md`                   | Index row for the new section; update the remaining-effects sentence in `## Not ported yet` (`M6's N remaining ...`)                                                                   |
| `docs/crucible/00-master-implementation-plan-in-progress.md`     | Resolved-API count                                                                                                                                                                      |
| `CLAUDE.md`                                                      | `M6 in progress: N of the corpus's 203` count and largest gaps (`unported-apis.sh` counts)                                                                                             |
| `docs/crucible/porting/forge-java-defects.md`                    | A row per Forge Java bug found (PORT-8): site, defect, what Crucible does meanwhile, upstream status                                                                                   |

`effects-batches.md` and the other existing `effects-*.md` files are closed: never append to them. Reason: parallel
porters appending to one file conflict on every merge; a new file per batch never does, and the index rows it adds merge
as a union (`scripts/merge-porters.sh`).

Count = registered APIs minus the three M5 casting entries. `genregistry -check` (in `gates.sh` and CI) prints it and
fails when `CLAUDE.md` or the plan quotes a different number:

```bash
cd crucible && go run ./tools/genregistry -dir internal/engine -check \
    -docs ../CLAUDE.md,../docs/crucible/00-master-implementation-plan-in-progress.md
```

## 6. Verify, then commit

Run the `rules-review` skill on the diff, fix findings, then commit. The commit hook runs `gates.sh full` and blocks on
red; to see failures without the context flood pipe `gates.sh full` through `tail -40` first.
