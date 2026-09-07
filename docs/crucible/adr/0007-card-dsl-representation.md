# ADR-0007 — Card DSL Representation

- **Status:** Accepted
- **Date:** 2026-09-07
- **Deciders:** `jc@archlab.pl`

## Context

Card behaviour is text. 33,682 scripts hold 103,611 parseable ability and variable lines:

| Key     |  Lines | Per card |
| ------- | -----: | -------: |
| `SVar:` | 59,405 |     1.76 |
| `A:`    | 18,449 |     0.55 |
| `T:`    | 16,971 |     0.50 |
| `S:`    |  7,093 |     0.21 |
| `R:`    |  1,693 |     0.05 |

**Java interprets those strings at runtime, per card instance, per game.** `CardRules.Reader` stores `A:`, `T:`, `S:`,
`R:` and `SVar:` values as raw strings; `AbilityFactory.getAbility` splits the param map, resolves the API name, parses
the cost, and recurses through `SubAbility$` every time a `Card` is constructed. A game builds roughly 120 cards. A
million-game run therefore repeats that work on the order of 10^8 times, for text that never changed.

**But the naive fix is wrong, and measuring says so.** There are 144 `setSVar`/`removeSVar` call sites in `forge-game`.
SVars are written during play, not only at load. Two distinct things are happening under one name:

| Role                                                    | Written by                              | Example                                                                 |
| ------------------------------------------------------- | --------------------------------------- | ----------------------------------------------------------------------- |
| **Definition** — an ability, a cost, a count expression | Card script, and card construction      | `SVar:TrigTreasure:DB$ Token \| ...`                                    |
| **Runtime value** — a scratchpad slot                   | 15 effect classes, `GameAction`, `Card` | `FlipCoinEffect`, `CountersNoteEffect`, `StaticEffectTimestamp`, `Foil` |

`CardFactoryUtil` also writes SVars while _building_ a card from keywords — Cascade synthesising `CascadeX`, Morph
copying `X`. That happens once per instance at construction, and is definition work wearing a runtime disguise.

Conflating the two is exactly why Java cannot cache: because any SVar might be written, none can be shared.

## Decision Drivers

- Parse work must be amortised across the run, not repeated per game (ADR-0005 shares the card database by pointer).
- The shared structure must be immutable, or goroutine-per-game needs locks.
- Typed parameters are the point of porting, not a side effect — `Map<String,String>` params are the largest silent-bug
  source in the Java engine (GO-8).
- Runtime SVar writes are real behaviour and cannot be dropped.

## Considered Options

1. **Interpret at runtime, as Java does.** Rejected — repeats 10^8 parses of unchanged text, and keeps params untyped.
2. **Compile everything to a fully static immutable AST.** Rejected — it cannot represent the 144 runtime writes. This
   is what the implementation plan proposed, and it is wrong.
3. **Compile definitions once; keep a small per-card runtime overlay for written values.** **Chosen.**

## Decision

**Split the two roles the Java `SVar` conflates.**

```text
load time, once per process
  .txt ──► line parse ──► typed AST ──► CompiledCard   immutable, shared by pointer
                                          abilities, triggers, statics, replacements,
                                          costs, valid specs, count expressions
                                          definition SVars, already resolved

game time, per card instance
  Card { def *CompiledCard; svars overlay }            tiny, mutable, per game
```

**Definitions compile once.** Every `A:`, `T:`, `S:`, `R:`, and every SVar reachable from them as a sub-ability, cost,
or count expression is parsed at load into typed structures, with `SubAbility$` chains resolved into direct references
rather than names. Nothing re-parses a string during a game.

**Runtime values live in a per-card overlay.** A `Card` holds no SVar map until something writes one; the first write
allocates a small map, and reads fall through to the compiled definitions. Copy-on-write, so the overwhelming majority
of cards carry no overlay at all and the shared structure stays immutable.

**Keyword-synthesised abilities are compile-time work.** Cascade, Morph, Dash and the rest expand during compilation,
not during card construction. They are deterministic functions of the script, so their output belongs in the shared
structure.

**Parameters are typed structs, generated per API from the DSL vocabulary spec.** `DealDamageParams` has an `expr.Count`
and a `valid.Spec`, not two strings. The generator's input is the vocabulary scan that already hard-fails on unknown
keys (P2 gate), so a param the generator does not know about is a build error rather than a silent nil.

**The compiled form is a build artefact, not a source format.** It is derived from the `.txt` files on every load and
never committed. Upstream card-script changes therefore arrive with no conversion step (ADR-0001).

## Consequences

**Good.** Parse cost becomes O(cards in corpus) instead of O(cards × games), which is the single largest performance
difference between the two engines. The shared structure is immutable, so goroutine-per-game needs no synchronisation
around it (ADR-0005). Typed params turn a class of silent Java bugs into compile errors. Malformed scripts fail at load,
naming the file, instead of throwing mid-game on card 12,004.

**Bad.** Two representations of a card exist — compiled definition and runtime overlay — and every read has to know
which one it is asking. Get that wrong and a card either ignores a runtime write or writes into shared state, and the
second failure mode is a data race that only appears under parallel load. The overlay's fallthrough also makes the SVar
lookup path harder to reason about than Java's single map.

**Neutral.** Load time grows, since all 33,682 scripts compile before the first game starts. Irrelevant for a batch run
of hours; noticeable when iterating on a single fixture, which argues for a corpus-scoped load in test binaries.

## Related

- [01-go-coding-standards.md](../guidelines/01-go-coding-standards.md) — GO-8, typed params
- [02-java-to-go-translation.md](../guidelines/02-java-to-go-translation.md) — PORT-2
- [00-master-implementation-plan.md](../00-master-implementation-plan.md) — Section 1.4, and the compile-once claim this
  ADR corrects
- ADR-0005 — sharing the compiled structure by pointer
- ADR-0008 — how a compiled ability reaches its effect
