#!/bin/bash
# Launch a hotseat game (two human players) using the Forge desktop GUI.
# Usage: ./play_hotseat.sh [deck_a.dck] [deck_b.dck]
#
# Default decks: mono_red_pingers vs caw_gates
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FORGE_ROOT="$(dirname "$SCRIPT_DIR")"

# Resolve deck paths relative to forge-research before cd'ing to forge-gui-desktop
DECK_A="${1:-$SCRIPT_DIR/src/main/resources/decks/mono_red_pingers.dck}"
DECK_B="${2:-$SCRIPT_DIR/src/main/resources/decks/caw_gates.dck}"
DECK_A="$(cd "$(dirname "$DECK_A")" && pwd)/$(basename "$DECK_A")"
DECK_B="$(cd "$(dirname "$DECK_B")" && pwd)/$(basename "$DECK_B")"

# Build classpath
CP_FILE="$SCRIPT_DIR/target/classpath.txt"
if [ ! -f "$CP_FILE" ]; then
    echo "Classpath file not found. Run: mvn dependency:build-classpath -Dmdep.outputFile=target/classpath.txt"
    exit 1
fi

DEPS_CP=$(cat "$CP_FILE")
CP="$SCRIPT_DIR/target/classes"
for module in forge-core forge-game forge-ai forge-gui forge-gui-desktop; do
    DIR="$FORGE_ROOT/$module/target/classes"
    if [ -d "$DIR" ]; then
        CP="$CP:$DIR"
    fi
done
CP="$CP:$DEPS_CP"

# Run from forge-gui-desktop so Forge finds assets at ../forge-gui/
cd "$FORGE_ROOT/forge-gui-desktop"

java -cp "$CP" forge.research.PlayHotseat "$DECK_A" "$DECK_B"
