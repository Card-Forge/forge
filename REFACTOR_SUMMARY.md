# Network Code Isolation - Quick Reference

## Problem Statement

AbstractGuiGame.java contains **846 lines** of network-specific deserialization code, creating:
- Merge conflicts when Master branch modifies core game logic
- Code bloat in non-network builds
- Tight coupling between network and core functionality

## Solution Overview

Extract all network code into separate classes using inheritance and interface segregation.

---

## Architecture Transformation

### BEFORE (Current State)

```
┌─────────────────────────────────────────────────────────┐
│  AbstractGuiGame.java (1749 lines)                      │
│  ┌────────────────────────────────────────────────┐    │
│  │  Core Game Logic (900 lines)                   │    │
│  ├────────────────────────────────────────────────┤    │
│  │  ⚠️ NETWORK CODE (846 lines) ⚠️                │    │
│  │  - setGameView() with tracker init             │    │
│  │  - applyDelta() (170 lines)                    │    │
│  │  - fullStateSync() (120 lines)                 │    │
│  │  - createObjectFromData()                      │    │
│  │  - applyDeltaToObject()                        │    │
│  │  - 15+ helper methods                          │    │
│  └────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
                         ↑ extends
         ┌───────────────┴───────────────┐
         │                               │
    NetGuiGame                    MatchController
   (network server)              (mobile client)
```

### AFTER (Refactored)

```
┌─────────────────────────────────────────────────────────┐
│  AbstractGuiGame.java (900 lines)                       │
│  ┌────────────────────────────────────────────────┐    │
│  │  Core Game Logic ONLY                          │    │
│  │  - Simple setGameView()                        │    │
│  │  - No network dependencies                     │    │
│  │  - Stub implementations for network methods   │    │
│  └────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
                         ↑ extends
         ┌───────────────┴───────────────┐
         │                               │
┌────────────────────┐          MatchController
│ NetworkGuiGame.java│         (mobile client)
│ (850 lines)        │
│                    │
│ ✅ ALL network code│
│ - tracker init     │
│ - applyDelta()     │
│ - fullStateSync()  │
│ - all helpers      │
└────────────────────┘
         ↑ extends
    NetGuiGame
   (network server)
```

---

## Interface Transformation

### BEFORE (Current State)

```
┌──────────────────────────────────────┐
│  IGuiGame (Core Interface)           │
│  ┌────────────────────────────────┐  │
│  │ Core Methods (50+ methods)     │  │
│  ├────────────────────────────────┤  │
│  │ ⚠️ Network Methods (8) ⚠️      │  │
│  │ - applyDelta()                 │  │
│  │ - fullStateSync()              │  │
│  │ - gamePaused()                 │  │
│  │ - gameResumed()                │  │
│  │ - reconnectAccepted()          │  │
│  │ - reconnectRejected()          │  │
│  │ - setRememberedActions()       │  │
│  │ - nextRememberedAction()       │  │
│  └────────────────────────────────┘  │
└──────────────────────────────────────┘
```

### AFTER (Refactored)

```
┌──────────────────────────────────────┐
│  IGuiGame (Core Interface)           │
│  ┌────────────────────────────────┐  │
│  │ Core Methods ONLY (50+ methods)│  │
│  │ - No network pollution         │  │
│  └────────────────────────────────┘  │
└──────────────────────────────────────┘
                 ↑ extends
┌──────────────────────────────────────┐
│  INetworkGuiGame (Network Extension) │
│  ┌────────────────────────────────┐  │
│  │ ✅ Network Methods (8)          │  │
│  │ - applyDelta()                 │  │
│  │ - fullStateSync()              │  │
│  │ - gamePaused()                 │  │
│  │ - gameResumed()                │  │
│  │ - reconnectAccepted()          │  │
│  │ - reconnectRejected()          │  │
│  │ - setRememberedActions()       │  │
│  │ - nextRememberedAction()       │  │
│  └────────────────────────────────┘  │
└──────────────────────────────────────┘
```

