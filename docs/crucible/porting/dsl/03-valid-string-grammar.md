# Valid String Grammar

- **Status:** Active
- **Java counterpart:** `forge-game/src/main/java/forge/game/card/CardProperty.java` (2,135 LOC), `PlayerProperty.java`
  (517 LOC)

Predicates over game objects. The largest and most error-prone unit in the port outside `GameAction`.

## Grammar

```ebnf
valid       = alternative , { "," , alternative } ;      (* "," is OR *)
alternative = base , [ "." , property , { "+" , property } ] ;   (* "+" is AND *)
base        = card-type | card-name | "Any" | "You" | "Opponent" | "Player" | ... ;
property    = [ "!" ] , property-name ;
```

`,` is disjunction, `+` is conjunction. `Instant.YouCtrl,Sorcery.YouCtrl` means _(instant you control) or (sorcery you
control)_ — the property list does **not** distribute across alternatives.

## Which keys hold one

Seventy param keys start with `Valid`, and the prefix does not say what the value is. Fourteen of them hold something
else entirely — `ValidZone$ Hand`, `ValidCounterType$ ENERGY`, `ValidKeyword$ Landwalk:Swamp`, `ValidResult$ EQ6` — and
the `...Desc`, `...Des` and `...Message` suffixes mark prose. `Affected$` is the one valid-typed key without the prefix.

So the rule is a prefix minus a named exception list
([`internal/carddb/vocab`](../../../crucible/internal/carddb/vocab)), not a list of the keys that do hold one: upstream
adds `Valid` keys, and a missing entry on the exception list shows up as a zone name in the base vocabulary rather than
as silence.

`ValidTgtsDes$` and `ValidTgtsDesc$` both appear. Nothing parses either, which is why the typo survived.

## Measurements

56,224 valid strings across every valid-typed key and the `Count$Valid` family.

**Alternation is shallow**, over the 44,051 strings in the eight most common valid-typed keys:

| Alternatives | Occurrences | Share |
| -----------: | ----------: | ----: |
|            1 |      40,887 | 92.8% |
|            2 |       2,858 |  6.5% |
|            3 |         233 |  0.5% |
|            4 |          59 |       |
|            5 |           8 |       |
|            6 |           1 |       |

**252 distinct base tokens.** Most common: `Card`, `Creature`, `You`, `Player`, `Permanent`, `Artifact`, `Land`,
`Opponent`, `Any`, `Sorcery`, `Instant`, `Planeswalker`, `Enchantment`, `Spell`, `Emblem`.

A base is a card type, a supertype, a specific card name, or a player selector. Card names appear verbatim, which means
**a base token can contain spaces and punctuation** and cannot be lexed as an identifier.

**1,257 distinct property tokens.** Most common: `Self`, `YouCtrl`, `Other`, `EnchantedBy`, `YouOwn`, `OppCtrl`,
`IsRemembered`, `EquippedBy`, `nonLand`, `attacking`, `nonCreature`, `!token`, `AttachedBy`, `YouDontCtrl`,
`inZoneBattlefield`.

## Two negation forms, both required

They are not interchangeable:

| Form                      |  Uses | Example             | Meaning                             |
| ------------------------- | ----: | ------------------- | ----------------------------------- |
| `!` prefix on a property  |   459 | `Card.!token`       | Negates that property               |
| `non` baked into the name | 1,424 | `Permanent.nonLand` | A distinct property in Java's table |

`nonLand` is not parsed as `non` + `Land`; it is its own entry in `CardProperty`. Treating the `non` prefix as an
operator would produce different results for properties where Java defines only one of the pair.

## Property families

The properties group into recognisable families, which is how the Go port subdivides an otherwise flat 2,135-line
switch:

| Family                | Examples                                                          |
| --------------------- | ----------------------------------------------------------------- |
| Control and ownership | `YouCtrl`, `OppCtrl`, `YouOwn`, `YouDontCtrl`                     |
| Self reference        | `Self`, `Other`, `IsRemembered`, `IsImprinted`                    |
| Attachment            | `EnchantedBy`, `EquippedBy`, `AttachedBy`                         |
| Zone                  | `inZoneBattlefield`, `inZoneGraveyard`                            |
| Combat                | `attacking`, `blocking`, `blocked`                                |
| Type negation         | `nonLand`, `nonCreature`, `nonToken`                              |
| Numeric comparison    | `powerGE1`, `cmcLE3` — property name encodes operator and operand |
| Colour                | `Red`, `White`, `Monocolored`, `MultiColor`                       |

Numeric comparison properties are the family most likely to be mis-ported, because the operator is inside the token:
`powerGE1` is _power greater than or equal to 1_, and the suffix may itself be a count expression reference.

## Evaluation

Evaluated against a game state, so `valid` is a separate package importing `engine` rather than part of it
([ADR-0003](../../adr/0003-go-project-layout.md)). Signature is
`valid.Match(g *engine.Game, spec Spec, id EntityID) bool` — the spec is compiled at load, the game is passed in.

## Invalidated by

- A property token appearing in the corpus that the compiled spec cannot represent
- Any change to `,` / `+` precedence, which would silently alter thousands of targeting restrictions
