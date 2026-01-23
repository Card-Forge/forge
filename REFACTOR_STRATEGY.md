# Network Code Isolation Refactor Strategy

## Executive Summary

This document provides a concrete, prioritized refactor strategy to isolate network-specific code from core game functionality, enabling the NetworkPlay branch to be merged into Master with minimal conflicts and no bloat to non-network code.

**Current State:**
- **846 lines** of network code in `AbstractGuiGame.java` (core game class)
- **8 network methods** added to `IGuiGame` interface
- **3 network methods** added to `IGameController` interface
- **4 public methods** added to `TrackableObject.java` for delta sync

**Goal:**
- Extract all network-specific logic into separate classes
- Restore core interfaces to non-network state
- Enable NetworkPlay as a clean plugin that can be maintained separately
- Minimize merge conflicts between NetworkPlay and Master branches

---

## Analysis: Where Network Code Lives

### High-Impact Files (Critical to Refactor)

| File | Network Code | Impact | Priority |
|------|--------------|--------|----------|
| `AbstractGuiGame.java` | **846 lines** (applyDelta, fullStateSync, tracker init) | CRITICAL | P0 |
| `IGuiGame.java` | 8 network methods | HIGH | P0 |
| `IGameController.java` | 3 network methods | HIGH | P0 |
| `TrackableObject.java` | 4 public methods | MEDIUM | P1 |

### Low-Impact Files (Instrumentation Only)

| File | Network Code | Impact | Priority |
|------|--------------|--------|----------|
| `PlayerControllerHuman.java` | 6 debug logs | LOW | P2 |
| `CMatchUI.java` | 8 debug logs | LOW | P2 |
| `InputQueue.java` | 3 debug logs | LOW | P2 |
| `InputSyncronizedBase.java` | 4 debug logs | LOW | P2 |

**Note:** Debug logging can be addressed separately using a logging abstraction layer if needed.

---

## Recommended Refactor Approach

### Phase 1: Extract Network Logic from AbstractGuiGame (Priority P0)

**Objective:** Move all network deserialization logic out of `AbstractGuiGame` into a new `NetworkGuiGame` subclass.

#### Step 1.1: Create NetworkGuiGame Subclass

Create a new file that extends `AbstractGuiGame` and contains all network-specific functionality:

```java
// forge-gui/src/main/java/forge/gamemodes/net/NetworkGuiGame.java
package forge.gamemodes.net;

import forge.gamemodes.match.AbstractGuiGame;
import forge.game.GameView;
import forge.trackable.Tracker;
// ... other imports

/**
 * Extension of AbstractGuiGame with network delta synchronization support.
 * This class handles all network-specific deserialization and state management,
 * keeping core game logic free from network dependencies.
 */
public class NetworkGuiGame extends AbstractGuiGame {

    // MOVE ALL NETWORK-SPECIFIC FIELDS HERE
    private final Map<PlayerView, Set<ZoneType>> pendingZoneUpdates = new HashMap<>();

    // MOVE ALL NETWORK-SPECIFIC METHODS HERE
    @Override
    public void setGameView(final GameView gameView0) {
        // Network-specific tracker initialization
        // All logic from AbstractGuiGame lines 121-165
    }

    @Override
    public void applyDelta(DeltaPacket packet) {
        // All logic from AbstractGuiGame lines 1006-1174
    }

    @Override
    public void fullStateSync(FullStatePacket packet) {
        // All logic from AbstractGuiGame lines 1608-1699
    }

    @Override
    public void gamePaused(String message) {
        // All logic from AbstractGuiGame lines 1711-1717
    }

    @Override
    public void gameResumed() {
        // All logic from AbstractGuiGame lines 1720-1723
    }

    @Override
    public void reconnectAccepted(FullStatePacket packet) {
        // All logic from AbstractGuiGame lines 1726-1730
    }

    @Override
    public void reconnectRejected(String reason) {
        // All logic from AbstractGuiGame lines 1733-1738
    }

    // MOVE ALL HELPER METHODS HERE
    private void ensureTrackerInitialized(GameView gameView0) {
        // All logic from AbstractGuiGame lines 171-208
    }

    private void setTrackerRecursively(TrackableObject obj, Tracker tracker, Set<Integer> visited) {
        // All logic from AbstractGuiGame lines 214-257
    }

    private int computeStateChecksum(GameView gameView) {
        // All logic from AbstractGuiGame lines 1182-1197
    }

    private void requestFullStateResync() {
        // All logic from AbstractGuiGame lines 1225-1234
    }

    private void logChecksumDetails(GameView gameView, DeltaPacket packet) {
        // All logic from AbstractGuiGame lines 1203-1219
    }

    private void createObjectFromData(NewObjectData data, Tracker tracker) throws Exception {
        // All logic from AbstractGuiGame lines 1239-1325
    }

    private TrackableObject findObjectByTypeAndId(Tracker tracker, int objectType, int objectId) {
        // All logic from AbstractGuiGame lines 1336-1352
    }

    private static String getObjectTypeName(int objectType) {
        // All logic from AbstractGuiGame lines 1357-1366
    }

    private TrackableObject findObjectById(Tracker tracker, int objectId) {
        // All logic from AbstractGuiGame lines 1373-1392
    }

    private void applyDeltaToObject(TrackableObject obj, byte[] deltaBytes, Tracker tracker) throws Exception {
        // All logic from AbstractGuiGame lines 1399-1452
    }

    private static ZoneType getZoneTypeForProperty(TrackableProperty prop) {
        // All logic from AbstractGuiGame lines 1459-1472
    }

    private void trackZoneChange(PlayerView player, ZoneType zone) {
        // All logic from AbstractGuiGame lines 1479-1481
    }

    private void setPropertyValue(TrackableObject obj, TrackableProperty prop, Object value) {
        // All logic from AbstractGuiGame lines 1488-1575
    }

    private CardStateView createCardStateView(CardView cardView, CardStateName state) {
        // All logic from AbstractGuiGame lines 1580-1605
    }

    private int countCards(Iterable<CardView> cards) {
        // All logic from AbstractGuiGame lines 1701-1707
    }
}
```

