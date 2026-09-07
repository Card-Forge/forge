# Card Script Grammar

- **Status:** Active
- **Java counterpart:** `forge-core/src/main/java/forge/card/CardRules.java:691` (`Reader.parseLine`)

One `.txt` file per card, under `forge-gui/res/cardsfolder/<letter>/`. Line-oriented, `Key:Value`.

## Grammar

```ebnf
script      = { line } ;
line        = comment | blank | entry ;
comment     = "#" , { any-char } ;
entry       = key , ":" , value ;
key         = letter , { letter | digit | "_" } ;
value       = { any-char - newline } ;
```

Parsing is stateful in exactly one way: `AlternateMode:` switches subsequent entries onto the next face
(`CardRules.java:868`). Everything before the first `AlternateMode:` belongs to face 0.

## Keys

31 distinct keys appear in the corpus. Counts as of 2026-09-07:

```console
$ find forge-gui/res/cardsfolder -name '*.txt' -exec cat {} + \
    | grep -oE '^[A-Za-z0-9_]+:' | sort | uniq -c | sort -rn
```

| Key                                                 |  Count | Value grammar                                                                        |
| --------------------------------------------------- | -----: | ------------------------------------------------------------------------------------ |
| `SVar:`                                             | 59,405 | `name ":" ( param-map \| count-expression \| text )` — see below                     |
| `Types:`                                            | 34,627 | Space-separated supertypes, card types, subtypes                                     |
| `Oracle:`                                           | 34,627 | Free text. `\n` is a literal line break. Display only                                |
| `ManaCost:`                                         | 34,626 | Mana cost, or the literal `no cost`                                                  |
| `Name:`                                             | 34,618 | Free text; the card's identity                                                       |
| `PT:`                                               | 19,198 | `power "/" toughness`, either may be `*` or a `Count$` reference                     |
| `A:`                                                | 18,449 | Param map, first key `SP$` or `AB$`                                                  |
| `K:`                                                | 18,245 | Keyword, optionally `name ":" arguments`                                             |
| `T:`                                                | 16,971 | Param map, first key `Mode$`                                                         |
| `DeckHas:`, `DeckHints:`, `DeckNeeds:`, `DeckRule:` | 13,590 | Deckbuilder metadata, `type$value` pairs                                             |
| `S:`                                                |  7,093 | Param map, first key `Mode$`                                                         |
| `AI:`                                               |  4,952 | AI hints, e.g. `RemoveDeck:All`                                                      |
| `R:`                                                |  1,693 | Param map, first key `Event$`                                                        |
| `AlternateMode:`                                    |    902 | `Split` \| `Transform` \| `Adventure` \| `Meld` \| `Specialize` \| `Modal` \| `Flip` |
| `Colors:`                                           |    379 | Colour override, comma-separated                                                     |
| `Loyalty:`                                          |    355 | Integer                                                                              |
| `Variant:`                                          |    297 | Functional variant marker                                                            |
| `HandLifeModifier:`                                 |    106 | Vanguard hand/life deltas                                                            |
| `SPECIALIZE:`                                       |     95 | Specialize face mapping                                                              |
| `Text:`                                             |     72 | Rules text override                                                                  |
| `Draft:`                                            |     48 | Draft-time behaviour                                                                 |
| `Defense:`                                          |     37 | Battle defense                                                                       |
| `CopyFaceFrom:`                                     |     25 | Face inheritance                                                                     |
| `MeldPair:`                                         |     14 | Meld partner name                                                                    |
| `SETCODEID:`, `ODeckHints:`, `Lights:`              |      3 | Long tail; each appears once                                                         |

`DBCleanup:` also appears once, as a malformed line in a single script — a real corpus defect worth knowing about before
the strict-mode parser rejects it.

## `SVar:` values

Three shapes, disambiguated by content rather than by a marker:

```ebnf
svar-entry  = "SVar:" , name , ":" , svar-value ;
svar-value  = param-map          (* holds a sub-ability: begins DB$/SP$/AB$/ST$/RE$ *)
            | count-expression   (* begins "Count$" *)
            | text ;             (* everything else: AI hints, literals, references *)
```

At load, only sub-ability and count SVars are compiled. The rest stay text, and runtime writes go to the per-card
overlay ([ADR-0007](../../adr/0007-card-dsl-representation.md)).

## Invalidated by

- An upstream sync introducing a 32nd top-level key — caught by the P2 vocabulary scan, not by review
- Any change to `AlternateMode:` face-switching semantics
