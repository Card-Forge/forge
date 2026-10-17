# Cast leftovers: Revolt, crimes, random amounts, Devour, Emerge, alternative costs (R9d)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `trigger.go` (`revoltMatches`),
  `entercounters.go`, `replacement.go`, `stack.go` (`isCrime`), `triggerpack.go` (`crimeTriggers`),
  `castspell.go`/`trigger.go`/`copyspellabilityeffect.go` (crime triggers after each push), `amountbranches.go`
  (`randomCountValue`, `Emerged`, `AltCost`), `devour.go`, `sacrificeeffect.go`, `replacemententry.go`, `emerge.go`,
  `altcost.go`, `castoptions.go`, `unlesscost.go`, `amountcontext.go`, `amountheads.go`, `valid.go`, `card.go`,
  `game.go`; [`internal/keyword`](../../../../../crucible/internal/keyword) — `expand.go` (`expandDevour`)
- **Java:** `CardTraitBase.meetsCommonRequirements` (`CardTraitBase.java:323-330`); `Player.commitCrime`
  (`Player.java:3962-3968`), `MagicStack.add` (`MagicStack.java:408-451`), `TriggerCommitCrime.java:17-28`;
  `AbilityUtils.xCount` (`:2782-2787` Random, `:2194-2197` Devoured, `:2061-2063` Emerged, `:2064-2066` AltCost),
  `calculateAmount` (`:509-510` `Emerged$`); `CardFactoryUtil.java:2211-2238` (Devour), `:2808-2821` (Emerge);
  `Devour.java:32-51`; `SacrificeEffect.java:138`, `:162-169`; `Card.java:1186-1194`, `:1226-1231`;
  `CardProperty.java:1823-1826`; `TriggerDevoured.java:56-68`; `CostAdjustment.java:247-249`, `:404-422`;
  `PlaySpellAbility.java:484-489`, `:548-553`; `StaticAbilityAlternativeCost.java:19-116`; `CostExile.java:172-269`

## What refused

BEFORE `card-smoke -all -workers 8`: 30,670 plays, 2,864 refused. The R9d rows of the open plan, by card:

| Shape                                           | Cards | Refusal before                                                                                       |
| ----------------------------------------------- | ----- | ---------------------------------------------------------------------------------------------------- |
| `Count$Devoured <valid>`                        | 4     | Marrow Chomper, Skullmulcher, Tar Fiend, Voracious Dragon: `"<Api>: <Param>$ "_" is not resolvable"` |
| `Count$Madness`                                 | 2     | Avacyn's Judgment, From Under the Floorboards                                                        |
| `Count$Emerged`                                 | 1     | Adipose Offspring                                                                                    |
| `Count$AltCost`                                 | 1     | Bringer of Green Zenith's Twilight                                                                   |
| `Count$Random.<min>.<max>`                      | 2     | Ashuza's Breath, Rainbow Knights                                                                     |
| `Count$CastTotalManaSpent Cave/Desert/Treasure` | 3     | Bat Colony, Cataclysmic Prospecting, Spoils of the Hunt                                              |

Silent, so invisible to the probe: `Revolt$` on 25 trigger and replacement lines (skipped as unmet), the 21
`Mode$ CommitCrime` triggers (never fired), the Devour keyword on 24 cards (inert), Emerge on 15 (never offered), the
`Mode$ AlternativeCost` statics (never offered).

## What landed

