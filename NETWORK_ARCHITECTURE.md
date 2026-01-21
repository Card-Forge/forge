# NetworkPlay Architectural Analysis & Refactoring Guide

## Executive Summary

The NetworkPlay branch contains **significant architectural changes to core non-network classes** that pose moderate to high risk of merge conflicts with the Main branch. This document analyzes these changes and provides a detailed refactoring strategy to isolate network-specific functionality from core game logic.

**Current State:**
- 8 core files modified (5 high/medium risk)
- ~846 lines added to AbstractGuiGame.java alone
- Core interfaces extended with 11 network methods
- **Estimated merge conflict probability: 60-80%**

**Proposed Refactoring:**
- Isolate network functionality using adapter and inheritance patterns
- Restore core classes to near-original state
- **Projected merge conflict probability after refactoring: 10-20%**

---

## Architectural Overlap Analysis

### 🔴 HIGH RISK - Core Architecture Changes

#### 1. `TrackableObject.java` (forge-game module)

**Location**: Core game engine module
**Current Changes:**
- Changed `set()` method visibility: `protected` → `public`
- Added 4 new public methods: `hasChanges()`, `getChangedProps()`, `clearChanges()`, `serializeChangedOnly()`

**Why This Is Problematic:**
- **Module boundary violation**: Network-specific functionality in core game module
- **Visibility change**: Could enable new usage patterns in Main branch that conflict with delta sync assumptions
- **Name collision risk**: Main branch could add protected methods with same names
- **Performance implications**: Change tracking adds overhead to all property modifications

**Impact if Unchanged:**
- Any Main branch changes to TrackableObject will require careful merge
- Core game engine couples to network implementation details
- Future refactoring becomes increasingly difficult

**Conflict Scenarios:**
```java
// Main branch might add:
protected void clearChanges() { /* different implementation */ }

// NetworkPlay already has:
public final void clearChanges() { changedProps.clear(); }
// → Compilation error on merge
```

---

#### 2. `AbstractGuiGame.java` (forge-gui module)

**Location**: Core GUI game implementation
**Current Changes:**
- ~846 lines of modifications (largest single-file change)
- Added `applyDelta()` method (~500 lines of network deserialization logic)
- Added `fullStateSync()` method
- Modified `setGameView()` with network-specific tracker initialization
- Added 4 helper methods: `ensureTrackerInitialized()`, `setTrackerRecursively()`, `computeStateChecksum()`, `requestFullStateResync()`

**Why This Is Problematic:**
- **setGameView() modification**: Changes fundamental game state initialization that local games also use
- **Code footprint**: Large amount of network code increases line-level conflict probability
- **Tight coupling**: Network delta application logic embedded in base GUI class
- **Maintenance burden**: Future developers must understand network protocol to modify GUI

**Impact if Unchanged:**
- Very high likelihood of merge conflicts if Main modifies game state management
- Local game performance potentially affected by network code paths
- Testing complexity increases (all GUI tests must consider network scenarios)

**Conflict Scenarios:**
```java
// Main branch modifies setGameView() for new feature:
public void setGameView(final GameView gameView0) {
    // Add new initialization logic here
    gameView = gameView0;
}

// NetworkPlay has complex tracker management:
public void setGameView(final GameView gameView0) {
    if (gameView0.getTracker() == null) {
        // 50+ lines of tracker initialization
    }
    // ... existing logic
}
// → Complex merge conflict requiring understanding of both features
```

---

#### 3. `GameLobby.java`

**Location**: Core game lobby implementation
**Current Changes:**
- Reordered game start sequence: `onGameStarted()` now called BEFORE `hostedMatch.startMatch()`
- Added public getter `getHostedMatch()`

**Why This Is Problematic:**
- **Execution order change**: Subtle behavioral modification that affects all game types
- **Timing dependency**: Network games require session credentials before match starts
- **Non-obvious**: Change isn't immediately apparent from code inspection
- **Race condition risk**: Other features may assume original execution order

**Impact if Unchanged:**
- Main branch features depending on original sequence could break
- Difficult to debug issues (timing-related bugs are subtle)
- Merge conflicts if Main also modifies game start sequence

