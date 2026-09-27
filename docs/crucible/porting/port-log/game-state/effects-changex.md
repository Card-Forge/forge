# Effects: ChangeX

One script-driven `ApiType` resolves, 171 of the corpus's 203. Corpus lines: ChangeX 2. Needs two engine pieces first:
the announced X stored on the stack item, and `HasXManaCost$` on SpellCast triggers.

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

Past ChangeX's own `Value$` (below), nothing reads the recorded X yet: `Count$xPaid` (927 corpus files) stays unresolved
in `resolveAmount` (`amount.go` has no ability context), and `etbCounter:...:X` is unported. A changed X therefore has
no game consequence yet. Tests: `xannounced_test.go`.

## `HasXManaCost$` fires SpellCast triggers

`checkSpellCastTriggers` (`trigger.go`) resolves `HasXManaCost$` instead of skipping every trigger that carries it:
`TriggerSpellAbilityCastOrCopy.java:171-181`'s spell branch, the cast card's printed mana cost carrying at least one
`{X}` (`cast.getManaCost().countX()`). Printed, not announced: a spell cast without paying its mana cost still fires it.
The activated-ability branch (`getCostMana().getAmountOfX()`) never arises, since the walk runs only for a cast spell.

Corpus: 2 `Mode$ SpellCast` lines carry `HasXManaCost$`. `unbound_flourishing.txt` now fires; `brass_infiniscope.txt`'s
`CastTrigger` also names `ValidSA$`, still on the skip list, so it stays unfired.

## ChangeX lands

`changexeffect.go` ports `ChangeXEffect.java`'s resolve: the X a spell or ability on the stack was paid with becomes
`Value$`. Mana already paid is untouched. Corpus:

| Card                          | Line                                                                                              | Reaches ChangeX                                                                                                                         |
| ----------------------------- | ------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| `unbound_flourishing.txt`     | `DB$ ChangeX \| Defined$ TriggeredSpellAbility \| Value$ TriggeredSpellAbility>Count$xPaid/Twice` | Yes: `Mode$ SpellCast \| ValidCard$ Permanent \| HasXManaCost$ True`                                                                    |
| `glava_five_advents_mage.txt` | `DB$ ChangeX \| Defined$ TriggeredSpellAbility \| Value$ 5`                                       | No: `Mode$ SpellAbilityCast` (unported trigger mode) with `ValidSA$` and `ResolvedLimit$`; the effect itself resolves its literal shape |

Resolution:

| Step                  | Behavior                                                                                                                                                                                                                                  |
| --------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Target                | `Defined$ TriggeredSpellAbility` only: `triggeredObjects.spellAbility`, the stack item `checkSpellCastTriggers` recorded. None recorded -> error                                                                                          |
| Spell gone            | Not on the stack any more (countered, resolved) -> no-op, no error. Java writes to the orphaned `SpellAbility`; no later reader in this port could see it                                                                                 |
| `Value$`              | Literal; named SVar (`resolveNamedAmount`, sign applied); or `TriggeredSpellAbility>Count$xPaid[/op]`, the triggering spell's X (0 when unannounced, `AbilityUtils.java:1636-1637`, `:1688`), operator via `doXMath` (`replaceeffect.go`) |
| Write                 | Only when the spell announced an X (`ChangeXEffect.java:24,27`'s null checks). `Value$` is computed first, as Java does                                                                                                                   |
| Cast SA vs stack copy | Java writes both `tgtSA` and the host's cast SA (`ChangeXEffect.java:22-29`); they differ only for a `SpellAbilityStackInstance`, which this port lacks: the stack item is the cast ability                                               |

`changeXValue` evaluates the `TriggeredSpellAbility>` context head itself because `resolveAmount` has no `Ability` to
read an xPaid from; it is the first xPaid reader, not a general `Count$xPaid` port.

Rejected with an error before acting (GO-7): any other `Defined$` (`Targeted`, `Parent`, `Remembered`, ...);
`ValidTgts$`/`TargetType$` (a targeted spell, no corpus line); `Condition$`/`ConditionDefined$` (read as never met,
silently, by `subAbilityConditionMet`); a missing `Value$`; any other `Value$` shape (another `Count$` head, a `doXMath`
operator it lacks such as `Pow`, an SVar `resolveNamedAmount` cannot resolve such as `Count$xPaid`).

Tests: `changexvalue_test.go` (Unbound Flourishing's verbatim lines doubling X = 3 to 6, literal, named SVar, operand
and sign, unannounced X left alone, a copy keeping the changed X per CR 707.10, spell gone, every rejected shape).
Scenario `unbound-flourishing-trigger-outlives-countered-x-spell`: the real trigger fires off Farmer Cotton,
Counterspell counters it, and ChangeX resolves as a no-op; `expect.state` has no stack or X, so the doubling itself
stays module-tested.
