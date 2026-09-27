# Port Log — Game State: Layer 7a CDA Amounts

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Java:** `forge-game/src/main/java/forge/game/ability/AbilityUtils.java` — `calculateAmount` (:367), `xCount`
  (:1566), `doXMath` (:3206), `playerXCount` (:3288), `playerXProperty` (:3420), `handlePaid` (:3675);
  `staticability/StaticAbilityDevotion.java`
- **Go:** [`amount.go`](../../../../../crucible/internal/engine/amount.go),
  [`amountheads.go`](../../../../../crucible/internal/engine/amountheads.go),
  [`amountpaid.go`](../../../../../crucible/internal/engine/amountpaid.go),
  [`internal/expr/expr.go`](../../../../../crucible/internal/expr/expr.go),
  [`compile.go`](../../../../../crucible/internal/carddb/compile/compile.go) (`addInlineAmounts`)

Characteristic-defining power/toughness amounts past the `Count$Valid` family.

## Layer 7a amounts: the real corpus vocabulary, and what resolves

### Vocabulary

Measured, not assumed: every `SetPower$`/`SetToughness$` on a real `CharacteristicDefining$ True` line, its SVar chain
followed to the body. 405 dimensions on 242 lines. `xPaid` appears on **none** of them; `CardCounters` on 6, `Devotion`
on 5.

| Shape (after SVar chain)                                                                | Dims | Status                                                 |
| --------------------------------------------------------------------------------------- | ---- | ------------------------------------------------------ |
| `Count$Valid[<Zone>[,<Zone>]] <spec>`, no suffix                                        | 275  | resolved before this slice                             |
| same, with a doXMath suffix (`/Twice`, `/Plus.1`, `/Times.2`)                           | 23   | resolved                                               |
| same, with a `$<property>` (`CardTypes`, `GreatestCardManaCost`, `P1P1`, ...)           | 27   | 25 resolved; `DifferentCardNames` (2) deferred         |
| `SVar$X/Plus.1`, `SVar$Y/HalfUp` (the goyfs, Malignus)                                  | 13   | resolved                                               |
| `Count$Domain[/Twice]`                                                                  | 9    | resolved                                               |
| `Count$YourLifeTotal[/Minus.Z]`, Z = `Count$OppGreatestLifeTotal`                       | 8    | resolved                                               |
| `Number$N[/Plus.X]`, `Count$ChosenNumber` (Shapeshifter, Mwonvuli Ooze)                 | 8    | resolved                                               |
| literal                                                                                 | 6    | resolved before this slice                             |
| `Count$CardCounters.<TYPE>[/Twice]`                                                     | 6    | resolved                                               |
| `Count$Devotion.<Color>`                                                                | 5    | resolved                                               |
| `Count$NumInAllHands`, `Count$Chroma.<C>`, `ChromaInGrave.<C>`, `Count$YouDrewThisTurn` | 9    | resolved                                               |
| `PlayerCountOpponents$HighestCardsInHand`                                               | 2    | resolved                                               |
| `ExiledWith$CardPower`/`ExiledWith$Colors`                                              | 5    | deferred: no exiled-with tracking                      |
| `Remembered$Amount`/`Remembered$CardManaCost`, `PlayerCountRemembered$Life...`          | 6    | deferred: Remembered-list heads                        |
| `Count$YourTurns`                                                                       | 2    | deferred: `Player.Turn` is never incremented           |
| `Count$Party`                                                                           | 1    | deferred: needs `*cardtype.Registry` and Java's greedy |

