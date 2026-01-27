# NetworkPlay Architectural Analysis & Refactoring Options

## Executive Summary

The NetworkPlay branch contains significant architectural changes to core non-network classes to support delta synchronization and reconnection features. This document analyzes these changes and provides refactoring options that could be considered if the Main branch development team determines that greater isolation of network-specific functionality is needed.

**Current State:**
- 8 core files modified
- ✅ **AbstractGuiGame.java refactored** - Network code extracted into NetworkGuiGame subclass
- Core interfaces extended with 11 network methods
- Network functionality integrated with core game state management

**Completed Refactorings:**
- ✅ Inheritance pattern applied to AbstractGuiGame (network logic isolated in NetworkGuiGame)
- ✅ GameLobby.java documented with execution order requirements

**Available Refactoring Approaches (Future):**
- Adapter pattern for TrackableObject access
- Interface segregation to separate network and core concerns
- Further isolation of network-specific methods

---

## Architectural Overlap Analysis

### Core Architecture Modifications

#### 1. `TrackableObject.java` (forge-game module)

**Location**: Core game engine module

**Current Changes:**
- Changed `set()` method visibility: `protected` → `public`
- Added 4 new public methods: `hasChanges()`, `getChangedProps()`, `clearChanges()`, `serializeChangedOnly()`

**Characteristics:**
- Network-specific functionality added to core game module
- Visibility change enables broader access to property modification
- New public methods expand the API surface of a fundamental class
- Change tracking adds logic to all property modifications

**Integration Considerations:**
- TrackableObject is a base class for all game view objects
- Changes affect the core game engine module
- Main branch modifications to this class would interact with network additions

**Potential Conflict Scenario:**
```java
// If Main branch adds a method with the same name:
protected void clearChanges() { /* different implementation */ }

// NetworkPlay already has:
public final void clearChanges() { changedProps.clear(); }
// → Would result in compilation error
```

---

#### 2. `AbstractGuiGame.java` (forge-gui module) - ✅ REFACTORED

**Location**: Core GUI game implementation

**Status**: ✅ **Network code successfully isolated** (as of latest commit)

**Refactoring Completed:**
- Created `NetworkGuiGame.java` subclass containing all network-specific logic (~850 lines)
- Restored `AbstractGuiGame.java` to core game logic only (~900 lines)
- Network methods in AbstractGuiGame reduced to simple no-op stubs
- `setGameView()` simplified to basic property copying (network tracker init moved to NetworkGuiGame)

**Architecture:**
```java
AbstractGuiGame (core game logic, ~900 lines)
    ↑ extends
NetworkGuiGame (network deserialization, ~850 lines)
    ↑ extends
NetGuiGame (server-side network proxy)
```

**Benefits:**
- AbstractGuiGame restored to pre-network state (no bloat)
- Clear separation between local and network game functionality
- Main branch merges will have minimal conflicts in AbstractGuiGame
- Network code can be maintained independently

**Integration Impact:**
- Main branch modifications to AbstractGuiGame will no longer conflict with network code
- Network functionality isolated in separate subclass hierarchy
- Local games unaffected by network implementation details

---

#### 3. `GameLobby.java` - ✅ DOCUMENTED

**Location**: Core game lobby implementation

**Status**: ✅ **Comprehensive inline documentation added** (as of latest commit)

**Current Changes:**
- Reordered game start sequence: `onGameStarted()` now called BEFORE `hostedMatch.startMatch()`
- Added public getter `getHostedMatch()`
- **Added detailed inline comments** explaining critical execution order for network games

**Documentation Added:**
```java
// ═══════════════════════════════════════════════════════════════════════════════
// CRITICAL EXECUTION ORDER FOR NETWORK GAMES
// ═══════════════════════════════════════════════════════════════════════════════
//
// onGameStarted() MUST be called BEFORE hostedMatch.startMatch()
//
// Why: Network games require GameSession to exist before sending initial state
//      packets to clients. The GameSession is created in onGameStarted() and
//      contains session credentials needed for reconnection support.
//
// Flow: 1. onGameStarted() → creates GameSession (network) or no-op (local)
//       2. hostedMatch.startMatch() → calls openView()
//       3. openView() → sends FullStatePacket with session credentials (network)
//
// Impact of reordering: Clients cannot reconnect (no session credentials sent)
//
// Local games: Unaffected (onGameStarted is typically a no-op for local play)
// ═══════════════════════════════════════════════════════════════════════════════
```

