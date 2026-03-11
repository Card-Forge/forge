"""Integration test for ForgeRlEnv.

Requires the Forge Research Server to be running:
    cd forge-research && mvn exec:java -Dexec.mainClass=forge.research.ForgeResearchServer
"""

import os

import numpy as np
import pytest


# Skip if server is not running
def server_available():
    try:
        import grpc
        channel = grpc.insecure_channel("localhost:50051")
        grpc.channel_ready_future(channel).result(timeout=2)
        channel.close()
        return True
    except Exception:
        return False


@pytest.mark.skipif(not server_available(), reason="Forge server not running")
class TestForgeRlEnv:

    def _make_env(self):
        from forge_rl import ForgeRlEnv
        # Use absolute paths from env var or default
        base = os.environ.get(
            "FORGE_DECK_DIR",
            os.path.join(os.path.dirname(__file__), "..", "..", "forge-research", "src", "main", "resources", "decks"),
        )
        return ForgeRlEnv(
            deck_path_a=os.path.join(base, "ramunap_red.dck"),
            deck_path_b=os.path.join(base, "bg_constrictor.dck"),
        )

    def test_reset_returns_valid_obs(self):
        env = self._make_env()
        obs, info = env.reset()
        assert "game_info" in obs
        assert "action_mask" in obs
        assert obs["game_info"].shape == (4,)
        assert obs["action_mask"].shape == (256,)
        assert np.any(obs["action_mask"] == 1), "Should have at least one legal action"
        env.close()

    def test_random_policy_completes_game(self):
        env = self._make_env()
        obs, info = env.reset()

        steps = 0
        terminated = False
        while not terminated and steps < 5000:
            # Pick a random legal action
            mask = obs["action_mask"]
            legal = np.where(mask == 1)[0]
            action = int(np.random.choice(legal)) if len(legal) > 0 else 0
            obs, reward, terminated, truncated, info = env.step(action)
            steps += 1

        assert terminated, f"Game should terminate within 5000 steps, got {steps}"
        assert reward in (-1.0, 0.0, 1.0), f"Unexpected reward: {reward}"
        print(f"Game finished in {steps} steps, reward={reward}")
        env.close()
