# NetworkPlay Refactoring Plan
## Isolating Network Changes from Core Architecture

This document outlines the implementation plan for addressing the architectural risks identified in ARCHITECTURAL_REVIEW.md.

---

## Overview

**Goal**: Isolate network-specific functionality from core game classes to minimize merge conflict risk with Main branch.

**Total Estimated Effort**: 12-16 developer days

**Risk Reduction**: 60-80% merge conflict probability → 10-20%

---

## Priority 1: TrackableObject Isolation (HIGH RISK)

### Current Problem
```java
// forge-game/src/main/java/forge/trackable/TrackableObject.java
// Changed from protected to public - affects core game engine
public final <T> void set(final TrackableProperty key, final T value) { ... }

// Added network-specific methods to core class
public final boolean hasChanges() { ... }
public final Set<TrackableProperty> getChangedProps() { ... }
public final void clearChanges() { ... }
public final void serializeChangedOnly(final TrackableSerializer ts) { ... }
```

**Risk**: Core game module modified for network needs. Main branch changes to TrackableObject will conflict.

### Solution: Package-Private Access Pattern

**Approach**: Use package-private methods instead of public visibility, accessed via a network adapter.

#### Step 1: Revert TrackableObject Public Changes
**Complexity**: ⭐⭐ (2/5) - Low complexity, medium testing
**Time Estimate**: 1 day
**Files Modified**: 1

```java
// forge-game/src/main/java/forge/trackable/TrackableObject.java

// REVERT: Change back to protected
protected final <T> void set(final TrackableProperty key, final T value) {
    // ... existing implementation
}

// KEEP: Delta sync methods but make package-private instead of public
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

**Testing Required**:
- Unit tests for TrackableObject to ensure behavior unchanged
- Delta sync tests to verify network functionality still works
- Local game tests to verify protected visibility still works

**Potential Issues**:
- ⚠️ Reflection-based code that calls `set()` may break if it relied on public visibility
- ⚠️ Network serialization code needs to be in same package or use new `setForNetwork()`

---

#### Step 2: Create Network Access Adapter
**Complexity**: ⭐⭐⭐ (3/5) - Medium complexity
**Time Estimate**: 1.5 days
**Files Modified**: 3 (1 new, 2 updated)

Create new adapter class in trackable package:

```java
// forge-game/src/main/java/forge/trackable/NetworkTrackableAccess.java
package forge.trackable;

import java.util.Set;

/**
 * Package-private accessor for network serialization.
 * This class bridges network code to TrackableObject without making
 * TrackableObject methods public.
 *
 * Should only be used by network delta sync code.
 */
public final class NetworkTrackableAccess {

    private NetworkTrackableAccess() {} // Prevent instantiation

    /**
     * Check if a trackable object has pending changes.
     * Package-private access to hasChanges().
     */
    public static boolean hasChanges(TrackableObject obj) {
        return obj.hasChanges();  // Calls package-private method
    }

    /**
     * Get the set of changed properties.
     * Package-private access to getChangedProps().
     */
    public static Set<TrackableProperty> getChangedProps(TrackableObject obj) {
        return obj.getChangedProps();  // Calls package-private method
    }

    /**
     * Clear change tracking flags after sync acknowledgment.
     * Package-private access to clearChanges().
     */
    public static void clearChanges(TrackableObject obj) {
        obj.clearChanges();  // Calls package-private method
    }

    /**
     * Serialize only changed properties for delta sync.
     * Package-private access to serializeChangedOnly().
     */
    public static void serializeChangedOnly(TrackableObject obj, TrackableSerializer ts) {
        obj.serializeChangedOnly(ts);  // Calls package-private method
    }

    /**
     * Set a property value for network deserialization.
     * Package-private access to setForNetwork().
     */
    public static <T> void setProperty(TrackableObject obj, TrackableProperty key, T value) {
        obj.setForNetwork(key, value);  // Calls package-private method
    }
}
```

**Update network code to use adapter**:

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

// BEFORE:
obj.set(prop, value);

// AFTER:
NetworkTrackableAccess.setProperty(obj, prop, value);
```

**Files to Update**:
- `DeltaSyncManager.java` - Replace direct TrackableObject calls
- `AbstractGuiGame.java` - Replace obj.set() calls with adapter
- `NetworkPropertySerializer.java` - Use adapter for property access