| Piece                   | Java                                                       | Engine                                                                                                                                                                                                                                                                 |
| ----------------------- | ---------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Revolt$`               | `CardTraitBase.java:323-330`                               | `revoltMatches` in `triggerCommonRequirementsMet`: True/False against `hasRevolt(host controller)`, None needs no player with revolt (Sarevok). Triggers and replacements share it: etbCounter extras (Greenwheel Liberator), Aether Revolt's damage line              |
| `Mode$ CommitCrime`     | `Player.java:3966-3967`, `TriggerCommitCrime`              | `isCrime` (the target test `noteCrime` counts) and `crimeTriggers`: `ValidPlayer$` against the criminal, appended after the push's BecomesTarget and Ward batch at every push site; `TriggeredPlayer` is the criminal                                                  |
| `Count$Random`          | `AbilityUtils.java:2782-2787`                              | `randomCountValue`: both bounds are amounts, one `nextInt(1+max-min)+min` draw from the game's generator per read; max below min is unresolved                                                                                                                         |
| Devour                  | `CardFactoryUtil.java:2211-2238`                           | `keyword.Expand`: an `Event$ Moved` Updated line whose chain is Sacrifice (`Optional$`, `Amount$` all valid, `SacValid$ <type>.Other`) → PutCounter `ETB$` (`Count$RememberedSize/Times.<N>`) → Cleanup; types and `X` (Thromok) read                                  |
| Devour, the choice      | `SacrificeEffect.java:138`                                 | The Devour sacrifice asks `ChooseCardsForEffect` for 0 to Amount$ of the candidates after the confirm; the answer is checked, a wrong one an error                                                                                                                     |
| Devour, before the move | `ReplacementHandler.run` before `changeZone`               | `entryUpdatedShape` accepts the Devour chain, so it runs in `entryReplaced` while the devourer is still on the stack; its counters wait on `pendingEnter` and join the entry's counter table                                                                           |
| Devoured record         | `Card.addDevoured`, `:1186-1194`                           | `Card.devoured`, appended per sacrifice by `noteDevoured`; ended on leaving the battlefield (and on an entry that did not land); `Count$Devoured <valid>` (LKI), property `HasDevoured` (Hellkite Hatchling)                                                           |
| `Mode$ Devoured`        | `TriggerDevoured.java:56-68`                               | `checkDevouredTriggers`: `ValidDevoured$` against the devoured LKI; `TriggeredDevoured$` amount head (Kresh)                                                                                                                                                           |
| Emerge                  | `CardFactoryUtil.java:2808-2821`, `CostAdjustment:404-422` | A cast option from the hand while something can be sacrificed; `payCastCost` asks `ChoosePermanentsToSacrifice` for one, takes its mana value off the generic part after the cost statics, sacrifices it once the mana is paid and records it (`castOptional.emerged`) |
| Emerge reads            | `AbilityUtils.java:509-510`, `:2061-2063`                  | `Count$Emerged.<a>.<b>`, `Emerged$<property>` (the sacrificed permanent as last known)                                                                                                                                                                                 |
| `Mode$ AlternativeCost` | `StaticAbilityAlternativeCost.java:19-116`                 | `altCostOptions`: the card's own `ValidSA$ Spell.Self` lines (or `Spell` with `ValidCard$ Card.Self`), conditions through `staticConditionsMet`, `Cost$` through `parseUnlessCost`: its mana replaces the mana cost, the rest is the cast's extra cost                 |
| `Count$AltCost`         | `:84-86`, `AbilityUtils.java:2064-2066`                    | `optAltCost` in the cast record; Bringer of Green Zenith's Twilight's "X is 5"                                                                                                                                                                                         |
| Exile cost parts        | `Cost.java:445-455`, `CostExile.java:172-269`              | `parseUnlessCost` reads `ExileFromHand<N/Type>` (the source never among them) and `Exile<N/Type>` (permanents the payer controls); both the cost's "Exiled" list. Lunar Hatchling's escape and Force of Will's pitch use them                                          |

## Design decisions

| Decision                                                                          | Why                                                                                                                                                                                                                                                                        |
| --------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Crime triggers computed at each push site, not fired from `PushAbility`           | Firing inside `PushAbility` would put them on the stack before the spell's own SpellCast and BecomesTarget triggers, and recurse inside `pushTriggeredAbilities`. The site already holds the controller and the BecomesTarget batch                                        |
| No pending-crime state                                                            | `isCrime` is a pure test of the pushed item; called again at the site, before anything moves, it answers what `noteCrime` counted                                                                                                                                          |
| `Count$Random` ported despite the oracle's draw count                             | Java also draws on the AI's and the stack description's reads, which this engine never makes, so the stream leaves the oracle's at the first such read; every random effect here already draws from the same generator. A refusal would hide two cards that play correctly |
| Devour runs before the move                                                       | Java's replacement runs before `changeZone`: the devourer is not on the battlefield while its food dies, so its own "another creature dies" triggers stay quiet. The existing pre-entry Other layer is that seam                                                           |
| Devour's "any number" through `ChooseCardsForEffect`, not a new controller method | `ChoosePermanentsToSacrifice` is exactly-N with three implementers; `ChooseCardsForEffect` is the existing lo..hi card pick, checked by the caller                                                                                                                         |
| Emerge and alternative costs only from the hand                                   | Sneak's precedent: Java's alternative spell has no zone of its own (`EffectZone$ All`), so a MayPlay grant could offer it from elsewhere; no corpus card combines the two today                                                                                            |
| Self-hosted alternative costs only                                                | `OptionalCost.AltCost` is recorded only for the host's own static (`:84-86`); the statics that grant a cost to other spells or abilities (Fist of Suns, the equip/cycling/crew ones) are a separate grant mechanism                                                        |

## Limits

| Limit                                                                                                                                                                                                       | Effect                                                                                                                     |
| ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| Emerge's reduction applies after every cost static; Java applies Trinisphere's set cost after it                                                                                                            | A Trinisphere-and-Emerge cast pays the set cost minus the reduction; no corpus card meets both                             |
| Alternative-cost lines with `XAlternative$`, `Announce$`, `ManaRestriction$`, `Named$`, `CostDesc$`, `ConvertedManaCost` or a cost `parseUnlessCost` does not read (`Discard<1/Mountain>`, `GainLife<...>`) | Not offered, never cast at a guessed price                                                                                 |
| `CheckSecondSVar$` on a static is unresolved (`unresolvedStaticConditions`)                                                                                                                                 | The Commander-free "opponent controls a Forest and you control a Swamp" alternative costs are not offered                  |
| Crime check sites: a spell cast, an ability pushed by `pushTriggeredAbilities`, a copy                                                                                                                      | `ChangeTargets` (no `MagicStack.add`) commits no crime, as in Java                                                         |
| A permanent sacrificed while a spell is cast (Emerge, an alternative cost's `Sac<...>`) has its dies triggers pushed at once, under the spell; Java's frozen stack puts them above it                       | Inherited from Bargain's and every additional cost's sacrifice; a dies trigger resolves before the spell rather than after |
| Devour's `Optional$` choice is 0 to Amount$; every other `Optional$`Sacrifice still sacrifices exactly min(Amount$, candidates) once confirmed (Java: 0 to Amount$,`SacrificeEffect.java:138`)              | Differs only for an optional "sacrifice N" with N above 1                                                                  |

## Not resolved, by name

| Shape                                            | Cards                             | Reason                                                                                                                                                                                                                                                                                                                     |
| ------------------------------------------------ | --------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Count$Madness` and the Madness keyword          | 2 (+ every Madness card silently) | Needs three unported pieces: the discard-to-exile `Event$ Moved` replacement with `Discard$ True` (`CardFactoryUtil.java:2345-2362`), the `Mode$ Exiled` trigger from the hand, and Play's `PlayCost$` (`playeffect.go` refuses it). Reading "not cast for madness" would turn the cards to plays while their text is dead |
| `Count$CastTotalManaSpent Cave/Desert/Treasure`  | 3                                 | `Pool` counts mana by color and snow-ness; Java's `ManaPool` holds `Mana` objects with their source card and the payer picks which unit pays which shard. Matching needs per-unit provenance in `Pool` and a payment-choice contract: an ADR first (ADRP-4)                                                                |
| Escape with `ExileFromGrave<X/...+withTypesGE4>` | 1                                 | Nethergoyf: an X announced for a non-mana part of a permanent's alternative cost and a test on the chosen set as a whole (`CostExile.java:215-228`); the escape stays unoffered                                                                                                                                            |
| Rainbow Knights                                  | 1                                 | Its `K:ETBReplacement:Other:ChooseColor` now runs as it enters and refuses on `ChooseColor`'s `Random$` ([`m6-r9d-cast-remainder.md`](m6-r9d-cast-remainder.md))                                                                                                                                                           |

