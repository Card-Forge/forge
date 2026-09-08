# Runbook — Upstream Sync

- **Status:** Active. `tools/metrics` lands in the PR that implements
  [ADR-0015](../adr/0015-upstream-sync-procedure.md); until then its check is run by eye.
- **Applies to:** every merge of `Card-Forge/forge` into this fork
- **Decision and reasoning:** [ADR-0015](../adr/0015-upstream-sync-procedure.md). This file is the procedure only.

Commits are read from `Card-Forge/forge` and land in `jczastkiewicz/crucible`. Never the other way: nothing Crucible
does is pushed to upstream.

Upstream lands about 10 commits a day, so a week between syncs is roughly 70 commits and a handful of card scripts, and
a month is closer to 300. Most syncs are a rubber stamp. The ones that are not are the reason every step below exists.

A clone has the `upstream` remote only if someone added it. Once, per checkout:

```bash
git remote add upstream https://github.com/Card-Forge/forge.git
git remote -v      # origin -> jczastkiewicz/crucible, upstream -> Card-Forge/forge
```

---

## The sync itself

Nothing runs on a schedule. Sync when you want the new cards, and prefer the gap between milestones over the middle of
one — the oracle moving under a port in flight is the one timing that costs you something.

```bash
git fetch upstream                                       # Card-Forge/forge
git log --oneline master..upstream/master | wc -l        # how much is coming; zero means stop here
git checkout -b sync/$(date +%F)-$(git rev-parse --short upstream/master) master
git merge upstream/master
git push -u origin HEAD                                  # jczastkiewicz/crucible
gh pr create --repo jczastkiewicz/crucible --base master --fill
```

Open the pull request as yourself rather than from a script: GitHub raises no workflow runs for events from the default
`GITHUB_TOKEN`, so an automated PR would arrive with no checks on it at all.

The pull request runs the same checks as any other. Four of them can be broken by upstream:

| Check                             | What it means when it goes red                                       |
| --------------------------------- | -------------------------------------------------------------------- |
| `internal/mana` corpus golden     | A new mana symbol, or a changed cost on a card already in the golden |
| `internal/cardtype` corpus golden | A new subtype, or an edit to `TypeLists.txt`                         |
| `javacycles -expect 82`           | The coupling in `forge-game` moved — ADR-0003's premise              |
| `tools/metrics`                   | A measured figure in the documents no longer matches the tree        |

---

## Reviewing the sync PR

1. Read the commit list. It is upstream's, so you are looking for the shape of the change rather than the detail: a new
   set of card scripts, a rules change, a refactor of something Crucible has already ported.
2. Check the four gates above. Green means the port is unaffected and the PR can merge.
3. Skim the diff for `forge-core/src/main/java` and `forge-gui/res/lists`. Those are the files Crucible ports and reads
   directly, and a change there usually deserves a line in the PR description even when every check is green.
4. **Merge with a merge commit.** Do not squash. Squashing replaces upstream's commits with one unrelated commit, so the
   next sync re-resolves everything this one resolved, and the ancestry that makes syncs cheap is gone. This is the only
   irreversible mistake in the whole procedure.

---

## When a corpus golden fails

A moving golden is usually correct: upstream added a card, and the new value is data rather than a defect. The diff is
what tells you which.

1. Read the failure. The test names the input, so it will say which mana cost or type line is new or changed.
2. Decide whether the new value is something the parser should already handle. A new subtype is data. A mana symbol
   Crucible has never seen is a vocabulary gap, and the parser has to learn it before the golden is regenerated.
3. Regenerate only after that decision:

   ```bash
   cd crucible
   go test ./internal/mana -run TestCorpusManaCosts -update
   go test ./internal/cardtype -run TestCorpusTypeLines -update
   go test ./internal/cardtype -run TestCorpusUnknownTypes -update
   ```

4. Read the regenerated diff line by line and commit it in the sync PR, with a body saying which cards moved and why. A
   golden accepted without reading the diff turns the whole suite into a change detector that approves every regression
   (TEST-6).

---

## When `javacycles` fails

**Do not change `-expect 82`.** That number is the premise of [ADR-0003](../adr/0003-go-project-layout.md): 82 direct
two-package cycles in `forge-game` are why `internal/engine` is a single Go package. If the count moved, the premise
moved, and the right response is to find out what changed upstream and record it — not to make the check agree with the
new number.

Run it directly to see the current figure, then open an issue quoting both numbers and the commits between the two
syncs:

```bash
cd crucible
go run ./tools/javacycles -root ../forge-game/src/main/java -prefix forge.game
```

The sync itself is not blocked by this in principle — upstream's coupling is upstream's business — but merging it
without recording the change hides the one signal ADR-0003 asked for.

---

## When `tools/metrics` fails

The tree changed and a document still states the old figure. Refresh it in the sync PR, so the numbers and the corpus
they describe land together:

```bash
cd crucible
go run ./tools/metrics -docs ../docs/crucible          # report what drifted
go run ./tools/metrics -docs ../docs/crucible -update  # rewrite the canonical table
```

Every figure is reproducible from the command printed beside it in
[`../architecture/system-overview.md`](../architecture/system-overview.md). If a document restates a figure instead of
linking to that table, prefer replacing the restatement with a link (DOC-11); the copy is what goes stale.

---

## When the merge conflicts

Crucible touches two reserved paths plus six files at the repository root, so a conflict means upstream edited one of
those six. Nothing else can collide.

Resolve it by hand, and read the upstream side before you keep either version:

```bash
git fetch upstream
git checkout -b sync/$(date +%F)-$(git rev-parse --short upstream/master) master
git merge upstream/master
git status --short | grep '^UU'
```

For each conflicted file, decide deliberately: Crucible-owned configuration keeps the Crucible version plus whatever
upstream added, and an upstream file keeps upstream's version entirely. **Never resolve a conflict by editing an
upstream file to match Crucible's expectations.** That converts a one-time conflict into a permanent one, which is the
failure mode [ADR-0001](../adr/0001-fork-layout-and-upstream-sync.md) exists to prevent. If an upstream edit genuinely
has to be kept, log it in [`../porting/upstream-patches.md`](../porting/upstream-patches.md) in the same commit.

Then push the branch and open the PR as usual, so the gates still run.

---

## When differential parity goes red

From M5, the nightly oracle run compares the Go engine against the Java one. A sync that changes rules behaviour — a
comprehensive-rules update, an effect rewrite — moves the Java side, and the Go side has not been ported yet.

That is expected, and it does not block the sync. Merge it, open a bug naming the upstream commit, and let
[ADR-0010](../adr/0010-differential-testing-strategy.md)'s layer 4 carry it. The sync is how you learn the rules
changed; reverting it only delays learning.

---

## Checking locally before pushing

Optional — CI runs all of it on the pull request — but it turns a red check into a fixed one before anybody sees it.

```bash
cd crucible
go test -race -count=1 ./...
go run ./tools/javacycles -root ../forge-game/src/main/java -prefix forge.game -expect 82
```

## Never

- Squash a sync PR.
- Rebase Crucible commits onto a moving upstream (ADR-0001).
- Edit an upstream file to resolve a conflict.
- Regenerate a golden without reading the diff.
- Change `-expect 82` to make a check pass.

## Related

- [ADR-0015](../adr/0015-upstream-sync-procedure.md) — the decision and its alternatives
- [ADR-0001](../adr/0001-fork-layout-and-upstream-sync.md) — fork layout, merge not rebase
- [REV-7](../guidelines/05-commit-and-review.md) — the rule that points here