**Testing Required**:
- Full integration test of delta sync with adapter
- Verify no regression in network game performance
- Verify local games unaffected

**Potential Issues**:
- ⚠️ Performance: Extra method call overhead (minimal, ~1-2% impact)
- ⚠️ Code search: Harder to find usages of TrackableObject methods
- ✅ Benefit: TrackableObject.java unchanged → zero Main branch conflict

---

## Priority 2: AbstractGuiGame Isolation (HIGH RISK)

### Current Problem
AbstractGuiGame has ~846 lines of network-specific code mixed with core game logic:
- `applyDelta()` method (~500 lines)
- `fullStateSync()` method
- Modified `setGameView()` with tracker initialization
- Network-specific helper methods

**Risk**: Core GUI class heavily modified. Any Main branch changes = merge conflict.

### Solution: Extract to NetworkGuiGame Subclass

**Approach**: Move network logic to a dedicated subclass, restore AbstractGuiGame to near-original state.

#### Step 1: Create NetworkGuiGame Class
**Complexity**: ⭐⭐⭐⭐ (4/5) - High complexity, requires careful refactoring
**Time Estimate**: 3 days
**Files Modified**: 2 (1 new, 1 restored)

```java
// forge-gui/src/main/java/forge/gamemodes/match/NetworkGuiGame.java (NEW)
package forge.gamemodes.match;

import forge.game.GameView;
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.FullStatePacket;
import forge.trackable.Tracker;
import forge.trackable.TrackableObject;

/**
 * Extension of AbstractGuiGame with network delta sync support.
 * This class handles delta packet application and tracker management
 * for network games without polluting AbstractGuiGame.
 */
public class NetworkGuiGame extends AbstractGuiGame {

    @Override
    public void setGameView(final GameView gameView0) {
        if (gameView == null || gameView0 == null) {
            if (gameView0 != null) {
                ensureTrackerInitialized(gameView0);
                gameView0.updateObjLookup();
            }
            gameView = gameView0;
            return;
        }

        // Network-specific tracker handling
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
        // All the delta application logic from AbstractGuiGame
        // ~500 lines moved here
    }

    @Override
    public void fullStateSync(FullStatePacket packet) {
        // Full state sync logic
    }

    private void ensureTrackerInitialized(GameView gameView0) {
        // Tracker initialization logic from AbstractGuiGame
    }

    private void setTrackerRecursively(TrackableObject obj, Tracker tracker,
                                       java.util.Set<Integer> visited) {
        // Recursive tracker setup logic from AbstractGuiGame
    }

    private int computeStateChecksum(GameView gameView) {
        // Checksum computation logic
    }

    private void requestFullStateResync() {
        // Resync request logic
    }
}
```

#### Step 2: Restore AbstractGuiGame to Original State
**Complexity**: ⭐⭐⭐ (3/5) - Medium complexity
**Time Estimate**: 1.5 days
**Files Modified**: 1

```java
// forge-gui/src/main/java/forge/gamemodes/match/AbstractGuiGame.java

@Override
public void setGameView(final GameView gameView0) {
    // RESTORE to original simple implementation:
    if (gameView == null || gameView0 == null) {
        if (gameView0 != null) {
            gameView0.updateObjLookup();
        }
        gameView = gameView0;
        return;
    }

    // Original simple implementation (no tracker management)
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

**Diff from Main**: Should be minimal (<50 lines), mostly additive for base network support

#### Step 3: Update Factory/Construction Points
**Complexity**: ⭐⭐ (2/5) - Low complexity, many files
**Time Estimate**: 1 day
**Files Modified**: 5-10

Update all places that create AbstractGuiGame for network scenarios:

```java
// forge-gui/src/main/java/forge/gamemodes/net/server/NetGuiGame.java

