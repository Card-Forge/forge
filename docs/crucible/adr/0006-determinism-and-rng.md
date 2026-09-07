# ADR-0006 — Determinism and RNG

- **Status:** Accepted
- **Date:** 2026-09-06
- **Deciders:** `jc@archlab.pl`

## Context

Determinism is a product requirement, not hygiene. Every run writes a `manifest.json` claiming it can be reproduced, and
ADR-0005 makes each game's RNG the only thing standing between parallel execution and results that depend on scheduling
order.

**Randomness is everywhere in the Java engine, and mostly in the AI.** Counting `MyRandom.*` call sites:

| Module       | Call sites |
| ------------ | ---------: |
| `forge-ai`   |         81 |
| `forge-core` |         49 |
| `forge-game` |         20 |

By method: `getRandom` 116, `percentTrue` 29, `setRandom` 2, `splitIntoRandomGroups` 1. Shuffles go through
`Collections.shuffle(list, MyRandom.getRandom())` at eight sites including `Player.shuffle`, `Zone`, and `GameAction`.

**Forge is non-deterministic by default.** `MyRandom.random` is initialised to `new SecureRandom()`
(`forge-core/src/main/java/forge/util/MyRandom.java:34`). Reproducibility exists only when something calls
`setRandom(new Random(seed))` — the hook is there (`MyRandom.java:62`) but it is opt-in, and `SecureRandom` does not
implement the LCG at all.

That matters because `java.util.Random` **is** precisely specified — a 48-bit linear congruential generator with
documented `next(bits)`, `nextInt(bound)`, and a documented `Collections.shuffle` algorithm. It can be reproduced
exactly in Go. `SecureRandom` cannot.

**And the LCG has a ceiling worth measuring before adopting it wholesale:**

```text
java.util.Random distinct states : 2^48 = 2.815e+14
60-card deck permutations        : 60!  = 8.321e+81
fraction of shuffles reachable   :        3.383e-68
```

The honest reading is narrower than "shuffles are broken". Distinct 7-card openers number about 1.95e+12, so roughly 144
generator states map to each one — **opening hands are adequately covered**. The real costs are downstream: deep draw
sequences come from a structured sliver of permutation space, and successive shuffles within one game (mulligan, tutor,
`Brainstorm`) advance the same LCG stream, so they are not independent. For a tool whose output is distributional claims
about card draw, a 48-bit ceiling is a needless limit on what those claims can mean.

## Decision Drivers

- Reruns from a manifest must be byte-identical, or the manifest is a lie.
- Differential testing needs Java-identical shuffles, at least for fixtures without an AI.
- AI behaviour will diverge from Java during the port. That divergence must not silently perturb card draw.
- Statistical output quality is the product. The generator should not be the limiting factor.

## Considered Options

### Generator

1. **`javarand` everywhere.** Simple, one implementation, parity for free. Rejected — adopts the 2^48 ceiling as a
   permanent property of every statistical claim Crucible makes, to buy a property only the oracle needs.
2. **`math/rand/v2` everywhere.** Good statistics, no parity. Rejected — gives up cheap fixture-level differential
   testing.
3. **Both, selected per run.** **Chosen.**

### Stream structure

1. **One RNG per game.** Rejected — the AI is 81 of 150 call sites, so any AI divergence shifts every later shuffle and
   turns a behaviour difference into an unrelated card-draw difference.
2. **Streams partitioned by purpose.** **Chosen.**

## Decision

**Two generators, selected by run mode, behind one interface on `*Game`.**

| Mode                | Generator                                          | Used for                                     |
| ------------------- | -------------------------------------------------- | -------------------------------------------- |
| Production, default | `math/rand/v2` — ChaCha8                           | Every real simulation run                    |
| Parity              | `pkg/javarand` — bit-compatible `java.util.Random` | Differential testing against the oracle only |

`pkg/javarand` reproduces the 48-bit LCG exactly — seed scrambling, `next(bits)`, `nextInt(bound)` including its
power-of-two special case and rejection loop — plus the `Collections.shuffle` algorithm and `percentTrue`. Its M1 exit
gate is 10^6 seeded draws and shuffles diffed against a Java dump, and it is never the default.

**Streams are partitioned by purpose, seeded independently.**

```text
gameSeed(runSeed, gameIndex, stream) -> per-stream generator

stream Shuffle : deck shuffles, library ordering, random selection among cards
stream Game    : coin flips, dice, rules-mandated randomness
stream AI      : tie-breaking, percentTrue, exploration
```

The partition exists for one reason: **AI divergence must not move the cards.** The Go AI will not consume randomness in
the same order as Java's during the port, and with a single stream that difference would shift every subsequent shuffle
— presenting a behaviour difference as a card-draw difference and making every parity failure ambiguous.

**Java parity does not rely on matching RNG consumption.** Full-game replay (ADR-0010, Layer 3) records the AI's
decisions _and the resulting shuffle permutations_, and Go applies the recorded permutations rather than generating its
own. Consumption counts may then differ freely without desynchronising anything. `javarand` is what makes the cheaper
Layer 2 case work: scenario fixtures with no AI, run from the same seed in both engines, produce identical shuffles with
nothing recorded.

**Nothing reads a global generator.** The RNG reaches code through `*Game`, same as the card database (GO-2, ADR-0005).
`math/rand`'s package-level functions are banned by lint, since they share a global source and would reintroduce exactly
the cross-game coupling ADR-0005 removes.

**The manifest records the run seed, and a failed game records its own.** A panicking game is reproducible in isolation
from `runSeed` plus `gameIndex` without re-running the batch.

## Consequences

**Good.** Production statistics are limited by sample count rather than by generator state. A parity failure means a
rules difference, because AI divergence cannot reach the shuffle stream. Any single game out of a million-game run is
reproducible from two integers. `SecureRandom`'s non-determinism, the reason Forge cannot do this today, is gone by
construction rather than by remembering to call a setter.

**Bad.** Two generators mean two code paths and a mode flag that can be set wrong, so parity runs and production runs
are not the same binary behaviour — the mode belongs in the manifest and in the parity harness output, or someone will
compare a ChaCha8 run against the oracle and be confused for an afternoon. Partitioned streams also mean a new
randomness consumer must be assigned to a stream deliberately; putting AI tie-breaking on the `Game` stream would
quietly restore the coupling this ADR removes, and nothing but review catches it.

**Neutral.** `javarand` is a permanent piece of code that production never executes. It is small and its correctness is
pinned by a total gate, so the maintenance cost is low, but it will look like dead code to anyone who has not read
ADR-0010.

## Related

- [01-go-coding-standards.md](../guidelines/01-go-coding-standards.md) — GO-2, injected dependencies
- [03-testing-standards.md](../guidelines/03-testing-standards.md) — TEST-11, no default `math/rand` source in tests
- ADR-0005 — per-game RNG is what makes parallel execution reproducible
- ADR-0010 — differential testing, which is the only consumer of `javarand`
