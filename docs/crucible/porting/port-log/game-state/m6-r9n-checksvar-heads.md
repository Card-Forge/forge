# Port Log — Game State: turn and game records behind `CheckSVar$` heads, `TriggeredDefender`/`DefendingPlayer`, loud trigger and sub-ability conditions (R9n)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `amountrecords.go` (new: `recordCountValue`,
  `castAdamant`, `castManaConditionsMet`, `checkDefinedPlayerMatches`, `werewolfConditionsMet`, `cardsInGame`),
  `amountheads.go`, `amountbranches.go` (`IsPrime`, `StartingPlayer`), `amountcontext.go` (`CastSA>` Adamant,
  `ChromaSource`), `amountpaid.go` (`ValidAll`), `amountplayers.go` (`LifeLostLastTurn`), `castrecord.go`
  (`leftGraveyardThisTurn`, `enteredCount`), `condition.go`, `presentcheck.go`, `trigger.go`
  (`triggerCommonRequirementsMet`, `lifeTotalMatches`), `defined.go` (`Registered`, `ActivePlayer`, `DefendingPlayer`,
  `triggeredDefender`), `valid.go` (`isMonarch`, `hasInitiative`, `ControlledBy` fallback), `turn.go`, `game.go`,
  `player.go`, `card.go`, `manapay.go`, `castspell.go`, `scryeffect.go`, `restartgameeffect.go`, `entercounters.go`;
  `internal/smoke/timing.go`
- **Java:** `AbilityUtils.java:1110-1111`, `:1129-1130`, `:1171-1172`, `:1616-1622`, `:1795-1800`, `:1951-1979`,
  `:2001-2020`, `:2147-2161`, `:2219-2221`, `:2280-2282`, `:2312-2342`, `:2377-2379`, `:2410-2416`, `:2543-2547`,
  `:2703-2720`, `:2754-2756`, `:2819-2831`, `:2846-2863`, `:3595-3596`; `CardTraitBase.java:355-401`, `:458-520`;
  `SpellAbilityCondition.java:169-237`, `:286-298`, `:342-429`, `:476-507`; `Player.java:822-831`, `:2313`,
  `:2467-2498`; `PlayerProperty.java:85-92`; `CardProperty.java:310-317`; `MagicStack.java:961-979`;
  `PhaseHandler.java:362`, `:418`; `GameAction.java:539-552`, `:2395`, `:2684`; `Game.java:128`, `:298-313`, `:761-789`;
  `Combat.java:452-463`; `CardFactoryUtil.java:318-346`; `RestartGameEffect.java:60-74`

## Records added

Each mirrors the Java owner, is copied by `Game.Clone` and resets where Java resets it.

| Record                                    | Java                                                               | Set / rolled                                                       | Read by                                                    |
| ----------------------------------------- | ------------------------------------------------------------------ | ------------------------------------------------------------------ | ---------------------------------------------------------- |
| `Game.startingPlayer`                     | `Game.startingPlayer` (`GameAction.java:2395`)                     | `StartTurn`; `SetTurnState` on turn 1; `SetStartingPlayer`         | `Count$StartingPlayer` (unset is unresolved, never parity) |
| `Game.castLastTurn`                       | `MagicStack.lastTurnCast` (`:965-979`)                             | `castThisTurn` moved over as each turn starts                      | Werewolf conditions                                        |
| `Game.leftGraveyardThisTurn`              | `Game.leftGraveyardThisTurn`, pre-move LKI (`GameAction.java:551`) | `recordEntered`, `from == Graveyard`; cleared at cleanup           | `Count$LeftGraveyardThisTurn`                              |
| `Game.enteredLastTurn`                    | `Zone.cardsAddedLastTurn`                                          | `enteredThisTurn` moved over at cleanup                            | `Count$LastTurnEntered_…`                                  |
| `Game.endOfTurnsThisTurn`                 | `PhaseHandler.nEndOfTurnsThisTurn` (`:362`)                        | end step begun; reset with the turn                                | `Count$FinishedEndOfTurnsThisTurn`                         |
| `Player.lifeLostLastTurn`/`drawnLastTurn` | `Player.java:2474`, `:2493`                                        | cleanup rolls this turn's over; RestartGame as Java (`:63-67`)     | `LifeLostLastTurn`, `Count$YouDrewLastTurn`                |
| `Player.scryThisTurn`                     | `Player.scryThisTurn` (`GameAction.java:2684`)                     | each scrying player after its trigger; cleanup                     | `Count$YouScryThisTurn`                                    |
| `Player.spellsCastThisGame`               | `Player.java:2313`                                                 | `recordSpellCast`; RestartGame                                     | `Count$YouCastThisGame`                                    |
| `Card.castColorless`                      | `getPayingMana` `{C}` units                                        | `payManaCostX` (kept apart from `castPaid`: Converge reads colors) | `Adamant…Colorless`, `EachSpentToCast.C`                   |

