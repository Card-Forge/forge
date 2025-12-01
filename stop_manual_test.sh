#!/bin/bash

echo "Stopping Manual Agent Test..."

# Kill by port
lsof -i :5001 | grep LISTEN | awk '{print $2}' | xargs kill -9 2>/dev/null
lsof -i :8081 | grep LISTEN | awk '{print $2}' | xargs kill -9 2>/dev/null

# Kill by name (cleanup)
pkill -f "forge-headless"
pkill -f "manual_agent.py"

echo "All test processes stopped."
