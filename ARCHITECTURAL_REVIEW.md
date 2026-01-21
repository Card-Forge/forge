# NetworkPlay Branch - Architectural Review

## Executive Summary

The NetworkPlay branch contains **significant architectural changes to core non-network classes** that pose moderate to high risk of merge conflicts with the Main branch. While most changes are additive (new methods, wider visibility), there are critical modifications to core game mechanics that could interfere with parallel development.

## Risk Categories

### 🔴 HIGH RISK - Core Architecture Changes

#### 1. `TrackableObject.java` (forge-game module)
**Changes:**
- Changed `set()` method visibility: `protected` → `public`
- Added 4 new public methods: `hasChanges()`, `getChangedProps()`, `clearChanges()`, `serializeChangedOnly()`

**Risks:**
- **Visibility change** could enable new usage patterns in Main branch that conflict with delta sync assumptions
- **Main branch could add protected methods** with same names as new public methods
- **Performance implications**: Change tracking adds overhead to all property modifications
- **Module boundary violation**: Network-specific functionality in core game module

**Impact**: Any Main branch changes to TrackableObject will require careful merge

#### 2. `AbstractGuiGame.java` (forge-gui module)
**Changes:**
- ~846 lines of modifications (largest single-file change)
- Added `applyDelta()` method (~500 lines)
- Added `fullStateSync()` method
- Modified `setGameView()` with tracker initialization logic
- Added 4 helper methods: `ensureTrackerInitialized()`, `setTrackerRecursively()`, `computeStateChecksum()`, `requestFullStateResync()`

**Risks:**
- **setGameView() modification** changes fundamental game state initialization
- **Main branch may modify setGameView()** for other features, creating merge conflict
- **Tracker initialization logic** could conflict with other Tracker changes
- **Large code footprint** increases probability of line-level conflicts

**Impact**: Very high likelihood of merge conflicts if Main modifies game state management

#### 3. `GameLobby.java`
**Changes:**
- Reordered game start sequence: `onGameStarted()` now called BEFORE `hostedMatch.startMatch()`
- Added public getter `getHostedMatch()`

**Risks:**
- **Execution order change** could break Main branch features that depend on original sequence
- **Race conditions**: Session credentials now sent earlier, could conflict with other timing-sensitive features
- **Subtle behavior change** that may not be caught in code review

**Impact**: Medium risk - behavior change could cause hard-to-debug issues

---

### 🟡 MEDIUM RISK - Interface Extensions

#### 4. `IGameController.java` (core interface)
**Changes:**
- Added 3 new methods: `ackSync()`, `requestResync()`, `reconnectRequest()`

**Risks:**
- **Interface pollution**: All implementations must add these methods (even non-network)
- **Main branch adding methods** would require coordination to avoid conflicts
- **Backward compatibility**: Any Main branch code implementing IGameController breaks

**Mitigation**: NetworkPlay added no-op stubs to PlayerControllerHuman

#### 5. `IGuiGame.java` (core interface)
**Changes:**
- Added 8 new methods for delta sync and reconnection

**Risks:**
- Similar to IGameController - interface bloat
- **All GUI implementations** must implement these methods
- Main branch GUI changes require awareness of new methods

**Mitigation**: Methods are network-specific and clearly documented

---

### 🟢 LOW RISK - Localized Changes

#### 6. `PlayerZoneUpdate.java`
**Changes:**
- Made 2 methods public: `addZone()`, `add()`

**Risks:**
- Minimal - only visibility widening
- Could enable unintended usage but unlikely to conflict

#### 7. `StackItemView.java`
**Changes:**
- Added network deserialization constructor

**Risks:**
- Low - additive change only
- Constructor overload pattern is safe

#### 8. `PlayerControllerHuman.java`
**Changes:**
- Added 3 no-op method implementations

**Risks:**
- Very low - stub implementations that don't change behavior

---

## Conflict Scenarios

### Scenario 1: Main Branch Modifies TrackableObject
**Probability**: Medium
**Impact**: High

If Main branch:
- Adds new protected methods → Conflict if names match NetworkPlay's public methods
- Modifies `set()` method → Merge conflict with visibility change
- Changes change tracking mechanism → Logic conflict with delta sync

**Example**:
```java
// Main branch adds:
protected void clearChanges() { /* different implementation */ }

// NetworkPlay already has:
public final void clearChanges() { changedProps.clear(); }
```

### Scenario 2: Main Branch Modifies setGameView()
**Probability**: High
**Impact**: Critical

AbstractGuiGame.setGameView() is a critical initialization point. If Main branch:
- Adds new initialization logic
- Changes GameView handling
- Modifies tracker setup

Result: Complex merge conflict requiring understanding of both branches

### Scenario 3: Main Branch Implements IGameController
**Probability**: Low-Medium
**Impact**: Medium

