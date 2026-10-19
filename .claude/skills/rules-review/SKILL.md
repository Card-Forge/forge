---
name: rules-review
description: Review a Crucible Go diff against the non-negotiables (GO-2/3/4/7/8/9/12, PORT-2/8, TEST-1/8, REV-1, DOC-12) before committing engine or carddb work. Use when asked to review a change or before a commit.
---

Review Crucible Go changes in the main session before committing. Read only the diff; read rule text only for rules you flag.

Default scope: `git diff HEAD` plus untracked files under `crucible/`. If given a commit range, use `git diff <range>`.

Read the changed code, then the rule text for anything you flag: `docs/crucible/guidelines/01-go-coding-standards.md`
(GO-n), `02-java-to-go-translation.md` (PORT-n), `03-testing-standards.md` (TEST-n), `05-commit-and-review.md` (REV-n).

## Check each of these

| Rule | Violation looks like |
| --- | --- |
| GO-2 | package-level `var` holding mutable state in `internal/engine/...` or `internal/carddb/...` |
| GO-3 | `sync.Mutex`, `sync.RWMutex`, `atomic` in the engine |
| GO-4 | `reflect` in the engine; effect dispatch not through `NewRegistry()` |
| GO-7 | `panic` on something a card script can cause; script-reachable failure not returned as `error` |
| GO-8 | `any` / `interface{}` in engine or carddb; `map[string]string` for ability params |
| GO-9 | identity by pointer instead of `CardID` / `PlayerID` |
| GO-12 | ranging over a bare Go `map` where order reaches game state, events or triggers (use `collect.Ordered*`) |
| PORT-2 | parsing script strings at resolve time instead of using the compiled AST / typed params |
| PORT-8 | Go code compensating for a Forge bug instead of reporting it |
| effects | new `<api>effect.go` that does not reject unresolved params with an `error` before acting, or skips `subAbilityConditionMet` |
| TEST-1 | new test in `package engine` (internal) without a why-comment; default is `package engine_test` |
| TEST-8 | mocking library or fake that a different DB / RNG / controller would replace |
| tests | engine test with one player (game ends at first SBA check, CR 104.2a); ignored `error` returns |
| REV-1 | edits outside `crucible/`, `docs/crucible/`, `.claude/`, `CLAUDE.md` not logged in `porting/upstream-patches.md` |
| DOC-12 | new package without `architecture/module-map.md` row; a reproduced Java quirk or deviation without a `port-log/quirks.md` row (ADR-0043) |

Also flag plain correctness bugs you are confident of: wrong CR rule, off-by-one in counts, error swallowed, event not
emitted where Java fires a trigger.

## Output

Fix findings, then commit. One line per finding, most severe first:

`path:line  RULE  problem. fix.`

Report only what you verified by reading the code; mark anything uncertain `(unsure)`.
