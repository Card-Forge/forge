# Port Log — Game State: amounts read before targets are chosen (R25i)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `amount.go`, `amountheads.go`, `valid.go`,
  `castspell.go`

A `TargetMin$`/`TargetMax$` that names an SVar is read while the spell is cast (`resolveNamedAmount`), so an SVar the
engine could not resolve made the cast fail without a word. Each shape below now resolves.

| Amount                                                               | Cards                                                                          | Java                          |
| -------------------------------------------------------------------- | ------------------------------------------------------------------------------ | ----------------------------- |
| `PlayerCountPlayers$Amount/Plus.MaxPermanents`                       | Aurelia's Fury, Conflagrate, Meteor Shower, Rolling Thunder, Avacyn's Judgment | `AbilityUtils.java:3431-3434` |
| `Count$LastStateBattlefieldWithFallback <valid>` / `...Graveyard...` | Jaws of Stone, Reap, Volcanic Wind, Aether Burst                               | `AbilityUtils.java:1804-1846` |
| `Count$Kicked.<SVar>.<n>`                                            | Divine Resilience, The Eagles Are Coming!                                      | `AbilityUtils.java:1695-1698` |
| `PlayerCountPropertyYou$AttractionsVisitedThisTurn`                  | Black Hole                                                                     | `AbilityUtils.java:3683`      |
| `Count$TotalDamageDoneByThisTurn`                                    | Brightflame's life gain                                                        | `AbilityUtils.java:2400`      |

- `PlayerCount...$Amount` refused any operator, a guard written for the Highest/Lowest forms that Java applies twice. It
  now refuses only those.
- The `LastState...WithFallback` count reads the battlefield and graveyards as the spell was put on the stack
  (`Card.castLast`, `putSpellOnStack`), every card now in the zone when that snapshot is empty, and 0 for a card that
  was not cast. The snapshot holds IDs, so a card is judged as it is at resolution, not as it was.
- `Count$Kicked.<a>.<b>`: each branch is `calculateAmount`'s input, so a name (`Z`) is an SVar.
- `AttractionsVisitedThisTurn` is 0 because the engine never visits an Attraction (RollDice's `ToVisitYourAttractions`
  is refused); it moves when visiting is ported.
- `IsPoisoned` and `IsCorrupted` (three or more poison counters) are player properties now
  (`PlayerProperty.java:273-281`).
- `Card.damageDealtThisTurn`: damage the card dealt after prevention and replacement, cleared at cleanup.

## Not done

- Monstrous Onslaught: `Count$Valid Creature.YouCtrl$GreatestCardPower`. `perCardMeasure` refuses `CardPower` because a
  Layer 7 effect folding mid-rebuild would read a half-folded power; a resolution-time read needs a flag for "layers are
  being applied".
- Wildfire Howl: `Count$PromisedGift.1.0` (gift is not ported).
- Geth's Summons: still not castable, for a reason other than its amount (`IsCorrupted` resolves).
- The spells above that now cast are refused later by `DealDamage: DividedAsYouChoose$` (open plan R9).
