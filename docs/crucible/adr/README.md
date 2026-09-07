# Architecture Decision Records

- **Status:** Active

Process, format, and numbering: [../guidelines/04-adr-process.md](../guidelines/04-adr-process.md).

Numbers are permanent. Files are never deleted. A superseded ADR keeps its number and gains a
`**Status:** Superseded by ADR-nnnn` first line.

## Index

| ADR                                             | Title                           | Status   |
| ----------------------------------------------- | ------------------------------- | -------- |
| [0001](0001-fork-layout-and-upstream-sync.md)   | Fork layout and upstream sync   | Accepted |
| [0002](0002-toolchain-and-dependency-policy.md) | Toolchain and dependency policy | Accepted |
| [0003](0003-go-project-layout.md)               | Go project layout               | Accepted |
| [0005](0005-concurrency-model.md)               | Concurrency model               | Accepted |
| [0006](0006-determinism-and-rng.md)             | Determinism and RNG             | Accepted |
| [0007](0007-card-dsl-representation.md)         | Card DSL representation         | Accepted |
| [0008](0008-effect-dispatch.md)                 | Effect dispatch                 | Accepted |
| [0009](0009-game-state-representation.md)       | Game state representation       | Accepted |
| [0010](0010-differential-testing-strategy.md)   | Differential testing strategy   | Proposed |
| [0011](0011-card-corpus-scoping.md)             | Card corpus scoping             | Proposed |

## Remaining for the M0 gate

The plan's exit gate (§5, M0) is ADR-0001 through ADR-0011 all `Accepted`. Still to write:

| Next | Subject                         | Why it matters                                                     |
| ---- | ------------------------------- | ------------------------------------------------------------------ |
| 0004 | Java-to-Go translation patterns | Already written as `PORT-n` in the guidelines; the ADR records why |