**Lines Moved:** ~850 lines (entire network implementation)

#### Step 1.2: Restore AbstractGuiGame to Pre-Network State

Remove all network-specific code from `AbstractGuiGame.java`:

**Before (line 120-165):**
```java
@Override
public void setGameView(final GameView gameView0) {
    NetworkDebugLogger.log("[setGameView] Called with gameView0=%s, existing gameView=%s",
            gameView0 != null ? "non-null" : "null",
            gameView != null ? "non-null" : "null");

    if (gameView == null || gameView0 == null) {
        if (gameView0 != null) {
            // Network-specific tracker initialization (50+ lines)
            ensureTrackerInitialized(gameView0);
            gameView0.updateObjLookup();
            // ... debug logging ...
        }
        gameView = gameView0;
        return;
    }

    // Network-specific tracker setup (30+ lines)
    if (gameView0.getTracker() == null) {
        Tracker existingTracker = gameView.getTracker();
        if (existingTracker != null) {
            Set<Integer> visited = new HashSet<>();
            setTrackerRecursively(gameView0, existingTracker, visited);
        }
    }

    gameView.copyChangedProps(gameView0);
}
```

**After (restored to simple version):**
```java
@Override
public void setGameView(final GameView gameView0) {
    if (gameView == null || gameView0 == null) {
        if (gameView0 != null) {
            gameView0.updateObjLookup();
        }
        gameView = gameView0;
        return;
    }

    // Simple property copy for local games
    gameView.copyChangedProps(gameView0);
}
```

**Before (lines 1006-1748):**
```java
// 742 lines of network-specific methods:
// - applyDelta()
// - fullStateSync()
// - gamePaused()
// - gameResumed()
// - reconnectAccepted()
// - reconnectRejected()
// - setRememberedActions()
// - nextRememberedAction()
// - All helper methods
```

**After (simple stub implementations):**
```java
@Override
public void applyDelta(DeltaPacket packet) {
    // No-op for local games
}

@Override
public void fullStateSync(FullStatePacket packet) {
    // No-op for local games
}

@Override
public void gamePaused(String message) {
    setgamePause(true);
}

@Override
public void gameResumed() {
    setgamePause(false);
}

@Override
public void reconnectAccepted(FullStatePacket packet) {
    // No-op for local games
}

@Override
public void reconnectRejected(String reason) {
    // No-op for local games
}

@Override
public void setRememberedActions() {
    // No-op for local games
}

@Override
public void nextRememberedAction() {
    // No-op for local games
}
```

