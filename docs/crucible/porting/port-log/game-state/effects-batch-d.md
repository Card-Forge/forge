# Effects batch D: AdvanceCrank and ClaimThePrize land

Two script-driven `ApiType`s resolve, 160 of the corpus's 203. Corpus lines: AdvanceCrank 1
(`clock_of_doooooooooooom.txt`), ClaimThePrize 1 (`pick_a_beeble.txt`). Four more APIs in this batch's assignment
(RunChaos 1, RollPlanarDice 1, RestartGame 1, Camouflage 1) were researched and deferred — table below.

## AdvanceCrank

`advancecrankeffect.go` ports `AdvanceCrankEffect.java`'s resolve and `Player.advanceCrankCounter`
(`Player.java:4014-4023`) in full for the one real corpus line: each `Defined$`-or-targeted player (default the
activator) advances their own CRANK! counter to the next sprocket (1, 2, 3, then back to 1 — Java's own
`crankCounter % 3 + 1`, an exact port), finds every Contraption they control dialed to that sprocket
(`CardPredicates.isContraptionOnSprocket` — `Card.Sprocket` (`assemblecontraptioneffect.go`, batch A) is 0 for every
non-Contraption and every Contraption not yet assembled, so a bare `Sprocket == sprocket` scan already excludes them
with no separate type check needed), lets the controller choose any number of them to crank (reusing
`PlayerController.ChooseCardsForEffect`'s existing `lo 0`/`hi len(options)` shape rather than adding a
`ChooseContraptionsToCrank` method — its `(lo, hi int) []CardID` contract already fits, `control.go` untouched), then
fires CR's own `Mode$ CrankContraption` trigger once per chosen Contraption.

New engine state: `Player.CrankCounter int` (`player.go`), Java's own field, defaulted to 3 at player creation
(`NewGame`, `game.go`) the same way Java's own field initializer is — the first `AdvanceCrank` ever resolved moves it to

1. A plain `int`, copied for free by `Game.Clone`'s existing whole-struct `Player` value copy
   (`append([]Player(nil), g.players...)`), the same free ride `Card.Sprocket` already gets.

New trigger dispatch: `checkCrankContraptionTriggers`/`isCrankContraptionTrigger` (`advancecrankeffect.go`) — CR's own
"whenever you crank CARDNAME" trigger, `checkExertedTriggers`'s own exact structural sibling (`exertcost.go`): a single
unified battlefield walk matching `ValidCard$` against the cranked card, which every one of the 45 real corpus
`T:Mode$ CrankContraption` lines names as `Card.Self` alone (`widget_contraption.txt` et al.), so no other param is
read.

Not ported: `Player.setCrankCounter`'s own `contraptionSprocketEffect` (`Player.java:4024-4038`) — a synthetic
Command-zone "Contraption Sprockets" display card whose only job is holding overlay text for the UI (Java's own
`setOverlayText`/`updateStateForView`), with no rules effect of its own. Nothing reads it, so this port does not build
it.

## ClaimThePrize

`claimtheprizeeffect.go` ports `ClaimThePrizeEffect.java`'s resolve for each `Defined$` (default `Self`) card: run CR's
own Mode$ ClaimPrize trigger once (`TriggerHandler.runTrigger(TriggerType.ClaimPrize, ...)`, Java's own).

`pick_a_beeble.txt`'s own real line also names `ConditionDefined$`/`ConditionPresent$`; `ConditionDefined$` is rejected,
not silently gated as this file originally said.

`isPresentMatches` (called from `subAbilityConditionMet`) treats a defined condition as never met and returns false
silently. That would make the corpus's only real line resolve to an invisible no-op instead of running (GO-7).

Found porting `SetInMotion`, fixed the same way every other effect naming `ConditionDefined$` already is.

New trigger dispatch: `checkClaimPrizeTriggers`/`isClaimPrizeTrigger` (`claimtheprizeeffect.go`) —
`checkExertedTriggers`'s own exact structural sibling again: a single unified battlefield walk covers both a claimed
Attraction's own `ClaimPrize` ability (`ValidCard$ Card.Self`, `pick_a_beeble.txt`'s own `K:Prize` keyword expansion,
not itself compiled by this port yet — see below) and another permanent's own watching trigger
(`ValidCard$ Attraction.YouCtrl`, `the_most_dangerous_gamer.txt`'s real corpus line) in the one pass.

Not modeled: the `Prize` keyword itself (`K:Prize:TrigPrize`, `pick_a_beeble.txt` line 5) — this port's `carddb/compile`
does not expand it into the literal `T:Mode$ ClaimPrize | ValidCard$ Card.Self | ...` trigger Java's own keyword table
generates, so an Attraction relying on the keyword form rather than a literal `T:` line
(`the_most_dangerous_gamer.txt`'s own shape) does not fire yet. `ClaimThePrize` itself — the `DB$` ability
pick_a_beeble.txt's own `Visit` ability chains into — is unaffected: it is a literal `SVar:` line, already compiled, and
this file's own test suite exercises the trigger side directly with a hand-written `T:Mode$ ClaimPrize` line standing in
for the keyword's own expansion.

## Researched and deferred

| API              | Blocker                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| ---------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `RunChaos`       | The one real corpus line (`pools_of_becoming.txt`, a Plane card) fires from a `Mode$ ChaosEnsues` trigger and reads `Defined$ Remembered` off a plane's own `PlanarDeck`-sourced reveal. Same blocker `Planeswalk` was already deferred for (batch A): `game.getActivePlanes() != nil` gates every reachable Java branch, this port has no active-planes state at all (`effecthelpers.go:99` excludes `PlanarDeck` from movable zones), and `ChaosEnsues` itself is a new trigger mode with no `check*Triggers` walk — owned by a parallel porter's batch this run, not built here either way                                                                                                                  |
| `RollPlanarDice` | `RollPlanarDiceEffect.java:25`: `if (game.getActivePlanes() == null) return;` — a no-op outside Planechase, real only inside it. Identical blocker to `Planeswalk`/`RunChaos`: no active-planes state exists to make the real branch (`PlanarDice.roll`, a game-mode die this port's `pkg/javarand` wrapper has no equivalent for either) ever reachable                                                                                                                                                                                                                                                                                                                                                       |
| `RestartGame`    | Karn Liberated's ultimate (`karn_liberated.txt`'s own one real line). `RestartGameEffect.java` re-initializes essentially every `Game`/`Player` field at once — trigger suppression during the reset, `PhaseHandler.restart()`, every player's library rebuilt and reshuffled from their own battlefield/hand/graveyard/exile, `GameStage.RestartedByKarn` — a re-entrant "start a new game inside this one" mechanic this port's turn/mulligan machinery was never built to re-run mid-game. A partial port (reset some fields, not others) would silently diverge from Java rather than erroring, which PORT-8 rules out; this needs its own ADR-scale design, not a routine effect port                     |
| `Camouflage`     | `camouflage.txt`'s own real line is `SP$ Effect \| ReplacementEffects$ RDeclareBlocker`: casting it creates a persistent Command-zone effect substituting the entire declare-blockers step (`R:Event$ DeclareBlocker \| ReplaceWith$ DBCamouflage`) for the rest of the turn. This port's `replacement.go` dispatches `Event$ Moved`/`Untap`/`DamageDone`/`Draw`/`GainLife` only — there is no `DeclareBlocker` replacement event, and no hook in `block.go`'s own declare-blockers step to consult one even if there were. Building that is a new replacement-event category plus a change to `block.go`'s documented declare-blockers contract, not a routine state change over pieces this port already has |

No Forge bugs found researching this batch's ported or deferred APIs.
