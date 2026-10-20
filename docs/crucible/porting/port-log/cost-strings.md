# Port: Cost Strings

- **Java source:** `forge-game/src/main/java/forge/game/cost/Cost.java` (1,133 — its constructor, `parseCostPart` and
  `abCostParse`), `forge-core/src/main/java/forge/util/TextUtil.java` (`splitWithParenthesis`)
- **Go target:** `crucible/internal/cost`
- **Status:** Parsing done — M3 slice E. Paying a cost needs a game, so the 52 `CostPart` subclasses land with the
  engine

## What it does

Turns the value of a `Cost$` param into parts. `1 R Sac<1/Creature.Other/another creature> T` is two mana symbols, a
sacrifice and a tap.

12,680 cost strings in the corpus, using **43 of Java's 55 named parts**, and 43 distinct mana tokens.

## Three rules that decide the parser

**The split is bracket-aware.** `TextUtil.splitWithParenthesis(parse, ' ', '<', '>')` does not break inside a body, and
12% of bodies contain a space. `strings.Fields` would turn one sacrifice into two tokens and pass on 87% of the corpus
while doing it.

**Each part has its own field limit.** `abCostParse(parse, n)` splits the body on `/` with a cap that differs per
branch: `Sac` takes 3, `SubCounter` 5, `PayLife` 2, `Mana` 1. The cap is what keeps a description containing a slash
whole — `tapXType<2/Creature;Treasure/creatures and/or Treasures>` is three fields, not four. The table in `parts.go` is
transcribed from the 55 branches of `parseCostPart`, in their order.

**An unrecognised token is mana.** `parseCostPart` returns null and the constructor appends the token to a mana string.
So a part this table were missing would not fail: it would quietly become a mana symbol, and the mana parser would
reject it much later, on a different card, with a message about mana. The corpus test therefore checks the fall-through
list holds nothing but mana symbols, which is what makes the table's completeness a measured claim rather than a hope.

## Two things the grammar doc had wrong

`or` is **not** a separator. All 172 occurrences sit inside a description field, and `Cost.java` has no branch for it.

The empty-segment rule is Java's, not an accident: `splitWithParenthesis` skips empty segments, so `1  R` is two tokens
and `Sac<1//a creature>` is **two** fields rather than three — an empty field shifts every field after it.

## `Cost$` is not always a cost string

On a static ability it may name an SVar, which `StaticAbilityCantAttackBlock` resolves to a number with
`calculateAmount` before any `Cost` is built (`if (stAb.hasSVar(costString))`). `Whipgrass Entangler` writes
`Cost$ WhipgrassClericNum` and `War Tax` writes `Cost$ XChosen`, and neither is a cost string at that point.

The corpus test skips a `Cost$` whose whole value names an SVar on the same face, for the same reason Java does.

## Deviations from Java

| Java                                                         | Go                                                                                               |
| ------------------------------------------------------------ | ------------------------------------------------------------------------------------------------ |
| 52 `CostPart` subclasses, each parsing its own body          | One `Part` with named fields. Meaning belongs with payment, which is M5                          |
| Tap and untap are flags _and_ parts                          | Same. `Cost.Tap` is set in the pre-pass, and `T` is also a part, because Java does both          |
| The mana leftovers are joined and handed to `ManaCostParser` | Kept as a token list. `internal/mana` parses it when the caller needs a cost rather than a shape |
| A body field that does not exist is a `length >` check       | `Part.Field(i)` returns empty past the end, so the check is in one place                         |

## Not ported yet

| Java                                                 | When                                                              |
| ---------------------------------------------------- | ----------------------------------------------------------------- |
| Paying: `canPay`, `payAsDecided`, the 52 subclasses  | M5                                                                |
| `CostPartMana`'s restriction after a `\` in the body | M5                                                                |
| `XMin` semantics, and `Mandatory`                    | M5                                                                |
| The 12 named parts no corpus card uses               | If a card ever uses one, the fall-through test fails and names it |