**Benefits:**
- Execution order requirement clearly documented inline
- Prevents accidental reordering during refactoring
- Explains impact for both network and local games
- Visual separators make critical section highly visible

**Integration Considerations:**
- Documentation prevents merge conflicts from accidental reordering
- Local games explicitly noted as unaffected
- Main branch developers can see network dependency immediately

---

### Interface Extensions

#### 4. `IGameController.java` (core interface)

**Current Changes:**
- Added 3 new methods: `ackSync()`, `requestResync()`, `reconnectRequest()`

**Characteristics:**
- Network protocol methods added to core game controller interface
- All implementations must provide these methods
- Non-network implementations use no-op stubs

**Integration Considerations:**
- Interface pollution (network methods in core interface)
- Any new IGameController implementations must include network methods
- Main branch interface evolution would need to account for network additions

---

#### 5. `IGuiGame.java` (core interface)

**Current Changes:**
- Added 8 new methods for delta sync and reconnection:
  - `applyDelta()`, `fullStateSync()`
  - `gamePaused()`, `gameResumed()`
  - `reconnectAccepted()`, `reconnectRejected()`
  - `setRememberedActions()`, `nextRememberedAction()`

**Characteristics:**
- Substantial interface extension (8 methods)
- Network protocol and lifecycle methods in core GUI interface
- All implementations must provide these methods

**Integration Considerations:**
- Interface bloat (core interface extended with domain-specific methods)
- Main branch GUI refactoring would need to account for network methods
- Single Responsibility Principle consideration (GUI interface handling network protocol)

---

### Localized Changes

#### 6. `PlayerZoneUpdate.java`
- Made 2 methods public: `addZone()`, `add()`
- Visibility widening only

#### 7. `StackItemView.java`
- Added network deserialization constructor
- Additive change, constructor overload pattern

#### 8. `PlayerControllerHuman.java`
- Added 3 no-op method implementations (required by IGameController)
- Stub implementations, no behavioral change

---

## Refactoring Options

The following refactoring approaches are available if the Main branch team determines that greater isolation of network functionality is needed. These are presented as options that could be implemented based on integration requirements and architectural preferences.

### Option 1: TrackableObject Isolation

**Approach**: Use package-private methods accessed through an adapter class instead of public visibility.

**Implementation:**

**Step 1: Revert Visibility Changes**

```java
// forge-game/src/main/java/forge/trackable/TrackableObject.java

// Change back to protected
protected final <T> void set(final TrackableProperty key, final T value) {
    // ... existing implementation unchanged
}

// Make delta sync methods package-private instead of public
final boolean hasChanges() {  // Remove 'public'
    return !changedProps.isEmpty();
}

final Set<TrackableProperty> getChangedProps() {  // Remove 'public'
    return Collections.unmodifiableSet(changedProps);
}

final void clearChanges() {  // Remove 'public'
    changedProps.clear();
}

final void serializeChangedOnly(final TrackableSerializer ts) {  // Remove 'public'
    // ... implementation
}

// Add package-private setter for network deserialization
final <T> void setForNetwork(final TrackableProperty key, final T value) {
    set(key, value);  // Delegates to protected set()
}
```

**Step 2: Create Network Accessor**

