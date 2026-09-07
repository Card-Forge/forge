# ADR-0012 — Ports and Adapters, and Where They Stop

- **Status:** Proposed
- **Date:** 2026-09-07
- **Deciders:** `jc@archlab.pl`

## Context

Crucible already uses ports and adapters for the three things that vary, and has never said so. Nothing on file explains
why the domain core is deliberately not layered, which makes it a question that gets asked once per new contributor and
answered from memory.

**The three existing ports:**

| Port               | Adapters                                             | Decided in                                                                  |
| ------------------ | ---------------------------------------------------- | --------------------------------------------------------------------------- |
| `PlayerController` | Scripted, Replay, AI                                 | Inherited from Forge — 110 abstract methods                                 |
| `event.Sink`       | Telemetry recorder, discard for AI clones            | [ADR-0005](0005-concurrency-model.md), [design](../design/engine-design.md) |
| RNG                | `javarand` for parity, `math/rand/v2` for production | [ADR-0006](0006-determinism-and-rng.md)                                     |

`PlayerController` is the load-bearing one. [ADR-0010](0010-differential-testing-strategy.md)'s replay parity works
_because_ it is a port: a recorded Java decision log is just another adapter, which is what lets the AI be removed from
a rules comparison. The strategy is hexagonal testing whether or not it is called that.

**What makes Crucible unusual is the shape of the rest.** There is no database, no HTTP, no queue, no UI. Card scripts
and decklists are files read once at startup; everything after that is CPU-bound computation over in-memory state. The
classic payoff — swap Postgres for an in-memory double in tests — has no analogue.

And the domain is not what varies. It is 203 ability APIs of irreducible Magic rules, stable by definition, while the
things that change are exactly the three already behind interfaces.

## Decision Drivers

- The question recurs, and an unwritten answer is re-litigated.
- [ADR-0003](0003-go-project-layout.md): `forge-game` has 82 direct package cycles, so the engine core is one package
  and cannot be layered.
- [GO-16](../guidelines/01-go-coding-standards.md): the priority loop and state-based-action check run millions of times
  per game across 10^5–10^6 games.
- Testability is already achieved by the three existing ports; more would add cost without adding reach.

## Considered Options

1. **Hexagonal throughout, including the engine core.** Domain, application and infrastructure as separate packages with
   ports between them. Rejected on two independent grounds: it pushes toward splitting a core that
   [ADR-0003](0003-go-project-layout.md) measured as uncompilable when split, and it puts interface dispatch on the hot
   path that [GO-16](../guidelines/01-go-coding-standards.md) exists to keep clear.
2. **No ports anywhere.** Direct calls throughout, concrete types everywhere. Rejected — it would make replay parity
   impossible, since swapping the controller is the mechanism.
3. **Ports at the engine boundary, direct calls inside it.** **Chosen.**

## Decision

**The boundary where ports apply is exactly the engine boundary.**

```text
outside internal/engine   ports for anything that varies; dispatch cost is irrelevant
crossing the boundary     the three ports above, plus new ones by this rule
inside internal/engine    direct calls, concrete types, no interface introduced for structure
```

**A new port is justified when something genuinely varies at runtime or in tests.** Not when it would make a diagram
tidier. Anticipated:

| Port              | Adapters                                 | Milestone |
| ----------------- | ---------------------------------------- | --------- |
| Telemetry storage | NDJSON+zstd, Parquet, DuckDB             | M8        |
| Report output     | Markdown, HTML, JSON                     | M9        |
| Deck source       | Local `.dck` files, a metagame API later | M8        |

**Inside the engine, an interface needs a reason that is not structure.** `Effect` qualifies — 203 implementations
dispatched by API type, which is variation, and its cost is one array index and one indirect call per resolution
([ADR-0008](0008-effect-dispatch.md)). An interface introduced so that two halves of the rules engine can be described
as separate layers does not qualify, and the compiler will not stop it, so review has to.

**The vocabulary is adopted, the layering is not.** Calling `PlayerController` a port is useful — it explains why it has
110 methods and why that is acceptable. Calling `internal/engine` a hexagon and splitting it into domain and application
packages is the thing this ADR exists to prevent.

## Consequences

**Good.** A recurring design question has a written answer, including the measurement behind it. The three existing
ports get a name, which makes the differential testing strategy easier to explain than "we swap the controller". New
ports outside the engine have a clear justification test, so M8 and M9 do not each re-argue it.

**Bad.** "Ports and adapters, except in the part where most of the code is" is a genuinely awkward position to hold, and
it will read to anyone who likes the pattern as a rationalisation for not applying it. The defence is a measurement — 82
package cycles — rather than taste, but the defence has to be repeated each time, which is why it is written here.
Refusing interfaces inside the engine also means the core cannot be unit-tested by substituting doubles; that is
deliberate, and it is what [TEST-5](../guidelines/03-testing-standards.md) fixtures replace.

**Neutral.** The boundary is drawn at a package, so it moves if `internal/engine` is ever split. Nothing currently
suggests it can be, and any proposal to split it re-opens this ADR along with ADR-0003.

## Related

- [ADR-0003](0003-go-project-layout.md) — the 82 cycles that make the core one package
- [ADR-0008](0008-effect-dispatch.md) — the one interface inside the engine, and why it earns its place
- [ADR-0010](0010-differential-testing-strategy.md) — replay parity, which is a port swap
- [`../design/engine-design.md`](../design/engine-design.md) — how the ports compose at runtime