// BEFORE:
public class NetGuiGame extends AbstractGuiGame {

// AFTER:
public class NetGuiGame extends NetworkGuiGame {  // Now extends NetworkGuiGame
    // No changes to NetGuiGame implementation needed
}
```

```java
// Any factory methods or GUI creation code:

// BEFORE:
AbstractGuiGame gui = new AbstractGuiGame();

// AFTER (for network games):
AbstractGuiGame gui = new NetworkGuiGame();

// UNCHANGED (for local games):
AbstractGuiGame gui = new AbstractGuiGame();
```

**Files to Update**:
- `NetGuiGame.java` - Change parent class
- Client-side GUI factories if they create AbstractGuiGame directly
- Any test harnesses

**Testing Required**:
- Network game full flow test
- Local game full flow test (verify unaffected)
- Delta sync regression test
- Reconnection test

**Potential Issues**:
- ⚠️ If AbstractGuiGame is instantiated directly for network games, those need updating
- ⚠️ Polymorphism: Ensure NetworkGuiGame is used wherever network behavior is needed
- ✅ Benefit: AbstractGuiGame mostly restored → minimal Main branch conflict

---

## Priority 3: Interface Segregation (MEDIUM RISK)

### Current Problem
Core interfaces polluted with network methods:
- `IGameController` - 3 network methods added
- `IGuiGame` - 8 network methods added

**Risk**: All implementations must implement these methods, even non-network code.

### Solution: Interface Segregation Principle

**Approach**: Create network-specific interfaces, make core interfaces clean.

#### Step 1: Create Network-Specific Interfaces
**Complexity**: ⭐⭐ (2/5) - Low complexity, straightforward
**Time Estimate**: 0.5 days
**Files Modified**: 2 (2 new)

```java
// forge-gui/src/main/java/forge/interfaces/INetworkGameController.java (NEW)
package forge.interfaces;

/**
 * Network-specific extension of IGameController.
 * Only implemented by network game controllers (NetGameController).
 */
public interface INetworkGameController extends IGameController {
    /**
     * Acknowledge receipt of a delta or full state packet.
     * @param sequenceNumber the sequence number being acknowledged
     */
    void ackSync(long sequenceNumber);

    /**
     * Request a full state resync from the server.
     * Called automatically when checksum validation fails.
     */
    void requestResync();

    /**
     * Request to reconnect to an existing game session.
     * @param sessionId the session identifier
     * @param token the session token for authentication
     */
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
 * Only implemented by network GUI games (NetworkGuiGame, NetGuiGame).
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

#### Step 2: Restore Core Interfaces
**Complexity**: ⭐ (1/5) - Very low complexity
**Time Estimate**: 0.5 days
**Files Modified**: 2

```java
// forge-gui/src/main/java/forge/interfaces/IGameController.java

public interface IGameController {
    // ... existing methods ...

    void reorderHand(CardView card, int index);

    // REMOVE all network methods (moved to INetworkGameController):
    // void ackSync(long sequenceNumber);
    // void requestResync();
    // void reconnectRequest(String sessionId, String token);
}
```

```java
// forge-gui/src/main/java/forge/gui/interfaces/IGuiGame.java

public interface IGuiGame {
    // ... existing methods ...

    void setCurrentPlayer(PlayerView player);

    // REMOVE all network methods (moved to INetworkGuiGame):
    // void applyDelta(DeltaPacket packet);
    // void fullStateSync(FullStatePacket packet);
    // void gamePaused(String message);
    // void gameResumed();
    // void reconnectAccepted(FullStatePacket packet);
    // void reconnectRejected(String reason);
    // void setRememberedActions();
    // void nextRememberedAction();
}
```

#### Step 3: Update Implementations
**Complexity**: ⭐⭐ (2/5) - Low complexity, many files
**Time Estimate**: 1 day
**Files Modified**: 5-7

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
    // ...
    @Override
    public void ackSync(long sequenceNumber) { /* no-op */ }

    @Override
    public void requestResync() { /* no-op */ }