New IGameController implementations in Main will fail to compile until network methods are added.

---

## Recommendations

### Immediate Actions

#### 1. **Isolate Core Changes** (Recommended)
Extract network-specific functionality from core classes:

**Current (risky)**:
```java
// TrackableObject.java (core game module)
public final boolean hasChanges() { ... }  // Network logic in core
```

**Proposed (safer)**:
```java
// TrackableObject.java - no changes
// protected void set() remains protected

// NetworkTrackableAdapter.java (network module)
public class NetworkTrackableAdapter {
    public static boolean hasChanges(TrackableObject obj) {
        // Use reflection or package-private access
    }
}
```

**Benefits**:
- Core classes remain unchanged
- Network logic isolated to network package
- Zero merge conflict risk with Main

**Cost**: Slight performance overhead from delegation/reflection

#### 2. **Make TrackableObject.set() Visibility Configurable**
Instead of hard-coded public:

```java
// TrackableObject.java
protected final <T> void set(final TrackableProperty key, final T value) {
    setInternal(key, value);
}

// Package-visible for network serialization
final <T> void setInternal(final TrackableProperty key, final T value) {
    // Original implementation
}
```

Network code can access `setInternal()` via package access without making `set()` public.

#### 3. **Extract IGuiGame Network Methods to Separate Interface**
```java
// IGuiGame.java - unchanged

// INetworkGuiGame.java (new)
public interface INetworkGuiGame extends IGuiGame {
    void applyDelta(DeltaPacket packet);
    void fullStateSync(FullStatePacket packet);
    // ... other network methods
}

// AbstractGuiGame.java - implements IGuiGame (not INetworkGuiGame)
// NetGuiGame.java - implements INetworkGuiGame
```

**Benefits**:
- Core IGuiGame interface unchanged → zero Main branch conflict
- Network interface clearly separated
- Type-safe distinction between local and network games

#### 4. **Document Execution Order Change in GameLobby**
Add extensive comments explaining why `onGameStarted()` moved:

```java
// CRITICAL: onGameStarted() must be called BEFORE hostedMatch.startMatch()
// for network games to establish session credentials. Do not reorder without
// understanding network reconnection flow. See BRANCH_DOCUMENTATION.md#reconnection
onGameStarted();
hostedMatch = GuiBase.getInterface().hostMatch();
```

### Long-Term Strategies

#### 5. **Merge Frequently**
- Merge Main → NetworkPlay weekly to minimize divergence
- Each merge should be small and reviewable
- Test thoroughly after each merge

#### 6. **Coordinate Development**
- Main branch developers should be aware of NetworkPlay changes
- NetworkPlay should monitor Main branch commits to core files
- Consider feature flags to disable NetworkPlay features during testing

#### 7. **Extract Delta Sync to Plugin Architecture**
Longer-term refactoring to make delta sync a pluggable component:

```java
// Core game uses abstraction
public interface IStateSyncStrategy {
    void syncState(GameView gameView);
}

// NetworkPlay provides implementation
public class DeltaSyncStrategy implements IStateSyncStrategy { ... }
```

This would completely isolate network changes from core.

---

## Summary Table

| File | Module | Risk | Conflict Probability | Recommendation |
|------|--------|------|---------------------|----------------|
| TrackableObject.java | forge-game | 🔴 High | 60% | Isolate via adapter pattern |
| AbstractGuiGame.java | forge-gui | 🔴 High | 80% | Extract network methods to subclass |
| GameLobby.java | forge-gui | 🟡 Medium | 40% | Add detailed comments |
| IGameController.java | forge-gui | 🟡 Medium | 30% | Split into network interface |
| IGuiGame.java | forge-gui | 🟡 Medium | 30% | Split into network interface |
| PlayerZoneUpdate.java | forge-gui | 🟢 Low | 10% | Accept as-is |
| StackItemView.java | forge-game | 🟢 Low | 5% | Accept as-is |
| PlayerControllerHuman.java | forge-gui | 🟢 Low | 5% | Accept as-is |

---

## Conclusion

The NetworkPlay branch has **violated the goal of avoiding architectural changes to non-network functionality**. The most concerning changes are:

1. **TrackableObject.set() visibility change** - affects core game mechanics
2. **AbstractGuiGame.setGameView() modification** - changes fundamental initialization
3. **Interface pollution** - IGameController and IGuiGame extended with network methods

**Estimated merge conflict probability: 60-80%** if Main branch continues active development.

**Recommended Priority Actions**:
1. ✅ Isolate TrackableObject changes via adapter (1-2 days work)
2. ✅ Split network interfaces from core interfaces (1 day work)
3. ⚠️ Document GameLobby execution order change (30 minutes)
4. 📊 Set up merge monitoring to catch Main branch changes early

Without remediation, expect significant merge pain when NetworkPlay integrates into Main.
