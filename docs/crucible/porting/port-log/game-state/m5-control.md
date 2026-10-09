# Port Log — Game State: M5 Control Change (batch F)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine)

`GainControl` `LoseControl$`/`AddKWs$`, Java's per-card and per-phase command lists, Soulbond pairing, and the delayed
`Mode$ ChangesController`/`ChangesZone` lines of Ray of Command, Magus of the Unseen, Stolen Uniform and Seraph.

## Commands as data (`controlcommands.go`)

Java registers `GameCommand` closures. A closure is code in game state, so each is a `cardCommand` record (`ADR-0030`'s
data-only shape, `scheduledaction.go`): `commandLoseControl` (`ControlGainEffect.getLoseControlCommand`) and
`commandEndEffect` (an Effect card's `addUntilCommand` closure).

| Java                                                              | Go                                                                                      |
| ----------------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| `Card.add/runLeavesPlayCommands` (`Card.java:3598`)               | `Card.leavesPlayCmds`, `runLeavesPlayCommands`, run from `effectCardsSeeMove`           |
| `Card.add/runUntapCommands` (`Card.java:4724`)                    | `Card.untapCmds`, `runUntapCommands` at every untap site (below)                        |
| `Card.add/runChangeControllerCommands` (`Card.java:3628`)         | `Card.changeControllerCmds`, run and cleared by `correctControllerZone` before move     |
| `Card.add/runPhaseOutCommands` (`Card.java:5650`)                 | `Card.phaseOutCmds`, run in `switchPhaseState` before the permanent phases out          |
| `EndOfTurn.addUntil`, `executeUntil` (`PhaseHandler.java:406`)    | `Game.endOfTurnCmds`, `runEndOfTurnCommands` in `cleanupStep`                           |
| `EndOfCombat.addUntil`, `executeUntil` (`PhaseHandler.java:1262`) | `Game.endOfCombatCmds`, `runEndOfCombatCommands` in `endCombat`                         |
| `addUntilEnd`/`registerUntilEnd` (`Phase.java`)                   | `Game.endOfNextTurnCmds` with `Armed`, same semantics as `effectUntilEndOfYourNextTurn` |

Untap sites that run `untapCmds` (Java funnels all through `Card.untap`): `untapStep`, `untapeffect.go`,
`untapalleffect.go`, `taporuntapeffect.go`, `taporuntapalleffect.go`, `gaincontroleffect.go`. `PhasesEffect`'s untap
flag sets `Tapped` directly in Java too and runs none.

Commands that fire where no `PlayerController` is at hand (a zone change, a phase out, end of combat) pass `nil`: a
lose-control command then removes the temp controller and leaves `correctControllerZone` to the next state-based pass,
which Java runs too (`GameAction.java:1205-1207`). Only the moment the permanent changes battlefield lists differs.

Leaving the battlefield also clears the untap, change-controller and phase-out lists: Java's zone change builds a new
`Card` with empty ones (CR 400.7).

## `GainControl` (`gaincontroleffect.go`)

| Java (`ControlGainEffect.resolve`)                                                                                         | Go                                                                     |
| -------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| `LoseControl$` split on `,`                                                                                                | `parseLoseControl`; unknown or refused token is an error (GO-7)        |
| early returns for `LeavesPlay`, `LoseControl`, `Untap` (`:127-135`)                                                        | same checks before the target loop                                     |
| `canBeControlledBy`: `isInGame`, phased-out skip                                                                           | `Lost` and `IsPhasedOut` skips; `CantGainControl` static not evaluated |
| `addTempController(newController, tStamp)` then untap, keywords, remember, commands, then `controllerChangeZoneCorrection` | same order                                                             |
| `AddKWs$` split on `&`, keywords until end of turn                                                                         | non-permanent `pumpRecord` at the same timestamp                       |
| `LeavesPlay` registered only when host differs from target                                                                 | same                                                                   |

Registered tokens: `EOT` (127 corpus lines), `LeavesPlay`/`LoseControl` combinations (31), `Untap` combinations (9),
`UntilTheEndOfYourNextTurn` (5), `EndOfCombat` (1). `StaticCommandCheck` and `UntilSourceUnattached` are ported in
[`m5-control-2.md`](m5-control-2.md). Skipped: the `SacMe` SVar (`ControlGainEffect.java:185`, an AI hint) and
`Card.gainControlTargets` (read only by the optional-untap prompt's default, `Untap.java:180`).

## `Duration$` on Effect (`effecteffect.go`)

`AsLongAsControl` and `UntilLoseControlOfHost` register a `commandEndEffect` on the host's leaves-play and
change-controller lists, `AsLongAsControl` on its phase-out list too (`SpellAbilityEffect.java:1008-1017`), after
`checkValidDuration` (host in play or on the stack, controller is the activator, not phased out for `AsLongAsControl`).
Corpus: `AsLongAsControl` 17 `Effect`, 6 `Pump`, 3 `Animate`, 1 `Goad`; `UntilLoseControlOfHost` 0. `Pump`, `Animate`
and `Goad` still refuse it (their own lifetime tables).

## Soulbond (`soulbond.go`)

| Java                                                       | Go                                                                   |
| ---------------------------------------------------------- | -------------------------------------------------------------------- |
| `Card.pairedWith`, `isPaired` (`Card.java:1390`)           | `Card.pairedWith`, `IsPaired`, `PairedWith`                          |
| `BondEffect` (`DB$ Bond`)                                  | `bondEffect`; optional pick is `ChooseCardsForEffect(lo 0, hi 1)`    |
| keyword's two triggers (`CardFactoryUtil.java:1740-1760`)  | `keyword/expand.go` `Soulbond` case; `Defined$ TriggeredCardLKICopy` |
| unpair on leaving (`GameAction.java:622-628`)              | `unpairOnLeave` from both `Move` paths                               |
| unpair on controller change (`GameAction.java:1015-1020`)  | `unpair` in `correctControllerZone`                                  |
| unpair in the state check (`GameAction.java:1209-1215`)    | `unpairInvalid` after the layer pass in `checkStateBasedActions`     |
| unpair on phasing out (`Card.java:5654`)                   | `unpair` in `switchPhaseState`                                       |
| `Paired`/`PairedWith` properties (`CardProperty.java:564`) | `valid.go`                                                           |

Pinned quirk (PORT-7): leaving the battlefield frees the partner, but a token keeps its own `pairedWith`
(`!c.isRealToken()`); a token ceases to exist, so only last-known information shows it.

Not ported: `BondEffect`'s `equalsWithGameTimestamp` guard (an ability does not record the `zoneStamp` of a `Defined$`
triggered card, only of targets). Java's `GameState` text format has no pairing key, so fixtures observe the pair
through Wolfir Silverheart's +4/+4.

## Delayed lines, end to end

| Card                | Needed                                                                                                                                                                                                       |
| ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Ray of Command      | `LoseControl$ EOT`, `AddKWs$`; the EOT command runs inside cleanup, the delayed trigger gets CR 514.3a priority                                                                                              |
| Magus of the Unseen | same, as an activated ability                                                                                                                                                                                |
| Stolen Uniform      | `IsPresent$` on a delayed trigger (`delayedPresentMatches`), `Pump` `RememberObjects$`, `Attach` `Object$`, targets of a sub-ability (below)                                                                 |
| Seraph              | `Creature.DamagedBy` (`Damage.Sources`), `Defined$ TriggeredCard*`, delayed `Mode$ ChangesZone` from the battlefield with `ValidCard$ Card.StrictlySelf` and `Destination$ Any`, `Cleanup` `ClearTriggered$` |
| Krovikan Vampire    | landed, see [`m5-control-2.md`](m5-control-2.md)                                                                                                                                                             |

`delayedPresentMatches` reads `IsPresent$` once, as the trigger fires (Java checks again as it resolves), with
`Card.IsTriggerRemembered` applied against the trigger's `RememberObjects$` list. `PresentCompare$`/`PresentZone$`/
`PresentPlayer$`/`PresentDefined$`/`IsPresent2$` on a delayed `ChangesController` line are refused. `isPresentMatches`
returns true for a spec naming `IsTriggerRemembered`, since the delayed matcher owns it.

A delayed `ChangesZone` watch fires for every destination ([`m5-control-2.md`](m5-control-2.md)).

### Targets of a sub-ability (`chaintargets.go`)

`resolveSubAbility` used to give a sub-ability with its own `ValidTgts$` its parent's targets, so Stolen Uniform's
"target Equipment" was never chosen (891 corpus lines). `resolveChainTargets` now chooses every such link's targets in
chain order as the head's are, from `castInstantOrSorcery` and `pushTriggeredAbilities`; `resolveSubAbility` hands each
link its own. A link with no legal target keeps the whole ability off the stack (CR 601.2c). The CR 608.2b re-check of
link targets is in [`m5-control-2.md`](m5-control-2.md); Charm modes' and copies' links in
[`m5-t-sneak-charm.md`](m5-t-sneak-charm.md).

### Krovikan Vampire

Not landed. Its three `Static$ True` `ChangesZone` triggers (`Pump RememberObjects$ TriggeredCard`, `ForgetObjects$`,
`Cleanup`), the `Mode$ TurnBegin` static trigger, `CheckSVar$ X` over `Remembered$Amount`, `ChangeZoneAll`
`ChangeType$ Creature.IsRemembered+ThisTurnEnteredFrom_Battlefield` and `ForgetOtherRemembered$` are all unported
(`changesZoneResolvable` refuses `Static$`). The `Pump` `RememberObjects$`/`ForgetObjects$` and `TriggeredCard` halves
this batch added are the first links of that chain.

## Fixtures

`control-act-of-treason-*` (2), `control-mind-flayer-*` (3), `control-ray-of-command-*` (2),
`control-magus-of-the-unseen-*` (2), `control-stolen-uniform-*` (2), `control-seraph-*` (2), `soulbond-*` (4), under
`crucible/testdata/scenarios/`. The Act of Treason, Ray of Command and Magus returns run the EOT command through the
real cleanup step.
