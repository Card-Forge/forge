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

**1,210 distinct param keys** across the corpus:

```console
$ find forge-gui/res/cardsfolder -name '*.txt' -exec cat {} + \
    | grep -E '^(A|T|S|R|SVar):' \
    | grep -oE '(^|\| *)[A-Za-z0-9_]+\$' | grep -oE '[A-Za-z0-9_]+' \
    | sort -u | wc -l
1210
```

Distribution is steep, and this is the number that sizes the generated param structs
([ADR-0007](../../adr/0007-card-dsl-representation.md)):

|      Keys | Share of param occurrences |
| --------: | -------------------------: |
|    top 25 |                      66.5% |
|    top 50 |                      80.4% |
|   top 100 |                      90.6% |
|   top 200 |                      96.3% |
|   top 400 |                      98.9% |
|   top 800 |                      99.9% |
| all 1,210 |                       100% |

**The tail is not optional.** A key used by one card is still a key that card needs, and the corpus gate
([ADR-0011](../../adr/0011-card-corpus-scoping.md)) is scoped by deck, not by frequency — so which of the 1,210 matter
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

## Compile-time resolution

`SubAbility$`, `Execute$` and similar name an SVar. At compile time the name is replaced by a direct reference to the
compiled sub-ability, so no lookup happens during a game ([ADR-0007](../../adr/0007-card-dsl-representation.md)).

Chains are recursive and can be several deep — Ragavan's trigger is `TrigTreasure → TrigExile → DBEffect → DBCleanup`.

## Invalidated by

- The P2 vocabulary scan reporting a param key the generator has no type for
- Any change to the first-key record-type mapping
