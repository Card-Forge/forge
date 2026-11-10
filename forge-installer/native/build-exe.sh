#!/bin/sh
# Usage: build-exe.sh <unpacked tarball dir> <version> <output .exe>
set -eu
here=$(cd "$(dirname "$0")" && pwd)
dist=$(realpath "$1")
out=$(realpath -m "$3")
mkdir -p "$(dirname "$out")"

list=$(mktemp)
trap 'rm -f "$list"' EXIT
for p in "$dist"/*; do
  name=$(basename "$p")
  if [ -d "$p" ]; then
    # cmd's rmdir, unlike RMDir /r, does not delete through junctions into other folders.
    printf '%s\nPop $0\n' "nsExec::Exec 'cmd /c rmdir /s /q \"\$INSTDIR\\$name\"'"
  else
    printf 'Delete "$INSTDIR\\%s"\n' "$name"
  fi
done > "$list"

makensis -V2 \
  -DDIST="$dist" \
  -DVERSION="$2" \
  -DOUTFILE="$out" \
  -DICON="$here/../../forge-gui-desktop/src/main/config/forge.ico" \
  -DUNINSTALL_LIST="$list" \
  "$here/windows/forge.nsi"
