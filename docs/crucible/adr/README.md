# Architecture Decision Records

- **Status:** Active

Process, format, and numbering: [../guidelines/04-adr-process.md](../guidelines/04-adr-process.md).

Numbers are permanent. Files are never deleted. A superseded ADR keeps its number and gains a
`**Status:** Superseded by ADR-nnnn` first line.

## Index

| ADR                                             | Title                                     | Status   |
| ----------------------------------------------- | ----------------------------------------- | -------- |
| [0001](0001-fork-layout-and-upstream-sync.md)   | Fork layout and upstream sync             | Accepted |
| [0002](0002-toolchain-and-dependency-policy.md) | Toolchain and dependency policy           | Accepted |
| [0003](0003-go-project-layout.md)               | Go project layout                         | Accepted |
| [0004](0004-java-to-go-translation-patterns.md) | Java to Go translation patterns           | Accepted |
| [0005](0005-concurrency-model.md)               | Concurrency model                         | Accepted |
| [0006](0006-determinism-and-rng.md)             | Determinism and RNG                       | Accepted |
| [0007](0007-card-dsl-representation.md)         | Card DSL representation                   | Accepted |
| [0008](0008-effect-dispatch.md)                 | Effect dispatch                           | Accepted |
| [0009](0009-game-state-representation.md)       | Game state representation                 | Accepted |
| [0010](0010-differential-testing-strategy.md)   | Differential testing strategy             | Accepted |
| [0011](0011-card-corpus-scoping.md)             | Card corpus scoping                       | Accepted |
| [0012](0012-ports-and-adapters.md)              | Ports and adapters, and where they stop   | Accepted |
| [0013](0013-telemetry-event-bus.md)             | Telemetry event bus and schema versioning | Accepted |
| [0014](0014-telemetry-storage-format.md)        | Telemetry storage format                  | Accepted |

## Numbering

No gap and no missing number: 0001-0014, every number used exactly once. Numbers are allocated when an ADR is written,
never reserved — the plan lists remaining subjects without numbers for that reason.

The plan's M0 exit gate asked for ADR-0001 through ADR-0011 `Accepted`. The three subjects after it — ports and
adapters, the telemetry event bus, the storage format — took the next free numbers as they were written.
