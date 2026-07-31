# AGENTS.md — Reforge Commander

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