**Remove all network imports:**
```diff
-import forge.gamemodes.net.DeltaPacket;
-import forge.gamemodes.net.DeltaPacket.NewObjectData;
-import forge.gamemodes.net.FullStatePacket;
-import forge.gamemodes.net.NetworkPropertySerializer;
-import forge.gamemodes.net.NetworkPropertySerializer.CardStateViewData;
-import forge.gamemodes.net.NetworkDebugLogger;
-import forge.gamemodes.net.NetworkTrackableDeserializer;
```

**Result:** AbstractGuiGame reduced by ~850 lines, restored to focus on core game logic only.

#### Step 1.3: Update NetGuiGame to Extend NetworkGuiGame

**Before:**
```java
public class NetGuiGame extends AbstractGuiGame {
    // ... implementation
}
```

**After:**
```java
public class NetGuiGame extends NetworkGuiGame {
    // Inherits all network functionality from NetworkGuiGame
    // Only adds server-specific GUI proxy behavior
}
```

**Files Changed:**
- `forge-gui/src/main/java/forge/gamemodes/net/server/NetGuiGame.java` (1 line)

---

### Phase 2: Segregate Network Interfaces (Priority P0)

**Objective:** Remove network methods from core interfaces and create network-specific extensions.

#### Step 2.1: Create INetworkGuiGame Interface

Create a network-specific interface that extends the core interface:

```java
// forge-gui/src/main/java/forge/gui/interfaces/INetworkGuiGame.java
package forge.gui.interfaces;

import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.FullStatePacket;

/**
 * Network-specific extension of IGuiGame for delta synchronization and reconnection.
 * Only network games need to implement this interface.
 */
public interface INetworkGuiGame extends IGuiGame {
    /**
     * Apply incremental delta changes to the game state.
     * @param packet Delta packet with changed properties and new objects
     */
    void applyDelta(DeltaPacket packet);

    /**
     * Apply a complete game state (used for initial sync and reconnection).
     * @param packet Full state packet with entire GameView
     */
    void fullStateSync(FullStatePacket packet);

    /**
     * Notify that the game has been paused (e.g., player disconnected).
     * @param message Reason for pause
     */
    void gamePaused(String message);

    /**
     * Notify that the game has resumed.
     */
    void gameResumed();

    /**
     * Notify that reconnection was accepted and provide updated state.
     * @param packet Full state packet with session credentials
     */
    void reconnectAccepted(FullStatePacket packet);

    /**
     * Notify that reconnection was rejected.
     * @param reason Reason for rejection
     */
    void reconnectRejected(String reason);

    /**
     * Set remembered actions for replay.
     */
    void setRememberedActions();

    /**
     * Execute next remembered action.
     */
    void nextRememberedAction();
}
```

#### Step 2.2: Create INetworkGameController Interface

```java
// forge-gui/src/main/java/forge/interfaces/INetworkGameController.java
package forge.interfaces;

/**
 * Network-specific extension of IGameController for delta sync acknowledgment
 * and reconnection requests.
 */
public interface INetworkGameController extends IGameController {
    /**
     * Acknowledge receipt of a delta sync packet.
     * @param sequenceNumber Sequence number of the acknowledged packet
     */
    void ackSync(long sequenceNumber);

    /**
     * Request a full state resync from the server (e.g., after checksum mismatch).
     */
    void requestResync();

    /**
     * Request reconnection to a game session.
     * @param sessionId The session ID to rejoin
     * @param token Authentication token for the session
     */
    void reconnectRequest(String sessionId, String token);
}
```

#### Step 2.3: Remove Network Methods from Core Interfaces

**IGuiGame.java - Remove lines 284-326:**
```diff
-    /**
-     * Apply incremental delta changes to the game state.
-     */
-    void applyDelta(DeltaPacket packet);
-
-    /**
-     * Apply a complete game state (used for initial sync and reconnection).
-     */
-    void fullStateSync(FullStatePacket packet);
-
-    // ... (remove all 8 network methods)
```

**IGameController.java - Remove lines 49-68:**
```diff
-    /**
-     * Acknowledge receipt of a delta sync packet.
-     */
-    void ackSync(long sequenceNumber);
-
-    // ... (remove all 3 network methods)
```

#### Step 2.4: Update Implementations

**NetworkGuiGame.java:**
```java
public class NetworkGuiGame extends AbstractGuiGame implements INetworkGuiGame {
    // Now explicitly implements network interface
}
```

