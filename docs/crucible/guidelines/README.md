# Crucible Guidelines

- **Status:** Active

Binding rules for all Crucible work. Referenced from `/CLAUDE.md` at repo root, so both humans and Claude Code follow
the same set.

All guidelines are written in the compressed style defined by [00-documentation-style](00-documentation-style.md).
Compressed grammar, exact technical content, aimed at engineers.

---

## The set

| Doc                                                       | Area     | Governs                                                       |
| --------------------------------------------------------- | -------- | ------------------------------------------------------------- |
| [00-documentation-style](00-documentation-style.md)       | `DOC-n`  | How to write every doc, ADR, comment, commit body. Read first |
| [01-go-coding-standards](01-go-coding-standards.md)       | `GO-n`   | Go style, package layout, error and panic policy, engine bans |
| [02-java-to-go-translation](02-java-to-go-translation.md) | `PORT-n` | Normative Java→Go mapping, port procedure, port-log format    |
| [03-testing-standards](03-testing-standards.md)           | `TEST-n` | Module-first testing, when to drop to unit level, fixtures    |
| [04-adr-process](04-adr-process.md)                       | `ADRP-n` | When an ADR is required, format, numbering                    |
| [05-commit-and-review](05-commit-and-review.md)           | `REV-n`  | Fork hygiene, branches, commits, review order                 |
| [06-architecture-docs](06-architecture-docs.md)           | `ARCH-n` | How to describe the system: what exists, not what is planned  |

---

## How to use

**Citing:** rules have stable IDs. Use them in review comments, code comments, commit bodies.

```text
sba.go:88 — package-level cache breaks goroutine-per-game (GO-2).
```

**Numbers are permanent.** A superseded rule keeps its number and gains a line pointing at its replacement — for example
a bold "Superseded by GO-nn" line naming whatever new rule replaced it. Never renumber; old citations must keep
resolving.

**Conflict order:** a specific rule beats a general one. ADR beats a guideline. If a guideline and an ADR disagree, the
guideline is stale — fix it in the same PR.

**Disagree with a rule?** Change the rule in a PR. Do not work around it silently.

---

## Reading order for someone new

1. [00-documentation-style](00-documentation-style.md) — everything else is written in it
2. [01-go-coding-standards](01-go-coding-standards.md) — GO-2, GO-3, GO-4, GO-8 are the ones that get violated
3. [03-testing-standards](03-testing-standards.md) — TEST-1 and TEST-2 decide how you write every test
4. [02-java-to-go-translation](02-java-to-go-translation.md) — before touching any Java file
5. [05-commit-and-review](05-commit-and-review.md) — before opening a PR
6. [04-adr-process](04-adr-process.md) — when you hit a decision
7. [06-architecture-docs](06-architecture-docs.md) — before writing anything under `architecture/`

---

## Related

- `docs/crucible/00-master-implementation-plan.md` — the plan these rules serve
- `docs/crucible/adr/` — decisions
- `/CLAUDE.md` — entry point that points here
