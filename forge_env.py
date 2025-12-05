import json
import os
import signal
import subprocess
import sys
import time

import requests


class ForgeEnv:
    def __init__(self, jar_path=None, port=8080, skip_start=False):
        # Determine the JAR path based on the environment
        if jar_path is None:
            # Get the directory where this file is located
            forge_dir = os.path.dirname(os.path.abspath(__file__))
            jar_path = os.path.join(
                forge_dir,
                "forge-gui-desktop/target/forge-gui-desktop-2.0.07-SNAPSHOT-jar-with-dependencies.jar",
            )
        self.jar_path = jar_path
        self.port = port
        self.server_url = f"http://localhost:{port}"
        self.server_process = None
        self.last_state = None

        # Register signal handlers for cleanup
        signal.signal(signal.SIGINT, self._handle_signal)
        signal.signal(signal.SIGTERM, self._handle_signal)

        # Check if server is already running before trying to start
        if skip_start or self._check_server_running():
            print("Forge server is already running, skipping start")
        elif not self.start_server():
            raise Exception("Failed to start Forge Server.")

    def start_server(self):
        """Starts the ForgeHeadlessServer Java process."""
        print("Starting Forge Server...")
        self.log_file = open("forge_server.log", "w")
        cmd = ["java", "-cp", self.jar_path, "forge.view.ForgeHeadlessServer"]

        # Start in background
        self.server_process = subprocess.Popen(
            cmd, stdout=self.log_file, stderr=subprocess.STDOUT, text=True
        )

        # Wait for server to be ready (increased timeout to 60 seconds)
        for i in range(60):
            try:
                response = requests.get(f"{self.server_url}/api/state", timeout=2)
                if response.status_code == 200:
                    print(f"Forge Server is ready! (took {i+1} seconds)")
                    return True
            except (requests.exceptions.ConnectionError, requests.exceptions.Timeout):
                time.sleep(1)
                if i % 10 == 0:
                    print(f"Waiting for server... ({i} seconds elapsed)")

        print("Failed to connect to Forge Server after 60 seconds.")
        return False

    def _check_server_running(self):
        """Check if the Forge server is already running."""
        try:
            response = requests.get(f"{self.server_url}/api/state", timeout=1)
            return response.status_code == 200
        except (requests.exceptions.ConnectionError, requests.exceptions.Timeout):
            return False

    def stop_server(self):
        """Stops the ForgeHeadlessServer process."""
        if self.server_process:
            print("Stopping Forge Server...")
            self.server_process.terminate()
            try:
                self.server_process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                self.server_process.kill()
            self.server_process = None
            if self.log_file:
                self.log_file.close()

    @property
    def state(self):
        return self.last_state

    def reset(self, options=None):
        """Resets the environment and returns the initial observation."""
        try:
            response = requests.post(f"{self.server_url}/api/reset", json=options or {})
            response.raise_for_status()
            self.last_state = response.json()
            return self.last_state
        except Exception as e:
            print(f"Error during reset: {e}")
            # Read the log file
            with open("forge_server.log", "r") as f:
                print("Server Logs:\n" + f.read())
            return None

    def step(self, action):
        """
        Executes an action and returns (observation, reward, done, info).
        Action can be:
        - int: index of the action in 'possible_actions'
        - str: raw action string (e.g., "attack_all", "keep")
        - dict: full payload
        """
        payload = {}
        if isinstance(action, int):
            payload = {"index": action}
        elif isinstance(action, str):
            payload = {"action": action}
        elif isinstance(action, dict):
            payload = action
        else:
            payload = {"action": str(action)}

        try:
            response = requests.post(f"{self.server_url}/api/step", json=payload)
            response.raise_for_status()
            state = response.json()
            self.last_state = state

            # Calculate reward (placeholder)
            reward = 0

            # Check done
            done = state.get("game_over", False)

            if done:
                winner = state.get("winner", "")
                if winner == "Player 1":  # Assuming Player 1 is the agent
                    reward = 1
                elif winner == "Draw":
                    reward = 0
                else:
                    reward = -1

            return state, reward, done, {}

        except Exception as e:
            print(f"Error during step: {e}")
            return None, 0, True, {"error": str(e)}

    def _handle_signal(self, signum, frame):
        print(f"\nReceived signal {signum}. Cleaning up...")
        self.stop_server()
        sys.exit(0)


if __name__ == "__main__":
    # Example Usage
    env = ForgeEnv()

    try:
        if env.start_server():
            print("Resetting environment...")
            obs = env.reset()
            print("Initial State:", json.dumps(obs, indent=2))

            # Simple Loop
            for i in range(5):
                print(f"\n--- Step {i+1} ---")
                # Just pass priority for now
                action = "pass_priority"
                obs, reward, done, info = env.step(action)
                print("New State:", json.dumps(obs, indent=2))

    finally:
        env.stop_server()
