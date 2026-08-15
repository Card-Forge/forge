#!/bin/bash
set -euo pipefail

# Get the directory where this script is located
script_dir=$(cd "$(dirname "$0")" && pwd)

# Convert any relative deck paths to absolute paths
# Relative paths are resolved relative to the script's location, not the caller's pwd
args=()
for arg in "$@"; do
    # Check if this looks like a deck file path (ends in .dck and doesn't start with -)
    if [[ "$arg" == *.dck ]] && [[ "$arg" != -* ]]; then
        # If it's a relative path (doesn't start with /), resolve relative to script directory
        if [[ "$arg" != /* ]]; then
            arg="$script_dir/$arg"
        fi
    fi
    args+=("$arg")
done

# Find the JAR file - use the most recently modified if multiple exist
target_dir="$script_dir/forge-headless/target"
jar_file=$(ls -t "$target_dir"/forge-headless-*-SNAPSHOT-jar-with-dependencies.jar 2>/dev/null | head -1)

if [[ -z "$jar_file" ]]; then
    echo "Error: No forge-headless JAR found in $target_dir" >&2
    echo "Run 'make build' in forge-java/forge-headless first" >&2
    exit 1
fi

cd "$target_dir" && time java -jar "$(basename "$jar_file")" "${args[@]}"
