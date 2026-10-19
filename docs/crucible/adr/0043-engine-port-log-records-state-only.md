# ADR-0043 — Engine Port Log Records State Only

- **Status:** Accepted
- **Date:** 2026-10-19
- **Deciders:** `jc@archlab.pl`
- **Supersedes:** ADR-0004's "Deviations are recorded, not remembered" for `internal/engine` only: its per-unit
  `porting/port-log/<unit>.md` note. Every other package keeps its note; the rest of ADR-0004 stands

## Context

ADR-0004 gives every ported unit its own port-log note. For `internal/engine` that became 116 notes under
`port-log/game-state/`, 1.6 MB, one per landing batch. Each note narrates the landing: what was read, corpus counts on
that day, what an earlier batch had left. By measurement:

| Content                                                | Where it already is                                            |
| ------------------------------------------------------ | -------------------------------------------------------------- |
| Java `file:line` provenance (1,384 cites in the notes) | Go comments: 1,966 cites, 153 files with `Ported from` (GO-15) |
| Gaps still open                                        | `game-state.md` "Not ported yet", the open plan                |
| Forge defects                                          | `forge-java-defects.md`, `card-script-defects.md`              |
| Corpus counts at landing                               | Stale the next day; `card-smoke` measures the live ones        |
| Reproduced quirks (PORT-7) and deviations              | Only in the notes, scattered across 59 of 116 files            |

Claude Code reads these files while porting. Reading one costs up to 80 KB, and only the last row is information found
nowhere else. The notes also break DOC-16 ("state, never history").

## Decision Drivers

- A disagreement with the oracle must be explainable from one place (ADR-0004's reason for recording deviations)
- No fact may exist only in a narrative nobody reads (DOC-16)
- Docs Claude reads on every row cost tokens on every read

## Considered Options

1. Keep one note per engine unit (ADR-0004 as written)
2. Keep the notes, stop reading them
3. One state file for quirks and deviations, gaps in "Not ported yet", provenance in code; delete the narrative notes

## Decision

Option 3, for `internal/engine` only.

| Fact                                                | Lives in                                                          |
| --------------------------------------------------- | ----------------------------------------------------------------- |
| Reproduced Java quirk (PORT-7), deviation from Java | `port-log/quirks.md`, one table row: behavior, Java site, Go site |
| What is not ported                                  | `port-log/game-state.md` "Not ported yet", open-plan rows         |
| Forge defect                                        | `forge-java-defects.md`, `card-script-defects.md`                 |
| Java provenance, null decisions                     | Header and inline comments in the Go file (GO-15)                 |
| Package-level model (handles, zones, dispatch)      | `port-log/game-state.md`, the module-map row's note (docgate)     |

Option 1 rejected: the measurement above; 1.6 MB of which the unique part fits in 11 KB. Option 2 rejected: an unread
file still has to be written on every row (DOC-12), and its quirks stay undiscoverable.

## Consequences

Every engine change that reproduces a Java quirk or departs from Java adds a row to `quirks.md` in the same commit.
Without that row, a later parity failure has no written explanation, which is exactly the failure ADR-0004 guards
against; reviewers check for it the way they check for a test.

The deleted notes stay in git history at commit `624d87dba16`. Quirk extraction was keyword-based (PORT-7, quirk,
deviation, reproduce and similar) plus a read of every hit, so a quirk described without those words survives only
there.

Other packages are unaffected: their notes are small and stable, and docgate still requires each ported package's
module-map row to link one.

## Related

ADR-0004, ADR-0010, [02-java-to-go-translation](../guidelines/02-java-to-go-translation.md) (PORT-4),
[00-documentation-style](../guidelines/00-documentation-style.md) (DOC-12, DOC-16)
