# Crucible

- **Status:** Active
- **Phase:** M1 — Go foundation

Automated Magic: The Gathering deck testing and optimization suite, built on a Go port of the Forge rules engine.

Runs batch simulations of a target deck against a meta gauntlet, captures telemetry — dead cards, mana health, per-card
impact — and emits deck-improvement reports.

This repo is a fork of [Card-Forge/forge](https://github.com/Card-Forge/forge). All Crucible work lives in
`docs/crucible/` and `crucible/`. Upstream files are not edited (REV-1).

---

## Read in this order

| #   | Document                                                             | What it gives you                                                             |
| --- | -------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| 1   | [00-master-implementation-plan.md](00-master-implementation-plan.md) | Codebase analysis, port strategy, telemetry design, roadmap                   |
| 2   | [guidelines/README.md](guidelines/README.md)                         | The binding rule set. Start with the doc-style guide                          |
| 3   | [adr/README.md](adr/README.md)                                       | The 14 accepted decisions, indexed                                            |
| 4   | [design/](design/)                                                   | How those decisions compose: engine, card compilation, concurrency, telemetry |

`/CLAUDE.md` at the repo root is the entry point for Claude Code and points at the same rules.

---

## Layout

| Path                           | Contents                                                                         | State                                                  |
| ------------------------------ | -------------------------------------------------------------------------------- | ------------------------------------------------------ |
| [guidelines/](guidelines/)     | Binding rules — `DOC-n`, `GO-n`, `PORT-n`, `TEST-n`, `ADRP-n`, `REV-n`, `ARCH-n` | 7 documents                                            |
| [adr/](adr/)                   | Architecture Decision Records                                                    | 0001-0014, all `Accepted`                              |
| [design/](design/)             | Target design — how accepted decisions compose (ARCH-10)                         | 4 documents; `engine-state-model` omitted as redundant |
| [architecture/](architecture/) | What exists right now (ARCH-2)                                                   | `system-overview`, `module-map`                        |
| [porting/](porting/)           | Parity matrix, test port matrix, upstream patches, port log, 6 DSL grammars      | Written; the matrices fill as units land               |
| [telemetry/](telemetry/)       | Metric definitions; event schema and report formats later                        | `metric-definitions` — 21 `MET-n`, versioned           |
| [research/](research/)         | Meta gauntlet definition, format scope                                           | Structure written; Modern decklists pending            |
| `runbooks/`                    | How to run a batch, add card support, investigate a parity failure               | Needs working code — M8                                |

---

## Current state

M0 and M1 are complete. `crucible/` holds 42 Go files, 4,580 lines: `pkg/collect`, `pkg/javarand`, `internal/mana`,
`internal/cardtype`, `tools/javacycles`, `tools/enginelint`, `tools/docgate`, `tools/covergate`, and the `oracle-java`
Maven module.

[`architecture/module-map.md`](architecture/module-map.md) carries the package-by-package state and is the file that
changes when code lands. M2 starts the port proper, with `internal/carddb`.
