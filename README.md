# Crucible

Automated Magic: The Gathering deck testing and optimization.

Crucible plays a deck against a gauntlet of opposing decks tens or hundreds of thousands of times, records what happened
in every game, and reports **where the deck loses** — which cards sat dead in hand and why, whether the mana base
supports the curve, which cards actually change the win rate, and how the deck performs on the play versus the draw.

It is built on a Go port of the [Forge](https://github.com/Card-Forge/forge) rules engine, in a fork of that project.

---

## Status

**Pre-implementation.** There is no Go code yet.

```console
$ find crucible -name '*.go' | wc -l
0
```

What exists is the decision and rule set the code will be written against: 11 accepted ADRs, 7 binding guidelines, and
the implementation plan. That is deliberate — the port is documentation-driven, and the blueprint lands before the first
line of Go.

Progress is tracked in [the implementation plan](docs/crucible/00-master-implementation-plan.md), §5.

---

## Start here

| If you want to                      | Read                                                                  |
| ----------------------------------- | --------------------------------------------------------------------- |
| Understand what this is             | [System overview](docs/crucible/architecture/system-overview.md)      |
| See the whole plan                  | [Implementation plan](docs/crucible/00-master-implementation-plan.md) |
| Know why something is the way it is | [Architecture Decision Records](docs/crucible/adr/README.md)          |
| Contribute code or docs             | [Guidelines](docs/crucible/guidelines/README.md)                      |

`/CLAUDE.md` is the entry point for Claude Code and points at the same rules.

---

## Why a port

Forge is a desktop application that plays one game at a time, so process-global mutable state is free — `MyRandom` and
`StaticData` are both mutable statics. Two games in one JVM race on them.

Crucible's workload is the opposite shape: hundreds of thousands of independent games, embarrassingly parallel. The port
exists to make that reachable — one goroutine per game, an immutable card database shared by pointer, and card scripts
compiled once instead of re-parsed per card per game.

The reasoning is in [ADR-0005](docs/crucible/adr/0005-concurrency-model.md) and
[ADR-0007](docs/crucible/adr/0007-card-dsl-representation.md).

---

## Relationship to Forge

This repository is a fork of [Card-Forge/forge](https://github.com/Card-Forge/forge). **Almost everything in it is
Forge's work, not Crucible's** — 517,069 lines of Java and 33,682 card scripts, built by the Forge community over more
than a decade.

Crucible uses that work three ways:

| Purpose                 | What it means                                                                                                  |
| ----------------------- | -------------------------------------------------------------------------------------------------------------- |
| **Port source**         | `forge-core`, `forge-game` and `forge-ai` are the behaviour being reproduced in Go                             |
| **Differential oracle** | The Java engine generates golden outputs the Go engine is diffed against, in CI only                           |
| **Card scripts**        | `forge-gui/res/cardsfolder/` is read directly at runtime, so upstream card additions arrive with no conversion |

**Crucible never executes Java at runtime.** The shipped artefact is one Go binary plus the card script files it reads
as data.

Crucible owns exactly two paths — `crucible/` and `docs/crucible/` — plus root tooling configuration. Everything else
belongs to upstream and is not modified; anything landing outside those paths is recorded in
[`upstream-patches.md`](docs/crucible/porting/upstream-patches.md). This file is the one deliberate exception, since a
fork's front page has to describe the fork.

Forge itself is excellent and actively developed. If you want to _play_ Magic rather than analyse a decklist, go there:
**<https://github.com/Card-Forge/forge>**

Forge operates independently and is not affiliated with Wizards of the Coast. Neither is Crucible.

---

## License

GPLv3, inherited from Forge. See [LICENSE](LICENSE).
