# Known Bugs and Issues

This file tracks known bugs in the Forge codebase, particularly for the NetworkPlay branch. It serves as a reference for debugging sessions and tracks progress on fixes.

---

## Active Bugs

### 1. /skipreconnect AI takeover not working properly

**Status:** Fix Applied (Awaiting Verification)
**Branch:** NetworkPlay
**Severity:** High

**Description:**
When a client disconnects during a network game and the host uses the `/skipreconnect` command, the player is converted to AI control but the AI does not take any actions. The game appears to stall after the conversion.

**Root Cause Identified (2026-01-22 07:20):**
Race condition in `convertPlayerToAI()`. The code was:
1. Clearing inputs on old controller (releases game thread)
2. Creating AI controller
3. Replacing controller

Between step 1 and step 3, the game thread could unblock and call `chooseSpellAbilityToPlay()` on the **old** human controller, creating a new InputPassPriority that gets stuck.

**Log Evidence (07:17:16):**
```
07:18:02.714 - clearInputs() releases latch for MAIN1
07:18:02.715 - TestHost gets priority (MAIN1), skips
07:18:02.717 - TestClient's HUMAN controller called for COMBAT_BEGIN (still old!)
07:18:02.719 - AI controller finally installed (too late!)
```
After this, no more `chooseSpellAbilityToPlay` calls - game thread blocked forever.

**Fix (2026-01-22 07:23):**
Reordered operations in `convertPlayerToAI()` to:
1. Get reference to old controller
2. Create AI controller
3. Replace controller (so new calls go to AI)
4. Clear inputs on old controller (releases game thread)

This ensures when the game thread unblocks, it calls the new AI controller's method.

**Files Involved:**
- `forge-gui/src/main/java/forge/gamemodes/net/server/FServerManager.java` - AI takeover logic
- `forge-gui/src/main/java/forge/gamemodes/match/input/InputQueue.java` - Input management
- `forge-gui/src/main/java/forge/gamemodes/match/input/InputSyncronizedBase.java` - Latch handling
- `forge-game/src/main/java/forge/game/phase/PhaseHandler.java` - Game loop and priority

**Steps Taken:**

1. **Fixed player identification issue** (Commit 696e561a2d)
   - Problem: PlayerSession name wasn't updated when client changed name in lobby
   - Fix: Changed from finding player by name to finding by index using `game.getPlayer(playerIndex)`

2. **Fixed resetInputs() blocking issue**
   - Problem: `resetInputs()` called `selectButtonCancel()` which could trigger network communication with disconnected client
   - Fix: Removed `resetInputs()` call, only use `clearInputs()` which directly calls `stop()` on inputs

3. **Added ReplyPool.cancelAll()**
   - Added method to cancel all pending network replies when converting to AI
   - This prevents the game thread from blocking on network responses

4. **Added debug logging to game loop** (PhaseHandler)
   - Added System.out.println logging to `PhaseHandler.mainLoopStep()` and `onPhaseBegin()`
   - Logs show: phase, givePriorityToPlayer flag, priority player, turn player
   - Note: Goes to console, not NetworkDebugLogger (forge-game can't access it)

5. **Log analysis (2026-01-21 21:48)** - Failed test
   - AI takeover mechanics worked but game loop stopped after conversion
   - No `chooseSpellAbilityToPlay` calls logged after AI takeover

6. **Added comprehensive entry logging** (Commit 80898494b0)
   - `PlayerControllerHuman.chooseSpellAbilityToPlay()`: ENTRY log with phase and isGameOver
   - Added logging for all early return paths (mayAutoPass, skipPhase, autoYield)
   - `PlayerControllerAi.chooseSpellAbilityToPlay()`: ENTRY and return logging
   - `FServerManager.convertPlayerToAI()`: Game state logging after controller replacement

7. **Successful test (2026-01-22 06:58)**
   - AI takeover worked correctly
   - Game loop continued, host received priority requests
   - `isGameOver=false` throughout

8. **Failed test (2026-01-22 07:17)** - Race condition identified
   - Log showed clearInputs() called, latch released
   - Game thread unblocked and called HUMAN controller for COMBAT_BEGIN
   - AI controller installed AFTER game thread already blocked again
   - Root cause: operations were in wrong order

9. **Fix applied (2026-01-22 07:23)**
   - Reordered operations: replace controller BEFORE clearing inputs
   - This ensures when game thread unblocks, it calls AI controller

**Monitoring:**
Keep debug logging in place. If the issue recurs, the logs will provide detailed information about the game loop state.

**Test Procedure:**
1. Start host instance, start client instance
2. Change client name to "TestClient" in lobby
3. Start game, let client take a turn
4. Close client application mid-game (while client has priority)
5. On host, type `/skipreconnect` in chat
6. Observe if AI takes actions

---

## Resolved Bugs

| # | Bug | Branch | Resolution | Commit |
|---|-----|--------|------------|--------|
| 2 | Phase marker not updating on client | NetworkPlay | Fixed delta sync to track phase changes | - |
| 3 | Client hand not visible during mulligan | NetworkPlay | Changed GAMEVIEW_DELTA_KEY from 0 to Integer.MIN_VALUE to avoid ID collision | f06d2da2a7 |

---

## Debug Infrastructure

### NetworkDebugLogger

Location: `forge-gui/src/main/java/forge/gamemodes/net/NetworkDebugLogger.java`

Configurable logging for network debugging. Logs go to:
- Console (configurable level)
- File: `forge-gui-desktop/logs/`

Usage:
```java
NetworkDebugLogger.log("[Component] Message with %s formatting", args);
NetworkDebugLogger.debug("[Component] Detailed debug info");
NetworkDebugLogger.warn("[Component] Warning message");
NetworkDebugLogger.error("[Component] Error message");
```

### Game Loop Logging

Temporary logging in `PhaseHandler.java` (forge-game module) uses `System.out.println` since NetworkDebugLogger is not accessible from forge-game.

Key log prefixes:
- `[mainLoopStep]` - Game loop state
- `[onPhaseBegin]` - Phase transitions
- `[chooseSpellAbilityToPlay]` - Priority decisions
- `[InputQueue]` - Input stack management
- `[InputSyncronizedBase]` - Latch operations
- `[AI Takeover]` - AI conversion process
