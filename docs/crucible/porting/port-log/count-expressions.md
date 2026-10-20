# Port: Amount Expressions

- **Java source:** `forge-game/src/main/java/forge/game/ability/AbilityUtils.java` (3,950 — `calculateAmount`,
  `doXMath`, `adjustTriggerContext`; `xCount` and `playerXCount` not ported yet), `forge/game/card/CardFactoryUtil.java`
  (`extractOperators`)
- **Go target:** `crucible/internal/expr`
- **Go dependency:** `internal/valid`, because a count head can embed a whole valid string
- **Status:** Parsing done — M3 slice D. Every head measures something about a game, so evaluation lands with the engine

## What it does

Turns the number an ability computes into a value. `NumDmg$ 3` is a literal, `NumDmg$ X` names an SVar, and
`SVar:X:Count$CardsInYourHand/Twice` is a measurement with arithmetic — all three arrive through the same param, so all
three are one type.

16,544 amounts in the corpus: **100 heads, 226 count heads, 17 operators, 3 context prefixes.**

## The order calculateAmount tests things in

It is not obvious and it is load-bearing:

| Step | Rule                                                           | Consequence                                                                                |
| ---- | -------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| 1    | A leading `+` or `-` is stripped and kept as a multiplier      | `-Count$X` negates the whole expression, not its head                                      |
| 2    | `StringUtils.isNumeric` — digits only, no sign                 | The sign is already gone, so this still matches                                            |
| 3    | `amount.indexOf('$') > 0` means a raw expression               | **Strictly greater than zero.** A value starting with `$` is a name, and will not be found |
| 4    | Otherwise an SVar name, looked up on the ability then the card | A miss prints to stderr and yields zero                                                    |

## Only the first operator applies

`CardFactoryUtil.extractOperators` splits on `/` and returns **`l[1]`** — one segment. A second operator is silently
dropped, so `Count$X/Twice/Plus.3` doubles and never adds.

No corpus expression writes two, so the limit is unobservable today. It is reproduced rather than fixed (PORT-7): a port
that evaluates more than Forge does disagrees with the oracle, and the oracle is the thing being matched.

`doXMath` then matches the operator by **containment**, in its own order — `Plus`, `NMinus`, `Minus`, `Twice`, `Thrice`,
`HalfUp`, `HalfDown`, `ThirdUp`, `ThirdDown`, `Negative`, `Times`, `Pow`, `DivideEvenlyUp`, `DivideEvenlyDown`, `Mod`,
`Abs`, `LimitMax`, `LimitMin`. `NMinus` has to be tested before `Minus`, or every `NMinus` becomes a subtraction with
its operands the wrong way round. A name matching none leaves the number unchanged, which is the final `else` and not an
error.

Seventeen of the eighteen are used; `ThirdDown` is defined and unused.

## Context prefixes

A head can carry a `>`-terminated prefix that changes which ability the measurement is taken against:
`CastSA>Returned$CardManaCost` is what the spell that cast this card returned, not what this ability did.
`adjustTriggerContext` strips one prefix and returns immediately, so a head carrying two keeps the second as part of its
name.

All three are used, and rarely: `Spawner>` 6, `CastSA>` 5, `TriggeredSpellAbility>` 1. Reading them as part of the head
instead would invent eight heads that do not exist.

## Count heads take arguments

`Count$ValidGraveyard Creature.YouOwn/Twice` is a head, a space, a whole valid string, and then the arithmetic. The
whole `Valid` family behaves this way, and other heads take a space-separated argument that is not a valid string —
`Compare Y GE1`, `xColorPaid B`. Thirty values write a space _before_ the head, as `Count$ 2`.

## Deviations from Java

| Java                                                            | Go                                                                                            |
| --------------------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| The string is re-parsed on every evaluation, per card, per game | Parsed once at load (ADR-0007)                                                                |
| A missing SVar prints to stderr and yields 0                    | Not decided here. This package parses; resolution is the evaluator's, and it can see the card |
| Sign, literal and reference are all `int` at the end            | `Kind` says which of the three a value is, so a caller cannot mistake a name for a number     |

## Not ported yet

| Java                                                 | When                                                                                     |
| ---------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| `xCount` — what each of the 226 count heads measures | M5                                                                                       |
| `playerXCount` — the `PlayerCount*` family           | M5                                                                                       |
| `doXMath`'s arithmetic itself                        | M5                                                                                       |
| Wiring `valid.Compare.Operand` through this parser   | M5. `valid` cannot import `expr`, since `expr` imports `valid`; the evaluator calls both |