**Conflict Scenarios:**
```java
// Original order (Main branch expects):
hostedMatch.startMatch();
onGameStarted();

// NetworkPlay requires:
onGameStarted();  // Must create session first
hostedMatch.startMatch();

// If Main adds logic between these calls → conflict
```

---

### 🟡 MEDIUM RISK - Interface Extensions

#### 4. `IGameController.java` (core interface)

**Current Changes:**
- Added 3 new methods: `ackSync()`, `requestResync()`, `reconnectRequest()`

**Why This Is Problematic:**
- **Interface pollution**: All implementations must add these methods (even non-network)
- **Backward compatibility break**: Any Main branch code implementing IGameController must add stubs
- **Conceptual violation**: Network protocol methods in core game controller interface

**Impact if Unchanged:**
- Main branch cannot add new IGameController implementations without knowing about network methods
- Third-party code implementing IGameController breaks
- Interface becomes increasingly bloated with domain-specific methods

---

#### 5. `IGuiGame.java` (core interface)

**Current Changes:**
- Added 8 new methods for delta sync and reconnection:
  - `applyDelta()`, `fullStateSync()`
  - `gamePaused()`, `gameResumed()`
  - `reconnectAccepted()`, `reconnectRejected()`
  - `setRememberedActions()`, `nextRememberedAction()`

**Why This Is Problematic:**
- **Larger interface pollution**: 8 methods is significant bloat
- **All GUI implementations affected**: Even local-only implementations must provide stubs
- **Single Responsibility Principle violation**: GUI interface now handles network protocol

**Impact if Unchanged:**
- Main branch GUI refactoring requires coordinating with network protocol
- Interface becomes increasingly difficult to implement correctly
- Unclear separation between local and network game concerns

---

### 🟢 LOW RISK - Localized Changes

#### 6. `PlayerZoneUpdate.java`
- Made 2 methods public: `addZone()`, `add()`
- **Risk**: Minimal - only visibility widening

#### 7. `StackItemView.java`
- Added network deserialization constructor
- **Risk**: Low - additive change only, constructor overload pattern is safe

#### 8. `PlayerControllerHuman.java`
- Added 3 no-op method implementations (required by IGameController)
- **Risk**: Very low - stub implementations that don't change behavior

---

## Conflict Probability Summary

| File | Module | Risk Level | Merge Conflict Probability | Reason |
|------|--------|-----------|---------------------------|--------|
| TrackableObject.java | forge-game | 🔴 High | 60% | Core game mechanics, visibility changes |
| AbstractGuiGame.java | forge-gui | 🔴 High | 80% | Large footprint, fundamental initialization changes |
| GameLobby.java | forge-gui | 🔴 High | 40% | Execution order change |
| IGameController.java | forge-gui | 🟡 Medium | 30% | Interface extension |
| IGuiGame.java | forge-gui | 🟡 Medium | 30% | Interface extension |
| PlayerZoneUpdate.java | forge-gui | 🟢 Low | 10% | Visibility widening only |
| StackItemView.java | forge-game | 🟢 Low | 5% | Additive constructor |
| PlayerControllerHuman.java | forge-gui | 🟢 Low | 5% | No-op stubs |

**Overall Assessment**: Without refactoring, **60-80% probability** of encountering at least one significant merge conflict when integrating NetworkPlay into Main.

---

## Refactoring Strategy

The following refactoring approach isolates network-specific functionality from core game logic using well-established design patterns.

### Priority 1: TrackableObject Isolation (HIGH PRIORITY)

**Goal**: Remove network-specific public methods from core game module

**Current Problem:**
```java
// forge-game/src/main/java/forge/trackable/TrackableObject.java
public final <T> void set(final TrackableProperty key, final T value) { ... }  // Made public for network
public final boolean hasChanges() { ... }  // Network-specific
public final Set<TrackableProperty> getChangedProps() { ... }  // Network-specific
```

**Refactoring Approach**: Package-Private Access Pattern

Instead of making methods public, use package-private visibility accessed through an adapter:

**Step 1: Revert Visibility Changes**

