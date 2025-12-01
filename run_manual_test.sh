#!/bin/bash

# Kill existing processes
echo "Stopping existing processes..."
lsof -i :5001 | grep LISTEN | awk '{print $2}' | xargs kill -9 2>/dev/null
lsof -i :8081 | grep LISTEN | awk '{print $2}' | xargs kill -9 2>/dev/null
pkill -f "forge-headless"

# Trap Ctrl+C to kill background processes
trap "kill 0" EXIT

echo "Starting Manual Agent on port 5001..."
# Run agent in background but let it print to stdout/stderr
python3 manual_agent.py &

# Wait for Agent to start
sleep 2

echo "Starting Forge Headless..."
echo "Open http://localhost:5001 in your browser to control the game."
echo "Press Ctrl+C to stop everything."
echo "=================================================="

# Run Forge in foreground
./forge-headless --ai-endpoint http://localhost:5001/decide --game-id manual-test-user
