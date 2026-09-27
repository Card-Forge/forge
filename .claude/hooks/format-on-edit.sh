#!/usr/bin/env bash
# PostToolUse(Write|Edit): gofmt -s for Go (GO-1), prettier for Markdown (DOC-14).
in=$(cat)
f=$(jq -r '.tool_response.filePath // .tool_input.file_path // ""' <<<"$in")
case "$f" in
*.go) gofmt -s -w "$f" ;;
*.md) prettier --write --log-level warn "$f" ;;
esac
exit 0
