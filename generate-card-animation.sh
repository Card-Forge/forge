#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPT_PATH="$DIR/scripts/generate_card_animation.py"

if [ -z "$1" ]; then
    echo "===================================================================="
    echo "  Forge MTG Animated Card Generator"
    echo "===================================================================="
    echo ""
    echo "Usage:"
    echo "  ./generate-card-animation.sh <SET> \"<CARD_NAME>\" \"<VIDEO_PATH>\" [<CARD_NUMBER>]"
    echo "  ./generate-card-animation.sh <SET> \"<CARD_NAME>\" <CARD_NUMBER> \"<VIDEO_PATH>\""
    echo ""
    echo "Examples:"
    echo "  ./generate-card-animation.sh AFR \"Improvised Weaponry\" \"clip.mp4\""
    echo "  ./generate-card-animation.sh AFR \"Acererak the Archlich\" \"clip.mp4\" 372"
    echo "  ./generate-card-animation.sh AFR \"Acererak the Archlich\" 372 \"clip.mp4\""
    echo "  ./generate-card-animation.sh FDN \"Burst Lightning\" \"burst.mp4\""
    echo ""
    echo "Options:"
    echo "  --number 372     Card / collector number (for cards with multiple arts)"
    echo "  --fps 24         Frame rate [default: 24]"
    echo "  --quality 88     JPEG quality 1-100 [default: 88]"
    echo "  --x 35 --y 70    Art window position [default: x=35, y=70, w=420, h=314]"
    echo ""
    echo "===================================================================="
    exit 1
fi

PYTHON_CMD="python3"
if ! command -v python3 >/dev/null 2>&1; then
    if command -v python >/dev/null 2>&1; then
        PYTHON_CMD="python"
    else
        echo "[ERROR] Python 3 was not found on PATH. Please install Python 3." >&2
        exit 1
    fi
fi

exec "$PYTHON_CMD" "$SCRIPT_PATH" "$@"