    @Override
    public void reconnectRequest(String sessionId, String token) { /* no-op */ }
}

// AFTER:
public class PlayerControllerHuman extends PlayerController implements IGameController {
    // REMOVE the no-op network methods - interface no longer requires them
}
```

```java
// forge-gui/src/main/java/forge/gamemodes/match/NetworkGuiGame.java

// BEFORE:
public class NetworkGuiGame extends AbstractGuiGame {

// AFTER:
public class NetworkGuiGame extends AbstractGuiGame implements INetworkGuiGame {
    // Already implements the network methods
}
```

```java
// forge-gui/src/main/java/forge/gamemodes/net/server/NetGuiGame.java

// BEFORE:
public class NetGuiGame extends NetworkGuiGame {

// AFTER:
public class NetGuiGame extends NetworkGuiGame implements INetworkGuiGame {
    // NetworkGuiGame already implements INetworkGuiGame, but explicit is clearer
}
```

#### Step 4: Update Protocol Handlers
**Complexity**: ⭐⭐⭐ (3/5) - Medium complexity, type safety important
**Time Estimate**: 1 day
**Files Modified**: 3-5

Update code that uses these interfaces to be type-safe:

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

                // BEFORE:
                // if (gui instanceof NetGuiGame) {
                //     ((NetGuiGame) gui).processAcknowledgment(...);
                // }

                // AFTER - more type-safe:
                if (gui instanceof INetworkGuiGame) {
                    // Call through network interface, or delegate to NetGuiGame
                    // specific implementation
                    NetGuiGame netGui = (NetGuiGame) gui;
                    netGui.processAcknowledgment(sequenceNumber, client.getIndex());
                }
            }
        }
    }
    // ... similar for requestResync
}
```

**Files to Update**:
- `GameServerHandler.java` - Use INetworkGameController/INetworkGuiGame types
- `GameClientHandler.java` - Similar updates
- `FServerManager.java` - Where controllers are retrieved
- Protocol dispatch code

**Testing Required**:
- Full network game flow test
- Local game test (verify no regressions)
- Compile test to catch any missed implementations

**Potential Issues**:
- ⚠️ Instanceof checks need updating
- ⚠️ Cast safety - ensure casts are correct
- ✅ Benefit: Core interfaces clean → Main branch can modify without conflict

---

## Priority 4: GameLobby Documentation (MEDIUM RISK)

### Current Problem
```java
// forge-gui/src/main/java/forge/gamemodes/match/GameLobby.java

// Execution order changed - onGameStarted() moved before hostedMatch
onGameStarted();  // MOVED UP - was after startMatch()
hostedMatch = GuiBase.getInterface().hostMatch();
hostedMatch.startMatch(...);
```

**Risk**: Subtle timing change could break Main branch features.

### Solution: Extensive Documentation

**Approach**: Add comprehensive comments explaining the change and its necessity.

#### Step 1: Add Detailed Comments
**Complexity**: ⭐ (1/5) - Trivial
**Time Estimate**: 0.5 days
**Files Modified**: 1

```java
// forge-gui/src/main/java/forge/gamemodes/match/GameLobby.java

//if above checks succeed, return runnable that can be used to finish starting game
return () -> {
    // ═══════════════════════════════════════════════════════════════════
    // CRITICAL EXECUTION ORDER FOR NETWORK GAMES
    // ═══════════════════════════════════════════════════════════════════
    //
    // onGameStarted() MUST be called BEFORE hostedMatch.startMatch() for
    // network multiplayer games. Here's why:
    //
    // Network Game Initialization Flow:
    //   1. onGameStarted() creates GameSession on server
    //   2. GameSession generates session credentials (sessionId + tokens)
    //   3. hostedMatch.startMatch() calls game.openView()
    //   4. openView() triggers NetGuiGame.updateGameView()
    //   5. updateGameView() sends initial FullStatePacket with credentials
    //   6. Clients store credentials for reconnection
    //
    // If hostedMatch.startMatch() happens FIRST:
    //   - NetGuiGame tries to send credentials before GameSession exists
    //   - Session is null, credentials not sent
    //   - Clients cannot reconnect (no stored token)
    //   - Reconnection feature completely broken
    //
    // Local Game Impact: NONE
    //   - onGameStarted() is typically empty/no-op for local games
    //   - Moving it earlier has no effect on local game flow
    //
    // Merge Conflict Prevention:
    //   - If Main branch modifies this sequence, check network game flow
    //   - Test reconnection after any changes to this method
    //   - See BRANCH_DOCUMENTATION.md#reconnection for details
    //
    // DO NOT REORDER without testing network game reconnection!
    // ═══════════════════════════════════════════════════════════════════

    onGameStarted();  // MUST come first for network games

    hostedMatch = GuiBase.getInterface().hostMatch();
    hostedMatch.startMatch(GameType.Constructed, variantTypes, players, guis);

    // ... rest of method
};
```

#### Step 2: Add Unit Test
**Complexity**: ⭐⭐ (2/5) - Low-medium complexity
**Time Estimate**: 0.5 days
**Files Modified**: 1 (new test)

Create test that verifies execution order:

```java
// forge-gui-desktop/src/test/java/forge/gamesimulationtests/GameLobbyExecutionOrderTest.java

public class GameLobbyExecutionOrderTest {

    @Test
    public void testOnGameStartedCalledBeforeStartMatch() {
        // Create a spy/mock ServerGameLobby
        // Verify onGameStarted() is called
        // Verify it's called BEFORE hostedMatch.startMatch()

        // This test serves as documentation and regression prevention
    }

    @Test
    public void testNetworkGameSessionCreatedBeforeCredentialsSent() {
        // More complex integration test
        // Start a network game
        // Verify GameSession exists before FullStatePacket sent
        // Verify credentials in FullStatePacket are valid
    }
}
```

**Testing Required**:
- Network game start-to-finish
- Reconnection flow test
- Local game test (verify no regression)

**Potential Issues**:
- ⚠️ Test may be fragile if game startup changes
- ✅ Benefit: Comments prevent accidental reordering in Main branch

---

## Implementation Roadmap

### Phase 1: Foundation (3 days)
**Goal**: Set up new interfaces and accessor classes without breaking existing code

1. Create `NetworkTrackableAccess` ✓
2. Create `INetworkGameController` interface ✓
3. Create `INetworkGuiGame` interface ✓
4. Write unit tests for new classes ✓

**Validation**: All existing tests still pass, new interfaces compile

---

### Phase 2: TrackableObject Isolation (2.5 days)
**Goal**: Remove public visibility from TrackableObject

5. Update DeltaSyncManager to use NetworkTrackableAccess ✓
6. Update AbstractGuiGame delta application to use adapter ✓
7. Update NetworkPropertySerializer to use adapter ✓
8. Revert TrackableObject.set() to protected ✓
9. Make TrackableObject delta methods package-private ✓
10. Run full regression test suite ✓

**Validation**: Delta sync tests pass, local games unaffected

---

### Phase 3: AbstractGuiGame Refactoring (4.5 days)
**Goal**: Extract network logic to NetworkGuiGame subclass

11. Create NetworkGuiGame class ✓
12. Move network methods from AbstractGuiGame to NetworkGuiGame ✓
13. Update AbstractGuiGame.setGameView() to simple version ✓
14. Make NetGuiGame extend NetworkGuiGame instead ✓
15. Update all GUI factory code ✓
16. Run network game integration tests ✓

**Validation**: Network and local games both work correctly

---

### Phase 4: Interface Segregation (2.5 days)
**Goal**: Clean up core interfaces

17. Make NetGameController implement INetworkGameController ✓
18. Make NetworkGuiGame implement INetworkGuiGame ✓
19. Remove network methods from IGameController ✓
20. Remove network methods from IGuiGame ✓
21. Remove no-op stubs from PlayerControllerHuman ✓
22. Update protocol handlers to use network interfaces ✓

**Validation**: Clean compile, all tests pass

---

### Phase 5: Documentation & Testing (1 day)
**Goal**: Document changes and prevent regressions

23. Add extensive comments to GameLobby ✓
24. Create execution order test ✓
25. Update BRANCH_DOCUMENTATION.md with new architecture ✓
26. Create migration guide for merging to Main ✓

**Validation**: Documentation review, final regression test

---

## Complexity Summary

| Task | Complexity | Time | Risk if Skipped |
|------|-----------|------|-----------------|
| TrackableObject Isolation | ⭐⭐⭐ Medium | 2.5 days | High - Core module conflict |
| AbstractGuiGame Refactoring | ⭐⭐⭐⭐ High | 4.5 days | Critical - Largest conflict source |
| Interface Segregation | ⭐⭐ Low-Med | 2.5 days | Medium - Interface pollution |
| GameLobby Documentation | ⭐ Low | 1 day | Low - Easy to document in merge |
| **TOTAL** | **⭐⭐⭐ Medium** | **12-16 days** | **High overall** |

---

## Testing Strategy

### Unit Tests
- TrackableObject behavior unchanged ✓
- NetworkTrackableAccess delegates correctly ✓
- Interface implementations compile ✓

### Integration Tests
- Full network game flow (start → play → end) ✓
- Delta sync bandwidth verification ✓
- Disconnection/reconnection ✓
- Checksum validation and resync ✓

### Regression Tests
- Local games still work ✓
- No performance degradation ✓
- All existing tests pass ✓

### Performance Tests
- Delta sync latency unchanged ✓
- Adapter overhead < 2% ✓
- Memory usage unchanged ✓

---

## Risk Mitigation

### Implementation Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Breaking local games | Medium | Critical | Extensive local game testing |
| Performance regression | Low | High | Before/after benchmarks |
| Missed instanceof checks | Medium | Medium | Compiler warnings, IDE refactoring tools |
| Test coverage gaps | Medium | Medium | Code coverage analysis |

### Rollback Plan

Each phase is independent and can be rolled back:

**Phase 2 Rollback**: Revert TrackableObject changes, keep adapter (no-op)
**Phase 3 Rollback**: Delete NetworkGuiGame, restore AbstractGuiGame
**Phase 4 Rollback**: Add network methods back to core interfaces

**Recommendation**: Use feature branch, merge to NetworkPlay after full validation

---

## Success Criteria

### Technical Goals
- ✅ TrackableObject.set() is protected (not public)
- ✅ AbstractGuiGame has <100 lines of network code
- ✅ IGameController has 0 network methods
- ✅ IGuiGame has 0 network methods
- ✅ All tests pass

### Business Goals
- ✅ Merge conflict probability < 20% (from 60-80%)
- ✅ No performance regression
- ✅ No behavioral changes to local games
- ✅ Network features work identically

---

## Alternative Approaches Considered

### Alternative 1: Keep Current Architecture
**Pros**: No work required
**Cons**: High merge conflict risk, poor separation of concerns
**Verdict**: ❌ Rejected - too risky

### Alternative 2: Complete Rewrite
**Pros**: Perfect architecture
**Cons**: 4-6 weeks of work, high risk of bugs
**Verdict**: ❌ Rejected - excessive effort

### Alternative 3: Minimal Changes Only
Just add comments and hope for the best
**Pros**: Fast (1-2 days)
**Cons**: Doesn't solve core problem
**Verdict**: ⚠️ Fallback option if time-constrained

### Alternative 4: Recommended Approach (This Plan)
**Pros**: Good balance of effort vs. risk reduction
**Cons**: Still requires 2-3 weeks
**Verdict**: ✅ Selected

---

## Effort Breakdown by Role

### Senior Developer (Days 1-8)
- Architecture design review
- TrackableObject refactoring
- AbstractGuiGame extraction
- Code review

### Mid-Level Developer (Days 9-12)
- Interface segregation
- Protocol handler updates
- Test creation
- Documentation

### QA Engineer (Days 13-16)
- Integration testing
- Regression testing
- Performance testing
- Final validation

**Total Team Effort**: ~16 person-days (2-3 weeks calendar time)

---

## Questions for Decision

1. **Timing**: When should this refactoring happen?
   - Before Main merge (recommended)
   - After Main merge (riskier)
   - Not at all (highest risk)

2. **Scope**: Which priorities to implement?
   - All 4 priorities (recommended for max risk reduction)
   - Priorities 1-2 only (minimum viable)
   - Priority 1 only (quick win)

3. **Resources**: Who will implement?
   - NetworkPlay branch maintainer
   - Dedicated refactoring team
   - Split across multiple developers

4. **Timeline**: How urgent?
   - Before next Main release (recommended)
   - When convenient
   - Only if Main merge conflicts occur

---

## Conclusion

This refactoring plan provides a **structured, testable approach** to isolating network-specific code from core architecture. While requiring 12-16 days of effort, it reduces merge conflict probability from **60-80% to 10-20%**.

**Recommendation**: **Implement Priorities 1-3** (all except GameLobby docs) for maximum benefit. Priority 4 can be added incrementally.

**Next Steps**:
1. Review this plan with team
2. Get approval for timeline and resources
3. Create feature branch: `claude/refactor-network-isolation-Yzrli`
4. Begin Phase 1 implementation
