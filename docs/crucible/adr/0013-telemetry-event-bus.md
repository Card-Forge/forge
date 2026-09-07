# ADR-0013 — Telemetry Event Bus and Schema Versioning

- **Status:** Proposed
- **Date:** 2026-09-07
- **Deciders:** `jc@archlab.pl`

## Context

The engine emits, telemetry consumes ([ADR-0005](0005-concurrency-model.md)). Forge defines 59 `GameEvent*` classes, and
Crucible's metric definitions require more than Forge emits — a per-turn castability probe, per-turn mana samples, hand
tenure ([`../telemetry/metric-definitions.md`](../telemetry/metric-definitions.md)).

**Volume decides the design.** From the emissions those metrics require, at a typical 12-turn game:

| Rows per game                          |         |
| -------------------------------------- | ------: |
| Game outcome                           |       1 |
| Hand tenure, per card that enters hand |      19 |
| Mana sample, per turn                  |      12 |
| Castability probe rows                 |      48 |
| **Total**                              | **~80** |

At 10^6 games that is **80 million rows**, and the event stream behind them is larger still — every zone change, cast,
resolution and damage event, discarded after folding.

[`../design/engine-design.md`](../design/engine-design.md) left one question open, and it is a determinism question
rather than a performance one: **what happens when a telemetry writer falls behind?** Blocking costs throughput.
Dropping changes output, which would make a run's results depend on machine load — and reproducibility is a product
requirement ([ADR-0006](0006-determinism-and-rng.md)).

## Decision Drivers

- Determinism cannot depend on timing. Two runs with the same seed must produce identical telemetry.
- No allocation on the emission path — it runs millions of times per game.
- The engine must not know what records events ([ADR-0012](0012-ports-and-adapters.md), the sink is a port).
- Historical batches must stay interpretable after definitions change.

## Considered Options

### The emission path

1. **Interface per event type.** Idiomatic and unusable here: an interface value per event allocates and escapes,
   millions of times per game.
2. **Flat struct, emitted through a channel to a recorder goroutine.** The obvious concurrent design, and the source of
   the backpressure problem.
3. **Flat struct, folded synchronously in the game's own goroutine.** **Chosen.**

### Backpressure

1. Unbounded buffer — unbounded memory.
2. Bounded, drop on full — **rejected outright.** Output would depend on scheduling, which destroys the guarantee
   ADR-0006 exists to provide.
3. Bounded, block on full — correct but costs throughput on every game.
4. **No queue on the common path at all.** **Chosen.**

## Decision

**The recorder folds events synchronously, inside the game's goroutine. There is no channel on the common path.**

```go
type Sink interface{ Emit(Event) }

type Event struct {
    Kind     Kind      // TurnBegan, ZoneChange, SpellCast, DamageDealt, ...
    Turn     uint16
    Phase    PhaseType
    Active   PlayerID
    Actor    PlayerID
    Card     CardID
    Target   EntityID
    From, To ZoneType
    Amount   int32
    Flags    EventFlags
    Detail   uint32    // Kind-specific enum payload
}
```

A game already owns its state exclusively ([ADR-0005](0005-concurrency-model.md)), so its recorder can too. Folding in
place means **the backpressure question does not arise**: there is no queue to fill, no drop policy to get wrong, and no
scheduling input to the output. The worker writes finished per-game rows to its own shard when the game ends, which is
once per game rather than once per event.

**Two recording levels, chosen per game by the runner.**

| Level            | Share                                | Behaviour                                                         |
| ---------------- | ------------------------------------ | ----------------------------------------------------------------- |
| `LevelAggregate` | ~99%                                 | Events fold into counters in place. Nothing per-event is retained |
| `LevelFull`      | ~1% sampled, plus every errored game | The whole stream is retained for drill-down and debugging         |

`LevelFull` is where a buffer exists, and it is bounded and **blocking** — never dropping. At 1% of games the throughput
cost is negligible, and a dropped event in the one game someone is debugging is worse than a slow one.

**A clone's sink is a discard.** The AI explores lines that never happened; a clone holding the game's sink would record
imagined casts as real ([`../design/engine-design.md`](../design/engine-design.md) §5).

**`SchemaVersion` is stamped on every row and every shard**, alongside `MetricsVersion`
([MET-1](../telemetry/metric-definitions.md)). They version different things and move independently: the schema is what
was emitted, the metrics version is how it was interpreted. Adding an event kind or a field increments the schema;
changing what "mana flood" means increments the metrics version.

Tooling refuses to merge shards with differing `SchemaVersion`. A reader that silently unions two schemas produces a
column that means one thing for half the rows.

## Consequences

**Good.** Determinism holds by construction rather than by policy — with no queue on the common path there is nothing
for machine load to influence. The emission path allocates nothing. The engine still knows only the `Sink` interface, so
the port boundary is intact. Sampling keeps full detail available for the games that need it without paying for it on
the 99% that do not.

**Bad.** Synchronous folding puts recorder cost directly in the game's critical path, so a slow metric slows every game
rather than one goroutine — the recorder now needs the same benchmark discipline as the priority loop (GO-16).
Aggregate-level games also cannot be re-analysed after the fact: if a metric definition changes, the batch must be
re-run, which is exactly what writing `metric-definitions.md` before the engine was meant to reduce and cannot
eliminate.

**Neutral.** Two recording levels mean two code paths and a sampling decision that belongs in the manifest, or a
drill-down will be attempted on a run that never retained the detail.

## Related

- [ADR-0005](0005-concurrency-model.md) — one goroutine per game, which is what makes synchronous folding safe
- [ADR-0006](0006-determinism-and-rng.md) — the guarantee dropping would have broken
- [ADR-0014](0014-telemetry-storage-format.md) — what the shards are written as
- [`../telemetry/metric-definitions.md`](../telemetry/metric-definitions.md) — the emissions this must carry
