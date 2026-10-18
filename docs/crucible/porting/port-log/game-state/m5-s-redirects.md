# M5 batch S: search control, ignore-effect costs, targeting leftovers, Pump on players

## Search control and Leonin Arbiter (ADR-0040)

| Shape                                        | Port                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| -------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ControlOpponentsSearchingLibrary$`          | `rulesEffect` reads it into `RulesEffect.SearchControl`; `Game.SearchController(pid)` returns the newest controller and the static's timestamp. `changeZoneHidden` pushes a control grant keyed by that timestamp (`addControlGrantAt`, sorted insert) around the search choice only (`ChangeZoneEffect.java:1070-1076`, `:1268`), so `Game.ControllingPlayer` answers the Agent's controller inside `ChooseCardsForEffect`.                                                                                                                                                    |
| `canSearchLibraryWith` (`CantSearchLibrary`) | A decider with the keyword is offered nothing from the library; the shuffle follows `ShuffleNonMandatory$` (`ChangeZoneEffect.java:1018-1039`). `shuffleMandatory` is now per fetcher (`hiddenChoice.shuffleMandatory`), as Java declares it. `NoLooking$` skips the search rules. The `Spells and abilities you control can't cause you to search your library.` keyword is read too.                                                                                                                                                                                          |
| `IgnoreEffectCost$`                          | `compile` attaches an `InternalIgnoreEffect` activated ability to the static (`ignoreEffectSub`, golden AST regenerated for the corpus lines). `grantIgnoreEffect` grants it to the host each Layer 8 pass; any player the static affects may activate it (`ignoreActivatorValid`); it uses the stack. Resolving records the activator in `Game.ignores`; `playerIgnores` drops them from the static's player-facing passes (`StaticAbilityContinuous.java:1023`). Cleared at cleanup; keyed by the host's `zoneStamp`, so a host that left play and came back is a new object. |

`InternalIgnoreEffect` is a registered effect now (190 of 203 script-driven APIs).

Opposition Agent's `FoundSearchingLibrary$` replacement and `QuasiLibrarySearch$` on `ChooseCard` are ported
([`m5-t-replace-search.md`](m5-t-replace-search.md)). Not ported: `IgnoreEffectCost$` on a static whose `Mode$` is not
`Continuous` (Lost in Thought's `Continuous,CantAttack,CantBlock,CantBeActivated`, Damping Engine): the ability is built
but only Continuous statics read the ignore set. The cards of an affected card's controller (Java adds them to the valid
activators) are not modelled.

Tests: `searchcontrol_test.go` (the redirect inside the choice, own and hand searches untouched, a newer Mindslaver
grant wins), `ignoreeffect_test.go`. Fixtures: `leonin-arbiter-blocks-library-search`,
`leonin-arbiter-pay-two-to-ignore-and-search`. A scenario cannot observe the controller, so Opposition Agent has module
tests only.

## `ExchangeControl` targeting params

`TargetsWithSharedCardType$`, `TargetsWithSharedTypes$`, `TargetsWithRelatedProperty$`, `TargetsWithDefinedController$`
and `TargetingPlayer$` were already enforced when targets are chosen (`targeting.go`); `ExchangeControl` no longer
refuses them. `definedPlayers` reads `Non<spec>` (`AbilityUtils.java:1104-1106`, `NonTriggeredCardController`).
Fixtures: `exchange-control-confusion-in-the-ranks-swaps-same-type`,
`exchange-control-confusion-in-the-ranks-no-shared-type-no-swap`.

## Costs

| Shape                           | Port                                                                                                                                                                                                                                  |
| ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `UnlessUpTo$` on `DefinedCost_` | `definedUnlessCost` asks the ability's controller `ChooseNumber(0..N)` for the `Minus<N>` reduction (`AbilityUtils.java:1462-1464`). Ward passes no controller and still refuses it.                                                  |
| `MayFlashCost:Behold<N/Type>`   | `parseUnlessCost` reads `Behold` as a `Reveal` over the hand and the battlefield (`CostBehold`); out of timing `castFromHand` offers it through `ConfirmPayCost` and pays it as the cast's extra cost. The cast spell is not counted. |
| `MayFlashSac`                   | Java sacrifices by card id too (`SacrificeAllEffect.java:66`, `GameEntity.equals` is id only): a Forge bug (PORT-8). Reproduced, not worked around.                                                                                   |

Tests: `unlesscostdefined_test.go`, `castflashinfo_test.go`.

`CastWithFlash` `ValidSA$ Spell.Teamwork` (Quantum Reduction) and Cryptex's `T CollectEvidence<3>` activation cost
resolve; see [`m6-r10-r10.md`](m6-r10-r10.md).

## Pump on a player (`pumpeffect.go`)

`PumpEffect.java:498-504`: `KW$` goes onto each targeted player or the players `Defined$` names (`Defined$ You`). A
`pumpRecord` with `OnPlayer` carries it; `pumpLayerKeywords` adds it to the player's `KeywordMod`. `Defined$ You` no
longer fails the card lookup. `Duration$ UntilYourNextTurn` works for players and cards (`HasUntil`, ended as that
player's turn begins by `endPumpsUntilTurnOf`). `DefinedKW$` fills `Protection:Player.PlayerUID_ChosenPlayerUID:...`
(Eon Frolicker, Noble Heritage, Guardian Archon). Tests: `pumpplayer_test.go`; no fixture, `ChosenPlayer` needs a state
key.

## Sneak

Not ported. The keyword's cast path (return an unblocked attacker, cast in the declare-blockers step, enters attacking)
is an alternative cost plus an entering-attacking placement; about 15 cards (TMNT) carry `K:Sneak`.