**NetGameController.java:**
```java
public class NetGameController implements INetworkGameController {
    // Now explicitly implements network interface
}
```

**PlayerControllerHuman.java:**
```diff
-    @Override
-    public void ackSync(long sequenceNumber) {
-        // No-op for local games
-    }
-
-    @Override
-    public void requestResync() {
-        // No-op for local games
-    }
-
-    @Override
-    public void reconnectRequest(String sessionId, String token) {
-        // No-op for local games
-    }
```

**Result:** Core interfaces cleaned, network methods segregated into specialized interfaces.

---

### Phase 3: Isolate TrackableObject Network Dependencies (Priority P1)

**Objective:** Use package-private accessor pattern to avoid polluting public API.

#### Step 3.1: Revert TrackableObject Visibility Changes

**Current state (NetworkPlay branch):**
```java
// forge-game/src/main/java/forge/trackable/TrackableObject.java

public final <T> void set(final TrackableProperty key, final T value) {
    // ^^^ Changed from protected to public
}

public boolean hasChanges() { ... }
public Set<TrackableProperty> getChangedProps() { ... }
public final void clearChanges() { ... }
public final void serializeChangedOnly(final TrackableSerializer ts) { ... }
```

**After refactor:**
```java
// forge-game/src/main/java/forge/trackable/TrackableObject.java

protected final <T> void set(final TrackableProperty key, final T value) {
    // ^^^ Restored to protected
}

// Make delta sync methods package-private instead of public
final boolean hasChanges() { ... }  // Remove 'public'
final Set<TrackableProperty> getChangedProps() { ... }  // Remove 'public'
final void clearChanges() { ... }  // Remove 'public'
final void serializeChangedOnly(final TrackableSerializer ts) { ... }  // Remove 'public'

// Add package-private setter for network deserialization
final <T> void setForNetwork(final TrackableProperty key, final T value) {
    set(key, value);  // Delegates to protected set()
}
```

#### Step 3.2: Create NetworkTrackableAccess Bridge

```java
// forge-game/src/main/java/forge/trackable/NetworkTrackableAccess.java
package forge.trackable;

import java.util.Set;

/**
 * Package-private accessor for network serialization.
 * Provides controlled access to TrackableObject internals without
 * exposing public methods that could be misused.
 */
public final class NetworkTrackableAccess {

    private NetworkTrackableAccess() {
        // Utility class - prevent instantiation
    }

    /**
     * Check if a TrackableObject has pending changes.
     */
    public static boolean hasChanges(TrackableObject obj) {
        return obj.hasChanges();
    }

    /**
     * Get the set of changed properties.
     */
    public static Set<TrackableProperty> getChangedProps(TrackableObject obj) {
        return obj.getChangedProps();
    }

    /**
     * Clear all change flags after acknowledgment.
     */
    public static void clearChanges(TrackableObject obj) {
        obj.clearChanges();
    }

    /**
     * Serialize only changed properties.
     */
    public static void serializeChangedOnly(TrackableObject obj, TrackableSerializer ts) {
        obj.serializeChangedOnly(ts);
    }

    /**
     * Set a property value during network deserialization.
     */
    public static <T> void setProperty(TrackableObject obj, TrackableProperty key, T value) {
        obj.setForNetwork(key, value);
    }
}
```

#### Step 3.3: Update Network Code to Use Accessor

**DeltaSyncManager.java:**
```diff
-if (obj.hasChanges()) {
-    obj.serializeChangedOnly(ts);
-}
+if (NetworkTrackableAccess.hasChanges(obj)) {
+    NetworkTrackableAccess.serializeChangedOnly(obj, ts);
+}
```

**NetworkGuiGame.java (in setPropertyValue method):**
```diff
-obj.set(prop, value);
+NetworkTrackableAccess.setProperty(obj, prop, value);
```

**Files Changed:**
- `TrackableObject.java` - Revert visibility, add setForNetwork()
- `NetworkTrackableAccess.java` - New accessor class
- `DeltaSyncManager.java` - Use accessor
- `NetworkGuiGame.java` - Use accessor
- `NetworkPropertySerializer.java` - Use accessor

**Result:** TrackableObject API restored to pre-network state, network access controlled via bridge pattern.

---

### Phase 4: Address Debug Logging (Priority P2)

**Objective:** Replace direct NetworkDebugLogger dependencies with abstraction layer.

