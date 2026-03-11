#!/bin/bash
# Convenience script to manually start Forge RL gRPC servers.
# Usage: ./start_servers.sh [num_servers] [base_port]
# train_ppo.py handles server lifecycle automatically; this is for debugging.

set -e

NUM_SERVERS=${1:-2}
BASE_PORT=${2:-50051}
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SERVER_DIR="$SCRIPT_DIR/../forge-research"
CLASSPATH_FILE="$SERVER_DIR/target/classpath.txt"

if [ ! -f "$CLASSPATH_FILE" ]; then
    echo "ERROR: $CLASSPATH_FILE not found."
    echo "Run: cd $SERVER_DIR && mvn dependency:build-classpath -Dmdep.outputFile=target/classpath.txt"
    exit 1
fi

DEPS_CP=$(cat "$CLASSPATH_FILE")
CP="$SERVER_DIR/target/classes:$DEPS_CP"

echo "Starting $NUM_SERVERS server(s)..."
PIDS=()
for i in $(seq 0 $((NUM_SERVERS - 1))); do
    PORT=$((BASE_PORT + i))
    java -cp "$CP" forge.research.ForgeResearchServer "$PORT" &
    PIDS+=($!)
    echo "  Server PID=$! on port $PORT"
done

trap 'echo "Stopping servers..."; kill "${PIDS[@]}" 2>/dev/null; wait' EXIT INT TERM
echo "All servers started. Press Ctrl+C to stop."
wait
