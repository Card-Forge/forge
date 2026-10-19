#!/usr/bin/env bash
# PostToolUse(Write|Edit): gofmt -s for Go (GO-1), prettier for Markdown (DOC-14).
# prettier skips the Claude-only files /.prettierignore lists; --ignore-path
# makes that hold whatever directory the hook runs from.
. "$(dirname "$0")/lib.sh"
in=$(cat)
f=$(jq -r '.tool_response.filePath // .tool_input.file_path // ""' <<<"$in")
case "$f" in
*.go) gofmt -s -w "$f" ;;
*.md) prettier --write --log-level warn --ignore-path "$(repo_root "$(dirname "$f")")/.prettierignore" "$f" ;;
esac
exit 0
