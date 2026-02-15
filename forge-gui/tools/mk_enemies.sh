#!/usr/bin/env bash
# Recombine individual enemy JSON files into a single enemies.json for release
# Usage: mk_enemies.sh [ENEMIES_DIR] [--delete]
# Default ENEMIES_DIR: forge-gui/res/adventure/common/world/enemies

set -euo pipefail

ENEMIES_DIR=${1:-forge-gui/res/adventure/common/world/enemies}
DELETE_FLAG=false
if [[ "${2:-}" == "--delete" || "${1:-}" == "--delete" ]]; then
  DELETE_FLAG=true
fi

PARENT_DIR=$(dirname "$ENEMIES_DIR")
OUTPUT_FILE="$PARENT_DIR/enemies.json"
TMP_OUTPUT="$OUTPUT_FILE.tmp"

if [[ ! -d "$ENEMIES_DIR" ]]; then
  echo "Enemies folder not found: $ENEMIES_DIR"
  exit 1
fi

# Get list of json files; fail if none
shopt -s nullglob
files=("$ENEMIES_DIR"/*.json)
if [[ ${#files[@]} -eq 0 ]]; then
  echo "No .json files found in $ENEMIES_DIR"
  exit 1
fi

# Preferred method: jq
if command -v jq >/dev/null 2>&1; then
  echo "Using jq to combine ${#files[@]} files..."
  # Sort files for deterministic output
  mapfile -t sorted < <(printf "%s\n" "${files[@]}" | sort)
  jq -s '.' "${sorted[@]}" > "$TMP_OUTPUT"
else
  # Fallback: use python to read and write JSON
  echo "jq not found; falling back to Python (requires Python 3)."
  if ! command -v python3 >/dev/null 2>&1 && ! command -v python >/dev/null 2>&1; then
    echo "Neither jq nor python3/python available. Install jq or Python 3 and retry."
    exit 1
  fi
  PY=$(command -v python3 || command -v python)
  echo "Using $PY to combine files..."
  # Python reads each file and outputs a JSON array
  "$PY" - <<PYTHON > "$TMP_OUTPUT"
import sys, json, os
files = ${files}
# files will be a bash-style list string; instead, gather from directory
folder = os.path.normpath(r"${ENEMIES_DIR}")
fnames = sorted([os.path.join(folder, f) for f in os.listdir(folder) if f.lower().endswith('.json')])
arr = []
for fn in fnames:
    with open(fn, 'r', encoding='utf-8') as fh:
        arr.append(json.load(fh))
json.dump(arr, sys.stdout, indent=2, ensure_ascii=False)
PYTHON
fi

# Validate output is non-empty
if [[ ! -s "$TMP_OUTPUT" ]]; then
  echo "Failed to produce combined JSON file"
  rm -f "$TMP_OUTPUT"
  exit 1
fi

# Backup existing combined file if exists
if [[ -f "$OUTPUT_FILE" ]]; then
  echo "Backing up existing $OUTPUT_FILE to ${OUTPUT_FILE}.bak"
  mv "$OUTPUT_FILE" "${OUTPUT_FILE}.bak"
fi

mv "$TMP_OUTPUT" "$OUTPUT_FILE"
chmod 644 "$OUTPUT_FILE"

echo "Created $OUTPUT_FILE"

if [[ "$DELETE_FLAG" == true ]]; then
  echo "Deleting individual enemy files in $ENEMIES_DIR"
  # Keep directory, remove only .json files
  rm -f "$ENEMIES_DIR"/*.json
fi

echo "Done."

