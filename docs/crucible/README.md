# Crucible

- **Status:** Active
- **Phase:** pre-implementation

Automated Magic: The Gathering deck testing and optimization suite, built on a Go port of the Forge rules engine.

Runs batch simulations of a target deck against a meta gauntlet, captures telemetry — dead cards, mana health, per-card
impact — and emits deck-improvement reports.

This repo is a fork of [Card-Forge/forge](https://github.com/Card-Forge/forge). All Crucible work lives in
`docs/crucible/` and (once it exists) `crucible/`. Upstream files are not edited (REV-1).

---

## Read in this order

| #   | Document                                                             | What it gives you                                                                   |
| --- | -------------------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| 1   | [00-master-implementation-plan.md](00-master-implementation-plan.md) | Codebase analysis, port strategy, telemetry design, roadmap                         |
| 2   | [guidelines/README.md](guidelines/README.md)                         | The binding rule set. Start with the doc-style guide                                |
| 3   | [design/](design/)                                                   | How decisions compose before the code exists: engine, card compilation, concurrency |
| 3   | [adr/README.md](adr/README.md)                                       | Decisions, once written                                                             |

`/CLAUDE.md` at the repo root is the entry point for Claude Code and points at the same rules.

---

## Layout

| Path                           | Contents                                                                  | State                                                    |
| ------------------------------ | ------------------------------------------------------------------------- | -------------------------------------------------------- |
| [guidelines/](guidelines/)     | Binding rules — `DOC-n`, `GO-n`, `PORT-n`, `TEST-n`, `ADRP-n`, `REV-n`    | Written                                                  |
| [adr/](adr/)                   | Architecture Decision Records                                             | Empty — M0                                               |
| [design/](design/)             | Target design — how accepted decisions compose (ARCH-10)                  | 3 written; `telemetry-pipeline` blocked on ADR-0013/0014 |
| [architecture/](architecture/) | System overview, module map, state model, pipelines                       | Stub                                                     |
| [porting/](porting/)           | Parity matrix, test port matrix, upstream patches, port log, DSL grammars | Stub                                                     |
| `telemetry/`                   | Event schema, metric definitions, report formats                          | Not written — M0                                         |
| `runbooks/`                    | How to run a batch, add card support, investigate a parity failure        | Not written                                              |
| `research/`                    | Meta gauntlet definition, format scope                                    | Not written — M0                                         |

---

## Current state

No Go code yet. `crucible/` does not exist.

Next step is M0 in the plan: write the foundational ADRs and the remaining `architecture/` and `telemetry/` docs. No Go
until M0's gate is green.
