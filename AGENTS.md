# AGENTS.md — Reforge Commander

> **Personal project.** Not a community fork and not affiliated with the Forge team or Wizards of the
> Coast. The code is largely AI-generated and may contain bugs. It is provided as-is; anyone who uses it
> does so at their own risk and no responsibility is taken for anything resulting from its use.
> Contributions are welcome but nothing is guaranteed. The engineering rules below exist to keep the
> upstream sync conflict-free — they do not imply community standards or support.

Commander-first fork of [Card-Forge/forge](https://github.com/Card-Forge/forge). Java 17+, Maven multi-module
(`forge-game`, `forge-gui`, `forge-gui-desktop`, `forge-gui-ios`). GUI runs under the FlatLaf dark theme in
`ReforgeCommanderApp.main()`.

## Non-negotiable rules

- **Additive-only changes to upstream files.** Never modify an existing Forge class when a new class can extend it.
  Reforge classes are marked with a "REFORGE COMMANDER EXTENSION" header. This is what keeps the automated upstream
  sync conflict-free.
- **No comments unless they earn their place.** When a deliberate simplification has a known ceiling, use a
  `ponytail:` comment naming the ceiling and the upgrade path.
- **Roadmap status lives in code markers, not prose.** Completed/partial work carries `// doc:<item> <STATUS>`
  (`DONE`/`PARTIAL`) on the implementing line. `tools/doc-status.sh` fails CI if `docs/development.md` disagrees.
  When you finish a roadmap item: update `docs/development.md` section table + priority matrix, and mark the code.
- **Never push to master.** Branch protection ruleset "Condom" requires: two Test build checks (Java 17/21), CodeQL,
  and Copilot review. All changes land via PR.

## GitHub issues workflow

- **Start every session by checking open issues.** Run `gh issue list` (or use the GitHub MCP tools) to see known
  problems, planned work, and open questions. This is the backlog — don't re-discover what's already filed.
- **Document problems as issues, not just conversation.** If you find a bug, a docs inconsistency, a missing feature,
  or a governance gap during your work — create a GitHub issue immediately. Don't leave it as a conversation note
  that evaporates when the session ends. Title format: `type(scope): short summary` matching commit style.
- **Reference issues in PRs.** When a PR fixes an issue, mention it in the PR body (`Fixes #N`). This auto-closes
  the issue on merge and keeps the backlog clean.
- **Close stale issues.** If something you filed is no longer relevant (e.g., descope decided, upstream fixed it),
  close it with a comment explaining why.

## Docs to keep current

- `docs/development.md` — the roadmap + status matrix. Section tables have a `Status` column; keep it truthful.
- `README.md` — project overview.

## CI / automation

- `.github/workflows/test-build.yaml` — `mvn -U -B clean test` on Java 17+21 matrix + `doc-status` job.
- `.github/workflows/sync-upstream.yml` — daily 06:00 UTC sync from upstream, auto-merges via PR when green.
  Do not add a remote literally named `upstream` to any workflow: it makes `gh pr create` misattribute the repo.
- `.github/workflows/ios-compat-gate.yml` — iOS/MobiVM link audit gate.
- CodeQL (default setup), Dependabot, and CodeRabbit (quiet profile, critical issues only) are configured.

## Commit style

`feat(ui):` / `fix(engine):` / `style(engine):` / `ci:` / `docs:` — lowercase scope, imperative mood,
body with details. Identity: Reforge <dev@reforge-commander.dev>.
