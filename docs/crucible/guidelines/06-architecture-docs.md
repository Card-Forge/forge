# ARCH — Architecture Documentation

- **Status:** Active
- **Applies to:** `docs/crucible/architecture/`
- **Rule IDs:** cite as `ARCH-n`

Governs how the system is described. Style comes from [00-documentation-style](00-documentation-style.md); this document
governs content.

---

## Why this exists

The guideline set covers how to write, how to code, how to port, how to test, how to decide, and how to ship. Nothing
covered how to describe the system, and five architecture documents are the next work.

Without a rule, architecture docs become one of two useless things: a restatement of the ADRs with the reasoning
stripped out, or a description of the system somebody hoped to build. Both read as authoritative. Both mislead.

---

## ARCH-1 — Know which of the four you are writing

| Document         | Answers                                        | Lifecycle                                                  |
| ---------------- | ---------------------------------------------- | ---------------------------------------------------------- |
| **ADR**          | Why is it like this? What else was considered? | Dated. Facts corrected in place; a new decision supersedes |
| **Architecture** | How does it work, right now?                   | Living. Edited whenever the code moves                     |
| **Design**       | How will the pieces fit, before they exist?    | Living until built, then replaced by an architecture doc   |
| **Guideline**    | What must I do?                                | Living. Normative                                          |

Confusing them is the common failure. An architecture doc that argues for its design is a late ADR. An ADR that
describes current structure goes stale the moment the code changes, because an ADR records a decision, not a state.

**Test:** if a sentence could start with "we chose this because", it belongs in an ADR. If it could start with "once
this is built, it will", it belongs in a design document (ARCH-10), not here.

---

## ARCH-2 — Describe what exists, not what is planned

Aspirational description is the failure mode that makes architecture docs untrustworthy, because nothing marks it as
aspirational.

Where something does not exist yet, say so and name the milestone:

**Bad:**

```text
The engine loads compiled card definitions from the shared database and dispatches effects through a generated
registry.
```

**Good:**

```text
Not built. Compiled definitions land in M3 (ADR-0007); the effect registry in M6 (ADR-0008). Today `internal/carddb`
parses scripts and nothing consumes the result.
```

A document describing an empty repository is still useful. A document describing an imaginary one is not.

**Target state is allowed in its own section, never mixed into a description of the present.** A newcomer needs to know
where the system is going, so a document may carry a clearly separated target section — but every stage in it names the
milestone that builds it, and nothing outside that section describes anything unbuilt. Interleaving the two is what
ARCH-2 exists to prevent; separating them is not a loophole, it is the required shape.

Found while writing [`../architecture/system-overview.md`](../architecture/system-overview.md), where a rule that
forbade target state outright would have produced a document a newcomer could not use.

---

## ARCH-3 — Never restate a decision. Link the ADR

Architecture docs say _what the system does_. ADRs say _why_, and hold the alternatives.

**Bad:** "The engine core is a single package because Go forbids import cycles and `forge-game` has 82 of them."

**Good:** "`internal/engine` is one package ([ADR-0003](../adr/0003-go-project-layout.md))."

Reason: the reasoning has one home (DOC-11). Copied into two documents it diverges, and the copy without the
measurements is the one people read.

---

## ARCH-4 — Every claim is checkable against the code

A reader must be able to verify any statement without asking. Name the package, file, or command.

**Bad:** "Game state is copied cheaply for AI lookahead."

**Good:** "`Game.Clone` copies the card arena as a slice; no reference rewriting. `internal/engine/clone.go`,
benchmarked by `BenchmarkGameClone`."

Unverifiable prose is where architecture docs rot first, because nothing fails when it stops being true.

---

## ARCH-5 — Diagrams are text

Mermaid in a fenced block. Never a binary image, never an external editor's export.

Reason: diagrams must diff, merge, and be edited by whoever changes the code. A PNG cannot be reviewed and will not be
updated.

````text
```mermaid
flowchart LR
  txt[".txt scripts"] --> compile["carddb/compile"]
  compile --> def["CompiledCard (immutable, shared)"]
  def --> game["engine.Game"]
```
````

Keep them small. A diagram that needs a legend is two diagrams.

---

## ARCH-6 — Name what would make the document wrong

Every architecture doc ends with an invalidation condition — the change that means it needs rewriting.

```text
## Invalidated by

- A new package under `internal/` that is not in the module map
- Any type gaining a back-reference to `*Game` (ADR-0009)
```