Net: 389 of 405 by shape. `TestCharacteristicDefiningCorpusFloor` measures it end to end against the real card DB, one
card alone on the battlefield: 363 of 378 "\*" dimensions resolve (a printed integer is not counted; the four
planeswalker misses are the test's own loyalty-less walkers dying to CR 704.5i, not an amount gap). Its floor only goes
up.

### Model: extend `resolveAmount`, the one evaluator

`applyOneCharacteristicDefiningPT` reaches amounts through `ptParam` → `resolveNamedAmount` → `resolveAmount`. That
evaluator is shared by some 30 callers (DealDamage, Dig, Surveil, Sacrifice, trigger `CheckSVar$`, Layer 8, ...), so
every head added here resolves for all of them. Chosen over a CDA-only resolver because the heads are context-free —
each reads the host, its controller, or a zone — and one evaluator is one place for Java parity.

| Piece                                          | What                                                                                  | Why                                                                                                                                   |
| ---------------------------------------------- | ------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| `expr.Amount.Count`                            | `Count$` body already read by `ParseCount`                                            | Evaluator no longer parses script text per SBA pass (PORT-2). Amounts are not in the canonical AST, so golden fingerprints unchanged  |
| `expr.Amount.Numeric`, `expr.Op.Numeric/Value` | `Number$N` body and an integer operator operand, read at load                         | Same reason; an operand that is not an integer is an SVar name, looked up                                                             |
| `addInlineAmounts` (compile.go)                | Static P/T value written as a raw expression, keyed by its own text in `Face.Amounts` | `nethergoyf.txt` writes `SetPower$ Count$...` inline; `calculateAmount` reads it as raw (`indexOf('$') > 0`). An SVar name has no `$` |
| `applyOperator`                                | doXMath, all 18 operators, containment in Java's order                                | HalfUp/HalfDown on a negative use `math.Ceil`/`Floor`, not Go's truncating division                                                   |
| `namedAmount`                                  | Runtime SVar (`Card.svars`) first, then `amounts`, at every hop                       | Java's `getSVar` sees StoreSVar values at every level, not only the outermost name                                                    |
| `paidMeasure` (amountpaid.go)                  | handlePaid in its own branch order                                                    | A name an earlier branch catches never reaches a later one; unknown property → unresolved, never a plain match count                  |

No new `Game` state, no new controller decision: `Game.Clone` and `TestCloneAllocationsStayBounded` are untouched.

### Java behavior kept exact

| Behavior                                                                        | Where it shows                                                         |
| ------------------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| Sign multiplier applies last: `-Number$7/Plus.1` is −8                          | `calculateAmount`'s `val * multiplier`                                 |
| Empty matched list is 0 before the outer operator: Tarmogoyf is 0/1             | `handlePaid`'s first line, then `xCount`'s `doXMath(num, expr)`        |
| Operand with more than one `.` part is 0; `Plus.1.2` leaves the number alone    | `doXMath`'s `s.length == 2`                                            |
| Devotion: a hybrid symbol counts once toward any mask it touches, generic never | `ManaCostShard.isColor`; plus `Mode$ Devotion` (Altar of the Pantheon) |
| `OppGreatestLifeTotal` with no opponents is `Integer.MIN_VALUE`                 | `Aggregates.max`'s seed                                                |
| `PlayerCount...$Highest` seeds 0, `Lowest` 99999                                | `playerXCount`                                                         |

### Refused rather than guessed (GO-7)

| Shape                                             | Reason                                                                                                         |
| ------------------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| Context prefix (`CastSA>`, `Spawner>`, ...)       | `adjustTriggerContext` switches the measuring ability; a static has none. No CDA writes one                    |
| `Mod.0`                                           | Java throws `ArithmeticException`                                                                              |
| `PlayerCount...` with an operator                 | `playerXCount` hands `Highest<X>/<op>` down to `playerXProperty`, applying the operator twice. 0 corpus lines  |
| Per-card `CardPower`/`CardToughness` (Greatest\*) | Another permanent's power is mid-rebuild while `applyContinuousPT` runs; 143 real lines corpus-wide wait on it |
| Devotion to `Chosen...` or colorless              | Host's chosen color / a mana type with no `mana.Colors` bit; 0 CDA lines                                       |
| `Mode$ Devotion` line with any other param        | `checkConditions`' Condition$ family is not evaluated for this mode                                            |
| An operand SVar that does not resolve             | Java prints to stderr and uses 0; a wrong P/T is worse than an unresolved one                                  |

### Deferred, and what each needs

| Shape                                                    | Needs                                                                                                                           |
| -------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| `Count$Party`                                            | `*cardtype.Registry` for changeling's all-creature-types, and Java's `HashMultimap` greedy assignment order reproduced (PORT-7) |
| `$CreatureType`/`LandType`/`PlaneswalkerType`/`AllTypes` | `*cardtype.Registry` to tell a creature type from any other subtype (`CardType.getCreatureTypes`)                               |
| `$DifferentCardNames`                                    | `CardLists.getDifferentNamesCount`: Spy Kit's non-legendary names and no-name cards; no `sharesNameWith` in the engine          |
| `ExiledWith$...`                                         | `Card.getExiledCards` tracking, written by the effects that exile "with" a card                                                 |
| `Remembered$...`, `PlayerCountRemembered$...`            | Remembered-list heads over `Memory.Remembered`, per `handlePaid`/`playerXCount`                                                 |
| `Count$YourTurns`                                        | `Player.Turn` incremented at each turn start (turn.go); today nothing writes it                                                 |

### Known limits (not new, now load-bearing)

- **Layer order.** `CheckStateBasedActions` (action.go:197) runs `applyContinuousPT` before `applyContinuousType`/
  `applyContinuousColor`, so a CDA that reads `Type()` or `Colors()` (Domain, CardTypes, Colors, any Valid spec on a
  type) sees the previous pass's Layer 4/5 result. CR 613 puts Layers 4/5 first. The reorder belongs with the Layer
  4/5/6 work.
- **Unrecognized valid property.** `Matches` treats a property it does not know as a non-match, so a Valid-family count
  over such a spec (`Creature.ChosenCtrl`, Lost Order of Jarkeld) undercounts rather than failing to resolve.
- **`IsPresent$` on a continuous line** is not evaluated by any applier; Grand Master of Flowers' CDA `SetPower$ 7`
  applies below seven loyalty.
- **Printed mana value only.** `CardManaCost`, Devotion and Chroma read `Def.Faces[0].ManaCost` (`Card.CMC`'s own
  limit).

### Tests

`characteristic_defining_amounts_test.go` (Tarmogoyf, Devotion + Altar, Domain, a player/host-head table, every doXMath
operator, unresolvable shapes, Winter's Layer 8 hand size, the corpus floor); `TestParseCarriesCompiledCount`
(`internal/expr`); scenarios `tarmogoyf-survives-damage-under-its-card-type-toughness` and
`tarmogoyf-dies-when-graveyards-hold-too-few-card-types`.
