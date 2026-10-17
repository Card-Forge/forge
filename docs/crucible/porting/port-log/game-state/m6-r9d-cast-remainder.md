# Cast remainder: Madness, as-enters choices, cast-time triggers, Optional$ Sacrifice, Nethergoyf escape (R9d)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `replacementdiscard.go` (new),
  `madnesstrigger.go` (new), `replacemententry.go`, `leftto.go`, `trigger.go`, `castspell.go`, `game.go`, `stack.go`,
  `playeffect.go`, `changezoneeffect.go`, `defined.go`, `amountbranches.go`, `card.go`, `discardeffect.go`, `turn.go`,
  `unlesscost.go`, `sacrificeeffect.go`; [`internal/keyword`](../../../../../crucible/internal/keyword) — `expand.go`
  (`expandMadness`); [`internal/fixture`](../../../../../crucible/internal/fixture) — `actions.go`
  (`queue discardchoice`)
- **Java:** `CardFactoryUtil.java:1386-1420` (Madness trigger), `:2345-2362` (Madness replacement), `:515-543`
  (`createETBReplacement`); `ReplaceMoved.java:51`, `:79-90`; `Player.java:1424-1455` (`discard`);
  `TriggerExiled.java:59-97`; `PlayEffect.java:369-386`, `:420-422`; `AbilityUtils.java:1716-1718` (`Count$Madness`),
  `:3955-3963` (`countCardTypesFromList`); `ChangeZoneEffect.java:784-786` (`TrackDiscarded$`);
  `SacrificeEffect.java:137-148`; `CostExile.java:215-228`; `HumanCostDecision.java:323-336`;
  `PlaySpellAbility.java:682`, `:766-778`; `TriggerHandler.java:264`

## What refused

BEFORE `card-smoke -all -workers 8 -list`: 30,688 plays, 2,848 refused, 91.5% of 33,536 judged. Of this row's cards,
Avacyn's Judgment and From Under the Floorboards refused (`Count$Madness`); everything else was a silent miss:

| Gap                                    | Cards | Silent behavior before                                                                                  |
| -------------------------------------- | ----- | ------------------------------------------------------------------------------------------------------- |
| Madness                                | 62    | The keyword was inert: a discarded madness card went to the graveyard                                   |
| `K:ETBReplacement:Other` past counters | ~300  | No entry path ran a `Choose…`/`NameCard`/`GenericChoice`/… chain: Cavern of Souls, Iona, Pithing Needle |
| `Optional$` Sacrifice (not Devour)     | 109   | Exactly min(Amount$, candidates) once confirmed, where Java allows 0 to Amount$                         |
| Cast-time cost triggers                | all   | A dies/discard trigger a cost fired went under the spell                                                |
| Nethergoyf's escape                    | 1     | Not offered (`parseUnlessCost` refused the `X` part)                                                    |

## What landed

