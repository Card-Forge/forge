#!/bin/bash

# Test script for ForgeHeadless play_action command
# This script will send commands to the headless app and verify responses

JAVA_HOME=/opt/homebrew/Cellar/openjdk/25.0.1/libexec/openjdk.jdk/Contents/Home
JAR_PATH=/Users/travisbarton/PycharmProjects/forge/forge-gui-desktop/target/forge-gui-desktop-2.0.07-SNAPSHOT-jar-with-dependencies.jar

echo "=== Starting ForgeHeadless End-to-End Test ==="
echo ""

# Start the headless app and feed it commands
{
    # Wait for game to fully initialize
    sleep 5

    # Get initial state to see whose turn it is
    echo "=== Initial State ===" >&2
    echo "get_state"
    sleep 2

    # Pass through upkeep/draw to get to MAIN phase
    echo "=== Passing to MAIN1 ===" >&2
    echo "pass_priority"
    sleep 2

    # Check if we're in MAIN1 now
    echo "=== State after passing ===" >&2
    echo "get_state"
    sleep 2

    # Get possible actions - if it's Player 1's turn, should have land to play
    echo "=== Possible Actions ===" >&2
    echo "possible_actions"
    sleep 2

    # Try to play action 0 (should be a land if it's Player 1's turn)
    echo "=== Attempting play_action 0 ===" >&2
    echo "play_action 0"
    sleep 2

    # Check state after playing
    echo "=== State after play_action ===" >&2
    echo "get_state"
    sleep 2

    # If that didn't work (wrong player's turn), pass priority many times to get to Player 1's turn
    echo "=== Passing priority multiple times to get to Player 1 turn ===" >&2
    for i in {1..10}; do
        echo "pass_priority"
        sleep 1
    done

    # Now check state - should be Player 1's turn eventually
    echo "=== State after cycling turns ===" >&2
    echo "get_state"
    sleep 2

    # Get possible actions on Player 1's turn
    echo "=== Possible Actions (Player 1 turn) ===" >&2
    echo "possible_actions"
    sleep 2

    # Play a land
    echo "=== Playing land (play_action 0) ===" >&2
    echo "play_action 0"
    sleep 2

    # Verify the land was played
    echo "=== Final State (land should be on battlefield) ===" >&2
    echo "get_state"
    sleep 2

    # Exit
    echo "concede"

} | $JAVA_HOME/bin/java -Xmx4096m -cp $JAR_PATH forge.view.ForgeHeadless 2>&1

echo ""
echo "=== Test Complete ==="
