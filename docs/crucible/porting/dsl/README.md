# Card Script DSL

- **Status:** Active
- **Applies to:** `internal/carddb` and `internal/carddb/compile`
- **Derived from:** the corpus as of 2026-09-07

Six grammars. Together they specify everything `internal/carddb` must parse, and they are the input to the typed param
struct generator ([ADR-0007](../../adr/0007-card-dsl-representation.md), [ADR-0008](../../adr/0008-effect-dispatch.md)).

| Grammar                                            | Governs                            | Java counterpart                   |
| -------------------------------------------------- | ---------------------------------- | ---------------------------------- |
| [Card script](01-card-script-grammar.md)           | The `Key:Value` line format        | `CardRules.Reader.parseLine`       |
| [Param map](02-param-map-grammar.md)               | `Key$ Value \| Key$ Value`         | `AbilityFactory.getMapParams`      |
| [Valid string](03-valid-string-grammar.md)         | `Creature.Green+attacking+YouCtrl` | `CardProperty` (2,135 LOC)         |
| [Count expression](04-count-expression-grammar.md) | `Count$CardsInYourHand/Twice`      | `AbilityUtils` (3,950 LOC)         |
| [Cost string](05-cost-string-grammar.md)           | `2 R T Sac<1/Creature>`            | 52 files in `forge-game/.../cost/` |

---

## How these were derived

**From the corpus, not from reading the Java.** Reading 6,000 lines of `CardProperty` and `AbilityUtils` would produce a
grammar of what Java _can_ accept; extracting from 33,686 scripts produces a grammar of what is _used_, which is what
must actually parse.

Each grammar states the measurements behind it and the command that produced them. Corpus totals as of 2026-09-07:

```console
$ find forge-gui/res/cardsfolder -name '*.txt' | wc -l
33682
$ find forge-gui/res/cardsfolder -name '*.txt' -exec cat {} + | wc -l
302710
```

## What these are for

**The vocabulary is a build gate, not documentation.** The P2 exit gate is a corpus scan reporting zero unknown
top-level keys, param keys, card properties, count heads or cost parts. Anything the grammar does not cover fails the
build, and the deliberate-exclusion allowlist lives in [`../parity-matrix.md`](../parity-matrix.md).

That only works if the grammars are complete against the corpus rather than against intuition, which is why every count
below is stated rather than described.

## Three findings that change how the parsers are written

**1. Count expressions embed valid strings, with spaces.** `Count$Valid Creature.YouCtrl+powerGE1/LimitMax.1` is one
expression containing a whole valid string. A count expression is not a token, and cannot be lexed as one.

**2. 12.3% of cost `<>` bodies contain spaces** — 726 of 5,887. Splitting a cost string on whitespace is wrong; `<...>`
must be treated as a bracketed region first.

**3. Valid strings have two independent negation forms.** A `!` prefix on a property (459 uses) and a `non` prefix baked
into the property name (1,424 uses). They are not interchangeable and both must be supported.

## Related

- [ADR-0007](../../adr/0007-card-dsl-representation.md) — compile once, definitions immutable
- [ADR-0008](../../adr/0008-effect-dispatch.md) — the generated registry these feed
- [`../parity-matrix.md`](../parity-matrix.md) — support status and exclusions
