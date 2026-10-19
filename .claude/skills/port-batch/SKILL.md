---
name: port-batch
description:
  Port a large batch of M6 ApiType effects (roughly 8 or more) by splitting it across parallel effect-porter /
  effect-porter-hard subagents in their own worktrees, then merging, reconciling counts and docs, gating and cleaning
  up. Use when asked to port "at least N APIs", "the next big pack", or any M6 batch too big for one context.
---

# Port a batch through parallel porters

For a handful of effects, use `port-effect` inline instead: a worktree porter costs a cold start and a merge. This skill
is the orchestration around it; every porter follows `port-effect` itself.

## 1. Pick

```bash
crucible/scripts/unported-apis.sh 60
```

Drop every API marked `deferred`, unless the batch takes on its blocker. Take the most-used remaining APIs until the
batch reaches the requested size, with about 20% extra. Reason: some APIs turn out unportable once researched, and the
batch still has to reach its size.

## 2. Triage and group

Per API, decide routine or hard (definitions in `port-effect` step 0). Read the Java yourself only when the name does
not settle it; otherwise let the porter decide and report back.

Group APIs into porter batches:

| Rule                                                                                                             | Reason                                                                                   |
| ---------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| APIs that share an engine piece go to the same porter (all token makers; all Animate kin; all face-down effects) | Two porters building the same helper produce two helpers and a conflict                  |
| Routine porter: 5-8 APIs. Hard porter: 1-3                                                                       | A routine batch finishes in one context; a hard one needs room for design                |
| At most 4 porters at once                                                                                        | Each runs `gates.sh full` per commit; more than 4 saturate the machine's `go test -race` |
| Nothing that changes a documented contract (`port-effect` step 0) goes to a porter                               | Needs an ADR first (ADRP-4), which is the user's call                                    |

## 3. Spawn

Commit or stash first: the worktrees branch from the current `HEAD` (`settings.json` `worktree.baseRef`). Then spawn
every porter in one message, so they run in parallel: `Agent` with `subagent_type: effect-porter` (or
`effect-porter-hard`) and `isolation: worktree`. Each prompt names:

- the APIs, and the batch file slug (`effects-<slug>.md`), distinct per porter;
- the other porters' API lists, marked "not yours: do not touch";
- the report expected back (the agent definitions already specify its shape).

Wait for the completion notifications. Do not read a porter's output file mid-run.

## 4. Merge

On the branch that will carry the batch, with a clean tree:

```bash
crucible/scripts/merge-porters.sh list                   # branch, commits not on HEAD, uncommitted files
crucible/scripts/merge-porters.sh merge <branch>...      # cherry-picks, resolves routine conflicts, fixes counts
```

Merge the branch touching the most shared files first. The script resolves `registry_gen.go` (regenerated), index-row
and defect tables (union of both sides), and the count sentence in `CLAUDE.md` and the plan (rewritten from the
registry). It stops on any other conflict with the cherry-pick in progress. Typical ones are both porters adding
`PlayerController` methods, `Game` fields, or `Clone` lines. Keep both sides, since these are additive. Then
`git cherry-pick --continue` and rerun `merge` with the remaining branches.

Every porter prompt carries the prompt block of `docs/crucible/runbooks/subagent-worktrees.md`.

Porters never run `git`: the user's `rtk` hook rewrites it to `rtk git`, which the worktree-isolation guard refuses.
Their finished work is therefore always uncommitted, `uncommitted>0` in `list`. Read each diff, then commit what
belongs to the batch with `crucible/scripts/merge-porters.sh commit BRANCH "<message with the attribution trailer>"`
(drops the porter's scratch `PORTER_PLAN.md`, runs the commit hook's gates) and merge it. Anything else is either
explained or dropped, never silently lost. A porter reporting a bypass (`/usr/bin/git`) skipped those gates: rerun
`gates.sh full` on the merged result.

## 5. Reconcile, verify, commit

1. `## Not ported yet` in `game-state.md`: the remaining-effects sentence, and the largest-gaps list in `CLAUDE.md`
   (fresh `unported-apis.sh` numbers).
2. `gates.sh full` (pipe through `tail -40`). The merged batch is the first time the porters' changes run together, so a
   green porter branch does not imply a green merge. Coverage is the usual failure; `port-effect` step 4 has the
   per-file check.
3. `rules-review` skill on `git diff <base>..HEAD` plus the working tree. Fix its findings.
4. Commit the reconciliation. The commit hook runs the gates in this checkout.

## 6. Clean up

```bash
crucible/scripts/merge-porters.sh cleanup <branch>...
crucible/scripts/merge-porters.sh list    # must print nothing
```

`cleanup` refuses a branch with commits `HEAD` lacks or a worktree with uncommitted files. For each refusal, merge the
work or ask the user. Never force it. Reason: an orphaned porter branch or worktree is how ported work went missing
before.

## Report

Per API: ported / partial (rejected params) / deferred (blocker). Then the resolved-API count before and after, porter
findings worth the user's attention (Forge bugs, design calls), gates, commit hashes. Never push.
