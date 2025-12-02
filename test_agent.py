import json
import random
import time
from forge_env import ForgeEnv

def main():
    print("Initializing Forge Environment...")
    env = ForgeEnv()
    
    try:
        if not env.start_server():
            print("Failed to start server.")
            return

        print("\n--- Resetting Game ---")
        obs = env.reset()
        if not obs:
            print("Failed to reset game.")
            return
            
        print(f"Initial State: Turn {obs.get('turn')}, Phase {obs.get('phase')}")
        
        # Run for 20 steps or until done
        for i in range(20):
            print(f"\n--- Step {i+1} ---")
            
            possible_actions = obs.get("possible_actions", {}).get("actions", [])
            print(f"Possible Actions: {len(possible_actions)}")
            
            # Simple policy: 
            # 1. If we can play a land, do it.
            # 2. If we can cast a spell, do it.
            # 3. Otherwise, pass priority.
            
            action_to_take = None
            
            # Look for non-pass actions
            for idx, action in enumerate(possible_actions):
                if action.get("type") != "pass_priority":
                    # Prefer playing lands or casting spells
                    action_to_take = idx
                    print(f"Decided to take action: {action}")
                    break
            
            if action_to_take is None:
                print("Decided to pass priority.")
                action_to_take = "pass_priority"
            
            # Execute step
            obs, reward, done, info = env.step(action_to_take)
            
            if not obs:
                print("Error getting observation.")
                break
                
            print(f"New State: Turn {obs.get('turn')}, Phase {obs.get('phase')}")
            
            if done:
                print("Game Over!")
                winner = obs.get("winner", "Unknown")
                print(f"Winner: {winner}")
                print(f"Reward: {reward}")
                break
                
    except KeyboardInterrupt:
        print("\nTest interrupted by user.")
    finally:
        print("\nStopping server...")
        env.stop_server()

if __name__ == "__main__":
    main()
