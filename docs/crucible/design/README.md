# Design

- **Status:** Active

Target design: how accepted ADRs compose, before any of it is built. Governed by `ARCH-10`
([../guidelines/06-architecture-docs.md](../guidelines/06-architecture-docs.md)).

These exist because ADRs are narrow by design. Each argues one decision and none shows how the pieces fit, so a
milestone can satisfy every ADR individually and still assemble something incoherent. A design document is where the
cross-ADR interactions get pinned.

Each is **replaced** by an architecture document once the code exists, not edited into one.

| Document                                                         | Composes                                    | Real at    |
| ---------------------------------------------------------------- | ------------------------------------------- | ---------- |
| [engine-design.md](engine-design.md)                             | ADR-0003, 0005, 0006, 0007, 0008, 0009      | M3, M5, M6 |
| [card-compilation-pipeline.md](card-compilation-pipeline.md)     | ADR-0007, 0008, 0011 + the six DSL grammars | M2, M3     |
| [concurrency-and-determinism.md](concurrency-and-determinism.md) | ADR-0005, 0006, 0009                        | M8         |
| [telemetry-pipeline.md](telemetry-pipeline.md)                   | ADR-0005, 0006, 0013, 0014 + `MET-n`        | M8, M9     |

## Not written

| Document             | Why                                                                                                                |
| -------------------- | ------------------------------------------------------------------------------------------------------------------ |
| `engine-state-model` | Covered by [engine-design.md](engine-design.md)'s lifetimes table. A separate document would duplicate it (DOC-11) |

## Related

- [../adr/README.md](../adr/README.md) — the decisions these compose
- [../architecture/system-overview.md](../architecture/system-overview.md) — what exists today
