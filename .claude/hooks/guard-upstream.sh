#!/usr/bin/env bash
# PreToolUse(Write|Edit|NotebookEdit): REV-1. Every line touched outside
# Crucible's own paths is a future rebase conflict, so such an edit needs the
# user's explicit approval. Paths already logged in upstream-patches.md pass.
#
# The path is judged relative to the checkout that holds the file, so an edit
# to forge-gui/... inside a .claude/worktrees/<name> checkout is caught too.
. "$(dirname "$0")/lib.sh"
in=$(cat)
f=$(jq -r '.tool_input.file_path // .tool_input.notebook_path // ""' <<<"$in")
[ -n "$f" ] || exit 0
case "$f" in /*) ;; *) f="$(jq -r '.cwd // ""' <<<"$in")/$f" ;; esac
root=$(repo_root "$(dirname "$f")")
case "$f" in "$root"/*) rel=${f#"$root"/} ;; *) exit 0 ;; esac

case "$rel" in
crucible/* | docs/crucible/* | CLAUDE.md | PORTER_PLAN.md) exit 0 ;; # PORTER_PLAN.md: a porter's scratch plan, dropped at commit
.claude/worktrees/*) ;; # another checkout nested in this one; never exempt
.claude/**) exit 0 ;;
esac
grep -qF "\`$rel\`" "$root/docs/crucible/porting/upstream-patches.md" 2>/dev/null && exit 0

jq -n --arg r "REV-1: $rel is upstream Forge. Edit only for a PORT-8 fix, and log it in docs/crucible/porting/upstream-patches.md in the same commit." \
  '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "ask", permissionDecisionReason: $r}}'
