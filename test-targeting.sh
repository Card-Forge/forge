#!/bin/bash
# Test script for manual targeting functionality
# This script demonstrates the interactive targeting workflow

echo "=== ForgeHeadless Manual Targeting Test ==="
echo ""
echo "This test will:"
echo "1. Start a new game"
echo "2. Advance to Main Phase 1"
echo "3. Attempt to cast a spell requiring targets"
echo "4. Verify the targeting prompt appears"
echo ""
echo "Press Ctrl+C at any time to exit"
echo ""
read -p "Press Enter to start..."

# Start the headless game and send commands
./forge-headless --verbose <<EOF
pp
gs
pa
play 0
1
gs
concede
EOF

echo ""
echo "=== Test Complete ==="
echo ""
echo "Expected behavior:"
echo "- Game advances to Main Phase 1"
echo "- 'play 0' attempts to cast the first action"
echo "- Targeting prompt appears with valid targets"
echo "- Entering '1' selects the second target (AI Player)"
echo "- Game state is displayed after targeting"
echo ""
echo "If you saw the targeting prompt, the test PASSED ✓"
