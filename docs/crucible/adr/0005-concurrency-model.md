# ADR-0005 — Concurrency Model

- **Status:** Proposed
- **Date:** 2026-09-06
- **Deciders:** `jc@archlab.pl`

## Context

Concurrency is the reason Crucible is a port rather than a wrapper. Everything else could have been solved by scripting
the existing Java engine.

**The Java engine is effectively single-threaded, and its global state proves it.** Measured across the three modules
being ported:

| Module       | Files | Use `synchronized` | Thread-aware at all |
| ------------ | ----: | -----------------: | ------------------: |
| `forge-core` |   152 |                  5 |                   5 |
| `forge-game` |   805 |                  8 |                   7 |
| `forge-ai`   |   191 |                  2 |                   4 |

Eight files out of 805 in the rules engine. That is not an oversight — Forge is a desktop application that plays one
game at a time, so process-global mutable state is free. `MyRandom.random` is a `private static Random`
(`forge-core/src/main/java/forge/util/MyRandom.java:34`). `StaticData.lastInstance` is a `private static`
(`forge-core/src/main/java/forge/StaticData.java:64`). Running two games concurrently in one JVM would race on both.

Crucible's workload is the opposite shape: 10^5 to 10^6 games per run, each fully independent, no communication between
them, no shared mutable state required by the domain. Embarrassingly parallel, and currently unreachable.

The card database is what makes the naive approaches fail. 33,682 card scripts, 302,710 lines, parsed into an immutable
`CardRules` graph. Forge's own `CardStorageReader` is multi-threaded specifically because loading it is slow. Any design
that loads or copies that per game is dead on arrival.

## Decision Drivers

- Throughput is the product requirement. A run that takes a week is not a deck-tuning tool.
- Results must be reproducible from a seed (ADR-0006). Anything scheduling-dependent breaks that.
- The card database must be loaded once and shared, never copied.
- One malformed card must not kill a 100,000-game batch.
- Java's global-state habits are the highest-probability porting bug, so the model has to make them impossible rather
  than discouraged.

## Considered Options

1. **Process per game.** What Java effectively does today. Rejected — process startup plus a 33,682-file card database
   load per game dwarfs the game itself. This is precisely why the Java engine cannot serve this workload.
2. **Goroutine per game, shared mutable state guarded by locks.** A faithful port of the Java structure with
   `sync.Mutex` where `static` used to be. Rejected — serialises the exact thing the port exists to parallelise, and
   leaves every global as a latent race that only appears under load.
3. **Actor model.** Each game an actor, state reached only through channels. Rejected — games never communicate, so the
   channel machinery is pure overhead on the hot path.
4. **Goroutine per game over a bounded worker pool, sharing only immutable data.** **Chosen.**

## Decision

**The unit of parallelism is one game.** Not a turn, not an effect, not an AI search branch. Games are independent by
construction, which makes this the only boundary that needs no synchronisation at all.

**Nothing mutable is shared between games. Ever.** In practice that means three rules, all already enforced as
guidelines:

| Rule                                                                     | Guideline | What it prevents                                             |
| ------------------------------------------------------------------------ | --------- | ------------------------------------------------------------ |
| No package-level mutable state in `internal/engine` or `internal/carddb` | GO-2      | The direct translation of `MyRandom`, `StaticData`, `FModel` |
| No mutex in the engine                                                   | GO-3      | Locks reintroducing the serialisation the port removes       |
| Card DB and RNG injected via `*Game`, never reached globally             | GO-2      | A shared reference appearing by accident                     |

A `sync.Mutex` inside `internal/engine` is therefore not a fix. It is evidence that something mutable got shared, and
the correct response is to un-share it.

**The card database is loaded once, frozen, and shared by pointer.** After load it is never written, so concurrent
readers need no synchronisation. This is the single largest memory and startup saving in the design, and it is why the
compile-once AST decision (ADR-0007) matters at all: parse work done once is amortised across every game in the run.

**A bounded worker pool, not unbounded goroutines.** Default `GOMAXPROCS`, configurable. Goroutines are cheap but game
states are not, and 10^6 live game states is an out-of-memory error, not a scheduling problem. Work is pulled from a
channel of game specifications.

**No parallelism inside a game, including AI lookahead.** Someone will want to parallelise the search, and the answer is
no: game-level parallelism already saturates every core, so intra-game concurrency buys nothing while reintroducing
shared mutable state into the one place this ADR keeps clean. Lookahead stays sequential within its own goroutine.

**Each game gets its own RNG, derived from the run seed and the game index.** Never a shared source. A shared RNG makes
output depend on scheduling order, which would destroy reproducibility — the mechanism is ADR-0006's.

**Telemetry crosses the boundary by channel, one shard writer per worker.** No shared accumulator, no lock around a
counter. Aggregation happens in a single merge pass after the run.

**A panicking game fails alone.** Each worker recovers at the game boundary, records the failure with a full state dump
and the seed that produced it, and takes the next unit of work. The batch continues; the failure is reproducible from
its seed.

## Consequences

**Good.** Throughput scales with cores, which is the entire point of the port. Reproducibility survives parallelism,
because no two games can influence each other. The rules that keep it true are compiler- and lint-enforced rather than
conventional, so the Java habits cannot be ported by reflex. A single bad card costs one game instead of a run.

**Bad.** The prohibition on intra-game parallelism means one game is never faster than one core, so an interactive
single-game debug run stays as slow as Java. Memory becomes the scaling limit rather than CPU, and the worker count has
to be tuned against game-state size rather than set to core count and forgotten. `-race` on a parallel test suite is the
only thing standing between the design and a silently shared pointer, which makes TEST-7 load-bearing rather than
hygienic.

**Neutral.** Freezing the card database after load means no lazy loading and no hot reload; a card script change costs a
process restart. Acceptable for a batch tool, and it removes an entire class of concurrent-initialisation bugs.

## Related

- [01-go-coding-standards.md](../guidelines/01-go-coding-standards.md) — GO-2, GO-3, GO-14
- [03-testing-standards.md](../guidelines/03-testing-standards.md) — TEST-7, parallel tests under `-race`
- ADR-0006 — determinism and RNG, which this decision depends on
- ADR-0007 — compile-once AST, which is what makes sharing worthwhile
- ADR-0009 — state representation and clone cost, which sets the memory ceiling
