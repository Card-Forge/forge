#!/bin/bash
# Launch Forge GUI to play against an ONNX-trained agent.
#
# Usage: ./play_vs_onnx.sh <onnx_model.onnx> [your_deck.dck] [ai_deck.dck]
#
# Defaults:
#   your_deck = caw_gates.dck
#   ai_deck   = mono_red_pingers.dck (the ONNX agent's deck)
#
# Prerequisites:
#   mvn compile -pl forge-research -am
#   mvn dependency:build-classpath -pl forge-research -Dmdep.outputFile=target/classpath.txt

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
FORGE_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$SCRIPT_DIR"

ONNX_MODEL="${1:?Usage: $0 <onnx_model.onnx> [your_deck.dck] [ai_deck.dck]}"
YOUR_DECK="${2:-src/main/resources/decks/caw_gates.dck}"
AI_DECK="${3:-src/main/resources/decks/mono_red_pingers.dck}"

# Build classpath from target/classpath.txt (same approach as train_ppo.py)
CP_FILE="target/classpath.txt"
if [ ! -f "$CP_FILE" ]; then
    echo "Generating classpath file..."
    (cd "$FORGE_ROOT" && mvn dependency:build-classpath -pl forge-research -Dmdep.outputFile=target/classpath.txt -q)
fi

DEPS_CP=$(cat "$CP_FILE")
CLASSPATH="$SCRIPT_DIR/target/classes"
for mod in forge-core forge-game forge-ai forge-gui forge-gui-desktop; do
    CLASSES_DIR="$FORGE_ROOT/$mod/target/classes"
    [ -d "$CLASSES_DIR" ] && CLASSPATH="$CLASSPATH:$CLASSES_DIR"
done
CLASSPATH="$CLASSPATH:$DEPS_CP"

echo "Starting Forge GUI: You ($YOUR_DECK) vs ONNX Agent ($AI_DECK)"
echo "Model: $ONNX_MODEL"

# Run from forge-gui-desktop so Forge finds assets at ../forge-gui/
cd "$FORGE_ROOT/forge-gui-desktop"
java -cp "$CLASSPATH" \
    -Xmx2g \
    forge.research.PlayVsOnnx "$ONNX_MODEL" "$FORGE_ROOT/forge-research/$YOUR_DECK" "$FORGE_ROOT/forge-research/$AI_DECK"
