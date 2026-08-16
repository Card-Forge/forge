#!/bin/bash
set -euo pipefail

# Get the directory where this script is located
script_dir=$(cd "$(dirname "$0")" && pwd)
caller_dir=$(pwd -P)

# Convert any relative deck paths to absolute paths
# before changing to the directory that contains the assembled JAR.
args=()
for arg in "$@"; do
    # Check if this looks like a deck file path (ends in .dck and doesn't start with -)
    if [[ "$arg" == *.dck ]] && [[ "$arg" != -* ]]; then
        # If it is a relative path, preserve the caller's interpretation of it.
        if [[ "$arg" != /* ]]; then
            arg="$caller_dir/$arg"
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

cd "$target_dir" && exec java -jar "$(basename "$jar_file")" "${args[@]}"
