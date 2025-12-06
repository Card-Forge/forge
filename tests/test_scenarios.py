import os
import json
import time
import subprocess
import urllib.request
import urllib.error
import unittest
import glob

SERVER_URL = "http://localhost:8080"
SCENARIO_DIR = os.path.join(os.path.dirname(__file__), "scenarios")

def make_request(endpoint, data=None):
    url = f"{SERVER_URL}{endpoint}"
    req = urllib.request.Request(url)
    req.add_header('Content-Type', 'application/json')
    
    if data:
        json_data = json.dumps(data).encode('utf-8')
        req.data = json_data
    
    retries = 5
    for i in range(retries):
        try:
            with urllib.request.urlopen(req, timeout=5) as response:
                return response.status, json.load(response)
        except urllib.error.URLError as e:
            if i == retries - 1:
                print(f"Request failed after {retries} retries: {e}")
                if hasattr(e, 'code'):
                    return e.code, None
                return None, None
            time.sleep(1)
        except Exception as e:
             print(f"Error during request: {e}")
             if i == retries - 1: return None, None
             time.sleep(1)
    return None, None

class TestScenarios(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # Check if server is running, if not start it
        status, _ = make_request("/api/state")
        if status != 200:
            print("Server not running, starting it...")
            cls.server_process = subprocess.Popen(
                ["/usr/bin/java", "-jar", "forge-gui-desktop/target/forge-gui-desktop-2.0.08-SNAPSHOT-jar-with-dependencies.jar"],
                cwd=os.path.dirname(os.path.dirname(__file__)),
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE
            )

            
            # Wait loop to check for process exit
            for _ in range(45):
                if cls.server_process.poll() is not None:
                    print("Server process exited prematurely!")
                    stdout, stderr = cls.server_process.communicate()
                    print("STDOUT:", stdout.decode('utf-8'))
                    print("STDERR:", stderr.decode('utf-8'))
                    raise RuntimeError("Server failed to start")
                time.sleep(1)
                # Try a quick ping
                status, _ = make_request("/api/state")
                if status == 200:
                    print("Server started successfully.")
                    return
            
            print("Server timed out starting up.")
            cls.server_process.terminate()
            stdout, stderr = cls.server_process.communicate()
            print("STDOUT:", stdout.decode('utf-8'))
            print("STDERR:", stderr.decode('utf-8'))
            raise RuntimeError("Server timed out")
            
    def test_run_scenarios(self):
        scenario_files = glob.glob(os.path.join(SCENARIO_DIR, "*.json"))
        print(f"Found {len(scenario_files)} scenarios")
        
        for scenario_file in scenario_files:
            with self.subTest(scenario=os.path.basename(scenario_file)):
                print(f"Running scenario: {scenario_file}")
                with open(scenario_file, 'r') as f:
                    scenario = json.load(f)
                
                # Reset game with scenario
                status, state = make_request("/api/reset", scenario)
                self.assertEqual(status, 200, f"Reset failed for {scenario_file}")
                
                # Verify Forced Hand
                if "forced_hand" in scenario:
                    current_hand = [c['name'] for c in state['hand']]
                    # Check if all forced cards are in hand (allowing for duplicates)
                    for card_name in scenario['forced_hand']:
                        self.assertIn(card_name, current_hand, f"Expected {card_name} in hand, got {current_hand}")
                        
                # Verify Expected Action
                if "expected_action" in scenario:
                    possible_actions = state['possible_actions']
                    found = False
                    for action in possible_actions:
                        # Assuming structure of action string e.g. "play_land 0"
                        if scenario['expected_action'] in str(action):
                            found = True
                            break
                    
                    self.assertTrue(found, f"Expected action {scenario['expected_action']} not found in {possible_actions}")

if __name__ == '__main__':
    unittest.main()
