# Effects: ChangeX

## Announced X is recorded on the stack item

Java keeps the value of X a cost was paid with on the `SpellAbility` itself: `SpellAbility.xManaCostPaid`
(`SpellAbility.java:2562-2567`), a nullable `Integer` set as the cost is paid (`PlaySpellAbility.java:462-465`,
`announceValuesLikeX` at `:752-781`), null when no cost part carries an X. `Card.getXManaCostPaid`
(`Card.java:1621-1627`) reads it back off the card's cast SA.

| Piece                                                  | Where                | Why                                                                                                                                 |
| ------------------------------------------------------ | -------------------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| `Ability.xManaCostPaid` / `hasXManaCostPaid`           | `ability.go`         | Java's field; the bool is its null. A value, so `Game.Clone` and a `CopySpellAbility` copy (CR 707.10) carry it with no clone code  |
| `payManaCostX` returning `xAnnounced`                  | `manapay.go`         | `PayManaCost`'s own body; `PayManaCost` wraps it, so its five callers and its `bool` contract are unchanged                         |
| `payCastCost` returns `xAnnounced`; `xAnnounced.setOn` | `castspell.go`       | All three cast branches (permanent, Aura, Instant/Sorcery) set it before `PushAbility`, so `checkSpellCastTriggers` already sees it |
| `ActivateAbility` sets it                              | `activateability.go` | CR 602.2b: an activated ability's `{X}` is announced the same way; Java's `setXManaCostPaid` makes no spell/ability split           |
| `(*Ability).XManaCostPaid() (int, bool)`               | `changexeffect.go`   | Read accessor for module tests and later `Count$xPaid` readers                                                                      |

`WithoutManaCost$` cast (`castOpts.withoutManaCost`) records none: the cost has no X part left, and
`announceValuesLikeX` calls `setXManaCostPaid(null)` then. X = 0 announced is recorded as `(0, true)`, not as absent.

Nothing reads the recorded X yet: `Count$xPaid` (927 corpus files) stays unresolved in `resolveAmount` (`amount.go` has
no ability context), and `etbCounter:...:X` is unported. Tests: `xannounced_test.go`.

## `HasXManaCost$` fires SpellCast triggers

`checkSpellCastTriggers` (`trigger.go`) resolves `HasXManaCost$` instead of skipping every trigger that carries it:
`TriggerSpellAbilityCastOrCopy.java:171-181`'s spell branch, the cast card's printed mana cost carrying at least one
`{X}` (`cast.getManaCost().countX()`). Printed, not announced: a spell cast without paying its mana cost still fires it.
The activated-ability branch (`getCostMana().getAmountOfX()`) never arises, since the walk runs only for a cast spell.

Corpus: 2 `Mode$ SpellCast` lines carry `HasXManaCost$`. `unbound_flourishing.txt` now fires; `brass_infiniscope.txt`'s
`CastTrigger` also names `ValidSA$`, still on the skip list, so it stays unfired.
