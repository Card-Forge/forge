# M5 batch J: flash costs, counter placement, unless costs, targeting params, Pump DefinedKW

## Casting out of timing

| Shape                                                 | Port                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| ----------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `K:MayFlashCost:<mana>`                               | `castFromHand` (`castspell.go`): out of timing, `ConfirmPayCost` offers the keyword's mana (`GameActionUtil.java:514-517`); a yes adds it to the cost (`castOpts.flashCost`, `withExtraMana`). Asked only when the cast would otherwise be refused; Java lists the optional cost on every cast. A non-mana cost (`Behold<1/Dragon>`, Molten Exhale, 1 line) is not offered. Rout, Harbinger of the Tides.                             |
| `K:MayFlashSac`                                       | `castOptions` (`castoptions.go`) adds a flash option when the caster controls the card and could not cast a sorcery (`MayPlayNotSorcerySpeed$`, `GameActionUtil.java:343`); `sameWay` keeps it apart from the plain cast. A cast through it registers a `Phase$ Cleanup` delayed `SacrificeAll` of the card (`sacrificeAtCleanup`, the tree `CardFactoryUtil.java:2025-2038` builds from strings). Spider Climb, Necromancy, Parapet. |
| `CastWithFlash` `ValidSA$` with `XCost`/`IsTargeting` | `flashMode` (`staticability.go`): such a line lets the cast begin out of timing (`anyWithFlashNeedsInfo`), and `castsWithFlashChosen` decides once targets and X are chosen (`flashDecided`, called by every cast branch). `saspec.go` reads `XCost<cmp><n>` (`saXCost`) and `IsTargeting Valid <spec>` (`saIsTargeting`). Silver Scrutiny, Flash Photography, Timely Ward.                                                           |

Not ported: `ValidSA$ Spell.Teamwork` (Quantum Reduction): no Teamwork optional cost exists. A `MayPlayText$` on a
`MayPlay$` static is display text only (`GameActionUtil.java:398`); the old refusal in `continuous.go` is unchanged.

Fixtures: `flash-may-flash-cost-*` (2), `flash-may-flash-sac-spider-climb-on-opponents-turn`,
`flash-xcost-silver-scrutiny-x-two-at-instant-speed`. Module tests: `castflashinfo_test.go` (X over the limit, targets,
cleanup sacrifice only when cast out of timing).

## `Mode$ CantPutCounter`

The counters a token enters with (`createToken`, Incubate), `MakeCard`'s `WithCounter$` and a planeswalker's or Battle's
printed loyalty and defense now go through `countersReplaced` (AddCounter replacements and the static) or
`cantPutCounter` (`Game.Move`, no controller there). Java puts all of them through the counter table's
`replaceCounterEffect`. `K:etbCounter` and Saga lore counters already did. Tests: `cantputcounterenter_test.go`; no
fixture, no short real card combines the static with these placements.

## `UnlessCost$`

| Shape                                        | Port                                                                                                                                                                                                                                  |
| -------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `DefinedCost_<Defined>[_Minus<N>\|_Plus<N>]` | `definedUnlessCost` (`unlesscost.go`, `AbilityUtils.java:1450-1471`): the first named card's mana cost, generic part changed; no card resolves the ability without asking (`errUnlessNoCost`). `UnlessUpTo$` fails closed.            |
| `UnlessPayer$` absent                        | defaults to `TargetedController` (`AbilityUtils.java:1407`); no targeted card, no payer.                                                                                                                                              |
| `PutCardToLibFromGrave<N/Pos/Type>`          | the payer picks N cards from their graveyard, put on top (`0`) or bottom (`-1`) through `moveByEffect`.                                                                                                                               |
| `RemoveAnyCounter<N/Kind/Type>`              | one counter at a time: the payer picks a permanent with one, and a kind when it has several (`removeAnyCounters`).                                                                                                                    |
| `Mode$ CollectEvidence`                      | `checkCollectEvidenceTriggers` fires after the cost's cards are exiled (`CostCollectEvidence.java:78`), `ValidPlayer$` against the payer. Only the unless-cost payment fires it; an activation or spell cost paying it is not ported. |

Tests: `unlesscostdefined_test.go`. No fixture: Essence Leak and Disruption Aura need an `AddTrigger$` static, Perplex a
full `ValidTgts$`/`UnlessSwitched$` chain.

## Targeting parameters (`targeting.go`)

| Param                                                             | Port                                                                                                                                                                                                                        |
| ----------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `TargetsForEachPlayer$` (68)                                      | `trimTargetSet` reads it as `TargetsWithDifferentControllers$`; `recordForEachControllers` and `targetStillLegal` make a target that changed controller illegal at resolution.                                              |
| `TargetsWithSameCreatureType$`, `TargetsWithoutSameCreatureType$` | `sharesCreatureType` over the creature types in the registry; a Changeling already carries every type (`applyChangelings`).                                                                                                 |
| `TargetingPlayer$`, `TargetingPlayerControls$`                    | `targetingPlayerOf`: the one player named chooses (`ChooseTargets` with that decider) and only their own cards are candidates. Zero or several players: pushed with no targets, fails when it resolves (`targetsErr`, R9f). |
| `TargetsWithControllerProperty$`                                  | `cmcLECardsInGraveyard`, `powerLECardsInGraveyard` against the target's own controller's graveyard.                                                                                                                         |
| `TargetsWithSharedCardType$`, `TargetsWithSharedTypes$`           | `candidateRestrictionsMet`: the target shares a card type (or a listed one) with every card the Defined text names.                                                                                                         |
| `TargetsWithRelatedProperty$`                                     | `LEPower`/`LECMC` against the first card an ancestor targeted; a root ability has none, so nothing qualifies.                                                                                                               |

`GainControl` and `TapOrUntap` no longer refuse the params above. `ExchangeControl` takes them too
([`m5-s-redirects.md`](m5-s-redirects.md)). `TargetsWithSharedCardType$ ParentTarget` and `TargetsWithRelatedProperty$`
sit on sub-abilities, which are never targeted separately (`subability.go`), so only the `TriggeredCard`/`Remembered`
forms are reachable.

Fixtures: `target-no-shared-creature-type-fight`, `target-no-shared-creature-type-elves-not-cast`. Module tests:
`targetingrestrictions2_test.go` (the other params, the fizzle check, the decider).

## Pump `KW$` with `DefinedKW$`

`pumpDefinedKeywords` (`pumpeffect.go`, `PumpEffect.java:318-371`): `ChosenType`, `ChosenPlayer`, `ChosenColor` read the
host's choice; any other text is a player spec and each keyword naming `ChosenPlayerUID`/`ChosenPlayerName` becomes one
per player. A card target gets the keywords, so Courageous Resolve and Cliffside Rescuer protect a permanent from the
players named (test `pumpdefinedkw_test.go`).

Eon Frolicker, Noble Heritage and Guardian Archon's second ability pump a player (`Defined$ You`): ported through
`pumpRecord.OnPlayer` ([`m5-s-redirects.md`](m5-s-redirects.md)). `ChosenPlayer` needs a `GameState` key that sets a
chosen player, so no fixture.
