# Effects: ChooseSector lands, write side only

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `choosesectoreffect.go`, `control.go`
  (`ChooseSector`), `memory.go` (`chosenSector`)

Batch file for `ChooseSector`. Supersedes `ChooseSector`'s deferred rows in
`effects-abandon-switchblock-choosesector.md` and `effects-batch-c.md` (closed files, left as written). Those rows
deferred it for exactly the reason a rules-review of this file's first commit re-raised: registering the write side
alone would let Space Beleren's only two real corpus lines resolve successfully while silently affecting zero creatures
(`Creature.ChosenSector` has no `valid.go` case yet — see "Space Beleren is still not functional", below). That is worse
than the `ErrUnimplemented` it replaces (GO-7): a resolved ability with no visible effect is wrong deck-improvement
telemetry no diff against the oracle would explain (PORT-8's own reasoning, one layer up). Fixed by
`chooseSectorReadUnbuilt` (`choosesectoreffect.go`): before choosing, `ChooseSector` inspects its own `SubAbility$`
chain for a `ValidCards$`/`ValidTgts$` naming `Creature.ChosenSector`/`DifferentSector`, and rejects loudly if found,
the same as any other unresolved shape. The write side itself (below) is correct and fully tested; it is reachable only
from a chain that does not read the sector back — no real corpus line does that yet.
`effects-abandon-switchblock-choosesector.md` and `effects-batch-c.md` (closed files, left as written). Those rows defer
on the read side, and the read side is still missing; this file lands only the write.

---

## ChooseSector lands

Space Beleren's -1 and -5: "the sector of your choice". Ported from
`forge-game/src/main/java/forge/game/ability/effects/ChooseSectorEffect.java`'s `resolve` (`:10-14`). Corpus: 2 real
lines, both on one card (`forge-gui/res/cardsfolder/s/space_beleren.txt`); both are rejected today (both chain into a
`Creature.ChosenSector` read — see below), which is correct until the read side lands. lines, both on one card
(`forge-gui/res/cardsfolder/s/space_beleren.txt`); both resolve.

| Step           | Go                                                                                | Java                                                                            |
| -------------- | --------------------------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| Condition gate | `subAbilityConditionMet`; `Condition$` rejected, as `ChooseEvenOdd` does          | `SpellAbilityEffect` shared gate                                                |
| Decider        | host's controller, `g.Card(a.Source).Controller()`, not the activator             | `card.getController().getController()` (`:12`)                                  |
| Options        | `["Alpha", "Beta", "Gamma"]`, that order, built per call (GO-2: no package slice) | `PlayerController.chooseSector(Card, String)` (`PlayerController.java:254-256`) |
| Ask            | `PlayerController.ChooseSector(g, decider, NoCard, options)` → index              | `chooseSector(null, AILogic)`                                                   |
| Answer check   | index outside `options` → `engine: ChooseSector: choice N out of range` (GO-7)    | none (GUI/AI always return a listed sector)                                     |
| Record         | `Memory.SetChosenSector(options[i])` on the host; last pick wins                  | `card.setChosenSector(chosen)` (`:13`, `Card.java:2348-2350`)                   |

Params: `AILogic$` (2 lines) is the AI's hint (`PlayerControllerAi.java:722-724` ignores it too, picks at random);
`Ultimate$` (1) feeds only `AchievementTracker.java:23`; `Planeswalker$` (2) is the loyalty-ability marker
`ActivateAbility` already reads. None changes resolution, so none is rejected. `ChooseSectorParams`
(`compile/params_gen.go`) lists nothing else.

### New engine state

`Memory.chosenSector string` (`memory.go`), `SetChosenSector`/`ChosenSector`. Java's `Card.chosenSector`
(`Card.java:322`).

| Question              | Answer                                                                                                                      |
| --------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| Why `Memory`          | Every other `Card.setChosen*` value (even/odd, direction, type) lives there                                                 |
| `Game.Clone`          | `Memory.clone` copies it; a string, so `TestCloneAllocationsStayBounded` is unaffected                                      |
| Effect card copy      | Not copied by `copyChoicesFrom`: `EffectEffect.java:277-306` copies colors/cards/player/direction/type/number, never sector |
| Cleared               | Never. Java has no clearing call; no `Cleanup` param names it                                                               |
| Per-creature `sector` | Not added. That is Java's separate `Card.sector` (`assignSector`, `Card.java:2335-2344`), written by CR 704.5u's SBA        |

### New `PlayerController` method

```go
ChooseSector(g *Game, decider PlayerID, assignee CardID, sectors []string) int
```

Java's `chooseSector(Card assignee, String ai, List<String> sectors)` (`PlayerController.java:253`). Returns an index
into `sectors`, like `ChooseProtectionType`/`ChooseOption`; the caller range-checks it. `assignee` is `NoCard` from the
effect (Java passes `null`); it keeps Java's shape for the second caller, CR 704.5u's SBA (`GameAction.java:1815`),
which passes the creature being assigned. `AILogic` is not passed: Crucible's controllers take no AI hints (no
`AIController` yet). `ScriptedController` gains `QueueSector(i int)`, panicking when dry like every other queue.

