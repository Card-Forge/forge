# ADR — Architecture Decision Record Process

- **Status:** Active
- **Applies to:** `docs/crucible/adr/`
- **Rule IDs:** cite as `ADRP-n`

ADR records a decision that is expensive to reverse. Not a design doc. Not a tutorial.

---

## ADRP-1 — When an ADR is required

| Trigger                             | Example                                                   |
| ----------------------------------- | --------------------------------------------------------- |
| Changes shape of the whole codebase | Package layout, concurrency model                         |
| Hard to reverse once code exists    | Card AST vs runtime interpretation, arena IDs vs pointers |
| Adds a third-party dependency       | Any non-stdlib import (GO-14)                             |
| Changes a cross-cutting contract    | Event schema, error policy, telemetry storage format      |
| Deviates from an existing ADR       | Supersede it, do not silently diverge                     |

**No ADR needed:** naming a function, adding a fixture, fixing a bug, refactoring inside one package.

Test: would a new engineer six months from now ask "why is it like this?" → ADR.

---

## ADRP-2 — Format (MADR)

```text
# ADR-0009 — Game State Representation

- **Status:** Proposed | Accepted | Superseded by ADR-nnnn | Deprecated
- **Date:** 2026-09-06
- **Deciders:** <names>

## Context

What forces this decision. Constraints, measurements, what breaks if we do nothing. Cite numbers and file paths.

## Decision Drivers

- Goroutine-per-game, no shared mutable state (ADR-0005)
- AI lookahead needs cheap state clone
- Java GameCopier is the AI's bottleneck

## Considered Options

1. Pointer graph, deep clone
2. Arena + integer handles
3. Copy-on-write persistent structures

## Decision

Option 2. Per-Game arena, `CardID uint32` handles.

## Consequences

**Good:** clone is a slice copy; no pointer fixup; IDs stable across snapshots.
**Bad:** every access goes through the arena — an extra indirection.
**Neutral:** forces ID-based equality (GO-9), which we wanted anyway.

## Related

ADR-0005, ADR-0010, [01-go-coding-standards](../guidelines/01-go-coding-standards.md)
```

---

## ADRP-3 — Numbering and immutability

- Sequential, zero-padded, four digits: `0001`, `0002`.
- **Numbers never reused. Files never deleted.**
- Superseding: old ADR gets `**Status:** Superseded by ADR-0021` as its first line, body untouched. New ADR's Context
  says what changed.

Reason: an ADR's value is the record of what was believed at the time. Editing history destroys it.

---

## ADRP-4 — Write it before the code

ADR merges **before** the PR that implements it. Not same PR, not after.

`crucible/tools/docgate` fails the build on a `// ADR-nnnn` code comment with no matching ADR file.

Reason: ADR written after the fact is a justification, not a decision. It never says "we considered X and rejected it",
because by then X was never considered.

---

## ADRP-5 — Length

Target 1 page. Hard cap 2.

Over 2 pages → it is a design doc. Put the design in `architecture/`, keep the decision in the ADR, link them.

---

## ADRP-6 — Options must be real

"Considered Options" listing one option is not a decision record.

Each rejected option needs a one-line reason for rejection. "Slower" is acceptable if a number follows. "Not idiomatic"
alone is not.

---

## ADRP-7 — Style

Follows [00-documentation-style](00-documentation-style.md). Compressed prose, tables for parallel items, code blocks
normal.

Exception (DOC-9): **Consequences** section written in full sentences. That is the part someone reads while deciding
whether to break the rule, and ambiguity there is expensive.

---

## Related

- [00-documentation-style](00-documentation-style.md)
- `docs/crucible/adr/README.md` — index and status of all ADRs
