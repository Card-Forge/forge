#!/bin/sh
# Usage: build-dmg.sh <unpacked tarball dir> <version> <output .dmg>
set -eu
here=$(cd "$(dirname "$0")" && pwd)
icns="$here/../../forge-gui-desktop/src/main/config/Forge.icns"
version=$2
stage=$(mktemp -d)
trap 'rm -rf "$stage" "$stage.raw.dmg"' EXIT

make_app() {
  app="$stage/$1.app"
  mkdir -p "$app/Contents/MacOS" "$app/Contents/Resources"
  sed -e "s/@NAME@/$1/g" -e "s/@EXEC@/$2/g" -e "s/@ID@/$3/g" \
      -e "s/@VERSION@/${version%%-*}/g" \
      "$here/macos/Info.plist.in" > "$app/Contents/Info.plist"
  cp "$here/macos/$2" "$app/Contents/MacOS/$2"
  chmod 755 "$app/Contents/MacOS/$2"
  cp "$icns" "$app/Contents/Resources/Forge.icns"
}

make_app "Forge" forge org.cardforge.forge
make_app "Forge Adventure" forge-adventure org.cardforge.forge-adventure
cp "$here/macos/find-java.sh" "$stage/Forge.app/Contents/Resources/"
cp -a "$1" "$stage/Forge.app/Contents/Resources/forge"
ln -s /Applications "$stage/Applications"
cp "$here/macos/updating.txt" "$stage/Updating Forge.txt"

mkdir -p "$(dirname "$3")"
# makehybrid writes the image without mounting it; hdiutil create -srcfolder mounts one and often takes minutes.
hdiutil makehybrid -quiet -hfs -hfs-volume-name "Forge-$version" -o "$stage.raw.dmg" "$stage"
hdiutil convert -quiet "$stage.raw.dmg" -format ULFO -ov -o "$3"