```java
// forge-game/src/main/java/forge/trackable/NetworkTrackableAccess.java
package forge.trackable;

/**
 * Package-private accessor for network serialization.
 * Bridges network code to TrackableObject without exposing public API.
 */
public final class NetworkTrackableAccess {

    private NetworkTrackableAccess() {}

    public static boolean hasChanges(TrackableObject obj) {
        return obj.hasChanges();
    }

    public static Set<TrackableProperty> getChangedProps(TrackableObject obj) {
        return obj.getChangedProps();
    }

    public static void clearChanges(TrackableObject obj) {
        obj.clearChanges();
    }

    public static void serializeChangedOnly(TrackableObject obj, TrackableSerializer ts) {
        obj.serializeChangedOnly(ts);
    }

    public static <T> void setProperty(TrackableObject obj, TrackableProperty key, T value) {
        obj.setForNetwork(key, value);
    }
}
```

**Step 3: Update Network Code**

```java
// forge-gui/src/main/java/forge/gamemodes/net/server/DeltaSyncManager.java

// Before:
if (obj.hasChanges()) {
    obj.serializeChangedOnly(ts);
}

// After:
if (NetworkTrackableAccess.hasChanges(obj)) {
    NetworkTrackableAccess.serializeChangedOnly(obj, ts);
}
```

**Files Affected:**
- `TrackableObject.java` - Revert visibility, add package-private methods
- `NetworkTrackableAccess.java` - New accessor class
- `DeltaSyncManager.java` - Use accessor
- `AbstractGuiGame.java` - Use accessor for property setting
- `NetworkPropertySerializer.java` - Use accessor

**Outcome:**
- TrackableObject.java restored to protected visibility
- Network functionality isolated to network package
- Core game module independent of network implementation

---

### Option 2: AbstractGuiGame Refactoring

**Approach**: Extract network logic to a NetworkGuiGame subclass.

**Implementation:**

**Step 1: Create NetworkGuiGame Subclass**

```java
// forge-gui/src/main/java/forge/gamemodes/match/NetworkGuiGame.java (NEW)
package forge.gamemodes.match;

/**
 * Extension of AbstractGuiGame with network delta sync support.
 * Handles network-specific logic without modifying base class.
 */
public class NetworkGuiGame extends AbstractGuiGame {

    @Override
    public void setGameView(final GameView gameView0) {
        // Network-specific tracker initialization
        // Move tracker management code here
    }

    @Override
    public void applyDelta(DeltaPacket packet) {
        // Move ~500 lines of delta application logic here
    }

    @Override
    public void fullStateSync(FullStatePacket packet) {
        // Move full state sync logic here
    }

    private void ensureTrackerInitialized(GameView gameView0) {
        // Move tracker initialization logic here
    }

    private void setTrackerRecursively(TrackableObject obj, Tracker tracker,
                                       java.util.Set<Integer> visited) {
        // Move recursive tracker setup here
    }

    private int computeStateChecksum(GameView gameView) {
        // Move checksum computation here
    }

    private void requestFullStateResync() {
        // Move resync request logic here
    }
}
```

**Step 2: Restore AbstractGuiGame**

```java
// forge-gui/src/main/java/forge/gamemodes/match/AbstractGuiGame.java

@Override
public void setGameView(final GameView gameView0) {
    // Restore to original simple implementation
    if (gameView == null || gameView0 == null) {
        if (gameView0 != null) {
            gameView0.updateObjLookup();
        }
        gameView = gameView0;
        return;
    }

    // Simple update - no network-specific tracker management
    gameView.copyChangedProps(gameView0);
}

// Remove network methods (moved to NetworkGuiGame)
```

**Step 3: Update NetGuiGame**

```java
// forge-gui/src/main/java/forge/gamemodes/net/server/NetGuiGame.java

// Before:
public class NetGuiGame extends AbstractGuiGame {

// After:
public class NetGuiGame extends NetworkGuiGame {
}
```

**Files Affected:**
- `NetworkGuiGame.java` - New network-specific subclass
- `AbstractGuiGame.java` - Restore to near-original state
- `NetGuiGame.java` - Change parent class

**Outcome:**
- AbstractGuiGame mostly restored
- Network logic clearly separated via inheritance
- Local games use AbstractGuiGame directly
- Network games use NetworkGuiGame subclass

---

### Option 3: Interface Segregation

