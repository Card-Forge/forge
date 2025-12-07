#!/usr/bin/env python3
"""
Simple Agent Loop for ForgeHeadlessServer

Connects the Forge game server (port 8080) to a policy function (port 5005).
The policy receives the full game state JSON and returns an action index.
"""

import urllib.request
import urllib.error
import json
import time
import sys

# Configuration
FORGE_URL = "http://localhost:8080"
POLICY_URL = "http://localhost:5005"

def forge_request(endpoint, data=None):
    """Make a request to ForgeHeadlessServer."""
    url = f"{FORGE_URL}{endpoint}"
    req = urllib.request.Request(url)
    
    if data is not None:
        if isinstance(data, dict):
            req.data = json.dumps(data).encode('utf-8')
            req.add_header('Content-Type', 'application/json')
        else:
            req.data = data.encode('utf-8')
    
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return json.load(resp)
    except urllib.error.URLError as e:
        print(f"Forge request failed: {e}")
        return None

def policy_request(game_state):
    """Send game state to policy and get action index."""
    
    # Parse hand - handle both string and object formats
    raw_hand = game_state.get("hand", [])
    hand = []
    for card in raw_hand:
        if isinstance(card, str):
            hand.append(card)
        elif isinstance(card, dict):
            hand.append(card.get("name", str(card)))
        else:
            hand.append(str(card))
    
    # Transform to cleaner format for LLM consumption
    structured_payload = {
        "gameState": {
            "game_id": game_state.get("game_id", ""),
            "turn": game_state.get("turn", 0),
            "phase": game_state.get("phase", "UNKNOWN"),
            "game_over": game_state.get("game_over", False),
            "hand": hand,
            "library_count": game_state.get("library_count", 0),
            "battlefield": game_state.get("battlefield", {}),
            "player1": game_state.get("player1", {}),
            "player2": game_state.get("player2", {}),
            "combat": game_state.get("combat", {})
        },
        "actionState": {
            "possible_actions": [
                {
                    "index": i,
                    "type": action.get("type", "unknown"),
                    "card": action.get("card_name", ""),
                    "mana_cost": action.get("mana_cost", ""),
                    "requires_targets": action.get("requires_targets", False)
                }
                for i, action in enumerate(game_state.get("possible_actions", {}).get("actions", []))
            ]
        }
    }
    
    req = urllib.request.Request(POLICY_URL)
    payload = json.dumps(structured_payload).encode('utf-8')
    req.data = payload
    req.add_header('Content-Type', 'application/json')
    
    # Debug: show what we're sending
    print(f"  [DEBUG] Sending to policy: {len(payload)} bytes")
    print(f"  [DEBUG] Actions available: {len(structured_payload['actionState']['possible_actions'])}")
    
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            result = json.load(resp)
            print(f"  [DEBUG] Policy response: {result}")
            # Handle various response formats
            if isinstance(result, dict):
                return result.get('action_index', result.get('index', 0))
            elif isinstance(result, int):
                return result
            else:
                return int(result)
    except urllib.error.URLError as e:
        print(f"Policy request failed: {e}")
        return 0  # Default to first action
    except (ValueError, TypeError) as e:
        print(f"Failed to parse policy response: {e}")
        return 0

def run_game(scenario=None):
    """Run a single game using the policy."""
    print("=" * 60)
    print("Starting new game...")
    
    # Reset/start game
    reset_options = scenario if scenario else {}
    state = forge_request("/api/reset", reset_options)
    
    if not state:
        print("Failed to start game!")
        return None
    
    turn = 0
    step = 0
    max_steps = 500  # Safety limit
    game_log = []  # Collect game log entries
    
    while not state.get("game_over") and step < max_steps:
        step += 1
        current_turn = state.get("turn", 0)
        phase = state.get("phase", "?")
        
        if current_turn != turn:
            turn = current_turn
            print(f"\n--- Turn {turn} ---")
            game_log.append(f"\n=== Turn {turn} ===")
        
        actions = state.get("possible_actions", {}).get("actions", [])
        action_count = len(actions)
        
        if action_count == 0:
            print("No actions available!")
            game_log.append("No actions available!")
            break
        
        # Get action from policy
        action_index = policy_request(state)
        
        # Validate action index
        if action_index < 0 or action_index >= action_count:
            print(f"Invalid action index {action_index}, using 0")
            action_index = 0
        
        # Log the action
        chosen_action = actions[action_index]
        action_type = chosen_action.get("type", "unknown")
        card_name = chosen_action.get("card_name", "")
        
        if action_type == "pass_priority":
            log_entry = f"[{phase}] Pass priority"
        else:
            log_entry = f"[{phase}] {action_type}: {card_name}"
        
        print(f"  {log_entry}")
        game_log.append(log_entry)
        
        # Execute the action
        state = forge_request("/api/step", f"play_action {action_index}")
        
        if not state:
            print("Failed to get state after action!")
            game_log.append("ERROR: Failed to get state after action!")
            break
    
    if step >= max_steps:
        print(f"\nGame stopped after {max_steps} steps (safety limit)")
    
    # Game over
    if state and state.get("game_over"):
        print("\n" + "=" * 60)
        print("GAME OVER")
        # You can add more end-game stats here
    
    # Save game log
    if game_log:
        log_file = f"game_log_{int(time.time())}.txt"
        with open(log_file, 'w') as f:
            f.write("=" * 60 + "\n")
            f.write("FORGE HEADLESS GAME LOG\n")
            f.write("=" * 60 + "\n\n")
            for entry in game_log:
                f.write(entry + "\n")
        print(f"\nGame log saved to: {log_file}")
    
    return state

def main():
    # Check if puzzle file provided as argument
    scenario = None
    if len(sys.argv) > 1:
        puzzle_path = sys.argv[1]
        print(f"Loading puzzle: {puzzle_path}")
        scenario = {"puzzle_file": puzzle_path}
    
    # Run the game
    final_state = run_game(scenario)
    
    if final_state:
        print("\nFinal state summary:")
        print(f"  Turn: {final_state.get('turn')}")
        print(f"  Game Over: {final_state.get('game_over')}")

if __name__ == "__main__":
    main()
