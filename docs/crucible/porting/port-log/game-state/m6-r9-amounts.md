# Port Log — Game State: unresolved amount expressions (R9)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `amount.go`, `amountbranches.go`,
  `amountplayers.go`, `amountpaid.go` (`handlePaid`), `amountcontext.go` (`listHeadCards`), `amountheads.go`;
  [`internal/expr`](../../../../../crucible/internal/expr) (`Count.Compare`, `Op.Expr`);
  [`internal/carddb/compile`](../../../../../crucible/internal/carddb/compile) (`addInlineChecks`)
- **Java:** `AbilityUtils.java` — `calculateAmount` `:370-730`, `xCount` `:1569-2895`, `doXMath` `:3273-3340`,
  `playerXCount` `:3355-3487`, `playerXProperty` `:3489-3701`, `handlePaid` `:3742-3851`, `getCardListForXCount`
  `:3853-3944`; `Expressions.compare` (`forge/util/Expressions.java:45-65`); `SpellAbility.getSATargetingPlayer`
  `:2020-2032`, `getPaidList` `:951-953`

## What refused

`crucible card-smoke -all` refused 581 cards whose first refusal was an effect amount it could not evaluate
(`<Api>: <Param>$ "_" is not resolvable`, any API, any stage). The five largest refusal groups, as the probe's own cause
table counts them (stage `resolve` only):

| Refusal (stage `resolve`)           | Before | After |
| ----------------------------------- | ------ | ----- |
| `Draw: NumCards$`                   | 62     | 7     |
| `DealDamage: NumDmg$`               | 60     | 8     |
| `Token: TokenAmount$`               | 41     | 9     |
| `GainLife: LifeAmount$`             | 38     | 5     |
| `"_": ETB$ PutCounter: CounterNum$` | 37     | 13    |

The quoted value names an SVar; its definition, followed through `SVar$` and `Count$Compare` chains, groups the 581 by
the head that actually failed. 67 of the 581 wrote the expression as the param value itself
(`CharmNum$ Count$Compare Y GE1.2.1`), which only the Layer 7 and `CheckSVar$` keys compiled; they are counted under
their head below.

| Family                                                                   | Before | After | Cause before                                                       |
| ------------------------------------------------------------------------ | ------ | ----- | ------------------------------------------------------------------ |
| Player lists (`PlayerCount<hType>$`, `TargetedPlayer$`, `Triggered...$`) | 117    | 35    | Only `Players`/`Opponents` and four properties                     |
| Scalar `Count$` heads and the rest                                       | 87     | 35    | No head (`CardsInYourHand`, `RememberedSize`, `YourCounters...`)   |
| `Count$Compare <svar> <op><n>.<a>.<b>`                                   | 70     | 3     | No evaluator for the head                                          |
| Branch heads (`Morbid`, `Threshold`, `Delirium`, `wasCastFrom...`)       | 62     | 11    | No evaluator; `Blessing`/`Kicked` only with numeric branches       |
| `Remembered$`, `RememberedLKI$`, `Imprinted$` lists                      | 57     | 2     | `Valid` refused, LKI spellings unread                              |
| `Count$Valid <spec>$<measure>` (`GreatestCardPower`, ...)                | 44     | 3     | `CardPower`/`CardToughness` refused everywhere                     |
| Cast mana (`Converge`, `CastTotalManaSpent`, `EachSpentToCast`)          | 43     | 7     | No head; paid colors not recorded                                  |
| Cost paid lists (`Sacrificed$`, `Discarded$`, `Exiled$`, `Tapped$`)      | 43     | 4     | A spell's additional cost and a triggered `AB$` cost recorded none |
| Other card lists (`Targeted$`, `TriggerRemembered$`, `TriggeredSource$`) | 23     | 1     | Missing heads and measures (`CardSumPT`)                           |
| `TriggerCount$`, `ReplaceCount$`, `TriggerObjects...$`                   | 16     | 13    | Trigger and replacement records                                    |
| `Count$Valid <spec>` count (inline, `ValidGraveyard,Exile`)              | 8      | 0     | Inline value not compiled                                          |
| Total                                                                    | 581    | 125   |                                                                    |