| Piece                        | Java                                                         | Engine                                                                                                                                                                                                                                                                                                    |
| ---------------------------- | ------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Optional$` Sacrifice        | `SacrificeEffect.java:138`, `:148`                           | Optional$ without StrictAmount$: confirm, then `ChooseCardsForEffect(0..Amount$)`, checked; with StrictAmount$ or not optional, `ChoosePermanentsToSacrifice` of exactly Amount$ as before. Exploit and Devour share the path                                                                             |
| As-enters choices            | `CardFactoryUtil.java:515-543`, ADR-0022                     | `entryUpdatedShape` admits a chain of `entryChainAPIs` (the Choose family, `NameCard`, `GenericChoice`, `StoreSVar`, `Cleanup`, effects on other objects, an `ETB$` PutCounter or one on `Defined$ ChosenCard`, a `DelayedTrigger`) and runs it before the move; the card keeps its Memory through `Move` |
| As-enters refusals           | GO-7                                                         | An Other-layer `ReplacementResult$ Updated` entry line that applies and that no path runs (`AlterAttribute`, `Animate`, `RollDice`+`Animate`, `SetState`, `Attach`, a chain with any of those) is a pending error; Tap/Untap/DayTime and `ETB$` PutCounter stay with the after-move paths                 |
| Cast-time trigger hold       | `PlaySpellAbility.java:682`, `TriggerHandler.java:264`       | `pushTriggeredAbilities` holds every non-static match while `holdingCastExits` (`holdCastTriggers`); `castExitMatches` releases them, in firing order with the graveyard exits, into the SpellCast batch after the spell is pushed, and ends the hold; a cast that fails after a cost fired pushes them   |
| Madness replacement          | `CardFactoryUtil.java:2345-2362`, `ReplaceMoved.java:79-90`  | `keyword.Expand` writes the `Event$ Moved \| ActiveZones$ Hand \| Discard$ True` line; `discardToGraveyard` (every discard site) walks the card's own hand lines and every battlefield/command host's, exiles the card for the madness shape                                                              |
| Madness trigger              | `CardFactoryUtil.java:1386-1420`, `TriggerExiled.java:87-94` | `checkMadnessTriggers` fires the card's own `Mode$ Exiled` trigger of the same keyword line; its Play casts the card for `PlayCost$ <cost>` (`castOpts.madness`), else the ChangeZone puts it into the graveyard                                                                                          |
| `PlayCost$`                  | `PlayEffect.java:369-386`                                    | Plain mana replaces the mana cost (`playCost`); `ManaCost`, `SuspendCost`, `ConvertedManaCost` amounts and non-mana parts are refused                                                                                                                                                                     |
| `Count$Madness`              | `AbilityUtils.java:1716-1718`                                | `castOptional.madness`, read through `spellOptional`                                                                                                                                                                                                                                                      |
| Nethergoyf's escape          | `CostExile.java:215-228`, `HumanCostDecision.java:323-336`   | `escapeTypesPart` lifts `ExileFromGrave<X/Type+withTypesGE<N>>` out of the Escape cost (`exileTypesN`): payable when the valid cards' core types reach N; the payer picks one or more with N or more types, a short pick replaced by every valid card                                                     |
| `Defined$ Self.StrictlySelf` | `CardFactoryUtil.java:1403`                                  | The host, as `StrictlySelf` already reads in `valid.go`                                                                                                                                                                                                                                                   |

## Design decisions

| Decision                                                                       | Why                                                                                                                                                                                                                                                                                                     |
| ------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Hold all cast-time triggers in the existing `castExits` list                   | Java holds the whole waiting list while the stack is frozen; the graveyard-exit hold was the same seam. One ordered list (`castHeld`) keeps exits and other triggers in firing order. Activated abilities' costs still push at once (not this row)                                                      |
| A spell's trigger stays where it is after `castExitMatches`                    | castAura and the instant branch push BecomesTarget/Ward/crime matches after the SpellCast batch; ending the hold at release keeps them as they were                                                                                                                                                     |
| As-enters chains admitted by API allow-list, not by "everything registered"    | An effect that writes onto the entering card (AlterAttribute's prepared flag, Animate's P/T) would be rebuilt by `Move`; running it before the move would lose it silently. Those stay refused until the entry carries them, like `pendingEnter` for counters                                           |
| Madness trigger fired by the replacement, not by a general `Mode$ Exiled` scan | TriggerExiled admits the madness trigger only for its own keyword's cause; the corpus's other `Mode$ Exiled` lines (Soulherder, Market Gnome) are battlefield exiles this port does not fire yet                                                                                                        |
| `TrackDiscarded$` accepted with no flag                                        | Its only readers, `DiscardedThisTurn` (`CardProperty.java:987-993`) and Mayhem (`GameActionUtil.java:134`), are not ported; whoever ports them adds the discarded flag here and at the discard sites                                                                                                    |
| Nethergoyf: a pick short of four card types exiles every valid card            | Divergence: Java's human decision returns null and the cast is cancelled (`HumanCostDecision.java:331-334`). Here the extra cost is paid after the mana (`payUnlessParts`), with no rollback, so the pick is replaced as `collectEvidence` does. The smoke controller's minimal pick hits it every time |
| Nethergoyf's X not announced                                                   | Java announces it (`PlaySpellAbility.java:766-778`) but the human payment ignores it and nothing else reads Nethergoyf's X; asking would be a decision with no effect                                                                                                                                   |
| A Madness replacement and a graveyard replacement together refuse              | CR 616.1 has the player order them (Rest in Peace or Madness first); the discard walk does not model that choice                                                                                                                                                                                        |
| A triggered ability does not move its card off the stack                       | Madness's trigger casts its own card: the card is then that spell, waiting on the stack (`moveResolvedSpellToGraveyard`)                                                                                                                                                                                |

## Refused (GO-7)

| Shape                                                                                                | Error                                                            |
| ---------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------- |
| Discard replacements other than Madness's (Loxodon Smiter, Library of Leng, Nephalia Academy)        | `Event$ Moved Discard$ shape not resolvable yet`                 |
| A discard line with `EffectOnly$`/`ValidCause$` at a site that does not know the cause (costs)       | `the discard's cause is not known here`                          |
| Two discard lines, or a discard line beside a graveyard replacement                                  | `CR 616.1 order not resolvable yet`                              |
| `ChooseColor`'s `Random$` (Rainbow Knights, Gem Bazaar), `ChooseNumber`'s `Random$`/`Secretly$`      | the effect's own `not resolvable yet`, via the entry chain       |
| A `GenericChoice` whose choices hold a `Pump` (the Sieges), an AlterAttribute/Animate/RollDice chain | `as-it-enters replacement ReplaceWith$ <Api> not resolvable yet` |

## Not done

`CastTotalManaSpent` Cave/Desert/Treasure (Bat Colony, Cataclysmic Prospecting, Spoils of the Hunt): the mana pool keeps
no source card per unit; needs an ADR first (ADRP-4).

## Tests

Module: `optionalsacrifice_test.go`, `asenterschoice_test.go`, `casttimetriggers_test.go`, `nethergoyfescape_test.go`,
`madness_test.go`, `discardreplacement_test.go`. Scenarios: `madness-discarded-as-a-cost-is-cast-before-the-spell`,
`sacrifice-cost-dies-trigger-resolves-before-the-spell`, `as-enters-choose-color-iona-stops-the-chosen-color`; Story
Circle's `cast-an-enchantment-spell-resolves-to-battlefield` now queues its color.

## Measured

AFTER: 30,608 plays, 2,929 refused, 91.3% of 33,537 judged. 3 refused → plays (Avacyn's Judgment, From Under the
Floorboards, Blue Mage's Cane's `PlayCost$ 3`). 83 plays → refused and 1 placed → refused (Elvish Impersonation
Contest), every one an as-enters chain that never ran before and is now reported: 39 `AlterAttribute` (prepared), 16
`GenericChoice` chains whose choices hold a `Pump` (the Sieges' `SetChosenMode$` modes, Archangel of Strife), the
Random/Secretly choices, `RollDice`+`Animate`, `Reveal`, `RemoveCounter`, `Sacrifice`+`Animate`, `SetState`, `Attach`,
`Draft`, `ChangeZone` imprint.