**Approach**: Create network-specific interfaces that extend core interfaces.

**Implementation:**

**Step 1: Create Network Interfaces**

```java
// forge-gui/src/main/java/forge/interfaces/INetworkGameController.java (NEW)
package forge.interfaces;

/**
 * Network-specific extension of IGameController.
 */
public interface INetworkGameController extends IGameController {
    void ackSync(long sequenceNumber);
    void requestResync();
    void reconnectRequest(String sessionId, String token);
}
```

```java
// forge-gui/src/main/java/forge/gui/interfaces/INetworkGuiGame.java (NEW)
package forge.gui.interfaces;

/**
 * Network-specific extension of IGuiGame.
 */
public interface INetworkGuiGame extends IGuiGame {
    void applyDelta(DeltaPacket packet);
    void fullStateSync(FullStatePacket packet);
    void gamePaused(String message);
    void gameResumed();
    void reconnectAccepted(FullStatePacket packet);
    void reconnectRejected(String reason);
    void setRememberedActions();
    void nextRememberedAction();
}
```

**Step 2: Restore Core Interfaces**

```java
// forge-gui/src/main/java/forge/interfaces/IGameController.java
public interface IGameController {
    // ... existing methods ...
    void reorderHand(CardView card, int index);
    // Remove network methods
}
```

**Step 3: Update Implementations**

```java
// forge-gui/src/main/java/forge/gamemodes/net/client/NetGameController.java
public class NetGameController implements INetworkGameController {
    // Already implements the network methods
}

// forge-gui/src/main/java/forge/player/PlayerControllerHuman.java
public class PlayerControllerHuman extends PlayerController implements IGameController {
    // Remove no-op network method stubs
}
```

**Files Affected:**
- `INetworkGameController.java` - New interface
- `INetworkGuiGame.java` - New interface
- `IGameController.java` - Remove network methods
- `IGuiGame.java` - Remove network methods
- `NetGameController.java` - Implement network interface
- `NetworkGuiGame.java` - Implement network interface
- `PlayerControllerHuman.java` - Remove no-op stubs

**Outcome:**
- Core interfaces clean
- Type safety improved
- Clear separation between core and network concerns
- No no-op stubs in non-network implementations

---

### Option 4: GameLobby Documentation

**Approach**: Add comprehensive documentation explaining the execution order dependency.

**Implementation:**

```java
// forge-gui/src/main/java/forge/gamemodes/match/GameLobby.java

return () -> {
    // ═══════════════════════════════════════════════════════════════════
    // CRITICAL EXECUTION ORDER FOR NETWORK GAMES
    // ═══════════════════════════════════════════════════════════════════
    //
    // onGameStarted() MUST be called BEFORE hostedMatch.startMatch()
    //
    // Reason: Network games require GameSession to exist before sending
    //         initial state packets to clients. GameSession is created
    //         in onGameStarted().
    //
    // Flow: 1. onGameStarted() → creates GameSession
    //       2. hostedMatch.startMatch() → calls openView()
    //       3. openView() → sends FullStatePacket with session credentials
    //
    // Impact of reordering: Clients cannot reconnect (no credentials sent)
    //
    // Local games: Unaffected (onGameStarted typically no-op)
    //
    // See: NetworkPlay.md#reconnection-support
    //      RefactorOptions.md for details
    //
    // DO NOT REORDER without testing network reconnection!
    // ═══════════════════════════════════════════════════════════════════

    onGameStarted();  // MUST be first

    hostedMatch = GuiBase.getInterface().hostMatch();
    hostedMatch.startMatch(GameType.Constructed, variantTypes, players, guis);
    // ...
};
```

**Files Affected:**
- `GameLobby.java` - Add extensive comments

**Outcome:**
- Documents the timing dependency
- Prevents accidental reordering
- Explains rationale for future developers

---

## Architecture Comparison