## New engine state

| State                                                                     | Why                                           | Clone / cleanup                                                                                                             |
| ------------------------------------------------------------------------- | --------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| `Card.devoured []CardID`                                                  | `Card.devouredCards`                          | Copied in `Game.Clone`; replaced, never appended in place; nil on leaving the battlefield and on an entry that did not land |
| `castOptional.emerged CardID`                                             | `SpellAbility.getSacrificedAsEmerge`          | Value; cleared with `castOpt`                                                                                               |
| `optAltCost`                                                              | `OptionalCost.AltCost`                        | A bit of `castOptional.kinds`                                                                                               |
| `triggeredObjects.devoured CardID`                                        | `AbilityKey.Devoured`                         | Value                                                                                                                       |
| `castOption.emerge`/`altStatic`/`altExtra`, `castOpts.emerge`/`altStatic` | How the cast was chosen                       | Per cast                                                                                                                    |
| `unlessCost.exileHandN/Spec`, `exileFieldN/Spec`                          | `CostExile` from the hand and the battlefield | Per cost                                                                                                                    |

No new `PlayerController` method: Emerge asks `ChooseOption` and `ChoosePermanentsToSacrifice`, Devour `ConfirmEffect`
and `ChooseCardsForEffect`, the alternative costs `ChooseOption` and the questions their parts already ask.

