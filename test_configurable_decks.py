import json
from forge_env import ForgeEnv
import time

def test_configurable_decks():
    print("Initializing ForgeEnv...")
    env = ForgeEnv()
    
    try:
        print("Testing reset() with default random decks...")
        obs = env.reset()
        obs, reward, done, info = env.step("pass_priority")
        print(obs)
        obs, reward, done, info = env.step("pass_priority")
        print(obs)
        if obs:
            print("Reset successful!")
            print("Initial State Keys:", obs.keys())
            if "hand" in obs:
                print(f"Hand ({len(obs['hand'])} cards): {obs['hand']}")
            if "library_count" in obs:
                print(f"Library Count: {obs['library_count']}")

            if "possible_actions" in obs:
                print(f"Number of possible actions: {len(obs['possible_actions'])}")
                # Check if we have cards in hand (play_land or cast spell actions)
                has_cards = any("play_land" in a or "cast" in a for a in obs["possible_actions"])
                if has_cards:
                    print("Success: Player has cards to play (Deck generation worked).")
                else:
                    print("Warning: No playable cards found immediately. Might be mana screwed or empty hand.")
            else:
                print("Error: 'possible_actions' not found in observation.")
        else:
            print("Error: Reset failed (returned None).")

        # Test step
        print("\nTesting step('pass_priority')...")
        obs, reward, done, info = env.step("pass_priority")
        if obs:
            print("Step successful!")
            print("New Phase:", obs.get("phase"))
        else:
            print("Error: Step failed.")

    except Exception as e:
        print(f"Test failed with exception: {e}")
    finally:
        print("Stopping server...")
        env.stop_server()

if __name__ == "__main__":
    test_configurable_decks()