Reason: staleness is the default state of architecture documentation, and "review the docs occasionally" has never
worked anywhere. A named trigger turns it into something a reviewer can check on the PR that causes it.

---

## ARCH-7 — The required set, and what each must establish

| Document                         | Must establish                                                                                   |
| -------------------------------- | ------------------------------------------------------------------------------------------------ |
| `system-overview.md`             | What Crucible does, its boundaries, what it is not. The one document a newcomer reads first      |
| `module-map.md`                  | Every Go package: responsibility, Java provenance, port-log link. `docgate` enforces it (DOC-12) |
| `engine-state-model.md`          | Arena, handles, what is mutable, what is shared, what a clone copies                             |
| `card-compilation-pipeline.md`   | `.txt` to shared `CompiledCard`, and where the runtime overlay begins                            |
| `concurrency-and-determinism.md` | Worker pool, per-game isolation, stream partitioning, what reproducibility guarantees hold       |
| `telemetry-pipeline.md`          | Engine event to stored row to report figure, end to end                                          |

Each is one document with one subject. A seventh may be added when something exists that none of the six covers.

---

## ARCH-8 — Data flow is traced end to end, with no gaps

A pipeline description that stops at a component boundary is where bugs live. Trace an input to its output through every
stage, and name the stage that transforms it.

If a stage is not built yet, ARCH-2 applies — name it and its milestone rather than skipping it.

---

## ARCH-9 — Numbers are measured, dated, and reproducible

Any figure in an architecture doc carries the command that produced it and the date it was run.

**Bad:** "The card corpus is 33,686 scripts." Exact, sourced from nothing, and wrong by the next upstream sync.

**Good:**

```text
33,686 card scripts (2026-09-08):
find forge-gui/res/cardsfolder -name '*.txt' | wc -l
```

Reason: unsourced numbers get copied forward past the point where they are true, and a stale number is more damaging
than no number because it looks researched.

This governs the figure where it is _stated_. A document arguing from the magnitude — "tens of thousands of scripts",
"roughly 210,000 lines" — is doing something else, and should round rather than restate (DOC-11).

---

## ARCH-10 — Design documents, for what does not exist yet

`architecture/` describes what is. `design/` describes what a set of accepted ADRs adds up to, before any of it is
built.

The category exists because ADRs are deliberately narrow. Each argues one decision and none shows how the pieces
compose, so a milestone can satisfy every ADR individually and still assemble something incoherent. A design document is
where the cross-ADR interactions get pinned — the questions no single ADR owns.

| Rule      | Design document                                                               |
| --------- | ----------------------------------------------------------------------------- |
| Location  | `docs/crucible/design/`                                                       |
| Header    | Must carry **Status: Target** and name the milestone that makes it real       |
| Reasoning | Still forbidden. Link the ADR (ARCH-3)                                        |
| Content   | Composition, lifetimes, and interactions — not decisions, and not restatement |
| Lifecycle | **Replaced** by an architecture document once built, not edited into one      |

Two rules keep it from becoming fiction.

**It may only compose accepted ADRs.** A design document that introduces a decision is an ADR avoiding review. If
writing one requires a choice nobody has made, stop and write the ADR.

**Every unresolved interaction is listed, not smoothed over.** The value is in naming what the ADRs left ambiguous, so
an open question is a finding rather than a gap to hide.

---

## Checklist before merging an architecture doc

- [ ] Describes what exists; anything absent is named with its milestone (ARCH-2)
- [ ] No decision reasoning — ADRs linked instead (ARCH-3)
- [ ] Every claim names a package, file, or command (ARCH-4)
- [ ] Diagrams are mermaid, not images (ARCH-5)
- [ ] Ends with an invalidation condition (ARCH-6)
- [ ] Data flow has no gaps (ARCH-8)
- [ ] Numbers carry their command and date (ARCH-9)
- [ ] Design documents carry **Status: Target**, compose only accepted ADRs, and list unresolved interactions (ARCH-10)
- [ ] Style follows [00-documentation-style](00-documentation-style.md); `prettier` and `markdownlint` clean

## Related

- [00-documentation-style](00-documentation-style.md) — how to write it
- [04-adr-process](04-adr-process.md) — where the reasoning goes instead
- [01-go-coding-standards](01-go-coding-standards.md) — GO-10, the package boundaries the module map records
