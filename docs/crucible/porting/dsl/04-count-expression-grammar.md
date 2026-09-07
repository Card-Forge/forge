# Count Expression Grammar

- **Status:** Active
- **Java counterpart:** `forge-game/src/main/java/forge/game/ability/AbilityUtils.java` (3,950 LOC)

Numeric expressions. Wherever an ability needs a number that is not a literal — damage amounts, counter counts, X.

## Grammar

```ebnf
count-expr  = "Count$" , head , { "/" , operator , [ "." , operand ] } ;
head        = simple-head
            | "Valid" , " " , valid-string ;    (* NOTE the space, and the embedded grammar *)
simple-head = name , { "." , parameter } ;
operator    = "Plus" | "Minus" | "Times" | "Twice" | "Thrice"
            | "HalfUp" | "HalfDown" | "LimitMax" | "LimitMin"
            | "NMinus" | "Abs" | "DivideEvenlyDown" ;
operand     = integer | svar-name ;
```

## The finding that matters most

**A count expression can embed a whole valid string, separated by a space.**

```text
Count$Valid Creature.YouCtrl+powerGE1/LimitMax.1
Count$Valid Creature.OppCtrl+powerGE2/LimitMax.1
Count$Valid Creature.IsRemembered/Times.2
```

So a count expression **is not a token and cannot be lexed as one.** A tokenizer that splits on whitespace, or that
treats the value after `Count$` as opaque up to the next delimiter, breaks on every one of these. The parser must
recognise the `Valid` head, consume the space after it, and hand the remainder to the valid-string parser
([03](03-valid-string-grammar.md)) before looking for `/` operators.

This is also why `Count$` values cannot be validated by regex during the vocabulary scan — they need the real parser.

## Arithmetic

Operators are suffixes introduced by `/`, optionally taking an operand after `.`:

```console
$ find forge-gui/res/cardsfolder -name '*.txt' -exec cat {} + \
    | grep -oE '(Count|SVar)\$[A-Za-z0-9_.]+/[A-Za-z]+' \
    | sed 's|.*/||' | sort | uniq -c | sort -rn | head -3
    191 Plus
    117 Twice
     76 Times
```

| Operator           | Uses | Meaning          |
| ------------------ | ---: | ---------------- |
| `Plus`             |  191 | Add operand      |
| `Twice`            |  117 | ×2, no operand   |
| `Times`            |   76 | × operand        |
| `Minus`            |   52 | Subtract operand |
| `LimitMax`         |   28 | Clamp above      |
| `HalfDown`         |   17 | ÷2, round down   |
| `HalfUp`           |   16 | ÷2, round up     |
| `Thrice`           |    8 | ×3, no operand   |
| `LimitMin`         |    8 | Clamp below      |
| `NMinus`           |    6 | operand − value  |
| `Abs`              |    6 | Absolute value   |
| `DivideEvenlyDown` |    4 | Integer division |

`Twice`, `Thrice` and `Abs` take no operand; the rest do. `NMinus` reverses the operands relative to `Minus`, which is
the one asymmetry worth a test of its own.

## Heads

**500 distinct full expressions, 224 distinct heads, 164 distinct dotted parameters.**

Heads fall into families:

| Family                | Examples                                                                                               |
| --------------------- | ------------------------------------------------------------------------------------------------------ |
| Zone and board counts | `CardsInYourHand`, `TypeInYourYard`, `Valid <valid-string>`                                            |
| Card state            | `CardCounters.<type>`, `CardPower`, `CardManaCost`                                                     |
| Player state          | `YourLifeTotal`, `OppLifeTotal`, `YourPoisonCounters`                                                  |
| Turn history          | `ThisTurnCast_Card`, `ThisTurnEntered_Battlefield_Creature`, `ThisTurnActivated_Activated`             |
| Threshold conditions  | `Adamant`, `Morbid`, `Delirium`, `Devotion`, `DevotionDual`, `Monarch`, `Teamwork`                     |
| Cast context          | `IfCastInOwnMainPhase`, `wasCastFromGraveyard`, `Kicked`, `OptionalGenericCostPaid`                    |
| Randomness            | `Random` — draws from the game's `Game` RNG stream ([ADR-0006](../../adr/0006-determinism-and-rng.md)) |

**Turn-history heads are structured, not opaque.** `ThisTurnEntered_Graveyard_from_Battlefield_Creature` decomposes as
zone, source zone, and a type filter. Porting them as 200 string constants would work and would be wrong — the
underscore segments are a grammar, and treating them as one avoids reimplementing near-identical logic per constant.

## Dotted parameters

`.`-separated segments after a simple head are parameters, **not arithmetic**:

```text
Count$CardCounters.LOYALTY      counters of a named type
Count$Devotion.Red              devotion to a colour
Count$Adamant_2                 threshold variant, underscore not dot
```

Arithmetic always uses `/`. Conflating `.` with arithmetic is an easy and silent error, because both appear in the same
expression: `Count$Valid Creature.YouCtrl/Times.2` has a `.` in the valid string, a `/` operator, and a `.` operand.

## Evaluation

Evaluated against game state, so `expr` is a package importing `engine`, not part of it
([ADR-0003](../../adr/0003-go-project-layout.md)). Compiled once at load into a typed tree; never re-parsed during a
game ([ADR-0007](../../adr/0007-card-dsl-representation.md)).

## Invalidated by

- A 13th arithmetic operator appearing in the corpus
- Any head that embeds a sub-grammar other than `Valid`
