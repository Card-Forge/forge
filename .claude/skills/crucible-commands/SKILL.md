---
name: crucible-commands
description: Every build, test, gate, lint, smoke-probe, landporter, registry-generation and Java-oracle command for this repo, with what each gates. Use before running or recommending any of them, and before a commit.
---

# Crucible commands

```bash
# Markdown — both required before every commit (DOC-14, DOC-15)
prettier --write .            # format; .prettierignore excludes upstream Forge files and Claude-only docs (DOC-14)
prettier --check .            # CI gate
npx markdownlint-cli2 "CLAUDE.md" "docs/crucible/**/*.md"   # semantic lint
```

```bash
# Go — every line below is a CI gate (.github/workflows/crucible-go.yml)
cd crucible && gofmt -l . && go vet ./...
cd crucible && go test -race ./...
cd crucible && go test -race -coverprofile=cover.out ./... && go run ./tools/covergate -profile cover.out   # TEST-12 floors
cd crucible && go run ./tools/enginelint -config internal/engine/enginelint.json   # new engine file → new group + allow-list
cd crucible && go run ./tools/docgate -module . -docs ../docs/crucible            # DOC-12 docs land with code
cd crucible && go run ./tools/apiscan -check && go run ./tools/apiscan -check -api
cd crucible && golangci-lint run   # v2.14.0, same as CI; scripts/ensure-golangci.sh installs one built with go.mod's Go

# All of the above in CI order. .claude/ hooks run full before every Claude commit (blocking) and fast on Stop (report
# only), both with GATES_AUTO_SKIP=1: gates whose inputs are unchanged since HEAD are skipped, and go test uses its cache
# (CI keeps -count=1). Measured: ~25 s with tests cached; a changed engine adds its own ~45 s test run.
crucible/scripts/gates.sh fast|full

# What to port next: unregistered APIs by corpus line count, deferred ones marked (port-effect step 0)
crucible/scripts/unported-apis.sh 30

# How many cards the engine plays, and why the rest are refused (smoke-probe.md); ~2 min for the whole corpus
cd crucible && go run ./cmd/crucible card-smoke -all -workers 8     # or -decks <dir> / -cards "A,B"

# After landing engine work in main: both smoke probes (corpus + precons), rewrite quoted numbers in smoke-probe.md,
# CLAUDE.md and the three plans, before/after table
cd crucible && go run ./tools/landporter reconcile
# Open-plan rows: delete or add (-after takes a row that still exists)
cd crucible && go run ./tools/landporter plan rm ROW ; go run ./tools/landporter plan add -after ROW -id ID "text"
# Rare worktree-subagent run: prompt for a row; commit+merge+remeasure; docs commit and cleanup
cd crucible && go run ./tools/landporter brief ROW slug ; go run ./tools/landporter land BRANCH "msg" ; go run ./tools/landporter finish BRANCH "docs: msg"
crucible/scripts/merge-porters.sh list|merge|cleanup [BRANCH...]

# After editing .claude/hooks: their decision logic (what counts as a commit, what REV-1 guards)
.claude/hooks/hooks_test.sh

# Regenerate NewRegistry after adding an effect (ADR-0017); CI fails on a stale registry_gen.go
cd crucible && go generate -run genregistry ./internal/engine

# Regenerate the golden AST fingerprints, then review the diff
cd crucible && go test ./internal/carddb/compile -run TestCorpusAST -update

# Java oracle
mvn -pl crucible/oracle-java -am test

# Upstream Java suite: TestNG, needs a display
mvn -U -B clean test          # CI runs this under Xvfb
```
