#!/usr/bin/env bash
# SessionStart (Claude Code on the web only): install the tools the
# container image doesn't ship. Synchronous, so the first commit or Bash
# call never races either install; both are no-ops once their binary
# exists.
#
# - golangci-lint: the container's copy is built with an older Go than
#   crucible/go.mod targets, which fails every commit
#   (crucible/scripts/ensure-golangci.sh has the details).
# - rtk: the condensed-command-output proxy RTK.md documents. Wired locally
#   through the user's own ~/.claude/settings.json (Homebrew install), which
#   the container never sees -- this is the cloud-only equivalent.
set -euo pipefail
[ "${CLAUDE_CODE_REMOTE:-}" = "true" ] || exit 0
"$CLAUDE_PROJECT_DIR/crucible/scripts/ensure-golangci.sh" >/dev/null
"$CLAUDE_PROJECT_DIR/.claude/hooks/ensure-rtk.sh"
