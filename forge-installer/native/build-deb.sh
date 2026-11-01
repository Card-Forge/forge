#!/bin/sh
# Usage: build-deb.sh <unpacked tarball dir> <version> <output .deb>
set -eu
here=$(cd "$(dirname "$0")" && pwd)
# "~" sorts before the release: 2.0.16~snapshot.2026.09.30 < 2.0.16
year=$(date -u +%Y)
debver=$(printf '%s' "$2" | sed -e "s/-SNAPSHOT-/~snapshot.$year./" -e 's/-SNAPSHOT$/~snapshot/')
root=$(mktemp -d)
trap 'rm -rf "$root"' EXIT

mkdir -p "$root/opt/forge" "$root/usr/bin" "$root/usr/share/applications" \
         "$root/usr/share/icons/hicolor/96x96/apps" "$root/DEBIAN"
cp -a "$1/." "$root/opt/forge/"
for app in forge forge-adventure; do
  printf '#!/bin/sh\nexec /opt/forge/%s.sh "$@"\n' "$app" > "$root/usr/bin/$app"
  chmod 755 "$root/usr/bin/$app"
done
cp "$here/linux/forge.desktop" "$here/linux/forge-adventure.desktop" "$root/usr/share/applications/"
cp "$here/../ic_launcher.png" "$root/usr/share/icons/hicolor/96x96/apps/forge.png"

size=$(du -sk "$root" | cut -f1)
sed -e "s/@VERSION@/$debver/" -e "s/@SIZE@/$size/" \
    "$here/linux/control" > "$root/DEBIAN/control"

mkdir -p "$(dirname "$3")"
dpkg-deb --root-owner-group -Zxz --build "$root" "$3"
