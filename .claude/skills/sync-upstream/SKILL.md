---
name: sync-upstream
description:
  Merge Card-Forge/forge's master into this fork, following docs/crucible/runbooks/upstream-sync.md, then verify the Go
  port still agrees with what moved. Use when asked to sync, update from upstream, or pull in the latest Forge changes.
---

# Sync with Card-Forge/forge

Procedure: `docs/crucible/runbooks/upstream-sync.md` (ADR-0015). Read it first, every time: merge commands, the gate
table, golden regeneration, `javacycles -expect 87`, `tools/metrics`, conflicts, the "Never" list. This skill adds only
what the runbook does not cover.

## Merge

| Situation | Do |
| --- | --- |
| Landing the sync PR | Merge commit. Never squash, never rebase (ADR-0001). Only irreversible mistake in the procedure |
| Conflict in `forge-gui/res/cardsfolder/` or upstream file | `git checkout --theirs <path>`, never a hand-merge |
| Conflict in a Crucible-owned root config (REV-1 list) | Crucible's side plus what upstream added |
| `gh` command | `--repo jczastkiewicz/crucible`: `gh` defaults to upstream |
| Branch diverged from its remote | Push to a new branch name. Never force-push: Claude Code's git guard blocks it, and routing around a harness denial is not allowed |

## Verify the port, not only the gates

Run the uncached suite once after the merge. `gates.sh full` is tuned for one small diff and caches tests:

```bash
cd crucible && go test -race -count=1 ./...
```

Two failure shapes, two responses:

| Failure | Response |
| --- | --- |
| Corpus golden moved, no "unknown"/"unexpected" wording | New data. Regenerate with that test's `-update`, read the diff (TEST-6). Beyond the runbook's table also `TestCorpusParses` (`internal/carddb`), `TestCorpusVocabulary`, `TestCorpusCosts`, `TestCorpusDecklists`, `TestCorpusAST`. Trust `go test` output over any list |
| Parser rejects a script, or `apiscan` reports a param nothing reads | Forge card-script bug (PORT-8). Fix locally, log in `upstream-patches.md` "Pending upstream fixes", separate branch `upstream-pr/<slug>` off `upstream/master` with only that fix. Never in the sync PR |

`apiscan -check -api` never checks `S:` static-ability params: `tools/apiscan/scan.go` `readEffectParams` walks only
`ability/effects/*.java`. Reason: Sanctum Lurker's `Affected$` (should be `ValidCard$`) passed every gate. A sync that
adds or touches an `S:` line in a mode Crucible ports: check its params against `forge-game/.../staticability/` by hand.

## Diff the Java against the port

```bash
git diff <old-sha> <new-sha> --stat -- forge-game/src/main/java forge-core/src/main/java forge-gui/res/lists
```

For each file touching something ported (grep its class/method in `crucible/internal/engine/*.go` and
`docs/crucible/porting/port-log/`), read the diff:

| Change | Do |
| --- | --- |
| Ported rule got a new escape hatch (static mode, param, SBA or combat exemption) | Port it in the same commit, with a test. Template: `ignoreLegendRule` in `staticability.go` |
| Unported rule refactored | Row in `game-state.md` "Not ported yet" if missing. Precedent: Shadow-blocking moved to `Mode$ CanBlockIfShadow`; tracking update, not new work |
