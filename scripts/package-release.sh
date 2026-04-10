#!/usr/bin/env bash
# Usage: ./scripts/package-release.sh <version>
# Prereq: master worktree = $PWD, beta worktree = ../forge-beta, both built
set -euo pipefail

VERSION="${1:?Usage: $0 <version>}"
STAGING="/tmp/forge-release-$VERSION"
OUT="/tmp/Forge-$VERSION.zip"

echo "==> Cleaning staging: $STAGING"
rm -rf "$STAGING"
mkdir -p "$STAGING/forge"

echo "==> Extracting stable tarball"
STABLE_TAR=$(ls forge-installer/target/forge-installer-*.tar.bz2 2>/dev/null | head -1)
if [ -z "$STABLE_TAR" ]; then
    echo "ERROR: no stable tarball. Run 'mvn -B -P windows-linux install -DskipTests' first."
    exit 1
fi
tar -xjf "$STABLE_TAR" -C "$STAGING/forge"

echo "==> Locating desktop jar"
DESKTOP_JAR=$(ls "$STAGING/forge/forge-gui-desktop-"*"-jar-with-dependencies.jar" 2>/dev/null | head -1)
if [ -z "$DESKTOP_JAR" ]; then
    echo "ERROR: forge-gui-desktop jar not found in staging."
    exit 1
fi
JAR_BASENAME=$(basename "$DESKTOP_JAR")

echo "==> Copying beta jar"
cp "$DESKTOP_JAR" "$STAGING/forge/forge-beta.jar"

echo "==> Generating forge-beta launchers"
for ext in command sh; do
    if [ -f "$STAGING/forge/forge.$ext" ]; then
        sed -e "s/${JAR_BASENAME}/forge-beta.jar/g" \
            -e 's|java |java -Dforge.commander.enhanced=true |g' \
            "$STAGING/forge/forge.$ext" > "$STAGING/forge/forge-beta.$ext"
        chmod +x "$STAGING/forge/forge-beta.$ext"
    fi
done
# forge-beta.exe comes from the Maven build (Launch4j) — already in the tarball
# Windows batch launcher with JVM flag (fallback for systems without the exe)
if [ -f "$STAGING/forge/forge.bat" ]; then
    sed -e "s/${JAR_BASENAME}/forge-beta.jar/g" \
        -e 's|java |java -Dforge.commander.enhanced=true |g' \
        "$STAGING/forge/forge.bat" > "$STAGING/forge/forge-beta.bat"
else
    cat > "$STAGING/forge/forge-beta.bat" <<'BAT'
@echo off
cd /d "%~dp0"
java -Xmx4096m -Dforge.commander.enhanced=true -Dio.netty.tryReflectionSetAccessible=true -Dfile.encoding=UTF-8 -jar forge-beta.jar %*
BAT
fi

echo "==> Creating release zip"
cd "$STAGING"
zip -rq "$OUT" forge
echo "==> Done: $OUT"
ls -lh "$OUT"