### Current Architecture
```
Core Game Engine (forge-game)
└── TrackableObject [PUBLIC network methods]

Core GUI (forge-gui)
├── AbstractGuiGame [846 lines of network code]
├── IGameController [3 network methods]
└── IGuiGame [8 network methods]

Network Implementation
├── NetGuiGame extends AbstractGuiGame
├── NetGameController implements IGameController
└── DeltaSyncManager uses TrackableObject.public methods
```

### Refactored Architecture (If Options Implemented)
```
Core Game Engine (forge-game)
└── TrackableObject [PROTECTED, package-private for network]
    └── NetworkTrackableAccess [Accessor for network code]

Core GUI (forge-gui)
├── AbstractGuiGame [Minimal changes]
│   └── NetworkGuiGame [Network-specific subclass]
├── IGameController [0 network methods]
│   └── INetworkGameController [3 network methods]
└── IGuiGame [0 network methods]
    └── INetworkGuiGame [8 network methods]

Network Implementation
├── NetGuiGame extends NetworkGuiGame
├── NetGameController implements INetworkGameController
└── DeltaSyncManager uses NetworkTrackableAccess
```

---

## Implementation Checklist

If refactoring is pursued, the following checklist outlines the implementation steps:

### Phase 1: Foundation
- [ ] Create `NetworkTrackableAccess.java`
- [ ] Create `INetworkGameController.java`
- [ ] Create `INetworkGuiGame.java`
- [ ] Write unit tests for new classes
- [ ] Verify compilation

### Phase 2: TrackableObject Isolation
- [ ] Update `DeltaSyncManager.java` to use `NetworkTrackableAccess`
- [ ] Update `AbstractGuiGame.java` to use accessor
- [ ] Update `NetworkPropertySerializer.java` to use accessor
- [ ] Revert `TrackableObject.set()` to protected
- [ ] Make TrackableObject delta methods package-private
- [ ] Run regression tests

### Phase 3: AbstractGuiGame Refactoring
- [ ] Create `NetworkGuiGame.java` class
- [ ] Move network methods from `AbstractGuiGame` to `NetworkGuiGame`
- [ ] Restore `AbstractGuiGame.setGameView()` to simple version
- [ ] Update `NetGuiGame` to extend `NetworkGuiGame`
- [ ] Run integration tests

### Phase 4: Interface Segregation
- [ ] Update `NetGameController` to implement `INetworkGameController`
- [ ] Update `NetworkGuiGame` to implement `INetworkGuiGame`
- [ ] Remove network methods from `IGameController`
- [ ] Remove network methods from `IGuiGame`
- [ ] Remove no-op stubs from `PlayerControllerHuman`
- [ ] Update protocol handlers
- [ ] Verify all tests pass

### Phase 5: Documentation
- [ ] Add extensive comments to `GameLobby.java`
- [ ] Update `NetworkPlay.md`
- [ ] Final regression test suite

---

## Testing Considerations

If refactoring is implemented, the following testing approach would apply:

### Unit Tests
- TrackableObject behavior unchanged after reverting visibility
- NetworkTrackableAccess delegates correctly
- Interface implementations compile and satisfy contracts

### Integration Tests
- Full network game flow (start → play → reconnect → end)
- Delta sync bandwidth verification (~99% savings maintained)
- Disconnection/reconnection flow
- Checksum validation and auto-resync

### Regression Tests
- Local games completely unaffected
- No performance degradation
- All existing NetworkPlay tests pass
- All Main branch tests pass (if available)

---

## Summary

The NetworkPlay branch modifies 8 core files to support delta synchronization and reconnection features. These modifications are deeply integrated with game state management, making complete isolation challenging.

This document provides four refactoring options that could be considered if the Main branch development team determines that greater separation between network and core functionality is needed:

1. **TrackableObject Isolation** - Package-private access pattern
2. **AbstractGuiGame Refactoring** - Extract to NetworkGuiGame subclass
3. **Interface Segregation** - Separate network and core interfaces
4. **GameLobby Documentation** - Document timing dependencies

These options are provided for consideration and may be implemented based on the Main branch team's assessment of integration requirements, development timeline, and architectural preferences.
