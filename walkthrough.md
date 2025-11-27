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
