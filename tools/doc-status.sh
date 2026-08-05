#!/usr/bin/env bash
# doc-status.sh — verifies docs/development.md roadmap statuses match code markers.
#
# Marker format in Java sources:  // doc:<item> <STATUS>   where STATUS is DONE or PARTIAL
#   e.g.  // doc:1a DONE
# Rule: every marker must have a matching row in docs/development.md of the form
#   | <item> | ... | **<STATUS>** |
# If a marker exists without its doc row, the build fails so docs can't go stale.
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOC="$ROOT/docs/development.md"
fail=0

markers=$(grep -rhoE '// doc:[0-9a-z]+ (DONE|PARTIAL)' "$ROOT"/forge-game "$ROOT"/forge-gui "$ROOT"/forge-gui-desktop 2>/dev/null \
    | sed 's|.*// doc:||' | sort -u)

if [ -z "$markers" ]; then
    echo "doc-status: no // doc: markers found in code — nothing to verify."
    exit 0
fi

while read -r item status; do
    [ -z "$item" ] && continue
    if ! grep -qE "^\| $item \|" "$DOC"; then
        echo "FAIL: docs/development.md has no row for item '$item' (code marker says $status)."
        fail=1
    elif ! grep -qE "^\| $item \|.*\*\*$status\*\*" "$DOC"; then
        echo "FAIL: docs/development.md row '$item' not marked '$status' (code: // doc:$item $status)."
        fail=1
    fi
done <<< "$markers"

if [ $fail -ne 0 ]; then
    echo "Fix: update the Status column in docs/development.md to match code markers, or remove stale markers."
    exit 1
fi

echo "doc-status: OK — all markers match docs/development.md."