Why a dedicated method rather than `ChooseOption`: the SBA caller needs `assignee`, which `ChooseOption`'s `source`
parameter would conflate with the ability's host; and a sector is a fixed three-way pick Java names separately, so an AI
can answer it without parsing option strings.

### Space Beleren is still not functional

The write happens; nothing reads it yet. Missing, all out of this API's scope:

| Piece                           | Java                                                                  | Crucible today                                                             |
| ------------------------------- | --------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| Per-creature sector             | `Card.sector`, `assignSector` (`Card.java:2335-2344`)                 | no field                                                                   |
| CR 704.5u sector assignment SBA | `stateBasedAction704_5u` (`GameAction.java:1801-1828`)                | no rule in `CheckStateBasedActions`                                        |
| `Creature.ChosenSector`         | source's chosen sector vs card's sector (`CardProperty.java:119-122`) | no case in `valid.go`: falls through to a type check, false for every card |
| `Creature.DifferentSector`      | `CardProperty.java:123-126`                                           | `blockerRelativeMatches` (`staticability.go`) skips the static             |

**Both real corpus lines are rejected, not silently resolved.** Without a guard, registering `ChooseSector` would have
let both lines resolve: the -1's `DB$ PutCounterAll | ValidCards$ Creature.ChosenSector` would put its counter on
nothing, and the -5's `DB$ DestroyAll | ValidCards$ Creature.ChosenSector` would destroy nothing, with no error --
`valid.go`'s general unknown-property fallthrough, invisible to `ChooseSector` itself since the property sits on the
sub-ability, not on this API's own params. `chooseSectorReadUnbuilt` (`choosesectoreffect.go`) checks the `SubAbility$`
chain for exactly this before asking anything, so both lines now fail with `not resolvable yet` instead -- cost already
paid (loyalty is spent before `Resolve` runs, same as any other ability whose resolve later errors), sector never asked
or recorded. `TestSpaceBelerenMinusOneRejectsWhileSectorReadIsNotPorted` and
`TestSpaceBelerenMinusFiveRejectsWhileSectorReadIsNotPorted` (`sectorchoice_test.go`) pin this and must flip -- back to
resolving, for real this time -- when the read side lands. The +1 is unchanged: its `Effect` static is skipped by
`blockerRelativeMatches`.

Tests: `sectorchoice_test.go`, all three picks, decider is the host's controller not the activator, options and
`assignee`, last pick wins, `Ultimate$`/`AILogic$` accepted, out-of-range answer, `Condition$` rejected, a failing
`ConditionCheckSVar$`, `Game.Clone` independence, and both of the real card's real lines rejected end to end. | CR
704.5u sector assignment SBA | `stateBasedAction704_5u` (`GameAction.java:1801-1825`) | no rule in
`CheckStateBasedActions` | | `Creature.ChosenSector` | source's chosen sector vs card's sector
(`CardProperty.java:119-122`) | no case in `valid.go`: falls through to a type check, false for every card | |
`Creature.DifferentSector` | `CardProperty.java:123-127` | `blockerRelativeMatches` (`staticability.go`) skips the
static |

**Registering `ChooseSector` changes how the card fails.** Before, both lines stopped at `ErrUnimplemented`. Now the
chain resolves: the -1's `DB$ PutCounterAll | ValidCards$ Creature.ChosenSector` puts its counter on nothing, and the
-5's `DB$ DestroyAll | ValidCards$ Creature.ChosenSector` destroys nothing, with no error. That silent no-op is
`valid.go`'s general unknown-property behavior, not something this effect can see: the property sits on the sub-ability,
not on `ChooseSector`. `TestSpaceBelerenMinusOneRecordsSectorButItsReadIsNotPorted` (`sectorchoice_test.go`) pins it and
must flip when the read side lands. The +1 is unchanged: its `Effect` static is skipped by `blockerRelativeMatches`.

Tests: `sectorchoice_test.go`, all three picks, decider is the host's controller not the activator, options and
`assignee`, last pick wins, `Ultimate$`/`AILogic$` accepted, out-of-range answer, `Condition$` rejected, a failing
`ConditionCheckSVar$`, `Game.Clone` independence, and the real card's -1 activated end to end.

No scenario fixture: no combat, SBA, layer or trigger behavior changes.

## Forge bugs found

None. `ChooseSectorEffect.java` does what its name says (PORT-8's bar).
