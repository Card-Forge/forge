# Architecture Decision Records

- **Status:** Active

Process, format, and numbering: [../guidelines/04-adr-process.md](../guidelines/04-adr-process.md).

Numbers are permanent. Files are never deleted. A superseded ADR keeps its number and gains a
`**Status:** Superseded by ADR-nnnn` first line.

## Index

| ADR                                                         | Title                                          | Status             |
| ----------------------------------------------------------- | ---------------------------------------------- | ------------------ |
| [0001](0001-fork-layout-and-upstream-sync.md)               | Fork layout and upstream sync                  | Accepted           |
| [0002](0002-toolchain-and-dependency-policy.md)             | Toolchain and dependency policy                | Accepted           |
| [0003](0003-go-project-layout.md)                           | Go project layout                              | Accepted           |
| [0004](0004-java-to-go-translation-patterns.md)             | Java to Go translation patterns                | Accepted           |
| [0005](0005-concurrency-model.md)                           | Concurrency model                              | Accepted           |
| [0006](0006-determinism-and-rng.md)                         | Determinism and RNG                            | Accepted           |
| [0007](0007-card-dsl-representation.md)                     | Card DSL representation                        | Accepted           |
| [0008](0008-effect-dispatch.md)                             | Effect dispatch                                | Superseded by 0017 |
| [0009](0009-game-state-representation.md)                   | Game state representation                      | Accepted           |
| [0010](0010-differential-testing-strategy.md)               | Differential testing strategy                  | Accepted           |
| [0011](0011-card-corpus-scoping.md)                         | Card corpus scoping                            | Accepted           |
| [0012](0012-ports-and-adapters.md)                          | Ports and adapters, and where they stop        | Accepted           |
| [0013](0013-telemetry-event-bus.md)                         | Telemetry event bus and schema versioning      | Accepted           |
| [0014](0014-telemetry-storage-format.md)                    | Telemetry storage format                       | Accepted           |
| [0015](0015-upstream-sync-procedure.md)                     | Upstream sync procedure                        | Accepted           |
| [0016](0016-reporting-without-duckdb.md)                    | Reporting without DuckDB for v1                | Accepted           |
| [0017](0017-effects-inside-engine-generated-registry.md)    | Effects inside engine, generated registry      | Accepted           |
| [0018](0018-instant-sorcery-spell-object.md)                | Instant/sorcery spells as stack objects        | Accepted           |
| [0019](0019-interactive-priority.md)                        | Interactive priority (CR 117)                  | Accepted           |
| [0020](0020-static-triggers-resolve-immediately.md)         | Static triggers resolve immediately            | Accepted           |
| [0021](0021-phasing-battlefield-view.md)                    | Phasing: battlefield view excludes phased-out  | Accepted           |
| [0022](0022-as-enters-replacements.md)                      | "As enters" replacements before landing        | Accepted           |
| [0023](0023-granted-abilities-over-compiled-definitions.md) | Granted abilities as a compiled-trait overlay  | Accepted           |
| [0024](0024-combat-declaration-legality.md)                 | Combat declaration legality                    | Accepted           |
| [0025](0025-continuous-effect-evaluation-order.md)          | Continuous effects: layer order and dependency | Accepted           |
| [0026](0026-turn-driver.md)                                 | Turn driver: priority wired into turns         | Accepted           |
| [0027](0027-per-target-fizzle-check.md)                     | CR 608.2b: per-target fizzle check             | Accepted           |
| [0028](0028-ward-native-triggered-ability.md)               | Ward: a natively constructed triggered ability | Accepted           |
| [0029](0029-planechase-active-plane-state.md)               | Planechase: active-plane state                 | Accepted           |

## Numbering

No gap and no missing number: 0001-0029, every number used exactly once. Numbers are allocated when an ADR is written,
never reserved — the plan lists remaining subjects without numbers for that reason.

The plan's M0 exit gate asked for ADR-0001 through ADR-0011 `Accepted`. The three subjects after it — ports and
adapters, the telemetry event bus, the storage format — took the next free numbers as they were written.
