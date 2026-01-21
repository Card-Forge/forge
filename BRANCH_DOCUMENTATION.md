# NetworkPlay Branch Documentation

This document describes the network optimization features implemented in this branch, including delta synchronization for bandwidth reduction and robust reconnection support for handling network interruptions.

---

## Overview

The NetworkPlay branch introduces three major features to improve the multiplayer experience:

1. **[Delta Synchronization](#feature-1-delta-synchronization)**: Instead of sending the complete game state on every update, only changed properties are transmitted. Combined with LZ4 compression, this achieves **~97-99% bandwidth reduction** compared to the original full-state approach (typical game: 12.4MB → 80KB).

2. **[Reconnection Support](#feature-2-reconnection-support)**: Players who disconnect (intentionally or due to network issues) can rejoin an in-progress game within a configurable timeout period (default: 5 minutes). If a player fails to reconnect before the timeout expires, they are automatically converted to AI control to allow the game to continue.

3. **[Enhanced Chat Notifications](#feature-3-enhanced-chat-notifications)**: Server events (player join/leave, ready state, game start/end, reconnection status) are clearly communicated through styled system messages, providing better visibility into game state and player actions.

**Additional Resources:**
- **[Debugging](#debugging)**: Comprehensive debug logging for diagnosing network synchronization issues
- **[Architectural Analysis](#architectural-overlap-with-main-branch)**: Impact on core game logic and merge considerations

---

## Architectural Overlap with Main Branch

**⚠️ Important for Main Branch Developers**

The NetworkPlay branch modifies several core (non-network) classes to support delta synchronization and reconnection features. While these modifications enable significant performance improvements, they create potential integration considerations with ongoing Main branch development.

### Summary of Core Class Changes

The following core files have been modified for network functionality:

| File | Module | Modification Type |
|------|--------|------------------|
| `TrackableObject.java` | forge-game | Changed `set()` visibility to public, added 4 delta sync methods |
| `AbstractGuiGame.java` | forge-gui | Added ~846 lines of network deserialization logic |
| `GameLobby.java` | forge-gui | Reordered execution sequence for `onGameStarted()` |
| `IGameController.java` | forge-gui | Added 3 network protocol methods |
| `IGuiGame.java` | forge-gui | Added 8 network protocol methods |

### Why These Changes Were Necessary

**Delta Synchronization** requires:
- Access to TrackableObject internals to detect and serialize property changes
- Network deserialization in AbstractGuiGame to apply delta packets
- Protocol methods in interfaces for client-server communication

**Reconnection Support** requires:
- Modified game initialization sequence to establish sessions before sending state
- Tracker management for deserialized game objects

These features are **deeply integrated** with the game state management system, making isolation from core classes challenging.

### Available Refactoring Options

If the Main branch team determines that the architectural overlap needs to be addressed, **[NETWORK_ARCHITECTURE.md](NETWORK_ARCHITECTURE.md)** provides detailed refactoring strategies that could be considered:

1. **Isolate TrackableObject changes** using package-private access patterns
2. **Extract network logic** from AbstractGuiGame to a NetworkGuiGame subclass
3. **Segregate interfaces** to separate network methods from core interfaces
4. **Document timing dependencies** in GameLobby to prevent accidental breakage

These refactoring options are provided for consideration and may be implemented if the Main branch development team determines they are necessary based on their development plans and integration timeline.

---

## Feature 1: Delta Synchronization

### Problem Statement

Previously, the network protocol sent the entire `GameView` object on every state update. For complex board states with many cards, this resulted in:
- High bandwidth consumption
- Increased latency
- Poor performance on slower connections

### Solution Architecture

#### Core Components

##### 1. TrackableObject Change Tracking (`forge-game/.../trackable/TrackableObject.java`)

The existing `TrackableObject` class (base class for all game view objects) was extended with delta tracking capabilities:

```java
// New methods added:
public boolean hasChanges()              // Check if object has pending changes
public Set<TrackableProperty> getChangedProps()  // Get changed properties
public void clearChanges()               // Clear change flags after acknowledgment
public void serializeChangedOnly(TrackableSerializer ts)  // Serialize only changes
```

**How it works**: When a property is modified via `set()`, it's added to `changedProps`. These flags persist until explicitly cleared after the client acknowledges receipt.

##### 2. DeltaPacket (`forge-gui/.../net/DeltaPacket.java`)

A serializable packet containing delta updates and new objects:

| Field | Type | Description |
|-------|------|-------------|
| `sequenceNumber` | `long` | Monotonically increasing sequence for ordering |
| `timestamp` | `long` | When the packet was created |
| `objectDeltas` | `Map<Integer, byte[]>` | Object ID → serialized changed properties (existing objects) |
| `newObjects` | `Map<Integer, NewObjectData>` | Object ID → full object data (newly created objects) |
| `removedObjectIds` | `Set<Integer>` | IDs of objects that no longer exist |
| `checksum` | `int` | Optional state checksum for validation |

**NewObjectData** contains:
- `objectId` - The object's unique ID
- `objectType` - Type constant (TYPE_CARD_VIEW=0, TYPE_PLAYER_VIEW=1, etc.)
- `fullProperties` - All properties serialized in compact binary format

##### 3. FullStatePacket (`forge-gui/.../net/FullStatePacket.java`)

Used for initial connection and reconnection scenarios:

| Field | Type | Description |
|-------|------|-------------|
| `sequenceNumber` | `long` | Current sequence number |
| `gameView` | `GameView` | Complete game state |
| `sessionId` | `String` | Session identifier (for reconnection) |
| `sessionToken` | `String` | Authentication token (for reconnection) |

##### 4. DeltaSyncManager (`forge-gui/.../net/server/DeltaSyncManager.java`)

Server-side manager that:
- Tracks which objects exist (`trackedObjectIds`) and which have been sent to the client (`sentObjectIds`)
- Distinguishes new objects (need full serialization) from existing objects (only send changes)
- Builds delta packets by walking the GameView hierarchy
- Manages client acknowledgments
- Detects removed objects
- Periodically includes checksums for validation (every 10 packets)

```java
// Key methods:
DeltaPacket collectDeltas(GameView gameView)  // Build delta packet (new objects + deltas)
void markObjectsAsSent(GameView gameView)     // Mark all objects as sent after full sync
void processAcknowledgment(int clientIndex, long seq)  // Handle client ack
boolean needsFullResync(int clientIndex)  // Check if client fell too far behind
```

**Object Tracking Logic**:
- Objects in `sentObjectIds`: Only changed properties are serialized (delta)
- Objects NOT in `sentObjectIds`: Full object data is serialized (new object)
- After sending, all current objects are added to `sentObjectIds`

**Why new objects need full serialization**: Delta sync only works for objects that already exist on the client. When a new card is drawn, a spell is cast, or a token is created, the client has no prior knowledge of that object - it doesn't exist in the client's Tracker yet. Sending only "changed properties" would be meaningless because there's no base object to apply changes to. Therefore, new objects must include all their properties so the client can instantiate them from scratch. Once created and registered in the client's Tracker, subsequent updates to that object can use efficient delta serialization.

##### 5. Compact Binary Serialization

To minimize packet sizes, the system uses custom binary serialization instead of Java's default `ObjectOutputStream`:

**Key Components**:
- **NetworkPropertySerializer** - Type-aware property serialization that writes primitives and collections efficiently
- **NetworkTrackableSerializer** - Writes `TrackableObject` references as 4-byte IDs instead of full object graphs
- **NetworkTrackableDeserializer** - Reads `TrackableObject` references by looking up IDs in the client's Tracker

**Performance**: This approach reduced CardView serialization from ~96KB (full Java serialization) to ~200 bytes (99.8% reduction).

##### 6. Bandwidth Monitoring

`NetGuiGame` includes bandwidth tracking to verify delta sync effectiveness:

```java
private long totalDeltaBytes = 0;
private long totalFullStateBytes = 0;
private int deltaPacketCount = 0;

// Logs show per-packet and cumulative bandwidth savings
```

Console output displays bandwidth savings percentages, demonstrating the efficiency gains from delta synchronization.

##### 7. LZ4 Compression

**All network packets are automatically compressed using LZ4** via `CompatibleObjectEncoder`/`CompatibleObjectDecoder`. This provides:

- **Compression Ratio**: 60-75% bandwidth reduction (on top of delta sync savings)
- **Speed**: 1-5ms compression/decompression time (minimal overhead)
- **Scope**: Applies to all network traffic (DeltaPacket, FullStatePacket, chat messages, etc.)

The LZ4 compression layer is applied transparently at the network protocol level:

```
Packet → ObjectOutputStream → LZ4BlockOutputStream → Network
```

**Combined Savings**: Delta synchronization (90% reduction) + LZ4 compression (60-75% reduction) = ~97% bandwidth reduction compared to uncompressed full state updates.

##### 8. Checksum Validation & Auto-Resync

To detect and recover from synchronization errors (e.g., packet corruption, missed updates), the system includes automatic checksum validation:

**Server-Side** (`DeltaSyncManager.java`):
- Computes state checksum every 10 packets
- Checksum includes: game ID, turn number, phase, player IDs and life totals
- Includes checksum in `DeltaPacket` when computed

**Client-Side** (`AbstractGuiGame.java`):
- Validates checksum when present in received packet
- Computes local state checksum using same algorithm
- On mismatch: Automatically requests full state resync

**Auto-Recovery Flow**:
```
1. Client receives DeltaPacket with checksum
2. Client computes local checksum
3. If mismatch detected:
   - Client logs error with both checksums
   - Client calls requestFullStateResync()
   - Client sends requestResync protocol message
   - Server receives resync request (GameServerHandler)
   - Server sends full state via sendFullState()
   - Client resets to authoritative state
4. Game continues seamlessly (player unaware)
```

**Benefits**:
- Detects data corruption or missed updates
- Automatic recovery without user intervention
- Validation overhead: <0.1% (only every 10 packets)
- Recovery time: ~50-200ms (one full state send)

**Implementation Files**:
- Server: `GameServerHandler.java` (handles requestResync)
- Client: `AbstractGuiGame.java` (validates checksums)
- Protocol: `ProtocolMethod.requestResync`

#### Protocol Methods

New protocol methods added to `ProtocolMethod.java`:

**Server → Client:**
- `applyDelta(DeltaPacket)` - Apply incremental changes
- `fullStateSync(FullStatePacket)` - Full state for initial/reconnection

**Client → Server:**
- `ackSync(long sequenceNumber)` - Acknowledge received delta
- `requestResync()` - Request full state resync (automatic desync recovery)

#### Data Flow

```
Normal Update Flow:
┌────────────────┐    ┌─────────────────┐    ┌────────────────┐
│  Game Engine   │───>│ DeltaSyncManager│───>│  DeltaPacket   │
│ (modifies state)    │ (collects changes)   │ (minimal data) │
└────────────────┘    └─────────────────┘    └───────┬────────┘
                                                     │
                                                     ▼
┌────────────────┐    ┌─────────────────┐    ┌────────────────┐
│     Client     │<───│   NetGuiGame    │<───│    Network     │
│ (applies delta)     │ (sends packet)       │  (transport)   │
└───────┬────────┘    └─────────────────┘    └────────────────┘
        │
        │ ackSync(sequenceNumber)
        ▼
┌────────────────┐
│ DeltaSyncManager│
│(clears changes) │
└────────────────┘
```

#### NetGuiGame Integration (`forge-gui/.../net/server/NetGuiGame.java`)

The `NetGuiGame` class (server-side GUI proxy) was modified to:

1. Use delta sync by default (`useDeltaSync = true`)
2. Call `updateGameView()` before most protocol methods
3. Send `FullStatePacket` for initial connection via `sendFullState()`

```java
public void updateGameView() {
    if (!useDeltaSync || !initialSyncSent) {
        send(ProtocolMethod.setGameView, gameView);  // Fallback to full state
        return;
    }
    DeltaPacket delta = deltaSyncManager.collectDeltas(gameView);
    if (delta != null && !delta.isEmpty()) {
        send(ProtocolMethod.applyDelta, delta);
    }
}
```

---

## Feature 2: Reconnection Support

### Problem Statement

If a player's connection dropped during a network game, they had to:
- Start a completely new game
- Lose all progress in the current match
- Inconvenience other players

### Solution Architecture

#### Session Management

##### 1. GameSession (`forge-gui/.../net/server/GameSession.java`)

Represents a game that can survive disconnections:

| Field | Type | Description |
|-------|------|-------------|
| `sessionId` | `String` | UUID identifying this session |
| `playerSessions` | `Map<Integer, PlayerSession>` | Per-player session state |
| `disconnectTimeoutMs` | `long` | How long to wait for reconnection (default: 5 min) |
| `gameInProgress` | `boolean` | Whether game is active |
| `paused` | `boolean` | Whether game is paused awaiting reconnection |

```java
// Key methods:
void markPlayerDisconnected(int playerIndex)
void markPlayerConnected(int playerIndex)
void pauseGame(String message)
void resumeGame()
boolean validateReconnection(int playerIndex, String token)
```

##### 2. PlayerSession (`forge-gui/.../net/server/PlayerSession.java`)

Per-player session state with secure token authentication:

| Field | Type | Description |
|-------|------|-------------|
| `playerIndex` | `int` | Player's position in the game |
| `sessionToken` | `String` | Cryptographically random 32-byte token |
| `connectionState` | `ConnectionState` | CONNECTED, DISCONNECTED, or RECONNECTING |
| `disconnectTime` | `long` | When the player disconnected |
| `playerName` | `String` | Player's display name |

**Security**: Tokens are generated using `SecureRandom` and compared using constant-time comparison to prevent timing attacks.

#### Reconnection Events

##### ReconnectRequestEvent (`forge-gui/.../net/event/ReconnectRequestEvent.java`)

Sent by client when attempting to rejoin:
- `sessionId` - The game session to rejoin
- `token` - The player's authentication token

##### ReconnectRejectedEvent (`forge-gui/.../net/event/ReconnectRejectedEvent.java`)

Sent by server if reconnection fails:
- `reason` - Human-readable explanation

#### Protocol Methods for Reconnection

**Server → Client:**
- `gamePaused(String message)` - Notify that game is paused
- `gameResumed()` - Notify that game has resumed
- `reconnectAccepted(FullStatePacket)` - Successful reconnection with full state
- `reconnectRejected(String reason)` - Failed reconnection

**Client → Server:**
- `reconnectRequest(String sessionId, String token)` - Request to rejoin

#### Server-Side Flow (FServerManager.java)

##### Session Lifecycle

```
Game Start:
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ServerGameLobby  │───>│  FServerManager │───>│   GameSession   │
│.onGameStarted() │    │.createGameSession()  │  (new session)  │
└─────────────────┘    └─────────────────┘    └────────┬────────┘
                                                       │
                                                       ▼
                                              ┌─────────────────┐
                                              │  PlayerSession  │
                                              │ (per player)    │
                                              └─────────────────┘
```

##### Disconnect Handling

```
Player Disconnects:
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│DeregisterClient │───>│  GameSession    │───>│  Other Players  │
│Handler.inactive │    │.markDisconnected│    │ (GamePausedEvent)
└─────────────────┘    └────────┬────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │ Timeout Timer   │
                       │ (5 minutes)     │
                       │ OR              │
                       │ /skipreconnect  │
                       └────────┬────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │ convertPlayerToAI│
                       │ (AI takeover)   │
                       └─────────────────┘
```

##### Reconnection Handling

The server supports two reconnection methods:

**Method 1: Token-based (if client retained credentials)**
```
Client sends ReconnectRequestEvent with sessionId + token
       │
       ▼
FServerManager.handleReconnectRequest()
       │
       ▼
Validate token against PlayerSession
       │
       ├── Valid: Send reconnectAccepted + FullStatePacket
       │
       └── Invalid: Send reconnectRejected
```

**Method 2: Username-based (if client lost credentials)**
```
Client sends LoginEvent with username
       │
       ▼
FServerManager.tryReconnectByUsername()
       │
       ▼
Find disconnected PlayerSession with matching name
       │
       ├── Found:
       │   1. Update client connection
       │   2. Send setGameView (game state)
       │   3. Send openView (trigger UI transition)
       │   4. Send fullStateSync (session credentials)
       │
       └── Not found: Normal login flow
```

#### Client-Side Flow (FGameClient.java, GameClientHandler.java)

##### Credential Storage

```java
// FGameClient stores session info:
private String sessionId;
private String sessionToken;
private boolean wasConnected = false;
private boolean isReconnecting = false;

public void setSessionCredentials(String sessionId, String token) {
    this.sessionId = sessionId;
    this.sessionToken = token;
    this.wasConnected = true;
}
```

##### Connection Behavior

```java
// GameClientHandler.channelActive():
if (client.isReconnecting() && client.canReconnect()) {
    // Send ReconnectRequestEvent with credentials
    ctx.channel().writeAndFlush(new ReconnectRequestEvent(
        client.getSessionId(),
        client.getSessionToken()
    ));
} else {
    // Normal LoginEvent
    ctx.channel().writeAndFlush(new LoginEvent(...));
}
```

#### GUI Instance Management

A critical part of reconnection is reusing the original `NetGuiGame` instances:

```java
// FServerManager stores GUI instances:
private final Map<Integer, NetGuiGame> playerGuis = new ConcurrentHashMap<>();

// getGui() returns stored instance or creates new one:
public IGuiGame getGui(int index) {
    NetGuiGame existingGui = playerGuis.get(index);
    if (existingGui != null) {
        return existingGui;  // Reuse existing (has GameView set)
    }
    // Create and store new instance
    NetGuiGame newGui = new NetGuiGame(client);
    playerGuis.put(index, newGui);
    return newGui;
}
```

This ensures that on reconnection, the stored `NetGuiGame` (which has the `GameView` already set from game start) can be updated with the new client connection and properly send the game state.

#### Timeout Handling and AI Takeover

When a player fails to reconnect within the timeout period (default: 5 minutes), the server **converts the player to AI control** instead of removing them from the game.

##### What Happens on Timeout

1. The disconnected player's lobby slot changes to `LobbySlotType.AI`
2. Player's name is updated to include " (AI)" suffix
3. An AI controller (`PlayerControllerAi`) replaces the player's human controller
4. All game state is preserved (hand, library, battlefield, permanents)
5. The player is marked as "connected" in the session (AI doesn't disconnect)
6. Game resumes automatically if no other players are disconnected

**Benefits:**
- Games continue instead of ending prematurely
- Player's cards/permanents remain controlled by AI
- Prevents 1v1 games from becoming unplayable
- No game state is lost during the transition

##### Host `/skipreconnect` Command

The host can manually trigger AI takeover before the timeout expires using a chat command.

**Usage:**
```
/skipreconnect                  - Take over first disconnected player
/skipreconnect <PlayerName>     - Take over specific player by name
```

**Features:**
- Only accessible to the host (player index 0)
- Cancels the reconnection timeout timer
- Immediately triggers AI takeover
- Command is not echoed to chat (processed silently)
- Broadcasts: `"Host skipped reconnection wait. <Player> replaced with AI."`

**Use Cases:**
- Player confirms they cannot reconnect (e.g., via external chat)
- Host knows player has permanently left
- Avoid waiting full 5 minutes when return is unlikely

##### Implementation: convertPlayerToAI() Method

The core of the AI takeover functionality is the `convertPlayerToAI()` method in `FServerManager.java`:

```java
private void convertPlayerToAI(int playerIndex, String username) {
    // 1. Update lobby slot to AI type
    LobbySlot slot = localLobby.getSlot(playerIndex);
    if (slot != null) {
        slot.setType(LobbySlotType.AI);
        slot.setName(username + " (AI)");
    }

    // 2. Find the player in the active game
    if (localLobby.getHostedMatch() != null) {
        forge.game.Match match = localLobby.getHostedMatch().getMatch();
        if (match != null) {
            forge.game.Game game = match.getGame();
            if (game != null) {
                // Find player by matching lobby name
                forge.game.player.Player targetPlayer = null;
                for (forge.game.player.Player p : game.getPlayers()) {
                    if (username.equals(p.getLobbyPlayer().getName())) {
                        targetPlayer = p;
                        break;
                    }
                }

                if (targetPlayer != null) {
                    // 3. Create AI controller
                    forge.ai.LobbyPlayerAi aiLobbyPlayer =
                        new forge.ai.LobbyPlayerAi(username + " (AI)", null);
                    forge.ai.PlayerControllerAi aiController =
                        new forge.ai.PlayerControllerAi(game, targetPlayer, aiLobbyPlayer);

                    // 4. Replace controller
                    targetPlayer.dangerouslySetController(aiController);
                }
            }
        }
    }

    // 5. Mark player as connected (AI is "connected")
    currentGameSession.markPlayerConnected(playerIndex);

    // 6. Update lobby state
    updateLobbyState();
}
```

**Key Points:**
1. **Lobby Slot Update**: Changes the slot type to AI so other players see the change
2. **Player Lookup**: Finds the in-game Player instance by matching username
3. **AI Controller Creation**: Creates a new `LobbyPlayerAi` and `PlayerControllerAi`
4. **Controller Substitution**: Uses `dangerouslySetController()` to replace the human controller with AI
5. **Session Update**: Marks player as connected so game can resume
6. **State Broadcast**: Updates all clients about the lobby change

**Why `dangerouslySetController()`?**
This method bypasses the normal controller creation flow and directly replaces the controller. This is necessary because:
- The Player object already exists and has game state
- We don't want to create a new Player instance (would lose state)
- We need to hot-swap the controller while maintaining all other state

##### Implementation: Chat Command Parsing

The `/skipreconnect` command is parsed in the `MessageHandler` class:

```java
private void handleSkipReconnectCommand(String message) {
    // Parse: /skipreconnect or /skipreconnect <playerName>
    String[] parts = message.trim().split("\\s+", 2);

    // Validation checks
    if (currentGameSession == null || !currentGameSession.isGameInProgress()) {
        broadcast(new MessageEvent("No active game session."));
        return;
    }

    // Find target player (specific name or first disconnected)
    String targetPlayerName = parts.length > 1 ? parts[1].trim() : null;
    int targetIndex = -1;
    String targetUsername = null;

    if (targetPlayerName != null) {
        // Find specific disconnected player by name
        for (int i = 0; i < 8; i++) {
            PlayerSession ps = currentGameSession.getPlayerSession(i);
            if (ps != null && ps.isDisconnected()) {
                if (targetPlayerName.equalsIgnoreCase(ps.getPlayerName())) {
                    targetIndex = i;
                    targetUsername = ps.getPlayerName();
                    break;
                }
            }
        }
    } else {
        // Find first disconnected player
        for (int i = 0; i < 8; i++) {
            PlayerSession ps = currentGameSession.getPlayerSession(i);
            if (ps != null && ps.isDisconnected()) {
                targetIndex = i;
                targetUsername = ps.getPlayerName();
                break;
            }
        }
    }

    // Error handling
    if (targetIndex == -1) {
        // Broadcast appropriate error message
        return;
    }

    // Execute AI takeover
    skipReconnectionTimeout(targetIndex, targetUsername);
}
```

**Security:**
- Only the host (client index 0) can execute the command
- Validates game session exists and is in progress
- Validates player is actually disconnected
- Command is not echoed to chat (prevents spam)

---

## Feature 3: Enhanced Chat Notifications

### Problem Statement

During network play, important server events (player joining, disconnecting, ready state changes, game outcomes) were either not communicated at all or buried in system console output. Players had no clear visibility into:
- Who was ready to start
- Whether all players had marked ready
- When a player disconnected or reconnected
- How much time remained for reconnection
- Who won the game
- The host player's identity

This lack of feedback made network play feel unresponsive and confusing, especially during connection issues.

### Solution Architecture

#### System Message Type System

##### ChatMessage Enhancement (`forge-gui/.../net/ChatMessage.java`)

Added a `MessageType` enum to distinguish player messages from system notifications:

```java
public enum MessageType {
    PLAYER,   // Regular player chat message
    SYSTEM    // System notification (displayed differently)
}
```

**Automatic Detection**: Messages with `null` source are automatically classified as SYSTEM.

**New Methods**:
- `isSystemMessage()` - Check if message is a system notification
- `getType()` - Get the message type
- `createSystemMessage(String)` - Factory method for system messages

#### Visual Styling

##### Mobile Platform (`OnlineChatScreen.java`)

System messages appear visually distinct from player messages:

| Attribute | Player Messages | System Messages |
|-----------|----------------|-----------------|
| **Color** | Zebra/contrast colors | **Light blue (#6496FF)** |
| **Alignment** | Left (remote) / Right (local) | **Center** |
| **Triangle pointer** | Yes | No |
| **Sender name** | Displayed | Hidden |

##### Desktop Platform (`FNetOverlay.java`)

System messages are prefixed with `[SERVER]` indicator:
```
[14:32:15] [SERVER] Alice joined the room
[14:32:20] [SERVER] Bob is ready (1/2 players ready)
```

*Note: Full blue color support on desktop would require migrating from `FTextArea` to `JTextPane` with `StyledDocument`.*

#### Server Event Notifications

##### 1. Player Ready Tracking

When a player marks ready, the server:
1. Counts ready players vs. total players
2. Broadcasts: `"PlayerName is ready (X/Y players ready)"`
3. If all ready: Broadcasts `"All players ready! Starting game..."`

**Implementation** (`FServerManager.java`):
```java
// In updateSlot() and LobbyInputHandler
if (updateEvent.getReady() != null && updateEvent.getReady()) {
    int readyCount = countReadyPlayers();
    int totalPlayers = countTotalPlayers();

    broadcast(new MessageEvent(String.format(
        "%s is ready (%d/%d players ready)",
        playerName, readyCount, totalPlayers
    )));

    if (readyCount == totalPlayers && totalPlayers > 1) {
        broadcast(new MessageEvent("All players ready! Starting game..."));
    }
}
```

##### 2. Reconnection Countdown

Instead of a single timeout at 5 minutes, notifications are sent every 30 seconds:

**Timeline**:
```
0:00 - Player disconnects → "Alice disconnected. Game paused. Waiting for reconnection..."
0:30 - "Waiting for Alice to reconnect... (4:30 remaining)"
1:00 - "Waiting for Alice to reconnect... (4:00 remaining)"
1:30 - "Waiting for Alice to reconnect... (3:30 remaining)"
...
4:30 - "Waiting for Alice to reconnect... (0:30 remaining)"
5:00 - "Alice timed out. AI taking over."
```

**Implementation** (`FServerManager.scheduleReconnectionTimeout()`):
```java
Timer timer = new Timer("ReconnectionTimeout-" + playerIndex);
long countdownInterval = 30 * 1000; // 30 seconds
long currentTime = 0;

while (currentTime < timeoutMs) {
    final long remainingTime = timeoutMs - currentTime;

    timer.schedule(new TimerTask() {
        public void run() {
            long minutes = remainingSeconds / 60;
            long seconds = remainingSeconds % 60;
            String timeStr = String.format("%d:%02d", minutes, seconds);

            broadcast(new MessageEvent(String.format(
                "Waiting for %s to reconnect... (%s remaining)",
                username, timeStr
            )));
        }
    }, currentTime);

    currentTime += countdownInterval;
}

// Final timeout at 5:00
timer.schedule(new TimerTask() {
    public void run() { handleReconnectionTimeout(...); }
}, timeoutMs);
```

##### 3. Game End Announcements

When a game ends, the server:
1. Determines the winner from `HostedMatch.getMatch().getWinner()`
2. Broadcasts winner announcement
3. Broadcasts "Returning to lobby..."

**Implementation** (`FServerManager.onGameEnded()`):
```java
private void announceGameWinner() {
    HostedMatch hostedMatch = localLobby.getHostedMatch();
    if (hostedMatch != null) {
        forge.game.Match match = hostedMatch.getMatch();
        RegisteredPlayer winner = match.getWinner();

        String message;
        if (winner != null) {
            String winnerName = winner.getPlayer().getName();
            message = String.format("Game ended. Winner: %s", winnerName);
        } else {
            message = "Game ended. Draw";
        }

        broadcast(new MessageEvent(message));
    }
}
```

##### 4. Host Identification

The host player's name is appended with " (Host)" in all chat messages:

**Server-side** (`FServerManager.MessageHandler`):
```java
String username = client.getUsername();
if (client.getIndex() == 0) {  // Index 0 is always the host
    username = username + " (Host)";
}
broadcast(new MessageEvent(username, message));
```

**Client-side** (`NetConnectUtil.java` - for host's own messages):
```java
String source = message.getSource();
if (source != null) {
    source = source + " (Host)";
}
chatInterface.addMessage(new ChatMessage(source, message.getMessage()));
```

#### Complete Notification List

The following system messages are now broadcast during network play:

| Event | Message Format | When Sent |
|-------|---------------|-----------|
| **Player Join** | `"PlayerName joined the room"` | Player connects to lobby |
| **Player Leave** | `"PlayerName left the room"` | Player disconnects from lobby (not during game) |
| **Player Ready** | `"PlayerName is ready (X/Y players ready)"` | Player marks ready |
| **All Ready** | `"All players ready! Starting game..."` | All players ready |
| **Disconnect (Game)** | `"PlayerName disconnected. Game paused. Waiting for reconnection..."` | Player disconnects during game |
| **Reconnect Timer** | `"Waiting for PlayerName to reconnect... (M:SS remaining)"` | Every 30 seconds during timeout |
| **Reconnect Success** | `"PlayerName has reconnected!"` | Player rejoins successfully |
| **AI Takeover (Timeout)** | `"PlayerName timed out. AI taking over."` | 5-minute timer expires |
| **AI Takeover (Manual)** | `"Host skipped reconnection wait. PlayerName replaced with AI."` | Host uses `/skipreconnect` command |
| **No Active Session** | `"No active game session."` | Host uses `/skipreconnect` when no game is active |
| **No Disconnected Players** | `"No disconnected players found."` | Host uses `/skipreconnect` when no one is disconnected |
| **Player Not Found** | `"No disconnected player found with name 'PlayerName'."` | Host specifies invalid player name |
| **Game End** | `"Game ended. Winner: PlayerName"` or `"Game ended. Draw"` | Match completes |
| **Return to Lobby** | `"Returning to lobby..."` | After game end |

#### Message Flow

```
Lobby Phase:
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Client    │───>│FServerManager│───>│All Clients  │
│ (joins)     │    │ broadcasts  │    │ (see join)  │
└─────────────┘    │ MessageEvent│    └─────────────┘
                   └─────────────┘

Ready State:
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Client    │───>│updateSlot() │───>│All Clients  │
│ clicks ready│    │ counts ready│    │ (see count) │
└─────────────┘    │ broadcasts  │    └─────────────┘
                   └─────────────┘

Reconnection:
┌─────────────┐    ┌──────────────┐   ┌─────────────┐
│  Disconnect │───>│scheduleTimer │──>│All Clients  │
│             │    │ (30s interval)   │ (countdown) │
└─────────────┘    └──────────────┘   └─────────────┘
                         │
                         │ every 30s
                         ▼
                   ┌──────────────┐
                   │ broadcast    │
                   │ countdown    │
                   └──────────────┘
```

### Files Modified

| File | Changes |
|------|---------|
| `forge-gui/.../net/ChatMessage.java` | Added `MessageType` enum, constructors, `isSystemMessage()`, `createSystemMessage()` |
| `forge-gui/.../net/server/FServerManager.java` | Added ready notifications, 30-second countdown timer, winner announcement logic, host indicator |
| `forge-gui/.../net/NetConnectUtil.java` | Added host indicator to local player messages |
| `forge-gui/.../match/GameLobby.java` | Added `getHostedMatch()` accessor for winner detection |
| `forge-gui-desktop/.../gui/FNetOverlay.java` | Added `[SERVER]` prefix for system messages |
| `forge-gui-mobile/.../screens/online/OnlineChatScreen.java` | Added blue styling, centered alignment for system messages |

### Visual Examples

#### Mobile Chat Display

```
┌─────────────────────────────────────┐
│                                     │
│  ┌──────────────────────────────┐  │
│  │  Alice joined the room       │  │  ← Blue, centered
│  │  [14:32:15]                  │  │
│  └──────────────────────────────┘  │
│                                     │
│ ┌──────────────────────────────┐◀  │
│ │ Alice (Host): Hello everyone │   │  ← Player message
│ │ [14:32:18]                   │   │
│ └──────────────────────────────┘   │
│                                     │
│  ┌──────────────────────────────┐  │
│  │  Bob joined the room         │  │  ← Blue, centered
│  │  [14:32:20]                  │  │
│  └──────────────────────────────┘  │
│                                     │
│  ┌──────────────────────────────┐  │
│  │  Alice (Host) is ready       │  │  ← Blue, centered
│  │  (1/2 players ready)         │  │
│  │  [14:32:25]                  │  │
│  └──────────────────────────────┘  │
│                                     │
│  ┌──────────────────────────────┐  │
│  │  Bob is ready                │  │  ← Blue, centered
│  │  (2/2 players ready)         │  │
│  │  [14:32:28]                  │  │
│  └──────────────────────────────┘  │
│                                     │
│  ┌──────────────────────────────┐  │
│  │  All players ready!          │  │  ← Blue, centered
│  │  Starting game...            │  │
│  │  [14:32:28]                  │  │
│  └──────────────────────────────┘  │
└─────────────────────────────────────┘
```

#### Desktop Chat Display

```
[14:32:15] [SERVER] Alice joined the room
[14:32:18] Alice (Host): Hello everyone
[14:32:20] [SERVER] Bob joined the room
[14:32:25] [SERVER] Alice (Host) is ready (1/2 players ready)
[14:32:28] [SERVER] Bob is ready (2/2 players ready)
[14:32:28] [SERVER] All players ready! Starting game...
...
[14:45:12] [SERVER] Bob disconnected. Game paused. Waiting for reconnection...
[14:45:42] [SERVER] Waiting for Bob to reconnect... (4:30 remaining)
[14:46:12] [SERVER] Waiting for Bob to reconnect... (4:00 remaining)
[14:46:23] [SERVER] Bob has reconnected!
...
[15:00:05] [SERVER] Charlie disconnected. Game paused. Waiting for reconnection...
[15:00:35] [SERVER] Waiting for Charlie to reconnect... (4:30 remaining)
[15:01:05] Alice (Host): /skipreconnect Charlie
[15:01:05] [SERVER] Host skipped reconnection wait. Charlie replaced with AI.
...
[15:12:45] [SERVER] Game ended. Winner: Alice
[15:12:45] [SERVER] Returning to lobby...
```

### Benefits

1. **Clear State Visibility**: Players always know who is ready, who is connected, and what's happening
2. **Connection Awareness**: Countdown timer provides clear feedback during reconnection attempts
3. **Host Identification**: Makes it obvious who is hosting the game
4. **Game Outcome**: Winner is clearly announced to all players
5. **Professional Polish**: Blue system messages (mobile) and [SERVER] prefix (desktop) distinguish official notifications from player chat

---

## Files Modified

### Core Game Engine
| File | Changes |
|------|---------|
| `forge-game/.../trackable/TrackableObject.java` | Added `hasChanges()`, `getChangedProps()`, `clearChanges()`, `serializeChangedOnly()` |
| `forge-game/.../spellability/StackItemView.java` | Added network deserialization constructor |

### Network Protocol
| File | Changes |
|------|---------|
| `forge-gui/.../net/ProtocolMethod.java` | Added delta sync and reconnection protocol methods |
| `forge-gui/.../net/DeltaPacket.java` | **NEW** - Delta update packet; added `NewObjectData` inner class |
| `forge-gui/.../net/FullStatePacket.java` | **NEW** - Full state packet with session info |
| `forge-gui/.../net/NetworkPropertySerializer.java` | **NEW** - Type-aware compact property serialization |
| `forge-gui/.../net/NetworkTrackableSerializer.java` | **NEW** - Writes TrackableObjects as 4-byte IDs |
| `forge-gui/.../net/NetworkTrackableDeserializer.java` | **NEW** - Reads TrackableObjects by ID lookup from Tracker |
| `forge-gui/.../net/NetworkDebugLogger.java` | **NEW** - Configurable debug logging with separate console/file verbosity levels |
| `forge-gui/.../net/CompatibleObjectEncoder.java` | **Existing** - Applies LZ4 compression to all outgoing network packets |
| `forge-gui/.../net/CompatibleObjectDecoder.java` | **Existing** - Decompresses LZ4-compressed incoming packets |
| `forge-gui/.../net/ChatMessage.java` | Added `MessageType` enum, `isSystemMessage()`, `createSystemMessage()` for chat notifications |
| `forge-gui/.../net/NetConnectUtil.java` | Added host indicator " (Host)" to local player chat messages |

### Server-Side
| File | Changes |
|------|---------|
| `forge-gui/.../net/server/DeltaSyncManager.java` | **NEW** - Delta collection; added `sentObjectIds` tracking, `serializeNewObject()`, `markObjectsAsSent()` |
| `forge-gui/.../net/server/GameSession.java` | **NEW** - Session state management |
| `forge-gui/.../net/server/PlayerSession.java` | **NEW** - Per-player session with secure tokens |
| `forge-gui/.../net/server/NetGuiGame.java` | Delta sync integration, bandwidth monitoring, change flag clearing |
| `forge-gui/.../net/server/FServerManager.java` | Session management, reconnection handling, debug logging, chat notifications (ready state, countdown, winner), host indicator, **AI takeover on timeout**, **`/skipreconnect` command handling** |
| `forge-gui/.../net/server/ServerGameLobby.java` | Initialize game session on game start |

### Client-Side
| File | Changes |
|------|---------|
| `forge-gui/.../net/client/FGameClient.java` | Session credential storage, reconnection logic |
| `forge-gui/.../net/client/GameClientHandler.java` | Handle reconnection events, immediate GameView setting |
| `forge-gui/.../net/client/NetGameController.java` | Added `ackSync()` method |

### Match/GUI
| File | Changes |
|------|---------|
| `forge-gui/.../gamemodes/match/AbstractGuiGame.java` | Added `applyDelta()` implementation, tracker initialization, object creation from delta data |
| `forge-gui/.../gamemodes/match/GameLobby.java` | Reordered `onGameStarted()` to execute before `startMatch()`; added `getHostedMatch()` accessor |
| `forge-gui-desktop/.../gui/FNetOverlay.java` | Added `[SERVER]` prefix for system messages in chat display |
| `forge-gui-mobile/.../screens/online/OnlineChatScreen.java` | Added blue styling, centered alignment, no-triangle rendering for system messages |

### Events
| File | Changes |
|------|---------|
| `forge-gui/.../net/event/ReconnectRequestEvent.java` | **NEW** - Client reconnection request |
| `forge-gui/.../net/event/ReconnectRejectedEvent.java` | **NEW** - Server rejection response |

### Interfaces
| File | Changes |
|------|---------|
| `forge-gui/.../gui/interfaces/IGuiGame.java` | Added delta sync and reconnection method signatures |
| `forge-gui/.../interfaces/IGameController.java` | Added `ackSync()` and `reconnectRequest()` |
| `forge-gui/.../player/PlayerControllerHuman.java` | Stub implementations for new controller methods |

### Tests
| File | Description |
|------|-------------|
| `forge-gui-desktop/.../gamesimulationtests/NetworkOptimizationTest.java` | **NEW** - Unit tests for delta sync and sessions |
| `forge-gui-desktop/.../gamesimulationtests/LocalNetworkTestHarness.java` | **NEW** - Test infrastructure |

---

## Configuration

### Reconnection Timeout

The default reconnection timeout is 5 minutes. To modify:

```java
// In GameSession or via FServerManager
gameSession.setDisconnectTimeoutMs(10 * 60 * 1000); // 10 minutes
```

### Delta Sync Toggle

Delta sync can be disabled per-client if needed:

```java
// In NetGuiGame
netGuiGame.setDeltaSyncEnabled(false); // Falls back to full state sync
```

---

## Debugging

### NetworkDebugLogger

The network play code includes a dedicated debug logging system (`NetworkDebugLogger`) designed for diagnosing synchronization and connectivity issues. It provides configurable verbosity levels for console versus file output.

#### Log Levels

| Level | Priority | Purpose | Console Default | File Default |
|-------|----------|---------|-----------------|--------------|
| `DEBUG` | 0 | Detailed tracing (hex dumps, per-property details, collection contents) | OFF | ON |
| `INFO` | 1 | Normal operation (sync start/end, summaries, important events) | ON | ON |
| `WARN` | 2 | Potential issues (missing objects, unexpected states) | ON | ON |
| `ERROR` | 3 | Failures and exceptions | ON | ON |

#### Log File Location

Log files are created in the `logs/` directory (relative to the Forge working directory, typically `forge-gui-desktop/logs/`):

```
logs/network-debug-20250121-075900-12345.log
```

The filename includes:
- Timestamp (YYYYMMDD-HHMMSS)
- Process ID (for distinguishing multiple Forge instances)

#### Configuring Verbosity

You can adjust log levels at runtime:

```java
import forge.gamemodes.net.NetworkDebugLogger;
import forge.gamemodes.net.NetworkDebugLogger.LogLevel;

// Show DEBUG messages on console (very verbose)
NetworkDebugLogger.setConsoleLevel(LogLevel.DEBUG);

// Only show errors in the file
NetworkDebugLogger.setFileLevel(LogLevel.ERROR);

// Disable all logging
NetworkDebugLogger.setEnabled(false);

// Query current levels
LogLevel consoleLevel = NetworkDebugLogger.getConsoleLevel();
LogLevel fileLevel = NetworkDebugLogger.getFileLevel();
```

#### Log Output Examples

**Console Output (Default: INFO and above)**
```
[07:59:02.583] [INFO] [setGameView] Called with gameView0=non-null, existing gameView=null
[07:59:02.787] [INFO] [CMatchUI.openView] Called
[07:59:03.070] [INFO] [DeltaSync] === START applyDelta seq=1 ===
[07:59:03.090] [INFO] [DeltaSync] Created 60 new objects
[07:59:03.119] [INFO] [DeltaSync] Summary: 60 new objects, 5 deltas applied, 0 skipped
[07:59:03.125] [WARN] [DeltaSync] Object ID 12345 NOT FOUND for delta application
```

**File Output (Default: DEBUG and above)** - includes additional detail:
```
[07:59:03.071] [DEBUG] [DeltaSync] GameView PlayerView ID=0 hash=1234567, inTracker=FOUND, sameInstance=true
[07:59:03.072] [DEBUG] [DeltaSync] applyDeltaToObject: objId=100, objType=CardView, deltaBytes=245, propCount=8
[07:59:03.073] [DEBUG] [DeltaSync] PlayerView 0: setting Hand = Collection[7]
[07:59:03.074] [DEBUG] [NetworkDeserializer] Collection read: type=CardViewType, size=7, found=7, notFound=0
```

#### Using Debug Logging in Code

```java
// DEBUG - detailed tracing (file only by default)
NetworkDebugLogger.debug("[MyFeature] Processing object %d with %d properties", objectId, propCount);

// INFO - normal operation events (console + file)
NetworkDebugLogger.log("[MyFeature] Sync completed: %d objects processed", count);

// WARN - potential issues (console + file)
NetworkDebugLogger.warn("[MyFeature] Object %d not found in tracker", objectId);

// ERROR - failures (always logged)
NetworkDebugLogger.error("[MyFeature] Failed to deserialize: %s", e.getMessage());
NetworkDebugLogger.error("[MyFeature] Exception details:", exception);
```

#### Hex Dump for Serialization Debugging

When investigating serialization issues, the logger provides hex dump functionality:

```java
NetworkDebugLogger.hexDump("[DeltaSync] Delta bytes:", byteArray, errorPosition);
```

Output (at DEBUG level):
```
[07:59:03.500] [DEBUG] HEXDUMP: [DeltaSync] Delta bytes:
Bytes 0-63 (error at 32):
0000: 00 00 00 05 00 00 00 01 00 00 00 00 00 00 00 07  | ................
0016: 00 00 00 02 00 00 00 03 00 00 00 04 00 00 00 05  | ................
0032: [FF]FF FF FF 00 00 00 00 00 00 00 01 00 00 00 02  | ................
```

#### Debugging Common Issues

**Issue: Cards not appearing in hand**
1. Set console level to DEBUG: `NetworkDebugLogger.setConsoleLevel(LogLevel.DEBUG)`
2. Look for `[DeltaSync] PlayerView X: setting Hand = Collection[N]` messages
3. Check if `[NetworkDeserializer] Collection has X missing objects!` warnings appear
4. Verify tracker lookups: `[FullStateSync] Card X (from hand): tracker lookup = FOUND/NOT FOUND`

**Issue: Delta sync errors**
1. Check for `Invalid ordinal` or `Unexpected marker` errors
2. Review the hex dump to identify byte stream misalignment
3. Look for `VERIFY FAILED: CardView X not in tracker` warnings

**Issue: Reconnection failures**
1. Check for `[FullStateSync]` messages showing state restoration
2. Look for `[DeltaSync] Creating NEW PlayerView` warnings (indicates identity mismatch)
3. Verify session credentials are being sent

---

## Potential Future Improvements

1. **Partial Reconnection**: Support reconnecting to games after client restart (currently requires same client process)
2. **Mobile Optimization**: Tune delta sync parameters for mobile network conditions
3. **Explicit Object Removal**: Remove objects from Tracker when they leave the game (currently relies on GC)

---

## Known Limitations

1. Objects are not explicitly removed from Tracker - relies on garbage collection when no longer referenced

---