## Forge script defects (PORT-8)

| Site                                      | Defect                                                                                                                                                                                                                                                                     |
| ----------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `bringer_of_green_zeniths_twilight.txt:6` | `K:etbCounter:P1P1:Y:no condition:...` writes `no condition` in lower case; `makeEtbCounter` compares `"no Condition"` case-sensitively (`CardFactoryUtil.java:557`), so the text becomes an extra replacement param with an empty value. Harmless: nothing reads that key |

## Smoke probe

`crucible card-smoke -all -workers 8 -list`, before → after:

| Status       | Before | After  |
| ------------ | ------ | ------ |
| plays        | 30,670 | 30,688 |
| refused      | 2,864  | 2,848  |
| placed       | 373    | 374    |
| inconclusive | 171    | 168    |
| judged play  | 91.5%  | 91.5%  |

18 → plays. Refused → plays (15): Adipose Offspring, Ashuza's Breath, Bringer of Green Zenith's Twilight, Marrow
Chomper, Rainbow Knights, Skullmulcher, Tar Fiend, Voracious Dragon (the R9d heads); Baleful, Devastating, Fervent,
Ingenious and Verdant Mastery and Graveyard Dig (their `Branch`/`TargetMax$` read `Count$AltCost`); Forsaken City (its
triggered `AB$ Cost$ ExileFromHand<1/Card>`). Inconclusive → plays (3): Fear of Abduction, Necrotic Fumes, Soul Exchange
(an additional `Exile<1/Creature>` cost). Dalek Intensive Care went refused → placed (its upkeep cost
`Mandatory Exile<1/Creature.nonDalek>` reads now). No plays → refused.

## Tests

`revolt_test.go` (Greenwheel Liberator's etbCounter, Aether Revolt's damage line), `trigger_test.go`
(`TestPlayLandRevoltFlagReadsTheTurnsDepartures`: True/False/None both ways), `crimetriggers_test.go` (once per turn,
`ValidPlayer$` You/Opponent, a graveyard host, a triggered ability's crime), `randomamount_test.go` (bounds, seed
determinism, SVar bounds, operator, bad shapes, Ashuza's Breath), `devour_test.go` (any number including none, the
multiplier and `Count$Devoured Card.Goblin`, `HasDevoured` and its end, Kresh's `Mode$ Devoured`, a bad answer),
`emerge_test.go` (reduction, record, no fodder, reduction floor, a bad sacrifice), `alternativecost_test.go` (Bringer's
record, Fireblast's sacrifice, Snuff Out's condition, Force of Will's pitch), `exilecosts_test.go` (Lunar Hatchling's
escape, an `ExileFromHand` unless cost), `keyword/expand_test.go` (Devour). Scenarios
`devour-skullmulcher-draws-for-each-creature-devoured`,
`revolt-fireblast-sacrifice-gives-countless-gears-renegade-a-servo`,
`emerge-adipose-offspring-makes-aliens-equal-to-the-sacrificed-toughness`.