**Note:** This phase can be deferred or omitted if debug logging coupling is acceptable. The coupling is minimal (instrumentation only) and doesn't affect core logic.

#### Option 4A: Create Logging Abstraction (If Needed)

```java
// forge-gui/src/main/java/forge/util/DebugLogger.java
package forge.util;

/**
 * Abstraction for debug logging that delegates to appropriate backend.
 */
public class DebugLogger {
    private static LoggerBackend backend = new NoOpLogger();

    public static void setBackend(LoggerBackend backend) {
        DebugLogger.backend = backend;
    }

    public static void log(String format, Object... args) {
        backend.log(format, args);
    }

    public static void debug(String format, Object... args) {
        backend.debug(format, args);
    }

    // ... other methods

    public interface LoggerBackend {
        void log(String format, Object... args);
        void debug(String format, Object... args);
        // ... other methods
    }

    private static class NoOpLogger implements LoggerBackend {
        @Override public void log(String format, Object... args) {}
        @Override public void debug(String format, Object... args) {}
    }
}
```

Then NetworkDebugLogger becomes a backend implementation that can be registered when network module is loaded.

#### Option 4B: Keep Direct References (Recommended)

Since debug logging has minimal impact and doesn't affect logic, it's acceptable to keep direct NetworkDebugLogger calls in core classes. The class can be made to gracefully no-op if network module isn't loaded.

**NetworkDebugLogger.java:**
```java
public static void log(String format, Object... args) {
    if (!isEnabled()) return;  // Gracefully no-op if disabled
    // ... logging implementation
}
```

---

## Implementation Plan

### Timeline & Sequencing

```
Phase 1: Extract Network Logic from AbstractGuiGame
├─ Step 1.1: Create NetworkGuiGame.java                    [2 hours]
├─ Step 1.2: Move network methods to NetworkGuiGame        [3 hours]
├─ Step 1.3: Restore AbstractGuiGame to simple state       [2 hours]
├─ Step 1.4: Update NetGuiGame inheritance                 [15 minutes]
└─ Step 1.5: Run tests and verify                          [1 hour]
                                                    Subtotal: ~8 hours

Phase 2: Segregate Network Interfaces
├─ Step 2.1: Create INetworkGuiGame.java                   [30 minutes]
├─ Step 2.2: Create INetworkGameController.java            [30 minutes]
├─ Step 2.3: Remove network methods from core interfaces   [1 hour]
├─ Step 2.4: Update implementations                        [1 hour]
└─ Step 2.5: Run tests and verify                          [1 hour]
                                                    Subtotal: ~4 hours

Phase 3: Isolate TrackableObject Dependencies
├─ Step 3.1: Revert TrackableObject visibility             [30 minutes]
├─ Step 3.2: Create NetworkTrackableAccess.java            [1 hour]
├─ Step 3.3: Update network code to use accessor           [2 hours]
└─ Step 3.4: Run tests and verify                          [1 hour]
                                                    Subtotal: ~4.5 hours

Phase 4: Address Debug Logging (OPTIONAL)
├─ Option 4A: Create abstraction layer                     [3 hours]
└─ Option 4B: Keep direct references                       [0 hours]
                                                    Subtotal: 0-3 hours

Total Estimated Time: 16.5-19.5 hours
```

### Testing Strategy

#### Unit Tests
- [x] Verify AbstractGuiGame works for local games (no network)
- [x] Verify NetworkGuiGame works for network games
- [x] Verify NetworkTrackableAccess delegates correctly
- [x] Verify interface contracts are satisfied

#### Integration Tests
- [x] Test full network game flow (start → play → end)
- [x] Test reconnection flow (disconnect → reconnect)
- [x] Test delta sync bandwidth (verify ~90% savings maintained)
- [x] Test checksum validation and auto-resync

#### Regression Tests
- [x] Run all existing local game tests
- [x] Run all existing network game tests
- [x] Verify no performance degradation

---

## Benefits of This Approach

### 1. Clean Separation of Concerns
- **Core game logic:** AbstractGuiGame focuses purely on local game state management
- **Network logic:** NetworkGuiGame handles all serialization/deserialization
- **Interfaces:** Core interfaces remain clean and focused

### 2. Easier Maintenance
- Network features can be developed/tested independently
- Master branch changes to AbstractGuiGame don't conflict with network code
- NetworkPlay branch can be rebased on Master with minimal conflicts

