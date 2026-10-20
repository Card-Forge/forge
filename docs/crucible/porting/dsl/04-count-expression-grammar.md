# Count Expression Grammar

- **Status:** Active
- **Java counterpart:** `forge-game/src/main/java/forge/game/ability/AbilityUtils.java` (3,950 LOC)

Numeric expressions. Wherever an ability needs a number that is not a literal — damage amounts, counter counts, X.

## Grammar

```ebnf
count-expr  = "Count$" , [ " " ] , head , { "/" , operator , [ "." , operand ] } ;
head        = simple-head
            | valid-head , " " , valid-string     (* NOTE the space, and the embedded grammar *)
            | name , " " , argument ;
simple-head = name , { "." , parameter } ;
valid-head  = "Valid" | "ValidHand" | "ValidGraveyard" | "ValidLibrary" | "ValidExile" | ... ;
operator    = "Plus" | "Minus" | "Times" | "Twice" | "Thrice" | "HalfUp" | "HalfDown"
            | "LimitMax" | "LimitMin" | "NMinus" | "Abs" | "ThirdUp" | "Mod" | "Negative"
            | "Pow" | "DivideEvenlyDown" | "DivideEvenlyUp" ;
operand     = integer | svar-name ;
```

**268 distinct heads**, and the same arithmetic applies to the amount expressions of [02](02-param-map-grammar.md) —
`Remembered$CardPower/Minus.1` is the same grammar with a different head.

## The finding that matters most

**A head can take a space-separated argument, and for the whole `Valid` family that argument is a valid string.** 172
heads in the corpus take one.

```text
Count$Valid Creature.YouCtrl+powerGE1/LimitMax.1
Count$Valid Creature.IsRemembered/Times.2
Count$ValidGraveyard Creature.YouOwn/Twice
Count$ValidHand Card
Count$Compare Y GE1
```

So a count expression **is not a token and cannot be lexed as one.** A tokenizer that splits on whitespace, or that
treats the value after `Count$` as opaque up to the next delimiter, breaks on every one of these. The parser must cut
the head at the first space, hand the remainder to the valid-string parser ([03](03-valid-string-grammar.md)) when the
head starts with `Valid`, and only then look for `/` operators. `Compare` shows why the rule is about the space rather
than about `Valid`: its argument is a comparison, not a valid string.

Thirty values write a space **before** the head — `Count$ 2`, `Count$ X` — so the head is what follows it.

This is also why `Count$` values cannot be validated by regex during the vocabulary scan — they need the real parser.

## Arithmetic

Operators are suffixes introduced by `/`, optionally taking an operand after `.`:

```bash
cd crucible && go run ./tools/vocabscan -kind countOperator
```

**Seventeen operators**, not the twelve a `grep` for `/[A-Za-z]+` finds: that pattern misses every operator on an amount
expression whose head is not `Count`.

| Operator           | Uses | Meaning              |
| ------------------ | ---: | -------------------- |
| `Plus`             |  329 | Add operand          |
| `Times`            |  180 | Multiply by operand  |
| `Twice`            |  169 | ×2, no operand       |
| `Minus`            |  116 | Subtract operand     |
| `LimitMax`         |   84 | Clamp above          |
| `HalfUp`           |   59 | ÷2, round up         |
| `HalfDown`         |   42 | ÷2, round down       |
| `NMinus`           |   14 | Operand minus value  |
| `LimitMin`         |   10 | Clamp below          |
| `Thrice`           |    9 | ×3, no operand       |
| `Abs`              |    6 | Absolute value       |
| `DivideEvenlyDown` |    6 | Divide, round down   |
| `ThirdUp`          |    5 | ÷3, round up         |
| `Mod`              |    3 | Remainder            |
| `DivideEvenlyUp`   |    1 | Divide, round up     |
| `Negative`         |    1 | Sign flip            |
| `Pow`              |    1 | Raise to the operand |

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