## Heads

| Head                                                                                                                                            | Reads                                                                                                      |
| ----------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| `Count$StartingPlayer.<then>.<else>`                                                                                                            | `startingPlayer` against the source controller                                                             |
| `Count$LeftGraveyardThisTurn <valid>`                                                                                                           | `leftGraveyardThisTurn`                                                                                    |
| `CastSA>Count$Adamant[_N].<color>.<then>.<else>`                                                                                                | host's cast record (`castAdamant`), number branches; `Colorless` reads `castColorless` here and unprefixed |
| `Count$EachSpentToCast.<W/U/B/R/G/C>`                                                                                                           | the cast's units of that mana, 0 for a card not cast (Java's bare `return 0`)                              |
| `Count$MostCardName[<zones>] <valid>`                                                                                                           | largest same-name group; Java skips `doXMath` (`:2856`), no corpus line writes an operator                 |
| `Count$MostProminentCreatureType <valid>`                                                                                                       | largest creature-type group; a changeling's line holds every type, the same total as Java's tally          |
| `LifeLostLastTurn`, `YouDrewLastTurn`, `YouScryThisTurn`, `YouSurveilThisTurn`, `YouCastThisGame`, `LifeYourTeamGainedThisTurn` (no teams: own) | player records                                                                                             |
| `Count$TotalTurns`, `Count$FinishedEndOfTurnsThisTurn`                                                                                          | game turn; end steps begun less the one in progress                                                        |
| `Count$MaxCombatDamageThisTurn`, `Count$MaxOppDamageThisTurn`                                                                                   | `Player.java:822-831`, read before the `DamageThisTurn` branch as Java does                                |
| `Count$IsPrime <amount>.<then>.<else>`                                                                                                          | Guava `isPrime`; a negative amount (Guava throws) unresolved                                               |
| `Count$ValidAll <valid>`                                                                                                                        | `cardsInGame` (`Game.java:761-789`: zones per player, phased-out included, then stack)                     |
| `Count$RememberedWithSharedCardType`                                                                                                            | largest remembered group sharing a core type, at least 1                                                   |
| `Count$LastTurnEntered_<zone>[_from_<zone>]_<valid>`                                                                                            | `enteredLastTurn` through `thisTurnEnteredCount`'s parser                                                  |
| `<list>$ChromaSource`                                                                                                                           | each card's colored symbols (`handlePaid` per card)                                                        |
| `HasPropertyisMonarch` / `hasInitiative`                                                                                                        | `Game.monarch` / `Game.initiative` (`PlayerProperty.java:85-92`)                                           |
| `PlayerCountDefinedRegistered$…`, `…DefinedActivePlayer$…`                                                                                      | `Defined$ Registered` (lost players kept, Java does not filter) and `ActivePlayer`                         |

## `Defined$` and `ControlledBy`

| Spelling                   | Does                                                                                                              |
| -------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| `TriggeredDefender`        | the recorded attacker's attack target, player or card (Kaalia); no recorded attacker is an error                  |
| `DefendingPlayer`          | `getDefendingPlayerRelatedTo`: defender of the host or what it is attached to; outside combat an error (Java NPE) |
| `Valid <spec>` in entities | `definedEntities` scans the game (`definedCardsInGame`), Zara's `Valid Planeswalker.ControlledBy DefendingPlayer` |
| `ControlledBy <spec>`      | a spec `matchesPlayerSpec` cannot read falls back to `definedPlayers` (`CardProperty.java:310-317`)               |

`TriggeredDefender` reads the live combat, as `TriggeredDefendingPlayer` already does; Java stores the trigger-time
defender. Same divergence, same row below.

## Conditions read, the rest loud (GO-7)