```java
// forge-game/src/main/java/forge/trackable/TrackableObject.java

// REVERT: Change back to protected
protected final <T> void set(final TrackableProperty key, final T value) {
    // ... existing implementation unchanged
}

// CHANGE: Make delta sync methods package-private instead of public
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
    ts.write(changedProps.size());
    for (TrackableProperty key : changedProps) {
        ts.write(TrackableProperty.serialize(key));
        key.serialize(ts, props.get(key));
    }
}

// ADD: Package-private setter for network deserialization
final <T> void setForNetwork(final TrackableProperty key, final T value) {
    set(key, value);  // Delegates to protected set()
}
```

**Step 2: Create Network Accessor**

```java
// forge-game/src/main/java/forge/trackable/NetworkTrackableAccess.java
package forge.trackable;

import java.util.Set;

/**
 * Package-private accessor for network serialization.
 * Bridges network code to TrackableObject without exposing public API.
 * Should only be used by network delta sync code.
 */
public final class NetworkTrackableAccess {

    private NetworkTrackableAccess() {} // Prevent instantiation

    public static boolean hasChanges(TrackableObject obj) {
        return obj.hasChanges();  // Access package-private method
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

// BEFORE:
if (obj.hasChanges()) {
    obj.serializeChangedOnly(ts);
}

// AFTER:
if (NetworkTrackableAccess.hasChanges(obj)) {
    NetworkTrackableAccess.serializeChangedOnly(obj, ts);
}
```

```java
// forge-gui/src/main/java/forge/gamemodes/match/AbstractGuiGame.java

// BEFORE (in applyDelta):
obj.set(prop, value);

// AFTER:
NetworkTrackableAccess.setProperty(obj, prop, value);
```

**Files to Update:**
- `TrackableObject.java` - Revert visibility, add package-private methods
- `NetworkTrackableAccess.java` - New accessor class
- `DeltaSyncManager.java` - Use accessor
- `AbstractGuiGame.java` - Use accessor for property setting
- `NetworkPropertySerializer.java` - Use accessor

**Benefits:**
- ✅ TrackableObject.java restored to protected visibility → zero Main branch conflict risk
- ✅ Network functionality isolated to network package
- ✅ Core game module independent of network implementation
- ✅ Performance: Minimal overhead (one extra method call)

**Complexity**: Medium - Requires updating multiple network files but pattern is straightforward

---

### Priority 2: AbstractGuiGame Refactoring (HIGH PRIORITY)

**Goal**: Extract 846 lines of network logic to separate subclass

**Current Problem**: AbstractGuiGame mixes local and network concerns

**Refactoring Approach**: Inheritance Pattern - Extract to NetworkGuiGame

**Step 1: Create NetworkGuiGame Subclass**

```java
// forge-gui/src/main/java/forge/gamemodes/match/NetworkGuiGame.java (NEW)
package forge.gamemodes.match;

import forge.game.GameView;
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.FullStatePacket;
import forge.trackable.Tracker;

/**
 * Extension of AbstractGuiGame with network delta sync support.
 * Handles delta packet application and tracker management without
 * polluting AbstractGuiGame with network-specific code.
 */
public class NetworkGuiGame extends AbstractGuiGame {

    @Override
    public void setGameView(final GameView gameView0) {
        // Network-specific tracker initialization
        if (gameView == null || gameView0 == null) {
            if (gameView0 != null) {
                ensureTrackerInitialized(gameView0);
                gameView0.updateObjLookup();
            }
            gameView = gameView0;
            return;
        }

        // Network-specific tracker handling for updates
        if (gameView0.getTracker() == null) {
            Tracker existingTracker = gameView.getTracker();
            if (existingTracker != null) {
                java.util.Set<Integer> visited = new java.util.HashSet<>();
                setTrackerRecursively(gameView0, existingTracker, visited);
            }
        }

        gameView.copyChangedProps(gameView0);
    }

    @Override
    public void applyDelta(DeltaPacket packet) {
        // Move ~500 lines of delta application logic here
        // (Full implementation from AbstractGuiGame.applyDelta)
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

**Step 2: Restore AbstractGuiGame to Original**

```java
// forge-gui/src/main/java/forge/gamemodes/match/AbstractGuiGame.java

