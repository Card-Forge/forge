# Card Script Grammar

- **Status:** Active
- **Java counterpart:** `forge-core/src/main/java/forge/card/CardRules.java`, `Reader.parseLine` (line number omitted
  deliberately: it moved twice in one upstream sync)
- **Port log:** [`../port-log/card-rules-reader.md`](../port-log/card-rules-reader.md)

One `.txt` file per card, under `forge-gui/res/cardsfolder/<letter>/`. Line-oriented, `Key:Value`, with one bare key
that carries no value at all.

## Grammar

```ebnf
script      = { line } ;
line        = comment | blank | entry | bare-key ;
comment     = "#" , { any-char } ;
entry       = key , ":" , value ;
bare-key    = "ALTERNATE" ;               (* no colon, no value *)
key         = letter , { letter | digit | "_" } ;
value       = { any-char - newline } ;
```

A line with no colon is not an error: the key is the whole line and the value is absent. `ALTERNATE` is the only such
line in the corpus, and it is also the one that switches faces.

## Faces, and the four ways parsing is stateful

A script fills an array of **seven faces**. Which one an entry lands on depends on state built by earlier lines, and
there are four separate mechanisms — not one.

| Mechanism               | Effect                                                                          | In corpus |
| ----------------------- | ------------------------------------------------------------------------------- | --------: |
| `ALTERNATE`             | Subsequent entries go to face 1                                                 |       875 |
| `SPECIALIZE:<COLOUR>`   | Subsequent entries go to face 2-6, by `WHITE BLUE BLACK RED GREEN`              |        95 |
| `Variant:<name>:<line>` | The rest of the line is re-parsed onto a named variant face of the current face |       297 |
| `CopyFaceFrom:<name>`   | Registers a placeholder resolved after the whole corpus has loaded              |        25 |

`Name:` is what creates a face: it constructs the face object at the current index, so an entry before the first `Name:`
on a face has nothing to attach to.

**`AlternateMode:` does not switch faces.** It records how the faces relate — `DoubleFaced`, `Adventure`, `Split`,
`Modal`, `Prepare`, `Flip`, `Specialize`, `Omen`, `Meld` — and nothing else. An earlier version of this document said
`AlternateMode:` performed the face switch, which would have put the wrong face on roughly 875 cards.

## Keys

31 distinct keys with a colon, plus the bare `ALTERNATE`. Counts as of 2026-09-08:

```console
$ find forge-gui/res/cardsfolder -name '*.txt' -exec cat {} + \
    | grep -oE '^[A-Za-z0-9_]+:' | sort | uniq -c | sort -rn
```

| Key                                                 |  Count | Value grammar                                                                        |
| --------------------------------------------------- | -----: | ------------------------------------------------------------------------------------ |
| `SVar:`                                             | 59,418 | `name ":" ( param-map \| count-expression \| text )` — see below                     |
| `Types:`                                            | 34,631 | Space-separated supertypes, card types, subtypes                                     |
| `Oracle:`                                           | 34,631 | Free text. `\n` is a literal line break. Display only                                |
| `ManaCost:`                                         | 34,630 | Mana cost, or the literal `no cost`                                                  |
| `Name:`                                             | 34,622 | Free text; the card's identity                                                       |
| `PT:`                                               | 19,199 | `power "/" toughness`, either may be `*` or a `Count$` reference                     |
| `A:`                                                | 18,452 | Param map, first key `SP$` or `AB$`                                                  |
| `K:`                                                | 18,245 | Keyword, optionally `name ":" arguments`                                             |
| `T:`                                                | 16,976 | Param map, first key `Mode$`                                                         |
| `DeckHas:`, `DeckHints:`, `DeckNeeds:`, `DeckRule:` | 13,590 | Deckbuilder metadata, `type$value` pairs                                             |
| `S:`                                                |  7,094 | Param map, first key `Mode$`                                                         |
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
| `SETCOLORID:`, `Lights:`                            |      2 | Long tail; each appears once                                                         |

**Two keys in the corpus are silently ignored by Forge**, because `parseLine` switches on the first character and then
matches the whole key, and neither matches anything:

| Key           | Card                   | What it looks like it meant |
| ------------- | ---------------------- | --------------------------- |
| `ODeckHints:` | `spirit_of_resilience` | `DeckHints:`, mistyped      |
| `DBCleanup:`  | `the_dawning_archaic`  | An `SVar:` body, unprefixed |

Both are corpus defects rather than vocabulary. A strict parser has to decide about them explicitly — Crucible's does,
in the port log — because "Forge ignores it" is behaviour, not permission.

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
- A change to which key switches faces, or a fifth way for parsing to be stateful
- Either ignored key being fixed upstream, or a third appearing
