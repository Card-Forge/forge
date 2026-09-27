#!/usr/bin/env bash
# Stop: report fast-gate failures to the user. Never blocks.
# Runs in the checkout the session works in (hook input cwd); skipped when
# nothing under crucible/ or docs/ changed since HEAD.
. "$(dirname "$0")/lib.sh"
in=$(cat)
cwd=$(jq -r '.cwd // ""' <<<"$in")
root=$(repo_root "${cwd:-${CLAUDE_PROJECT_DIR:-.}}")
cd "$root" || exit 0
[ -n "$(git status --porcelain -- crucible docs/crucible CLAUDE.md)" ] || exit 0
out=$(GATES_AUTO_SKIP=1 crucible/scripts/gates.sh fast 2>&1) && exit 0
jq -n --arg m "Fast gates failed (report only): $(printf '%s' "$out" | tail -1)" '{systemMessage: $m}'
exit 0
