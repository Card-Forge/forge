#!/usr/bin/env bash
# Lists every ApiType the corpus uses that NewRegistry does not resolve yet,
# most-used first: "<lines> <Api>". This is the port-effect skill's step 0 --
# what to port next.
#
#   unported-apis.sh [N]   top N (default 30)
#
# A count is the number of `AB$`/`SP$`/`DB$ <Api>` occurrences under
# forge-gui/res/cardsfolder (upcoming/ included). The name is matched
# case-insensitively, as ApiType.smartValueOf does: faces_of_the_past.txt
# writes "TaporUntapAll". An API the registry already resolves never shows
# up here, whatever its count.
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
engine="$root/crucible/internal/engine"
corpus="$root/forge-gui/res/cardsfolder"

registered=$(grep -oE 'r\[API[A-Za-z]+\]' "$engine/registry_gen.go" | sed -E 's/r\[API(.*)\]/\1/' | tr 'A-Z' 'a-z' | sort -u)
names=$(sed -n '/^var apiNames/,/^}/p' "$engine/apitype_gen.go" | grep -oE '"[A-Za-z]+"' | tr -d '"')

grep -rhoiE '\b(AB|SP|DB)\$ ?[A-Za-z]+' "$corpus" |
	sed -E 's/^.*\$ ?//' | tr 'A-Z' 'a-z' | sort | uniq -c |
	while read -r n api; do
		grep -qx "$api" <<<"$registered" && continue
		canon=$(grep -ix "$api" <<<"$names") || continue
		echo "$n $canon"
	done | sort -rn | head -n "${1:-30}"
