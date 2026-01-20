# NetworkPlay Branch Documentation

This document describes the network optimization features implemented in this branch, including delta synchronization for bandwidth reduction and robust reconnection support for handling network interruptions.

---

## Recent Changes (Post-Initial Commit)

The following improvements were made after the initial delta sync and reconnection commit (`910e1d4977`):

### 1. New Object Handling in Delta Sync

**Problem**: Delta sync only sent changed properties for existing objects. When new objects (cards drawn, spells cast) appeared during gameplay, the client had no way to create them - it would just receive property updates for non-existent object IDs.

**Solution**: Extended `DeltaPacket` with `NewObjectData` to send full object data for newly created objects.

**Changes**:
- `DeltaPacket.java`: Added `NewObjectData` inner class with type constants (`TYPE_CARD_VIEW`, `TYPE_PLAYER_VIEW`, etc.) and `newObjects` map
- `DeltaSyncManager.java`: Added `sentObjectIds` set to track which objects have been sent; new objects get full serialization via `serializeNewObject()`, existing objects only send changed properties
- `AbstractGuiGame.java`: Added `createObjectFromData()` to instantiate CardView, PlayerView, StackItemView, and CombatView on the client

### 2. Compact Binary Serialization

**Problem**: Java's built-in `ObjectOutputStream` serialization was producing massive packets (~96KB for a single CardView) because it serialized entire object graphs including all referenced objects.

**Solution**: Implemented custom binary serialization that writes object references as 4-byte IDs only.

**Changes**:
- `DeltaSyncManager.java`: Switched from `ObjectOutputStream` to `DataOutputStream` with `NetworkTrackableSerializer`
- `AbstractGuiGame.java`: Added `applyDeltaToObject()` using `NetworkTrackableDeserializer`
- **NEW FILES**:
  - `NetworkPropertySerializer.java` - Type-aware property serialization
  - `NetworkTrackableSerializer.java` - Writes TrackableObjects as IDs
  - `NetworkTrackableDeserializer.java` - Reads TrackableObjects by ID lookup

**Result**: CardView serialization reduced from ~96KB to ~200 bytes (99.8% reduction).

### 3. Tracker Initialization After Deserialization

**Problem**: The `Tracker` field in `TrackableObject` is transient (not serialized), so deserialized GameViews had null trackers, causing `NullPointerException` when accessing properties.

**Solution**: Added recursive tracker initialization after receiving game state.

**Changes**:
- `AbstractGuiGame.java`: Added `ensureTrackerInitialized()` and `setTrackerRecursively()` methods that traverse all TrackableObjects in the GameView hierarchy and set their tracker reference

### 4. Session Credential Timing Fix

**Problem**: Session credentials were being sent during `openView()`, but the game session wasn't created until after `startMatch()` completed, resulting in null session/credentials.

**Solution**: Reordered initialization so game session is created BEFORE starting the match.

**Changes**:
- `GameLobby.java`: Moved `onGameStarted()` call to execute before `hostedMatch.startMatch()`
- `NetGuiGame.java`: Added null checks and debug logging in `sendSessionCredentials()`

### 5. Immediate GameView Setting

**Problem**: The `setGameView` protocol method was dispatching to the EDT asynchronously, but subsequent handlers (like `openView`) needed the GameView to be available immediately in the Netty thread.

**Solution**: Set the GameView synchronously before EDT dispatch.

**Changes**:
- `GameClientHandler.java`: Added immediate `gui.setGameView()` call in `beforeCall()` for the `setGameView` method

### 6. Change Flag Management

**Problem**: Change flags on TrackableObjects weren't being cleared after sending deltas, causing delta packets to accumulate all historical changes.

**Solution**: Clear change flags after sending delta packets and mark objects as "sent" after full state sync.

**Changes**:
- `NetGuiGame.java`: Added `deltaSyncManager.clearAllChanges(gameView)` after sending delta packets
- `DeltaSyncManager.java`: Added `markObjectsAsSent(GameView)` method to mark all objects in the hierarchy as already sent to the client

### 7. StackItemView Network Constructor

**Problem**: `StackItemView` required a `StackItem` parameter in its constructor, making it impossible to create empty instances for network deserialization.

**Solution**: Added a minimal constructor for network deserialization.

**Changes**:
- `StackItemView.java`: Added `StackItemView(int id0, Tracker tracker)` constructor

### 8. Bandwidth Monitoring

**Added**: Debug logging to track bandwidth savings from delta sync.

**Changes**:
- `NetGuiGame.java`: Added bandwidth tracking fields (`totalDeltaBytes`, `totalFullStateBytes`, `deltaPacketCount`)
- `NetGuiGame.java`: Added `estimateFullStateSize()` to compare delta size vs full state
- Console logs show per-packet and cumulative bandwidth savings percentage

### 9. Debug Logging

Added diagnostic logging throughout the delta sync path:
- `FServerManager.java`: Game session creation and player registration
- `NetGuiGame.java`: Session credential flow
- `DeltaSyncManager.java`: New object creation and delta collection
- `AbstractGuiGame.java`: Full state sync object verification

---

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

### Server-Side
| File | Changes |
|------|---------|
| `forge-gui/.../net/server/DeltaSyncManager.java` | **NEW** - Delta collection; added `sentObjectIds` tracking, `serializeNewObject()`, `markObjectsAsSent()` |
| `forge-gui/.../net/server/GameSession.java` | **NEW** - Session state management |
| `forge-gui/.../net/server/PlayerSession.java` | **NEW** - Per-player session with secure tokens |
| `forge-gui/.../net/server/NetGuiGame.java` | Delta sync integration, bandwidth monitoring, change flag clearing |
| `forge-gui/.../net/server/FServerManager.java` | Session management, reconnection handling, debug logging |
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
| `forge-gui/.../gamemodes/match/GameLobby.java` | Reordered `onGameStarted()` to execute before `startMatch()` |

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

1. ~~**Bandwidth Metrics**: Add monitoring for packet sizes to verify delta sync effectiveness~~ ✓ Implemented
2. **Compression**: Apply compression to delta packets for further bandwidth reduction
3. **Partial Reconnection**: Support reconnecting to games after client restart (currently requires same client process)
4. **Spectator Reconnection**: Allow spectators to disconnect and rejoin
5. **Mobile Optimization**: Tune delta sync parameters for mobile network conditions
6. **Configurable Logging**: Make debug logging levels configurable for production use
7. **Explicit Object Removal**: Remove objects from Tracker when they leave the game (currently relies on GC)

---

## Known Limitations

1. `CardStateView` handling for `AlternateState` may skip updates if the state doesn't exist yet on the client
2. Objects are not explicitly removed from Tracker - relies on garbage collection when no longer referenced
3. Debug logging is verbose - should be reduced or made configurable for production

---

## Authors

This implementation was developed as part of the Forge network play optimization initiative.
