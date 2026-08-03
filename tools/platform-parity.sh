#!/usr/bin/env bash
# platform-parity.sh — recurring guard that desktop (*forge-gui-desktop*, Swing)
# and mobile (*forge-gui-mobile*, LiGDX) Reforge features stay in sync.
#
# Desktop and mobile are separate UI toolkits, so parity ISN'T automatic; this
# script makes the check explicit and CI-enforced so a feature added to one
# platform can't silently miss the other.
#
# Manifest format (tab/space separated), one capability per line:
#   <feature> <type> <desktop-path-or--> <mobile-path-or-->
#   type=shared    -> the feature lives in a single shared file (assert it exists)
#   type=required  -> BOTH a desktop and a mobile file must exist; missing either fails CI
#
# Note: a shared file may have BOTH frontends applied from it without a dual
# pair (type=shared). Only add type=required once you have actually written both
# platform implementations, so the guard doesn't go red before a port lands.
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
fail=0

check_present() {
    local label=$1 file=$2
    if [ -z "$file" ] || [ "$file" = "-" ]; then
        return
    fi
    if [ ! -f "$ROOT/$file" ]; then
        echo "FAIL: $label missing ($file)."
        fail=1
    fi
}

while read -r feat type d m; do
    [ -z "$feat" ] && continue
    case "$feat" in \#*) continue ;; esac

    case "$type" in
        shared)   check_present "$feat (shared)" "$d" ;;
        required) check_present "$feat (desktop)" "$d"; check_present "$feat (mobile)" "$m" ;;
    esac
done <<'EOF'
# feature                    type     desktop                               mobile
ReforgeTheme                  shared   forge-gui/src/main/java/forge/localinstance/skin/ReforgeTheme.java  -
Commander-lobby              required   forge-gui-desktop/src/main/java/forge/screens/home/playcommander/VSubmenuPlayCommander.java   forge-gui-mobile/src/forge/screens/constructed/LobbyScreen.java
EOF

if [ $fail -ne 0 ]; then
    echo "platform-parity: FAIL — desktop & mobile drifted. Port the feature to the missing platform (roadmap section 10) or mark it desktop-only."
    exit 1
fi
echo "platform-parity: OK"