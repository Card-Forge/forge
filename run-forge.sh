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

# --- LLM sidecar -------------------------------------------------------------
# Point Forge at a deck-recognition sidecar. Defaults to a local sidecar; set
# FORGE_SIDECAR_URL to a remote one, e.g. a home server reachable over Tailscale:
#   FORGE_SIDECAR_URL=http://home-server.tailnet-name.ts.net:18970 ./run-forge.sh
SIDECAR_URL="${FORGE_SIDECAR_URL:-http://localhost:18970}"
# The ON/OFF toggle is the "Enable AI Deck Recognition" checkbox in Forge's
# Settings > Preferences screen — a -Dforge.ai.deckRecognition flag does NOT
# work, because GamePlayerUtil overwrites that system property at game start
# from the UI preference. This only sets the sidecar URL, which is not bridged.
SIDECAR_PROPS="-Dforge.ai.deckRecognition.url=$SIDECAR_URL"

# Optional verbose logging for troubleshooting (e.g. why deck recognition
# does not attach):  FORGE_LOG_LEVEL=debug ./run-forge.sh
# Must override the per-writer level — Forge's tinylog.properties pins
# writerdefault.level=info, which a plain -Dtinylog.level cannot lift.
if [ -n "$FORGE_LOG_LEVEL" ]; then
    SIDECAR_PROPS="$SIDECAR_PROPS -Dtinylog.writerdefault.level=$FORGE_LOG_LEVEL"
fi

# --- run ---------------------------------------------------------------------
cd "$RES_DIR"
exec java -Xmx4096m \
    -Dio.netty.tryReflectionSetAccessible=true \
    -Dfile.encoding=UTF-8 \
    $SIDECAR_PROPS \
    $ADD_OPENS \
    -jar "$JAR" "$@"