The probe's per-card list shows 127 amount refusals after: these 125 plus Avacyn's Judgment (`Count$Madness`) and Impact
Resonance (`Count$SingleMaxDamageThisTurn`), whose amount sits inside a `DividedAsYouChoose$` refusal
(`DealDamage: engine: DividedAsYouChoose$ "_" is not resolvable`).

## What landed

| Piece                                                    | Java                                          | Engine                                                                                                                                                                                                                                                                                                                                                                 |
| -------------------------------------------------------- | --------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Inline expressions in any param                          | `calculateAmount` `:402` (`indexOf('$') > 0`) | `addInlineChecks` compiles every non-description param value that parses as an expression into `Face.Amounts` at load (PORT-2)                                                                                                                                                                                                                                         |
| `Count$Compare`                                          | `xCount` `:1608-1614`                         | `expr.Comparison`, parsed at load: split on `.` first, comparator kept whole, matched by containment (`javaCompare`)                                                                                                                                                                                                                                                   |
| Branch heads                                             | `:2163-2357`, `:2693-2697`                    | `branchCountValue`: each branch is an amount (number or SVar); predicates shared with `continuousConditionMet`                                                                                                                                                                                                                                                         |
| `IfCastInOwnMainPhase`, `wasCastFrom[Your]<Zone>[ByYou]` | `:2164-2177`, `:2206-2211`                    | `parseInt` branches for the first, as Java; `Card.castBy` for `ByYou`                                                                                                                                                                                                                                                                                                  |
| `Count$Converge`, `Count$Adamant[_N]`                    | `:2134-2137`, `:1795-1800`                    | `Card.castPaid`, filled from `xAnnounced.paid` by `payCastCost`; Adamant only while the spell itself resolves                                                                                                                                                                                                                                                          |
| Scalar heads                                             | `:1981-1989`, `:2295-2397`, `:2505-2509`      | `CardsInYourHand`, `RememberedSize`/`ImprintedSize`/`ChosenSize`, `YourCounters<Type>`, `TotalOppPoisonCounters`, `LifeOppsLostThisTurn`, `LifeYouLostThisTurn`, `YourStartingLife`, `YourSpeed`, `TopOfLibraryCMC`, bare `CastTotalManaSpent`, `ValidSelf`                                                                                                            |
| One `handlePaid`                                         | `:3742-3851`                                  | Every card list (Count$Valid `$prop`, Remembered[LKI], Imprinted[LKI], ExiledWith, DungeonsCompleted, ChosenCard, Enchanted/Equipped, the ability's lists)                                                                                                                                                                                                             |
| Ability lists                                            | `:672-719`                                    | `TriggeredSource$`, `TriggerRemembered$` added to `Targeted$`, `Sacrificed$`, ...                                                                                                                                                                                                                                                                                      |
| Player lists                                             | `:456-500`, `:574-636`                        | `playerCountPlayers` (`Players`, `YourTeam`, `Opponents`, `RegisteredOpponents`, `Other`, `Remembered…`, `NonActive`, `HasLost`, `PropertyYou`, `Property<p>`, `Defined<d>`); `TargetedPlayer`, `ThisTargetedPlayer`, `TargetedController`, `TriggeredPlayers*`, `TriggeredCardController`; the single-object `TriggeredPlayer`/`Target`/`DefendingPlayer`/`Activator` |
| `playerXCount`, `playerXProperty`                        | `:3355-3701`                                  | Highest/Lowest, TiedFor\*, Amount, HasProperty, Condition, DamageThisTurn; 20 `contains` properties in Java's order                                                                                                                                                                                                                                                    |
| Operand expressions                                      | `doXMath` `:3278-3287`                        | `expr.Op.Expr`: an operand `Head$Body` without a `.` is evaluated (`Minus.Remembered$Amount`)                                                                                                                                                                                                                                                                          |
| Spell and triggered-cost paid lists                      | `getPaidList` `:951`                          | `payUnlessPartsInto` records what the spell's additional cost, or a triggered `AB$` line's `Cost$`, sacrificed and discarded                                                                                                                                                                                                                                           |

## Where the operator applies

Java applies a head's arithmetic suffix in three different places, and each is observable:

| Family                                                     | Java                                                                                 | Engine                                     |
| ---------------------------------------------------------- | ------------------------------------------------------------------------------------ | ------------------------------------------ |
| `Count$`, `Number$`, `SVar$`, `TriggerCount$`              | Once, after the measure                                                              | `applyOperator` after `expressionValue`    |
| Card lists (`Remembered$`, `Sacrificed$`, ...)             | Inside `handlePaid`: on the count, or on each card's value before the sum/max        | `handlePaid(..., op)` via `opAwareValue`   |
| Player lists                                               | Inside `playerXCount`: 0 for no players, op unread; per player for a summed property | `playerXCount(..., op)` via `opAwareValue` |
| An empty paid list (`Sacrificed$` with nothing sacrificed) | `getPaidList` is null: 0, op unread (`:715-729`)                                     | `opAwareValue`'s empty paid-list case      |

Bad → Good, `Sacrificed$CardPower/Plus.1` over two sacrificed 2/2s:

```text
Bad:  (2 + 2) + 1 = 5     operator after the measure
Good: (2 + 1) + (2 + 1) = 6   xCount(c, "CardPower/Plus.1") per card, then the sum
```

Highest/Lowest under an operator applies it per player and again to the result (`:3375-3395`); no corpus line writes
one, so it stays refused rather than reproduced blind.

## New engine state

| State                       | Why                                                                                                                                                                                                                                                                   |
| --------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Game.layering` (int)       | Non-zero while `applyContinuousLayers` runs: a battlefield permanent's P/T is mid-rebuild, so `handlePaid` refuses it there and reads it everywhere else. A counter, not a bool, because a refresh can re-enter. Zero between calls, so `Game.Clone` does not copy it |
| `Card.castBy`               | `getCastSA().getActivatingPlayer()` for `wasCastFrom...ByYou`; set beside `castFrom`                                                                                                                                                                                  |
| `Card.castPaid [5]int`      | Mana spent per color (`getPayingMana`), for Converge and Adamant; an array, copied by value                                                                                                                                                                           |
| `xAnnounced.paid`           | `payManaCostX`'s per-color tally of the shards it paid, carried to `payCastCost`                                                                                                                                                                                      |
| `sacrificeCardsFor` returns | The cards actually sacrificed, so a cost records only those                                                                                                                                                                                                           |

No new `PlayerController` decision.

## Forge defects

| Where                                                | Defect                                                                                                                                                                                                                                                                                          |
| ---------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `forge-gui/res/cardsfolder/h/hollow_marauder.txt:11` | `Y:TargetedPlayer$Amount/Minus.Remembered$Valid Card.cmcGE4`: `doXMath` splits the operator on `.` (`AbilityUtils.java:3278-3287`), gets three parts and leaves the operand 0, so the subtraction never happens. Reproduced as Java reads it (the existing `s.length == 2` rule), reported here |

## Not resolved, by name

125 cards still refuse on an amount. Each stays a loud error naming the value:

| Shape                                                                                                                                                                         | Cards | Reason                                                                                                         |
| ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----- | -------------------------------------------------------------------------------------------------------------- |
| `TriggeredPlayersTargets$Amount`                                                                                                                                              | 1     | `Defined$ TriggeredTargets` is not a recorded key                                                              |
| `Count$CastTotalManaSpent <valid>`                                                                                                                                            | 6     | Counts mana by its source; the cast record keeps no sources                                                    |
| `ReplaceCount$DamageAmount`                                                                                                                                                   | 6     | Replacement context (`getReplacingObject`), not carried to the ability                                         |
| `Count$RememberedNumber`                                                                                                                                                      | 5     | `Memory` remembers entities, not integers                                                                      |
| `Count$Devoured`, `Convoked$`, `Emerged`, `Escaped`, `Bargained`, `Madness`, `Foretold`, `AltCost` (snapshot before R9d; only `Convoked$` and `Madness` are still unresolved) | 11    | No record of the cast option or the devoured list                                                              |
| `Count$Void`, `Count$Random`, `Count$LeftBattlefieldThisTurn`, `Count$YouDescendedThisTurn`, `Count$CommittedCrimeThisTurn`                                                   | 9     | No per-turn record (`DescendedThisTurn` is a bool where Java counts); an amount read twice must not roll twice |
| Long tail (`DraftNotes...` 4, `ValidAll` 2, `MostProminentCreatureType` 2, `DifferentCounterKinds_` 2, `LastStateBattlefield` 2, `Count$xPaid` chains 2, 21 single cards)     | 35    | One or two cards each                                                                                          |
| Total                                                                                                                                                                         | 125   |                                                                                                                |

## Smoke probe

`crucible card-smoke -all -workers 8`, before → after:

| Status       | Before | After  |
| ------------ | ------ | ------ |
| plays        | 29,786 | 30,313 |
| refused      | 3,734  | 3,203  |
| placed       | 360    | 367    |
| inconclusive | 198    | 195    |
| judged play  | 88.9%  | 90.4%  |

532 refused → plays. 8 plays → refused, each a path the engine now reaches that it skipped silently before (a condition
or target count that read unresolved and so never fired), stopping at a param no amount owns:

| Card                                                                         | Now refused on                                                                                                                                                                         |
| ---------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Joined Researchers, Paradox Shaper, Stingerquill Voxmancer, Woodwork Prodigy | `AlterAttribute: Attributes$ Prepared` (their `CheckSVar$ Count$ValidSelf ...` now reads)                                                                                              |
| Tolarian Contempt                                                            | `ChangeZone: DestinationAlternative$` (`TargetMax$ PlayerCountOpponents$Amount` now reads)                                                                                             |
| Myojin of Grim Betrayal, Myojin of Night's Reach and Grim Betrayal           | `ChangeZone: Defined$ ValidGraveyard ...` (the `etbCounter` `CheckSVar$ Count$wasCastFromYourHandByYou.1.0` now gives the indestructible counter, so the `SubCounter` activation pays) |
| The Biblioplex                                                               | `ChangeZone: Defined$ TopOfLibrary` (`CheckSVar$ X`, a `Count$Compare` over `Count$CardsInYourHand`, now lets the activation run)                                                      |

32 more changed only their reason (an amount refusal replaced by the next unported param), and 7 refused cards became
`placed`.

## Tests

`amountexpressions_test.go`: Compare's branches, SVar branch and right-hand side, operator after the branch; inline
param expressions; nine player-state branch heads both ways; Night, cast-zone and own-main-phase branches; malformed
branch shapes; the scalar heads and operand expressions; Converge and Adamant from a cast; live P/T measures over
`Count$Valid`; `Remembered$`/`RememberedLKI$` with the operator inside and the list measures they refuse; 41
`PlayerCount` shapes with the operator per player, and the ones refused; `TargetedPlayer$`/`ThisTargetedPlayer$`/
`TargetedController$`, and a card-targeting parent refused. `chosencosts_test.go`: a spell's additional-cost sacrifice
is its paid list; nothing sacrificed reads 0. `expr/parsing_test.go`: `TestParseCountCompare`. Scenarios
`amount-compare-feed-the-clan-ferocious-gains-ten`, `amount-branch-galvanic-blast-metalcraft-deals-four`,
`amount-greatest-power-essence-harvest-drains-the-largest`,
`amount-sacrificed-fling-deals-the-sacrificed-creatures-power`,
`amount-targeted-player-gaze-of-adamaro-counts-the-targets-hand`.
