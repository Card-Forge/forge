# Param Map Grammar

- **Status:** Active
- **Java counterpart:** `forge-game/src/main/java/forge/game/ability/AbilityFactory.java:366` (`getMapParams`)

The value of every `A:`, `T:`, `S:`, `R:` line and of sub-ability `SVar:` entries.

## Grammar

```ebnf
param-map   = param , { "|" , param } ;
param       = key , "$" , value ;
key         = letter , { letter | digit | "_" } ;
value       = { any-char - "|" } ;          (* trimmed; may contain spaces and "$" *)
```

Split on `|`, then on the **first** `$` only. Values routinely contain `$` — a `SubAbility$` value naming an SVar whose
own definition contains `Count$` is ordinary.

The first key selects the record type and, with it, how the rest of the map is interpreted:

| First key | Record             | Meaning                                                           |
| --------- | ------------------ | ----------------------------------------------------------------- |
| `SP$`     | Spell              | The card's own spell                                              |
| `AB$`     | Activated ability  | Requires a `Cost$`                                                |
| `DB$`     | Sub-ability        | Chained via `SubAbility$`, no cost of its own                     |
| `ST$`     | Static ability     |                                                                   |
| `RE$`     | Replacement effect |                                                                   |
| `Mode$`   | Trigger or static  | `T:` and `S:` lines lead with `Mode$` rather than a `$`-typed API |
| `Event$`  | Replacement        | `R:` lines                                                        |

## Vocabulary

**1,197 distinct param keys** across the corpus, measured by the scanner that has to compile them:

```console
$ cd crucible && go run ./tools/vocabscan
paramKey  1197 distinct  440014 occurrences
```

Three vocabularies sit next to it and are counted apart, because each closes at a different milestone: **192 APIs** (the
value of `SP$`/`AB$`/`DB$`/`ST$`/`RE$`), **29 AI-hint keys** (`AI`-prefixed SVar bodies, M7), and the **87 SVar heads**
below.

Distribution is steep, and this is the number that sizes the generated param structs
([ADR-0007](../../adr/0007-card-dsl-representation.md)):

|      Keys | Share of param occurrences |
| --------: | -------------------------: |
|    top 25 |                      70.5% |
|    top 50 |                      83.0% |
|   top 100 |                      92.1% |
|   top 200 |                      97.0% |
|   top 400 |                      99.2% |
|   top 800 |                      99.9% |
| all 1,197 |                       100% |

**The tail is not optional.** A key used by one card is still a key that card needs, and the corpus gate
([ADR-0011](../../adr/0011-card-corpus-scoping.md)) is scoped by deck, not by frequency — so which of the 1,197 matter
depends on the gauntlet, not on this curve.

## Value types

A param's value is one of a small set, decided per key by the generator's vocabulary spec:

| Type             | Example key                                              | Grammar                                   |
| ---------------- | -------------------------------------------------------- | ----------------------------------------- |
| Valid string     | `ValidTgts$`, `ValidCard$`, `Affected$`                  | [03](03-valid-string-grammar.md)          |
| Count expression | `NumDmg$`, `DigNum$`, `Amount$`                          | [04](04-count-expression-grammar.md)      |
| Cost string      | `Cost$`                                                  | [05](05-cost-string-grammar.md)           |
| SVar reference   | `SubAbility$`, `Execute$`, `StaticAbilities$`            | An `SVar:` name; resolved at compile time |
| Enum             | `Mode$`, `Origin$`, `Destination$`, `TgtZone$`           | A closed set per key                      |
| Boolean          | `Optional$`, `CombatDamage$`                             | `True` \| `False`                         |
| Integer          | `TokenAmount$` where literal                             | Digits, or a count expression             |
| Free text        | `SpellDescription$`, `TriggerDescription$`, `TgtPrompt$` | Display only, never parsed                |

Free-text values are the reason the split is on the **first** `$` and the value is otherwise opaque: descriptions
contain arbitrary punctuation.

## SVar bodies are three different things

An `SVar:` body is a param map only when a record key leads it. The parser has to branch before it splits:

| Body                                                            | Form              | Example                                                                  |
| --------------------------------------------------------------- | ----------------- | ------------------------------------------------------------------------ |
| Leads with `DB$`, `AB$`, `Mode$`, `Event$`, `SP$`, `ST$`, `RE$` | Param map         | `SVar:TrigDraw:DB$ Draw \| NumCards$ 1`                                  |
| Leads with any other `Head$`                                    | Amount expression | `SVar:X:Count$CardsInYourHand`, `SVar:Y:Enchanted$CardToughness/Minus.1` |
| No `$` at all                                                   | Literal           | `SVar:X:5`                                                               |

**87 distinct amount-expression heads.** `Count$` is the common one at 6,186 uses, and `Remembered$`, `TriggerCount$`,
`Targeted$`, `Sacrificed$`, `Number$`, `SVar$` and eighty more take the same shape: a head, a property, and the
`/Operator.operand` arithmetic of [04](04-count-expression-grammar.md).

Treating those heads as param keys is the mistake this table exists to prevent — `Enchanted` is not a key any ability
accepts.

## Compile-time resolution

`SubAbility$`, `Execute$` and similar name an SVar. At compile time the name is replaced by a direct reference to the
compiled sub-ability, so no lookup happens during a game ([ADR-0007](../../adr/0007-card-dsl-representation.md)).

Chains are recursive and can be several deep — Ragavan's trigger is `TrigTreasure → TrigExile → DBEffect → DBCleanup`.

## Invalidated by

- The P2 vocabulary scan reporting a param key the generator has no type for
- Any change to the first-key record-type mapping
