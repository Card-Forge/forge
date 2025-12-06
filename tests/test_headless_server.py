import urllib.request
import urllib.error
import subprocess
import time
import json
import sys
import os

# Configuration
SERVER_JAR = "forge-gui-desktop/target/forge-gui-desktop-2.0.08-SNAPSHOT-jar-with-dependencies.jar"
MAIN_CLASS = "forge.view.ForgeHeadlessServer"
PORT = 8080
BASE_URL = f"http://localhost:{PORT}"
SERVER_LOG = "headless_server.log"

def start_server():
    print(f"Starting server with {SERVER_JAR}...")
    cmd = [
        "java",
        "-cp", SERVER_JAR,
        MAIN_CLASS
    ]
    
    # Open log file
    log_file = open(SERVER_LOG, "w")
    
    # Start process
    process = subprocess.Popen(
        cmd,
        stdout=log_file,
        stderr=subprocess.STDOUT,
        cwd=os.getcwd()
    )
    return process, log_file

def make_request(method, endpoint, data=None):
    url = f"{BASE_URL}{endpoint}"
    req = urllib.request.Request(url, method=method)
    req.add_header('Content-Type', 'application/json')
    
    if data is not None:
        if isinstance(data, dict):
            json_data = json.dumps(data).encode('utf-8')
        else:
            json_data = data.encode('utf-8')
        req.data = json_data
        
    try:
        with urllib.request.urlopen(req, timeout=20) as response:
            if response.status >= 200 and response.status < 300:
                response_body = response.read().decode('utf-8')
                return json.loads(response_body)
            else:
                raise Exception(f"HTTP Error: {response.status}")
    except urllib.error.URLError as e:
        raise e

def wait_for_server():
    print("Waiting for server to become ready...")
    for i in range(30):
        try:
            # Check /api/state using urllib
            with urllib.request.urlopen(f"{BASE_URL}/api/state", timeout=1) as response:
                if response.status == 200:
                    print("Server is up!")
                    return True
        except Exception:
            time.sleep(1)
            sys.stdout.write(".")
            sys.stdout.flush()
    print("\nServer failed to start.")
    return False

def test_game_flow():
    # 1. Reset/Start Game
    print("\n=== STEP 1: Initializing Game (POST /api/reset) ===")
    try:
        state = make_request("POST", "/api/reset", {})
        print("Game Initialized Successfully!")
        print_state_summary(state)
        
        if not state.get("possible_actions"):
            print("FAIL: No possible actions returned in initial state.")
            return False
    except Exception as e:
        print(f"FAIL: Error during reset: {e}")
        return False

    # 2. Action Loop (Play first few turns)
    turn_limit = 3
    for i in range(turn_limit * 5): # rough estimate of steps
        print(f"\n=== STEP {i+2}: Processing Turn ===")
        
        # Determine check if we need to stop
        if state.get("game_over"):
            print("Game Over reached!")
            break
            
        actions_obj = state.get("possible_actions", {})
        actions = actions_obj.get("actions", [])
        
        if not actions:
            print("FAIL: No actions available but game not over.")
            return False
            
        # Decision Logic:
        # 1. Play Land if possible
        # 2. Pass Priority otherwise
        
        chosen_action_index = -1
        chosen_action_desc = "pass_priority"
        
        for idx, action in enumerate(actions):
            if action.get("type") == "play_land":
                chosen_action_index = idx
                chosen_action_desc = f"play_land ({action.get('card_name')})"
                break
        
        if chosen_action_index == -1:
            # Find pass priority
            for idx, action in enumerate(actions):
                if action.get("type") == "pass_priority":
                    chosen_action_index = idx
                    break
        
        # Send Action
        print(f"Selecting Action [{chosen_action_index}]: {chosen_action_desc}")
        
        try:
            # Send raw string for action as per previous logic analysis
            payload = f"play_action {chosen_action_index}"
            state = make_request("POST", "/api/step", payload)
            print_state_summary(state)
            
        except Exception as e:
            print(f"FAIL: Error sending action: {e}")
            return False
            
    return True

def print_state_summary(state):
    turn = state.get("turn")
    phase = state.get("phase")
    p_acts = state.get("possible_actions", {}).get("count", 0)
    print(f"State: Turn {turn} | Phase {phase} | Possible Actions: {p_acts}")

def main():
    server_proc, log_file = start_server()
    success = False
    try:
        if wait_for_server():
            success = test_game_flow()
    finally:
        print("\nStopping server...")
        server_proc.terminate()
        server_proc.wait()
        log_file.close()
        
        # Dump partial log if fail
        if not success:
            print("\n!!! TEST FAILED - Server Log Tail !!!")
            os.system(f"tail -n 50 {SERVER_LOG}")
        else:
            print("\n>>> TEST PASSED <<<")

if __name__ == "__main__":
    main()
