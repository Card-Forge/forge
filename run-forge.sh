#!/bin/sh
# Launches the Forge desktop GUI from a source build.
#
# Fixes three things the stock forge.sh does not handle when running from a
# build tree (especially inside tmux / SSH):
#   1. Runs from forge-gui/ so the "res" resource folder is found.
#   2. Sets DISPLAY (tmux does not inherit it) so the Swing GUI can open.
#   3. Passes the --add-opens flags XStream needs on Java 17+ (gauntlet load).

set -e

# --- locate the repo and the built jar --------------------------------------
REPO_DIR=$(cd "$(dirname "$0")" && pwd)
RES_DIR="$REPO_DIR/forge-gui"

# -t: newest first, so a stale jar from an older version is never picked
JAR=$(ls -t "$REPO_DIR"/forge-gui-desktop/target/forge-gui-desktop-*-jar-with-dependencies.jar 2>/dev/null | head -n 1)
if [ -z "$JAR" ]; then
    echo "ERROR: built jar not found. Build first with: mvn -DskipTests install" >&2
    exit 1
fi

# --- make sure there is a display --------------------------------------------
if [ -z "$DISPLAY" ] && [ -z "$WAYLAND_DISPLAY" ]; then
    for d in /tmp/.X11-unix/X*; do
        [ -e "$d" ] || continue
        DISPLAY=":${d##*/X}"
        export DISPLAY
        echo "DISPLAY was empty; using $DISPLAY"
        break
    done
fi
# X server started by GDM keeps its auth cookie here, not in ~/.Xauthority
if [ -z "$XAUTHORITY" ] && [ -r "/run/user/$(id -u)/gdm/Xauthority" ]; then
    XAUTHORITY="/run/user/$(id -u)/gdm/Xauthority"
    export XAUTHORITY
fi

# --- JVM module opens (XStream reflective access on Java 17+) ----------------
ADD_OPENS="--add-opens java.base/java.util=ALL-UNNAMED \
--add-opens java.base/java.lang=ALL-UNNAMED \
--add-opens java.base/java.lang.reflect=ALL-UNNAMED \
--add-opens java.base/java.text=ALL-UNNAMED \
--add-opens java.base/java.nio=ALL-UNNAMED \
--add-opens java.base/java.math=ALL-UNNAMED \
--add-opens java.base/java.util.concurrent=ALL-UNNAMED \
--add-opens java.base/java.net=ALL-UNNAMED \
--add-opens java.base/jdk.internal.misc=ALL-UNNAMED \
--add-opens java.base/sun.nio.ch=ALL-UNNAMED \
--add-opens java.desktop/java.beans=ALL-UNNAMED \
--add-opens java.desktop/javax.swing=ALL-UNNAMED \
--add-opens java.desktop/javax.swing.border=ALL-UNNAMED \
--add-opens java.desktop/javax.swing.event=ALL-UNNAMED \
--add-opens java.desktop/sun.swing=ALL-UNNAMED \
--add-opens java.desktop/java.awt=ALL-UNNAMED \
--add-opens java.desktop/java.awt.image=ALL-UNNAMED \
--add-opens java.desktop/java.awt.color=ALL-UNNAMED \
--add-opens java.desktop/java.awt.font=ALL-UNNAMED \
--add-opens java.desktop/sun.awt.image=ALL-UNNAMED"

# --- run ---------------------------------------------------------------------
cd "$RES_DIR"
exec java -Xmx4096m \
    -Dio.netty.tryReflectionSetAccessible=true \
    -Dfile.encoding=UTF-8 \
    $ADD_OPENS \
    -jar "$JAR" "$@"
