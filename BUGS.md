# Known Bugs and Issues

This file tracks known bugs in the Forge codebase, particularly for the NetworkPlay branch. It serves as a reference for debugging sessions and tracks progress on fixes.

---

## Active Bugs

### 1. /skipreconnect AI takeover not working properly

**Status:** In Progress
**Branch:** NetworkPlay
**Severity:** High

**Description:**
When a client disconnects during a network game and the host uses the `/skipreconnect` command, the player is converted to AI control but the AI does not take any actions. The game appears to stall after the conversion.

**Symptoms:**
- AI takeover mechanics work correctly (player found, inputs cleared, latch released, controller replaced)
- Game loop continues and phase advances (e.g., to COMBAT_BEGIN)
- But no further priority requests are made - the AI doesn't act
- Delta syncs continue (game is running) but no InputPassPriority is created for anyone

**Root Cause Analysis:**
Still under investigation. Current hypothesis is that the game loop may be stuck or `givePriorityToPlayer` is being set to false unexpectedly.

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

5. **Log analysis (2026-01-21 21:48)**
   - Test showed AI takeover worked: player found, inputs cleared, latch released, controller replaced
   - Phase advanced to COMBAT_BEGIN, priority shifted to host (player 0)
   - **Critical finding:** No `chooseSpellAbilityToPlay` calls logged after AI takeover
   - Delta syncs stopped at 21:48:47.361, indicating game loop stopped progressing
   - The game loop should have called host's `chooseSpellAbilityToPlay` but didn't

6. **Added comprehensive entry logging** (Current)
   - `PlayerControllerHuman.chooseSpellAbilityToPlay()`: Added ENTRY log with phase and isGameOver status
   - Added logging for all early return paths (mayAutoPass, skipPhase, autoYield)
   - `PlayerControllerAi.chooseSpellAbilityToPlay()`: Added ENTRY and return logging
   - `FServerManager.convertPlayerToAI()`: Added game state logging after controller replacement

**Current Hypothesis:**
The game loop may be exiting prematurely (possibly `game.isGameOver()` returning true) or something is preventing the next call to `chooseSpellAbilityToPlay`. The new logging should reveal whether the method is being called at all after the AI takeover.

**Next Steps:**
- Run test with new comprehensive logging
- Check if `chooseSpellAbilityToPlay` ENTRY log appears for host after AI takeover
- Check `isGameOver` status in the logs
- If no ENTRY log appears, investigate why game loop stopped calling the method

**Test Procedure:**
1. Start host instance, start client instance
2. Change client name to "TestClient" in lobby
3. Start game, let client take a turn
4. Close client application mid-game (while client has priority)
5. On host, type `/skipreconnect` in chat
6. Observe if AI takes actions

---

## Resolved Bugs

### 2. Phase marker not updating on client

**Status:** Resolved
**Branch:** NetworkPlay

**Description:**
The phase indicator on the client side wasn't updating to reflect the current game phase.

**Resolution:**
Fixed in delta sync implementation by ensuring phase changes are properly tracked and synchronized.

---

### 3. Client hand not visible during mulligan

**Status:** Resolved
**Branch:** NetworkPlay
**Commit:** f06d2da2a7

**Description:**
The client couldn't see their hand cards before making a mulligan decision.

**Root Cause:**
ID collision between GAMEVIEW_DELTA_KEY and card IDs. The delta sync was using ID 0 for the game view delta marker, which conflicted with actual card IDs.

**Resolution:**
Changed GAMEVIEW_DELTA_KEY from 0 to Integer.MIN_VALUE to avoid collision with card IDs.

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
