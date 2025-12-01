# ForgeHeadless HTTP Interface Walkthrough

I have implemented a new HTTP interface for the ForgeHeadless game engine. This allows external agents to interact with the game via HTTP requests instead of standard I/O.

## Changes
- **New Class**: `forge.view.ForgeHeadlessHttp`
    - Implements a simple HTTP server using `com.sun.net.httpserver`.
    - Listens on port 8080.
    - Endpoints:
        - `GET /`: Health check.
        - `GET /state`: Returns the current game state as JSON.
        - `POST /action`: Accepts a command string (e.g., "play 0", "pass") in the body.
        - `POST /reset`: Resets the game state and starts a new match immediately.
- **New Script**: `forge-headless-http`
    - Bash script to launch the HTTP headless server.
    - Supports the same arguments as `forge-headless` (e.g., `--p1-ai`, `--p2-human`, `--verbose`).

## How to Run
1.  **Build**: Ensure the project is built.
    ```bash
    mvn clean install -DskipTests
    ```
2.  **Start Server**:
    ```bash
    ./forge-headless-http --p1-ai --p2-human
    ```
    To specify a custom port (default is 8080):
    ```bash
    ./forge-headless-http --p1-ai --p2-human --port 9090
    ```

## API Usage

### Get Game State
```bash
curl http://localhost:8080/state
```
**Response (JSON):**
```json
{
  "turn": 1,
  "phase": "MAIN1",
  "activePlayerId": 0,
  "players": [...],
  "possible_actions": [...]
}
```

### Send Action
```bash
curl -X POST -d "pass" http://localhost:8080/action
```
or
```bash
curl -X POST -d "play 0" http://localhost:8080/action
```

### Reset Game
```bash
curl -X POST http://localhost:8080/reset
```
This will immediately end the current game (via concession) and start a new one with the same configuration.

## Verification Results
I verified the implementation by:
1.  Starting the server with `./forge-headless-http`.
2.  Fetching the initial state (`GET /state`).
3.  Sending a "pass" action (`POST /action`).
4.  Verifying the state updated (Phase changed).
5.  Sending a "reset" command (`POST /reset`).
6.  Verifying the game restarted (Turn reset to 1).

All tests passed successfully.
+
+# Walkthrough - Manual Agent Interface
+
+## Overview
+I have implemented a manual agent interface to test the AI Agent Mode of ForgeHeadless. This interface allows a human to act as the AI agent, receiving game states and selecting actions via a web UI.
+
+## Components
+1.  **Manual Agent Backend (`manual_agent.py`)**: A Python HTTP server (using `http.server`) running on port 5001. It receives POST requests from ForgeHeadless and exposes a polling API for the frontend.
+2.  **Frontend (`templates/index.html`)**: A simple HTML/JS interface that polls the backend for requests and displays them. It allows users to click buttons for actions or input manual JSON for complex decisions (like combat).
+3.  **Scripts**:
+    *   `run_manual_test.sh`: Starts the manual agent and ForgeHeadless.
+    *   `stop_manual_test.sh`: Stops all related processes.
+
+## Verification
+*   **Browser Test**: Verified that the browser can load the interface, receive a request from ForgeHeadless, and send a response back.
+*   **Process Management**: Verified that the scripts correctly start and stop the processes.
+*   **Timeout**: Increased the read timeout in `AIAgentClient.java` to 10 minutes to allow for manual interaction.
+
+## How to Run
+1.  Run `./run_manual_test.sh`.
+2.  Open `http://localhost:5001` in your browser.
+3.  Wait for Forge to start and send a request (Status will change to "ACTION REQUIRED").
+4.  Click an action button to send a decision.
