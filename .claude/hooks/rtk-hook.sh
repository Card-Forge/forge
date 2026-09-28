#!/usr/bin/env bash
# PreToolUse (Bash): pipe through the rtk condensing proxy on Claude Code on
# the web, where the container never sees the user's own ~/.claude/
# settings.json (which wires `rtk hook claude` globally on every local
# machine). A no-op locally, since that global hook already runs there --
# running both would pipe the same tool call through rtk twice.
set -euo pipefail
[ "${CLAUDE_CODE_REMOTE:-}" = "true" ] || exit 0
exec rtk hook claude