---

## TrackableObject Transformation

### BEFORE (Current State)

```java
// forge-game/.../trackable/TrackableObject.java

public final <T> void set(...) { }  // ⚠️ Changed to public

public boolean hasChanges() { }      // ⚠️ Added for network
public Set<TrackableProperty> getChangedProps() { }  // ⚠️ Added
public void clearChanges() { }       // ⚠️ Added
public void serializeChangedOnly(...) { }  // ⚠️ Added
```

**Problem:** Public API expanded for network-specific needs

### AFTER (Refactored)

```java
// forge-game/.../trackable/TrackableObject.java

protected final <T> void set(...) { }  // ✅ Restored to protected

final boolean hasChanges() { }      // ✅ Package-private
final Set<TrackableProperty> getChangedProps() { }  // ✅ Package-private
final void clearChanges() { }       // ✅ Package-private
final void serializeChangedOnly(...) { }  // ✅ Package-private

final <T> void setForNetwork(...) {  // ✅ Bridge for network access
    set(key, value);
}
```

**Access via Bridge:**
```java
// forge-game/.../trackable/NetworkTrackableAccess.java

public class NetworkTrackableAccess {
    public static boolean hasChanges(TrackableObject obj) {
        return obj.hasChanges();  // Package-private access
    }

    public static void setProperty(TrackableObject obj, ...) {
        obj.setForNetwork(key, value);
    }
    // ... other methods
}
```

**Result:** Public API restored, network access controlled via bridge

---

## Implementation Phases

### Phase 1: Extract NetworkGuiGame (Priority: P0)
```
⏱️ Estimated: 8 hours

Tasks:
1. Create NetworkGuiGame.java
2. Move 850 lines from AbstractGuiGame → NetworkGuiGame
3. Restore AbstractGuiGame to simple state
4. Update NetGuiGame inheritance
5. Test everything

Files Changed:
- AbstractGuiGame.java (846 lines removed)
- NetworkGuiGame.java (850 lines added, NEW FILE)
- NetGuiGame.java (1 line changed: extends NetworkGuiGame)
```

### Phase 2: Segregate Interfaces (Priority: P0)
```
⏱️ Estimated: 4 hours

Tasks:
1. Create INetworkGuiGame.java (8 methods)
2. Create INetworkGameController.java (3 methods)
3. Remove network methods from IGuiGame
4. Remove network methods from IGameController
5. Update implementations
6. Test everything

Files Changed:
- INetworkGuiGame.java (NEW FILE)
- INetworkGameController.java (NEW FILE)
- IGuiGame.java (8 methods removed)
- IGameController.java (3 methods removed)
- NetworkGuiGame.java (implements INetworkGuiGame)
- NetGameController.java (implements INetworkGameController)
- PlayerControllerHuman.java (remove 3 no-op stubs)
```

### Phase 3: Isolate TrackableObject (Priority: P1)
```
⏱️ Estimated: 4.5 hours

Tasks:
1. Revert TrackableObject.set() to protected
2. Make delta methods package-private
3. Create NetworkTrackableAccess.java
4. Update all network code to use accessor
5. Test everything

Files Changed:
- TrackableObject.java (visibility changes)
- NetworkTrackableAccess.java (NEW FILE)
- DeltaSyncManager.java (use accessor)
- NetworkGuiGame.java (use accessor)
- NetworkPropertySerializer.java (use accessor)
```

### Phase 4: Debug Logging (Priority: P2, OPTIONAL)
```
⏱️ Estimated: 0-3 hours (or skip)

Decision:
- Option A: Create abstraction layer (3 hours)
- Option B: Keep direct references (0 hours) ✅ RECOMMENDED

Rationale: Debug logging has minimal impact and doesn't
affect core logic. Direct references are acceptable.
```

---

## File Changes Summary

