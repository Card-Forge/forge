# ADR-0001 — Fork Layout and Upstream Sync

- **Status:** Accepted
- **Date:** 2026-09-06
- **Deciders:** `jc@archlab.pl`

## Context

Crucible is built inside a fork of [Card-Forge/forge](https://github.com/Card-Forge/forge), currently at `b05ee47a6fd`.
The fork is not a snapshot — it must keep pulling from upstream indefinitely.

The reason is the card corpus. `forge-gui/res/cardsfolder/` holds **33,686 card scripts, 302,749 lines**, and upstream
adds and corrects them continuously as sets release. Crucible reads those exact files. Every upstream sync is free card
support, and a fork that stops syncing goes stale within one set release.

Upstream is also the correctness oracle. Differential testing runs the Java engine against the Go engine on identical
inputs, so the Java tree has to stay present, buildable, and version-matched to the scripts it is being compared on.

That makes merge conflicts the primary operational risk of the entire project. Every line touched in an upstream file is
a conflict on the next sync, forever. Upstream is 521,958 lines of Java across 2,607 files and 12 Maven modules, plus 61
Markdown files under `docs/`, plus 492 TestNG tests. Nothing on the Crucible side needs to modify any of it.

## Decision Drivers

- Upstream syncs must stay cheap and near-automatic. A sync that requires conflict resolution will be skipped.
- Card scripts must arrive from upstream without manual work.
- The Java engine must remain buildable in-tree as the differential oracle.
- Crucible files must be trivially separable from upstream files, by path alone, with no judgement call.
- Tooling (formatters, linters, CI) must not touch upstream files as a side effect.

## Considered Options

1. **Interleave.** Add Go code as a sibling Maven-style module (`forge-go/`), put Crucible docs in the existing `docs/`
   tree.
2. **Two reserved top-level paths.** All Go under `crucible/`, all docs under `docs/crucible/`, zero edits elsewhere.
3. **Separate repository.** Crucible in its own repo; consume Forge as a git submodule, or vendor the card scripts.

Rejections:

- **Option 1** puts Crucible docs in the same directory upstream edits, so `docs/` conflicts on most syncs. It also
  makes "is this ours?" a per-file judgement call, which fails the moment someone is in a hurry.
- **Option 3** is the cleanest boundary but breaks the oracle. Differential testing needs the Java engine and the card
  scripts at a _known, identical_ revision (ADR-0010). A submodule can express that, but then every oracle run is a
  two-repo checkout, CI needs both, and the version-matching becomes a thing to get wrong. Reconsider only if the port
  reaches full independence from the oracle, which is not planned.

## Decision

**Option 2.** Two reserved top-level paths, and no edits outside them.

| Path                    | Contents                                                                                      | Owner                  |
| ----------------------- | --------------------------------------------------------------------------------------------- | ---------------------- |
| `crucible/`             | All Go code, its own `go.mod`                                                                 | Crucible               |
| `docs/crucible/`        | All Crucible documentation                                                                    | Crucible               |
| `crucible/oracle-java/` | The only Java Crucible may add: test-scoped dumpers and recorders, as a separate Maven module | Crucible               |
| Everything else         | Upstream Forge                                                                                | Upstream — do not edit |

Root-level tooling config files (`.prettierrc`, `.prettierignore`, `.markdownlint-cli2.jsonc`, `CLAUDE.md`) are the one
unavoidable exception: they only work at the repo root. Each is a new file, never a modification of an upstream file, so
it cannot conflict — only a same-named upstream addition could, and that has not happened.

Supporting rules:

- **Every tool that writes files must ignore upstream paths.** `.prettierignore` and the `ignores` block in
  `.markdownlint-cli2.jsonc` already do this. Any tool added later inherits the same obligation.
- **Unavoidable upstream edits get logged.** `docs/crucible/porting/upstream-patches.md`, in the same commit, with the
  reason. The expected contents of that file is nothing.
- **Sync is a merge, never a rebase.** Rebasing Crucible commits onto a moving upstream re-resolves the same conflicts
  repeatedly and rewrites reviewed history.

After each upstream sync, three checks run because upstream may have changed the card corpus:

1. L2 corpus golden diff — catches a new or changed script key the Go parser does not handle.
2. `crucible corpus-coverage` — catches new scripts using ability APIs the Go engine has not implemented.
3. Regenerate `docs/crucible/porting/parity-matrix.md`.

Upstream changes to `forge-game` Java require no Go change unless L4 differential parity goes red.

## Consequences

**Good.** Ownership is decided by path prefix alone, with no judgement involved, so the rule survives contact with
deadlines. Upstream syncs are ordinary merges that touch no Crucible file. New card scripts arrive for free and are
immediately visible to the corpus golden test, which turns "upstream added cards we do not support" from a silent gap
into a failing check. The Java oracle stays in-tree and automatically version-matched to the scripts it is compared
against, so differential testing needs no cross-repo coordination.

**Bad.** The repository carries 521,958 lines of Java that Crucible never executes, which makes clones large and
full-text search noisy — every `grep` needs a path filter. Contributors see a tree where most of the content is
off-limits, which is confusing until they read this ADR. Crucible's own history is interleaved with upstream merge
commits on the same branch, so `git log` needs a path filter to be readable.

**Neutral.** The `crucible/` directory being a separate Go module means Go tooling never sees the Java tree, which is
convenient but also means repo-wide tooling has to be run from two places. Extracting Crucible into its own repository
later remains straightforward — the two reserved paths move cleanly — so this decision is reversible if the oracle
dependency ever ends.

## Related

- [05-commit-and-review.md](../guidelines/05-commit-and-review.md) — REV-1 and REV-7 implement this ADR
- [00-master-implementation-plan.md](../00-master-implementation-plan.md) — Section 2.1
- ADR-0010 — differential testing, which is why the oracle stays in-tree
