# Network Play Optimization - Branch: NetworkPlay

This document describes the network optimization features implemented in this branch, including delta synchronization for bandwidth reduction and robust reconnection support for handling network interruptions.

## Overview

The NetworkPlay branch introduces two major features to improve the multiplayer experience:

1. **Delta Synchronization**: Instead of sending the complete game state on every update, only changed properties are transmitted, significantly reducing bandwidth usage.

2. **Reconnection Support**: Players who disconnect (intentionally or due to network issues) can rejoin an in-progress game within a configurable timeout period (default: 5 minutes).

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

A serializable packet containing only changed data:

| Field | Type | Description |
|-------|------|-------------|
| `sequenceNumber` | `long` | Monotonically increasing sequence for ordering |
| `timestamp` | `long` | When the packet was created |
| `objectDeltas` | `Map<Integer, byte[]>` | Object ID → serialized changed properties |
| `removedObjectIds` | `Set<Integer>` | IDs of objects that no longer exist |
| `checksum` | `int` | Optional state checksum for validation |

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
- Tracks which objects exist and which have changed
- Builds delta packets by walking the GameView hierarchy
- Manages client acknowledgments
- Detects removed objects
- Periodically includes checksums for validation (every 10 packets)

```java
// Key methods:
DeltaPacket collectDeltas(GameView gameView)  // Build delta packet
void processAcknowledgment(int clientIndex, long seq)  // Handle client ack
boolean needsFullResync(int clientIndex)  // Check if client fell too far behind
```

#### Protocol Methods

New protocol methods added to `ProtocolMethod.java`:

**Server → Client:**
- `applyDelta(DeltaPacket)` - Apply incremental changes
- `fullStateSync(FullStatePacket)` - Full state for initial/reconnection

**Client → Server:**
- `ackSync(long sequenceNumber)` - Acknowledge received delta

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

---

## Files Modified

### Core Game Engine
| File | Changes |
|------|---------|
| `forge-game/.../trackable/TrackableObject.java` | Added `hasChanges()`, `getChangedProps()`, `clearChanges()`, `serializeChangedOnly()` |

### Network Protocol
| File | Changes |
|------|---------|
| `forge-gui/.../net/ProtocolMethod.java` | Added delta sync and reconnection protocol methods |
| `forge-gui/.../net/DeltaPacket.java` | **NEW** - Delta update packet |
| `forge-gui/.../net/FullStatePacket.java` | **NEW** - Full state packet with session info |

### Server-Side
| File | Changes |
|------|---------|
| `forge-gui/.../net/server/DeltaSyncManager.java` | **NEW** - Delta collection and acknowledgment |
| `forge-gui/.../net/server/GameSession.java` | **NEW** - Session state management |
| `forge-gui/.../net/server/PlayerSession.java` | **NEW** - Per-player session with secure tokens |
| `forge-gui/.../net/server/NetGuiGame.java` | Delta sync integration, `updateClient()` for reconnection |
| `forge-gui/.../net/server/FServerManager.java` | Session management, reconnection handling, GUI instance caching |
| `forge-gui/.../net/server/ServerGameLobby.java` | Initialize game session on game start |

### Client-Side
| File | Changes |
|------|---------|
| `forge-gui/.../net/client/FGameClient.java` | Session credential storage, reconnection logic |
| `forge-gui/.../net/client/GameClientHandler.java` | Handle reconnection events, send reconnect requests |
| `forge-gui/.../net/client/NetGameController.java` | Added `ackSync()` method |

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
| `forge-gui/.../gamemodes/match/AbstractGuiGame.java` | Default implementations for new methods |
| `forge-gui/.../player/PlayerControllerHuman.java` | Stub implementations for new controller methods |

### Tests
| File | Description |
|------|-------------|
| `forge-gui-desktop/.../gamesimulationtests/NetworkOptimizationTest.java` | **NEW** - Unit tests for delta sync and sessions |
| `forge-gui-desktop/.../gamesimulationtests/LocalNetworkTestHarness.java` | **NEW** - Test infrastructure |

---

## Testing

### Running Unit Tests

```bash
# Install dependencies first (required once)
mvn install -DskipTests

# Run network optimization tests
mvn test -pl forge-gui-desktop -Dtest=NetworkOptimizationTest
```

### Manual Testing

1. **Start Host**: Launch Forge → Multiplayer → Host Game on port 36743
2. **Start Client**: Launch second Forge instance → Multiplayer → Join → `localhost:36743`
3. **Start Game**: Both players ready up and begin

**Testing Delta Sync:**
- Play several turns
- Monitor console for `DeltaPacket` messages instead of full `setGameView`

**Testing Reconnection:**
1. During a game, force-quit the client (Task Manager)
2. Host should show: "Waiting for [player] to reconnect..."
3. Restart client and rejoin `localhost:36743`
4. Client should transition directly to game screen with state restored

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

## Future Improvements

1. **Bandwidth Metrics**: Add monitoring for packet sizes to verify delta sync effectiveness
2. **Compression**: Apply compression to delta packets for further bandwidth reduction
3. **Partial Reconnection**: Support reconnecting to games after client restart (currently requires same client process)
4. **Spectator Reconnection**: Allow spectators to disconnect and rejoin
5. **Mobile Optimization**: Tune delta sync parameters for mobile network conditions

---

## Authors

This implementation was developed as part of the Forge network play optimization initiative.
