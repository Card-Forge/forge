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

1. **Straight onto `master`, by hand.** What happened for `b05ee47a6fd`.
2. **By hand, on demand, onto a sync branch opened as a pull request.**
3. **A scheduled job** that merges upstream weekly and opens the pull request itself.
4. **Vendor the card scripts** and drop the Java tree.

Rejections:

- **Option 1** puts the one class of change that can break every golden on the only path with no gate. That is how the
  last sync landed, and its corpus goldens were checked afterwards, by hand.
- **Option 3** buys a cadence and costs a credential. A pull request opened with the default `GITHUB_TOKEN` raises no
  workflow runs at all, by design, so an automated sync PR would arrive with **no checks on it** while looking entirely
  normal — the opposite of the point, and silent. Avoiding that means a personal access token in a repository secret,
  which expires, needs rotating, and can write to the repository. Cadence was never the binding constraint; the gate is,
  and option 2 gets the gate for nothing.
- **Option 4** breaks differential testing, which needs the Java engine and the scripts at one identical revision
  (ADR-0010). Already rejected in ADR-0001 and unchanged here.

## Decision

**Option 2.** A person starts the sync when they want new cards; CI decides whether it is safe; the same person merges
it.

One direction only: commits are read from `Card-Forge/forge` and land in `jczastkiewicz/crucible`. Nothing is ever
pushed the other way — the fork's own history is not upstream's business, and no Crucible branch tracks an upstream
branch.

| Element     | Rule                                                                                                         |
| ----------- | ------------------------------------------------------------------------------------------------------------ |
| Source      | `https://github.com/Card-Forge/forge`, branch `master`, fetched as the `upstream` remote                     |
| Destination | `jczastkiewicz/crucible`, branch `master`, which is `origin`                                                 |
| Trigger     | A person, when new cards are wanted or a milestone is about to start                                         |
| Branch      | `sync/<yyyy-mm-dd>-<upstream-short-sha>`, cut from `master`                                                  |
| Merge       | `git merge upstream/master`, never a rebase (ADR-0001)                                                       |
| Landing     | A pull request against `master`, like every other change (REV-2)                                             |
| Merge style | **A merge commit. Never squash**                                                                             |
| No-op       | `git log master..upstream/master` empty means there is nothing to do                                         |
| Conflict    | Resolved by hand: it means upstream edited one of the six files Crucible owns outside its two reserved paths |

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

`tools/metrics` is new and lands with this ADR's implementation: it recomputes each documented measurement from the tree
and fails when a document disagrees. Without it, the numbers drift on every sync and are corrected only when somebody
notices.

**A red gate is information, not an obstacle to route around.** What each one means is in
[`../runbooks/upstream-sync.md`](../runbooks/upstream-sync.md); the rule is that a golden may be regenerated from the
new corpus, and a cycle count may not.

## Consequences

**Good.** The corpus gate finally fires when the corpus changes, which is what ADR-0001 promised and what a `paths:`
filter scoped to `crucible/**` prevented. A sync is reviewed before it lands, like every other change, and it needs no
stored credential: a pull request opened by a person triggers CI the way an automated one does not. Syncing before a
milestone starts, rather than into the middle of one, is the natural rhythm and this makes it the easy one.

**Bad.** It happens only when someone remembers. Upstream lands about 10 commits a day, so a month of forgetting is a
300-commit diff and a lump of new cards arriving at once — reviewable, but not pleasantly. Nothing warns that a sync is
overdue; the only feedback is the size of the next one.

**Neutral.** Merge commits keep `master`'s history interleaved with upstream's, which `git log` already needed a path
filter to read (ADR-0001). Automation stays available later: if the interval starts slipping, a scheduled job becomes
worth its token, and nothing in the procedure would change except who runs it.

## Related

- [ADR-0001](0001-fork-layout-and-upstream-sync.md) — the fork layout this procedure serves, and merge-not-rebase
- [ADR-0003](0003-go-project-layout.md) — the 82-cycle premise a sync can invalidate
- [ADR-0010](0010-differential-testing-strategy.md) — why the Java tree stays in-tree and version-matched
- [`../runbooks/upstream-sync.md`](../runbooks/upstream-sync.md) — the procedure itself
- [REV-7](../guidelines/05-commit-and-review.md) — the rule that points here
