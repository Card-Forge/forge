import requests
import subprocess
import time
import json
import os
import signal
import sys

class ForgeEnv:
    def __init__(self, jar_path="forge-gui-desktop/target/forge-gui-desktop-2.0.07-SNAPSHOT-jar-with-dependencies.jar", port=8080):
        self.jar_path = jar_path
        self.port = port
        self.server_url = f"http://localhost:{port}"
        self.server_process = None
        self.last_state = None
        
        # Register signal handlers for cleanup
        signal.signal(signal.SIGINT, self._handle_signal)
        signal.signal(signal.SIGTERM, self._handle_signal)
        if not self.start_server():
            raise Exception("Failed to start Forge Server.")

    def start_server(self):
        """Starts the ForgeHeadlessServer Java process."""
        print("Starting Forge Server...")
        self.log_file = open("forge_server.log", "w")
        cmd = [
            "java",
            "-cp", self.jar_path,
            "forge.view.ForgeHeadlessServer"
        ]
        
        # Start in background
        self.server_process = subprocess.Popen(
            cmd,
            stdout=self.log_file,
            stderr=subprocess.STDOUT,
            text=True
        )
        
        # Wait for server to be ready
        for i in range(30):
            try:
                requests.get(f"{self.server_url}/api/state")
                print("Forge Server is ready!")
                return True
            except requests.exceptions.ConnectionError:
                time.sleep(1)
                if i % 5 == 0:
                    print("Waiting for server...")
                
        print("Failed to connect to Forge Server.")
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
                if winner == "Player 1": # Assuming Player 1 is the agent
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