@Override
public void setGameView(final GameView gameView0) {
    // RESTORED to original simple implementation
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

// REMOVE these methods (moved to NetworkGuiGame):
// - applyDelta()
// - fullStateSync()
// - ensureTrackerInitialized()
// - setTrackerRecursively()
// - computeStateChecksum()
// - requestFullStateResync()

// REMOVE network-specific imports
```

**Step 3: Update NetGuiGame to Use New Parent**

```java
// forge-gui/src/main/java/forge/gamemodes/net/server/NetGuiGame.java

// BEFORE:
public class NetGuiGame extends AbstractGuiGame {

// AFTER:
public class NetGuiGame extends NetworkGuiGame {  // Now extends NetworkGuiGame
    // No other changes to NetGuiGame implementation needed
}
```

**Files to Update:**
- `NetworkGuiGame.java` - New network-specific subclass
- `AbstractGuiGame.java` - Restore to near-original state
- `NetGuiGame.java` - Change parent class from AbstractGuiGame to NetworkGuiGame

**Benefits:**
- ✅ AbstractGuiGame restored to <100 lines changed from Main → minimal conflict risk
- ✅ Network logic clearly separated via inheritance
- ✅ Local games use AbstractGuiGame directly (unmodified)
- ✅ Network games use NetworkGuiGame (extends AbstractGuiGame)
- ✅ Polymorphism maintained (both are IGuiGame)

**Complexity**: High - Requires careful extraction of 846 lines, but clear separation makes it manageable

---

### Priority 3: Interface Segregation (MEDIUM PRIORITY)

**Goal**: Remove network methods from core interfaces

**Current Problem**: Core interfaces polluted with 11 network methods

**Refactoring Approach**: Interface Segregation Principle

**Step 1: Create Network-Specific Interfaces**

```java
// forge-gui/src/main/java/forge/interfaces/INetworkGameController.java (NEW)
package forge.interfaces;

/**
 * Network-specific extension of IGameController.
 * Only implemented by network game controllers.
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

import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.FullStatePacket;

/**
 * Network-specific extension of IGuiGame.
 * Only implemented by network GUI games.
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

    // REMOVE network methods (moved to INetworkGameController)
}
```

```java
// forge-gui/src/main/java/forge/gui/interfaces/IGuiGame.java

public interface IGuiGame {
    // ... existing methods ...
    void setCurrentPlayer(PlayerView player);

    // REMOVE network methods (moved to INetworkGuiGame)
}
```

**Step 3: Update Implementations**

```java
// forge-gui/src/main/java/forge/gamemodes/net/client/NetGameController.java

// BEFORE:
public class NetGameController implements IGameController {

// AFTER:
public class NetGameController implements INetworkGameController {
    // No other changes - already implements the network methods
}
```

```java
// forge-gui/src/main/java/forge/player/PlayerControllerHuman.java

// BEFORE:
public class PlayerControllerHuman extends PlayerController implements IGameController {
    @Override public void ackSync(long sequenceNumber) { /* no-op */ }
    @Override public void requestResync() { /* no-op */ }
    @Override public void reconnectRequest(String sessionId, String token) { /* no-op */ }
}

// AFTER:
public class PlayerControllerHuman extends PlayerController implements IGameController {
    // REMOVE the no-op network methods - interface no longer requires them
}
```

```java
// forge-gui/src/main/java/forge/gamemodes/match/NetworkGuiGame.java

public class NetworkGuiGame extends AbstractGuiGame implements INetworkGuiGame {
    // Already implements the network methods
}
```

**Step 4: Update Protocol Handlers**

```java
// forge-gui/src/main/java/forge/gamemodes/net/server/GameServerHandler.java