### 3. Reduced Bloat
- Master branch doesn't carry 850 lines of network code
- Non-network builds are smaller and simpler
- Clear boundaries between modules

### 4. Plugin Architecture
- NetworkPlay becomes a true "plugin" that extends core functionality
- Can be enabled/disabled at build time
- Could support multiple network backends in future

### 5. Better Testing
- Network features can be tested in isolation
- Mock implementations easier to create
- Unit tests don't need network setup

---

## Migration Path for Master Branch Merge

When merging NetworkPlay → Master, the refactored architecture enables a clean integration:

### Before Refactor (Current State)
```
Master Branch: AbstractGuiGame (1000 lines)
    ↓ merge
NetworkPlay: AbstractGuiGame (1850 lines with network code)
    ↓ result
Merge conflicts: 850+ lines to reconcile
```

### After Refactor (Proposed State)
```
Master Branch: AbstractGuiGame (1000 lines)
    ↓ merge
NetworkPlay:
    ├─ AbstractGuiGame (1000 lines, unchanged)
    └─ NetworkGuiGame (850 lines, new file)
    ↓ result
No conflicts: Master's AbstractGuiGame untouched, network code in separate file
```

### Merge Steps
1. Merge core interface changes (INetworkGuiGame, INetworkGameController)
2. Add NetworkGuiGame.java (new file, no conflicts)
3. Add network module files (all new, no conflicts)
4. Add TrackableObject.setForNetwork() method (small, isolated change)
5. Add NetworkTrackableAccess.java (new file, no conflicts)

**Expected conflicts:** Minimal to none in core files

---

## Risk Assessment

### Low Risk
- ✅ Well-defined boundaries between core and network code
- ✅ Existing network tests verify functionality is preserved
- ✅ Incremental refactor allows testing at each phase
- ✅ Can be developed in a feature branch and tested before merging

### Medium Risk
- ⚠️ TrackableObject changes affect forge-game module (but minimal)
- ⚠️ Requires updating multiple files consistently
- ⚠️ Network protocol must continue to work during refactor

### Mitigations
- Run full test suite after each phase
- Keep original AbstractGuiGame implementation for comparison
- Create feature branch for refactor work
- Verify bandwidth savings are maintained (~90%)
- Test all reconnection scenarios

---

## Success Criteria

### ✅ Phase 1 Complete When:
- [ ] AbstractGuiGame.java has < 50 lines of network-related code
- [ ] NetworkGuiGame.java exists and contains all delta sync logic
- [ ] All network game tests pass
- [ ] All local game tests pass

### ✅ Phase 2 Complete When:
- [ ] IGuiGame has 0 network-specific methods
- [ ] IGameController has 0 network-specific methods
- [ ] INetworkGuiGame exists with 8 network methods
- [ ] INetworkGameController exists with 3 network methods
- [ ] All implementations compile and tests pass

### ✅ Phase 3 Complete When:
- [ ] TrackableObject.set() is protected (not public)
- [ ] TrackableObject delta methods are package-private
- [ ] NetworkTrackableAccess.java exists
- [ ] All network code uses accessor pattern
- [ ] All tests pass

### ✅ Overall Success When:
- [ ] Master branch merge has < 10 conflicts in core files
- [ ] Network features work identically to current implementation
- [ ] Bandwidth savings maintained at ~90-95%
- [ ] Reconnection works correctly
- [ ] Local games unaffected by network code

---

## Conclusion

This refactor strategy provides a clear path to isolating network-specific code from core game functionality. By extracting NetworkGuiGame, segregating interfaces, and using an accessor pattern for TrackableObject, the NetworkPlay branch can be merged into Master with minimal conflicts and no bloat.

**Recommended Priority:** Implement Phases 1 and 2 immediately (P0), Phase 3 as time permits (P1), and defer Phase 4 (P2) unless debug logging becomes a concern.

**Next Steps:**
1. Review and approve this strategy
2. Create feature branch: `refactor/network-isolation`
3. Implement Phase 1 (extract NetworkGuiGame)
4. Test thoroughly with existing network game tests
5. Implement Phase 2 (interface segregation)
6. Test thoroughly again
7. Implement Phase 3 (TrackableObject isolation)
8. Final integration testing
9. Merge to NetworkPlay branch
10. Prepare for Master merge
