# Keyword Grammar

- **Status:** Active
- **Java counterpart:** `forge-game/src/main/java/forge/game/keyword/` (38 files), expanded by
  `CardFactoryUtil.setupKeywordedAbilities`

The value of every `K:` line. **18,245 occurrences**, the second most common ability-bearing key after `SVar:`.

## Grammar

```ebnf
keyword     = head , { ":" , argument } ;
head        = word , { " " , word } ;        (* may contain spaces: "First Strike" *)
argument    = { any-char - ":" } ;           (* positional; meaning depends on head *)
```

Colon-separated and **positional**. There is no key naming the arguments, so each head defines its own arity and meaning
— which is why this is a grammar per head rather than one uniform rule.

## Measurements

```console
$ find forge-gui/res/cardsfolder -name '*.txt' -exec cat {} + | grep -c '^K:'
18245
$ find forge-gui/res/cardsfolder -name '*.txt' -exec cat {} + \
    | grep '^K:' | sed 's/^K://; s/:.*//' | sort -u | wc -l
253
```

**253 distinct heads.** Argument counts:

| Arguments | Occurrences | Share |
| --------: | ----------: | ----: |
|         0 |      11,202 | 61.4% |
|         1 |       5,169 | 28.3% |
|         2 |       1,427 |  7.8% |
|         3 |         203 |  1.1% |
|         4 |         189 |  1.0% |
|         5 |          54 |       |
|         6 |           1 |       |

**7,043 keywords (38.6%) take at least one argument.** Most parameterised heads: `Enchant`, `Equip`, `etbCounter`,
`ETBReplacement`, `Cycling`, `Kicker`, `Chapter`, `Ward`, `Flashback`, `Crew`, `Morph`, `Landwalk`.

## Two traps

**Heads contain spaces.** `First Strike`, `Double Strike`, `Cumulative Upkeep`. So a head cannot be lexed as a single
word, and splitting a `K:` line on whitespace is wrong for the same reason it is wrong for cost strings
([05](05-cost-string-grammar.md)).

**Arguments are positional and heterogeneous.** They are not all the same type:

```text
K:Ward:2                       an amount
K:Dash:4 R W                   a mana cost (grammar 05)
K:Hexproof:White               a colour
K:Awaken:3:4 U                 a number and a mana cost
K:Reinforce:1:1 W              a number and a mana cost
K:ETBReplacement:Other:DBCounter   a mode and an SVar reference
```

`K:Ward:2` and `K:Hexproof:White` have the same shape and completely different argument types. A generic "keyword plus
string arguments" representation defers the whole problem to every consumer; the compiled form gives each head a typed
argument struct, the same approach as ability params ([02](02-param-map-grammar.md), GO-8).

## Keywords are abilities in disguise

Most keywords expand into triggers, statics, replacements or activated abilities. `CardFactoryUtil` does that expansion
at card construction in Java — Cascade synthesising a `CascadeX` SVar, Morph copying `X`.

**Crucible expands them at compile time instead** ([ADR-0007](../../adr/0007-card-dsl-representation.md)), because the
expansion is a deterministic function of the script. So the keyword grammar is a front-end to the same compiled
structures the other four grammars produce, not a parallel system.

That also means a keyword can pull in every other grammar: `K:Dash:4 R W` contains a cost string, and
`K:ETBReplacement:Other:DBCounter` names an SVar holding a param map.

## Invalidated by

- A 254th head appearing in the corpus with no compiled expansion
- Any head whose arguments are not positional