@Override
protected void beforeCall(final ProtocolMethod protocolMethod, final Object[] args) {
    if (protocolMethod == ProtocolMethod.ackSync && args.length > 0) {
        if (currentContext != null) {
            RemoteClient client = getClient(currentContext);
            if (client != null) {
                long sequenceNumber = (Long) args[0];
                IGuiGame gui = server.getGui(client.getIndex());

                // Type-safe check using network interface
                if (gui instanceof INetworkGuiGame) {
                    NetGuiGame netGui = (NetGuiGame) gui;
                    netGui.processAcknowledgment(sequenceNumber, client.getIndex());
                }
            }
        }
    }
    // Similar for requestResync
}
```

**Files to Update:**
- `INetworkGameController.java` - New network controller interface
- `INetworkGuiGame.java` - New network GUI interface
- `IGameController.java` - Remove network methods
- `IGuiGame.java` - Remove network methods
- `NetGameController.java` - Implement INetworkGameController
- `NetworkGuiGame.java` - Implement INetworkGuiGame
- `PlayerControllerHuman.java` - Remove no-op stubs
- `GameServerHandler.java` - Use network interface types

**Benefits:**
- ✅ Core interfaces clean → Main branch can modify without network concerns
- ✅ Type safety improved (instanceof checks more specific)
- ✅ Clear separation: IGameController = core, INetworkGameController = network
- ✅ No more no-op stubs in non-network implementations

**Complexity**: Medium - Straightforward interface extraction, multiple files affected

---

### Priority 4: GameLobby Documentation (MEDIUM PRIORITY)

**Goal**: Document execution order dependency to prevent accidental changes

**Refactoring Approach**: Defensive Documentation

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
    // See: BRANCH_DOCUMENTATION.md#reconnection-support
    //      NETWORK_ARCHITECTURE.md for details
    //
    // DO NOT REORDER without testing network reconnection!
    // ═══════════════════════════════════════════════════════════════════

    onGameStarted();  // MUST be first

    hostedMatch = GuiBase.getInterface().hostMatch();
    hostedMatch.startMatch(GameType.Constructed, variantTypes, players, guis);
    // ...
};
```

**Files to Update:**
- `GameLobby.java` - Add extensive comments

**Benefits:**
- ✅ Prevents accidental reordering in Main branch
- ✅ Documents rationale for future developers
- ✅ Low effort, high value

**Complexity**: Low - Documentation only, no code changes

---

## Summary of Refactored Architecture

### Before Refactoring (Current State)
```
Core Game Engine (forge-game)
└── TrackableObject [PUBLIC network methods] ❌

Core GUI (forge-gui)
├── AbstractGuiGame [846 lines of network code] ❌
├── IGameController [3 network methods] ❌
└── IGuiGame [8 network methods] ❌

Network Implementation
├── NetGuiGame extends AbstractGuiGame
├── NetGameController implements IGameController
└── DeltaSyncManager uses TrackableObject.public methods
```

### After Refactoring (Proposed State)
```
Core Game Engine (forge-game)
└── TrackableObject [PROTECTED, package-private for network] ✅
    └── NetworkTrackableAccess [Accessor for network code]

Core GUI (forge-gui)
├── AbstractGuiGame [Minimal changes, ~50 lines] ✅
│   └── NetworkGuiGame [846 lines of network code] ✅
├── IGameController [0 network methods] ✅
│   └── INetworkGameController [3 network methods]
└── IGuiGame [0 network methods] ✅
    └── INetworkGuiGame [8 network methods]

Network Implementation
├── NetGuiGame extends NetworkGuiGame ✅
├── NetGameController implements INetworkGameController ✅
└── DeltaSyncManager uses NetworkTrackableAccess ✅
```

### Merge Conflict Reduction

| Component | Before | After | Reduction |
|-----------|--------|-------|-----------|
| TrackableObject.java | 60% | 5% | 92% ↓ |
| AbstractGuiGame.java | 80% | 15% | 81% ↓ |
| IGameController.java | 30% | 5% | 83% ↓ |
| IGuiGame.java | 30% | 5% | 83% ↓ |
| GameLobby.java | 40% | 20% | 50% ↓ |
| **Overall** | **60-80%** | **10-20%** | **75% ↓** |

---

## Implementation Checklist

### Phase 1: Foundation
- [ ] Create `NetworkTrackableAccess.java`
- [ ] Create `INetworkGameController.java`
- [ ] Create `INetworkGuiGame.java`
- [ ] Write unit tests for new classes
- [ ] Verify compilation

### Phase 2: TrackableObject Isolation
- [ ] Update `DeltaSyncManager.java` to use `NetworkTrackableAccess`
- [ ] Update `AbstractGuiGame.java` to use accessor for property setting
- [ ] Update `NetworkPropertySerializer.java` to use accessor
- [ ] Revert `TrackableObject.set()` to protected
- [ ] Make TrackableObject delta methods package-private
- [ ] Run regression tests (local games)
- [ ] Run delta sync tests (network games)