| Gate                                      | Now reads                                                                                                                                                                                                                                                                                               | Loud (pending error)                                                                                                                                                                            |
| ----------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `triggerCommonRequirementsMet`            | `WerewolfTransformCondition$`/`Untransform…`, `ManaSpent$`/`ManaNotSpent$`/`Adamant$`, `CheckDefinedPlayer$`/`DefinedPlayerCompare$`, `EnduringStory$`, `LifeTotal$ OpponentSmallest/OpponentGreatest`, `DayTime$` any case, other values pass (Java)                                                   | `ClassLevel$` (no level tracked), unreadable `LifeTotal$`/`LifeAmount$`/compare                                                                                                                 |
| `subAbilityConditionMet`                  | `ConditionPresent2$` beside a first, `ConditionPlayerContains$`, `ConditionLifeTotal$` (`X` or a number, as Java), `ConditionManaSpent$`/`NotSpent$`, `ConditionChosenColor$`, `ConditionNoDifferentColors$`, `OrOtherConditionSVarCompare$`/`OrConditionCheckSVar$`; `ConditionZone$` was already read | `Condition$` values `conditionValueRead` lacks, `ConditionActivationLimit$`, `ConditionGameTypes$`, `ConditionSorcerySpeed$`, `ConditionTargetValidTargeting$`, `ConditionTargetsSingleTarget$` |
| `presentCountMatches`, `isPresentMatches` | —                                                                                                                                                                                                                                                                                                       | short or unresolvable compare, unknown zone name                                                                                                                                                |

