# ADR-0015 — Upstream Sync Procedure

- **Status:** Accepted
- **Date:** 2026-09-08
- **Deciders:** `jc@archlab.pl`

## Context

[ADR-0001](0001-fork-layout-and-upstream-sync.md) decided _that_ the fork keeps pulling from upstream, and that a sync
is a merge rather than a rebase. It did not decide when, by whom, or what has to be green before the merge lands.
[REV-7](../guidelines/05-commit-and-review.md) says "on a cadence" without naming one, and two of its three post-sync
steps name tooling that does not exist.

Measured on `Card-Forge/forge` at `b05ee47a6fd`: **878 commits in 90 days, about 10 per day**, with 176 card scripts
touched in the last 30 days and 79 `forge-game` Java files. `TypeLists.txt`, which `internal/cardtype` loads, changed 20
times in 180 days.

The sync that produced `b05ee47a6fd` exposed what the gap costs. It was merged straight onto `master`, which
[REV-2](../guidelines/05-commit-and-review.md) forbids for every other change, so no CI ran before it landed: the corpus
goldens were verified afterwards, by hand. Seven measured figures went stale across 22 documents in the same merge, and
were also corrected by hand.

## Decision Drivers

- A sync that requires work gets skipped, and a fork that stops syncing goes stale within one set release (ADR-0001).
- The checks that protect the port — corpus goldens, the cycle count, the coverage floors — are worth nothing if they
  run after the merge instead of before it.
- Review effort scales with diff size. At 10 commits a day, a monthly sync is a 300-commit diff nobody reads.
- The procedure must touch no upstream file, or it becomes the conflict it exists to avoid.

## Considered Options

1. **Manual, on demand, straight to `master`.** What happened for `b05ee47a6fd`.
2. **Scheduled weekly merge onto a branch, opened as a PR, gated by CI.**
3. **Per-milestone sync**, holding the oracle still for the length of a milestone.
4. **Vendor the card scripts** and drop the Java tree.

Rejections:

- **Option 1** puts the one class of change that can break every golden on the only path with no gate. It also relies on
  someone remembering, which is what produced 22 documents of stale numbers.
- **Option 3** keeps the oracle stable while porting a unit, which is genuinely useful, but a milestone is weeks: the
  diff reaches several hundred commits, and new cards that the corpus gate would have caught arrive in one lump.
  Available deliberately, as the manual path, when a port is mid-flight.
- **Option 4** breaks differential testing, which needs the Java engine and the scripts at one identical revision
  (ADR-0010). Already rejected in ADR-0001 and unchanged here.

## Decision

**Option 2.** A scheduled job proposes the sync; CI decides whether it is safe; a human merges it.

| Element     | Rule                                                                                                                                                                   |
| ----------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Trigger     | Weekly schedule, plus manual dispatch for a sync wanted sooner                                                                                                         |
| Branch      | `sync/<yyyy-mm-dd>-<upstream-short-sha>`, cut from `master`                                                                                                            |
| Merge       | `git merge upstream/master`, never a rebase (ADR-0001)                                                                                                                 |
| Landing     | A pull request against `master`, like every other change (REV-2)                                                                                                       |
| Merge style | **A merge commit. Never squash**                                                                                                                                       |
| No-op       | No new upstream commits means no branch and no PR                                                                                                                      |
| Conflict    | The job stops and opens nothing. A conflicted sync is a human task, because it means upstream edited one of the six files Crucible owns outside its two reserved paths |

**Squashing a sync is the one irreversible mistake here.** It replaces the upstream commits with a single unrelated
commit, so the next merge re-resolves everything already resolved, and the ancestry that makes syncs cheap is gone.

**Gates, all blocking, all already required for any other PR:**

| Gate                              | What a sync can break                                                     |
| --------------------------------- | ------------------------------------------------------------------------- |
| `internal/mana` corpus golden     | A new mana symbol, or a changed cost on an existing card                  |
| `internal/cardtype` corpus golden | A new subtype, or a `TypeLists.txt` edit                                  |
| `javacycles -expect 82`           | ADR-0003's premise: the coupling that forces a single-package engine core |
| `tools/metrics`                   | The measured figures the documents state                                  |
| `covergate`, `docgate`            | Unaffected by upstream, run because every PR runs them                    |

`tools/metrics` is new and lands with the workflow: it recomputes each documented measurement from the tree and fails
when a document disagrees. Without it, the numbers drift on every sync and are corrected only when somebody notices.

**A red gate is information, not an obstacle to route around.** What each one means is in
[`../runbooks/upstream-sync.md`](../runbooks/upstream-sync.md); the rule is that a golden may be regenerated from the
new corpus, and a cycle count may not.

## Consequences

**Good.** The corpus gate finally fires when the corpus changes, which is what ADR-0001 promised and what a `paths:`
filter scoped to `crucible/**` prevented. Diffs stay at roughly 70 commits, which is reviewable. Nothing depends on
anyone remembering to sync, and the numbers in the documents stop being a manual chore.

**Bad.** A weekly PR is a weekly interruption even when it is a rubber stamp, and a fork that is quiet for a month
collects four of them. The scheduled job is one more workflow to keep working, and its failure mode — quietly not
running — is silent by nature.

**Neutral.** Merge commits keep `master`'s history interleaved with upstream's, which `git log` already needed a path
filter to read (ADR-0001). Per-milestone syncing remains available: skip the PRs, or disable the schedule for the
duration.

## Related

- [ADR-0001](0001-fork-layout-and-upstream-sync.md) — the fork layout this procedure serves, and merge-not-rebase
- [ADR-0003](0003-go-project-layout.md) — the 82-cycle premise a sync can invalidate
- [ADR-0010](0010-differential-testing-strategy.md) — why the Java tree stays in-tree and version-matched
- [`../runbooks/upstream-sync.md`](../runbooks/upstream-sync.md) — the procedure itself
- [REV-7](../guidelines/05-commit-and-review.md) — the rule that points here
