#!/bin/sh
cd "$(dirname "$0")"

# Native Wayland scaling requires JetBrains Runtime; a stock JDK ignores
# -Dawt.toolkit.name=auto and falls back to XWayland.
if [ -n "$JBR_JAVA" ] && [ -x "$JBR_JAVA" ]; then
    JAVA="$JBR_JAVA"
elif [ -n "$JBR_HOME" ] && [ -x "$JBR_HOME/bin/java" ]; then
    JAVA="$JBR_HOME/bin/java"
else
    for candidate in /opt/*/jbr/bin/java "$HOME"/.local/share/JetBrains/Toolbox/apps/*/*/jbr/bin/java; do
        if [ -x "$candidate" ]; then
            JAVA="$candidate"
            break
        fi
    done
fi

if [ -z "$JAVA" ]; then
    echo "forge-wayland: JetBrains Runtime not found; set JBR_JAVA=/path/to/jbr/bin/java" >&2
    exit 2
fi

exec "$JAVA" $mandatory.java.args$ -Dawt.toolkit.name=auto -jar $project.build.finalName$ "$@"