### Phase 3: AbstractGuiGame Refactoring
- [ ] Create `NetworkGuiGame.java` class
- [ ] Move network methods from `AbstractGuiGame` to `NetworkGuiGame`
- [ ] Restore `AbstractGuiGame.setGameView()` to simple version
- [ ] Update `NetGuiGame` to extend `NetworkGuiGame`
- [ ] Run full integration tests (local and network)

### Phase 4: Interface Segregation
- [ ] Update `NetGameController` to implement `INetworkGameController`
- [ ] Update `NetworkGuiGame` to implement `INetworkGuiGame`
- [ ] Remove network methods from `IGameController`
- [ ] Remove network methods from `IGuiGame`
- [ ] Remove no-op stubs from `PlayerControllerHuman`
- [ ] Update `GameServerHandler` to use network interface types
- [ ] Update `GameClientHandler` similarly
- [ ] Verify all tests pass

### Phase 5: Documentation
- [ ] Add extensive comments to `GameLobby.java`
- [ ] Update `BRANCH_DOCUMENTATION.md` with new architecture
- [ ] Create migration guide for Main branch merge
- [ ] Final regression test suite

---

## Testing Strategy

### Unit Tests
- TrackableObject behavior unchanged after reverting visibility
- NetworkTrackableAccess delegates correctly
- Interface implementations compile and satisfy contracts

### Integration Tests
- Full network game flow (start → play → reconnect → end)
- Delta sync bandwidth verification (still ~99% savings)
- Disconnection/reconnection flow
- Checksum validation and auto-resync

### Regression Tests
- **Critical**: Local games completely unaffected
- No performance degradation (<2% tolerance)
- All existing NetworkPlay tests pass
- All Main branch tests pass (if available)

### Performance Tests
- Delta sync latency unchanged
- Adapter overhead < 2% (should be ~0.1%)
- Memory usage unchanged

---

## Rollback Strategy

Each phase is independent and can be rolled back individually:

**Phase 2 Rollback**: Revert TrackableObject changes, keep accessor (becomes no-op wrapper)
**Phase 3 Rollback**: Delete NetworkGuiGame class, restore AbstractGuiGame from backup
**Phase 4 Rollback**: Add network methods back to core interfaces, revert implementations
**Phase 5 Rollback**: Remove documentation (no code impact)

**Recommendation**: Implement on feature branch, merge to NetworkPlay only after full validation.

---

## Decision Points for Main Branch Team

The NetworkPlay refactoring is **optional** depending on Main branch development priorities:

### Option 1: Full Refactoring (Recommended)
**When**: Before merging NetworkPlay to Main
**Effort**: Significant (dedicated focus needed)
**Benefit**: Minimal merge conflicts, clean architecture, future-proof
**Risk**: Refactoring introduces bugs (mitigated by testing)

### Option 2: Partial Refactoring
**When**: Prioritize highest-risk items only
**Effort**: Moderate (focus on TrackableObject + AbstractGuiGame)
**Benefit**: Major conflict reduction (~50% fewer conflicts)
**Risk**: Interface pollution remains

### Option 3: No Refactoring (Current State)
**When**: Main branch not actively developing affected areas
**Effort**: None
**Benefit**: No refactoring risk
**Risk**: High merge conflict probability (60-80%), technical debt

### Option 4: Defer to Merge Time
**When**: Uncertain about Main branch development plans
**Effort**: Variable (depends on conflicts encountered)
**Benefit**: Only refactor what's necessary
**Risk**: Emergency refactoring under pressure, potential quality issues

---

## Recommendation

For **active Main branch development**, implement **Options 1 or 2** to avoid costly merge conflicts later. The refactoring isolates concerns and aligns with good architectural practices.

For **inactive Main branch**, **Option 3** is acceptable with the understanding that future merges will require careful conflict resolution.

The NetworkPlay branch has demonstrated the value of delta synchronization (~99% bandwidth reduction) and robust reconnection support. The architectural overlap with core game logic is a consequence of how deeply integrated these features are with the game state management system. The refactoring strategies outlined here provide a path to preserve these features while minimizing impact on the Main branch.