### Files Modified
| File | Lines Changed | Change Type |
|------|--------------|-------------|
| AbstractGuiGame.java | -846 lines | Remove network code |
| IGuiGame.java | -8 methods | Remove network methods |
| IGameController.java | -3 methods | Remove network methods |
| TrackableObject.java | ~10 lines | Revert visibility |
| PlayerControllerHuman.java | -3 methods | Remove no-op stubs |
| NetGuiGame.java | 1 line | Change parent class |
| DeltaSyncManager.java | ~20 lines | Use accessor |
| NetworkPropertySerializer.java | ~10 lines | Use accessor |

### Files Added (NEW)
| File | Lines | Purpose |
|------|-------|---------|
| NetworkGuiGame.java | 850 | All network deserialization logic |
| INetworkGuiGame.java | 50 | Network GUI interface |
| INetworkGameController.java | 30 | Network controller interface |
| NetworkTrackableAccess.java | 80 | TrackableObject accessor bridge |

**Total:** ~1,010 lines added, ~890 lines removed, net +120 lines
(Most "new" lines are moved from AbstractGuiGame)

---

## Benefits

### ✅ Reduced Merge Conflicts
- Master branch changes to AbstractGuiGame don't conflict with network code
- Network code lives in separate files

### ✅ No Non-Network Bloat
- Core game classes stay focused and lean
- Network code clearly separated

### ✅ Plugin Architecture
- NetworkPlay becomes a true extension
- Can be enabled/disabled at build time

### ✅ Easier Maintenance
- Network features developed independently
- Clear boundaries between modules

### ✅ Better Testing
- Network code tested in isolation
- Mock implementations easier to create

---

## Testing Checklist

### Unit Tests
- [ ] AbstractGuiGame works for local games
- [ ] NetworkGuiGame works for network games
- [ ] NetworkTrackableAccess delegates correctly
- [ ] Interface implementations compile

### Integration Tests
- [ ] Full network game flow (start → play → end)
- [ ] Reconnection (disconnect → reconnect)
- [ ] Delta sync bandwidth (~90% savings)
- [ ] Checksum validation and auto-resync

### Regression Tests
- [ ] All existing local game tests pass
- [ ] All existing network game tests pass
- [ ] No performance degradation

---

## Master Branch Merge Preview

### Before Refactor
```
Master: AbstractGuiGame.java (1000 lines)
  + NetworkPlay: AbstractGuiGame.java (1850 lines)
  = CONFLICT: 850 lines to reconcile ❌
```

### After Refactor
```
Master: AbstractGuiGame.java (1000 lines)
  + NetworkPlay:
      - AbstractGuiGame.java (1000 lines) ✅ NO CHANGE
      - NetworkGuiGame.java (850 lines) ✅ NEW FILE
  = MERGE: Clean, no conflicts ✅
```

---

## Next Steps

1. **Review** this strategy and REFACTOR_STRATEGY.md
2. **Approve** or provide feedback
3. **Create** feature branch: `refactor/network-isolation`
4. **Implement** Phase 1 (8 hours)
5. **Test** thoroughly
6. **Implement** Phase 2 (4 hours)
7. **Test** thoroughly
8. **Implement** Phase 3 (4.5 hours)
9. **Test** thoroughly
10. **Merge** to NetworkPlay branch
11. **Prepare** for Master merge

**Total Time:** ~16.5 hours of focused work

---

## Questions?

- **Why not keep network code in AbstractGuiGame?**
  → Causes merge conflicts and bloats non-network builds

- **Will this break existing network functionality?**
  → No, it's a pure refactor. Same logic, different location.

- **What if Master branch is already modifying AbstractGuiGame?**
  → That's exactly why we need this refactor - to avoid conflicts

- **Is this safe?**
  → Yes. Incremental approach with testing at each phase. Can rollback if issues.

- **When should we do this?**
  → Before merging to Master. Easier to refactor in NetworkPlay branch first.