`ETB$ PutCounter` replacements (`entercounters.go`) now accept the condition params `triggerCommonRequirementsMet` reads
(Ardenvale Paladin, Henge Walker: `Adamant$`), and `DamageDone` replacements (`damageReplacementMatches`)
`CheckDefinedPlayer$`/`DefinedPlayerCompare$` (Jared Carthalion's "while you're the monarch").

## Smoke probe

`card-smoke -all -workers 8 -list`, same binary flags before and after:

| Measure      | Before | After  |
| ------------ | ------ | ------ |
| plays        | 30,770 | 30,831 |
| refused      | 2,848  | 2,785  |
| placed       | 372    | 376    |
| inconclusive | 88     | 86     |
| hung         | 0      | 0      |
| judged play  | 91.5%  | 91.7%  |

Per-card lists: scratchpad `a984-smoke-before.txt`, `a984-smoke-after.txt`, diff `a984-smoke-diff.txt`.

| Flip                 | Cards | Why                                                                                                                                                                                                                                                                                                                                    |
| -------------------- | ----- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| refused → plays      | 66    | the heads above (47 of the 73 `CheckSVar` refusals; 3 more are placed, Omnath moved on to `ManaReflected`) plus `Defined$` `Valid …`/`Registered`/`ActivePlayer`/`DefendingPlayer` in `definedEntities` callers (Bane of Bala Ged, Energybending, Ogre Marauder, Seraphic Greatsword, …)                                               |
| refused → placed     | 4     | Mine Is the Only Truth, Necropotence Avatar, You Cannot Hide from Me, You Will Know True Suffering                                                                                                                                                                                                                                     |
| inconclusive → plays | 2     | Searing Blaze, The Fall of Kroog                                                                                                                                                                                                                                                                                                       |
| plays → refused      | 7     | `ControlledBy TriggeredTarget/TriggeredDefendingPlayer` targets now exist and reach unported params (Etrata, Fear of Falling, Mjölnir, Sensational Spider-Man, Tinybones); `CheckDefinedPlayer$ You.isMonarch` now passes (Tamiyo, Upriser Crowned: `Tap` `Defined$`); `ConditionLifeTotal$ TargetedOrController` (Pulse of the Forge) |

Each plays → refused card silently did nothing before: no legal target, or an unread condition reading false.

## Deferred, with the reason

| Head / shape                                                                                     | Cards                                                                | Why not here                                                                                    |
| ------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| `Count$CountersRemovedThisTurn`                                                                  | Churning Reservoir, Izzet Generatorium                               | ~15 counter-removal sites (costs, effects, damage, SBA) with no single choke point to record at |
| `Count$ThisTurnActivated_`, `Count$ActivatedThisGame`                                            | Kiora, The Chain Veil, Laboratory Drudge, Telekinesis                | needs an activation log matched by a SpellAbility valid string                                  |
| `Count$TotalManaSpent <valid>` on an activated ability                                           | Forsworn Paladin                                                     | activated abilities' paying mana is not recorded                                                |
| `SpellTargeted$CardManaCostLKI`, `Targeted$CastTotalManaSpent`, `TargetedByTarget$`              | Reject Imperfection, Sound the Trumpets, Unravel, Bane's Contingency | targeted-spell LKI heads                                                                        |
| `CastSA>Count$xPaid` for `Sac<X/…>`                                                              | Plumb the Forbidden                                                  | the cast does not record a cost's X as its `castX`                                              |
| `Count$SearchedLibrary`, `DamageToOppsThisTurn`, `RegeneratedThisTurn`, `PlaneswalkedToThisTurn` | Archive Trap, Antagonism, Spiny Starfish, Unyaro                     | one record each with no other reader                                                            |
| `PromisedGift`, `Doubles`, `RepeatCheckSVar$`                                                    | Coiling Rebirth, Celebr-8000, Mana Clash, Sword of D&D               | Gift casting, RollDice doubles, Repeat's own gate                                               |
| inline compare operand, `NonTargetedController`, `HighestSpeed`                                  | Spikeshell Harrier                                                   | compile keys `GT<expr>` whole; needs a compile change for one card                              |
| `TriggeredAttackedTargetAndYou`                                                                  | Undercellar Sweep                                                    | one card                                                                                        |

## Decisions

| Decision                                                                      | Why                                                                                                           |
| ----------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| Unset starting player is unresolved, not inferred from turn parity            | extra turns break parity; a turn-1 state or `StartTurn` always knows it                                       |
| `SetStartingPlayer` state injection; the smoke probe's turn-2 walk sets it    | the probe plays the opponent's turn 2 "as if the probe player had gone first"                                 |
| Colorless kept apart from `castPaid`                                          | `Count$Converge` counts `castPaid` entries as colors                                                          |
| Werewolf spells count for the caster recorded on the stack                    | Java counts `lastTurnCast` cards by controller; the stack object's controller is the caster                   |
| Port the cheap trigger and sub-ability conditions before making the rest loud | Werewolf (32 cards), `ConditionManaSpent$`/`NotSpent$` (16) and the rest would otherwise flip plays → refused |
| No new `PlayerController` method, no ADR                                      | every piece mirrors a Java record or reads existing state                                                     |

## Forge defects

| Where                                | Defect                                                                                                         |
| ------------------------------------ | -------------------------------------------------------------------------------------------------------------- |
| `AbilityUtils.java:1129-1130`        | `Defined$ DefendingPlayer` calls `game.getCombat()` unguarded: outside combat it throws. Refused here          |
| `SpellAbilityCondition.java:420-424` | `ConditionLifeAmount$` right side: only `X` is an SVar; any other name throws `NumberFormatException`. Refused |

## Engine findings left open

| Where                                               | Finding                                                                                                                                                |
| --------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `RememberTargets$` (most effects)                   | `AbilityUtils.java:1517`'s generic pre-resolve remember is not ported and most effects do not reject it: Sylvan Smite's `Remembered$CardPower` reads 0 |
| `defined.go` `TriggeredDefender`/`…DefendingPlayer` | read the current combat, not the trigger-time value Java stores                                                                                        |
| `turn.go` `upkeepsThisTurn`                         | reset as the next turn starts, not at cleanup (`PhaseHandler.java:415`); `endOfTurnsThisTurn` follows Java                                             |
| `CardTraitBase` `Class` levels                      | `ClassLevel$` is loud; every real T: line is on `Mode$ ClassLevelGained`, which never fires (no level-up)                                              |

## Tests

`turnrecordheads_test.go` (each head, malformed shapes, cleanup roll-over, clone, `ValidAll`, shared card type,
`ChromaSource`, `MostProminentCreatureType` on the corpus DB), `conditionsread_test.go` (cast mana,
`CheckDefinedPlayer$`, `LifeTotal$`/`EnduringStory$`/`DayTime$`/`ClassLevel$`, Werewolf both ways, sub-ability
conditions read and loud, `Registered`/`ActivePlayer`/`DefendingPlayer`, `ControlledBy` fallback, `TriggeredDefender`
error). Scenarios `checksvar-cindercone-smite-treasure-only-for-the-non-starting-player`,
`trigger-kaalia-puts-an-angel-attacking-the-defender`,
`werewolf-gatstaf-shepherd-transforms-after-a-turn-without-spells`.
