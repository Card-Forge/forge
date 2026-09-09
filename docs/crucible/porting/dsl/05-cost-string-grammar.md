# Cost String Grammar

- **Status:** Active
- **Java counterpart:** 52 files in `forge-game/src/main/java/forge/game/cost/`

The value of `Cost$`. 13,979 occurrences in the corpus.

## Grammar

```ebnf
cost        = cost-part , { " " , cost-part } ;
cost-part   = mana-symbol
            | "T" | "Q"                          (* tap, untap *)
            | "X"
            | named-cost , [ "<" , body , ">" ] ;
named-cost  = letter , { letter | digit } ;
body        = field , { "/" , field } ;          (* field may contain spaces *)
field       = { any-char - "/" - ">" } ;
```

## The finding that matters most

**`<...>` bodies contain spaces, so a cost string cannot be split on whitespace.**

```text
<> bodies total : 5887
containing a space : 726  (12.3%)
```

```text
Sac<1/Creature.Other/another creature>
Sac<1/Permanent.nonLand/nonland permanent>
Discard<1/Card.OppOwn/card an opponent owns>
Sac<2/Blood.token/Blood token>
```

The parser must scan for `<` and consume to the matching `>` **before** splitting on spaces. A naive
`strings.Fields(cost)` produces `Sac<1/Creature.Other/another` and `creature>` as separate tokens — which is what a
first implementation will do, and it will pass on 87.7% of cost strings.

## Body fields

Bodies are `/`-separated and positional:

```text
Sac<1/Creature.Other/another creature>
     │ │                 └── description, display only, may contain spaces
     │ └── valid string (grammar 03)
     └── amount: integer, "X", or a count expression reference
```

The third field is display text and is never parsed. It is also where every space comes from, which is why the space
problem exists at all.

## Cost parts

**88 distinct part shapes** after normalising `<...>` bodies:

```console
$ cd crucible && go run ./tools/vocabscan -kind costPart | wc -l
88
```

A shell pipeline that splits on whitespace before normalising reports 637 instead, and the extra 549 are fragments of
the bodies it tore in half — the same trap as above, in the tool used to measure it.

| Part                           | Shape     | Meaning              |
| ------------------------------ | --------- | -------------------- |
| `1`, `2`, `3`, `X`, `0`        | bare      | Generic mana         |
| `W` `U` `B` `R` `G`            | bare      | Coloured mana        |
| `T`                            | bare      | Tap this permanent   |
| `Q`                            | bare      | Untap this permanent |
| `Sac<n/valid/desc>`            | bracketed | Sacrifice            |
| `Discard<n/valid/desc>`        | bracketed | Discard              |
| `SubCounter<n/type/...>`       | bracketed | Remove counters      |
| `AddCounter<n/type/...>`       | bracketed | Put counters         |
| `PayLife<n>`                   | bracketed | Pay life             |
| `PayEnergy<n>`                 | bracketed | Pay energy           |
| `ExileFromGrave<n/valid/desc>` | bracketed | Exile from graveyard |
| `tapXType<n/valid/desc>`       | bracketed | Tap other permanents |

Roughly 45 named cost part types in total, matching the 52 files in Java's `cost/` package.

## `or` — cost alternatives

The token `or` appears between cost parts, expressing alternative payments. It is a separator at the same level as a
space, not a cost part, and a parser that treats it as a named cost will silently accept nonsense.

## Amounts can be expressions

The first body field is an integer, `X`, or an SVar naming a count expression. So the cost parser depends on the count
expression parser ([04](04-count-expression-grammar.md)), which depends on the valid string parser
([03](03-valid-string-grammar.md)), which is why these three are compiled together.

## Invalidated by

- A named cost part appearing in the corpus with no entry in the compiled set
- Any body field count other than the positional forms above
