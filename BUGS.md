# Known Bugs and Issues

This file tracks known bugs in the Forge codebase, particularly for the NetworkPlay branch. It serves as a reference for debugging sessions and tracks progress on fixes.

---

## Active Bugs

### 1. /skipreconnect AI takeover not working properly

**Status:** Tentatively Resolved (Monitoring)
**Branch:** NetworkPlay
**Severity:** High

**Description:**
When a client disconnects during a network game and the host uses the `/skipreconnect` command, the player is converted to AI control but the AI does not take any actions. The game appears to stall after the conversion.

**Resolution (2026-01-22):**
After adding comprehensive logging, subsequent tests showed the AI takeover working correctly. The game loop continued after conversion and the host received priority requests as expected. The exact cause of the original failure is not fully understood, but may have been:
- A timing/race condition that was inadvertently fixed by code changes
- Non-deterministic thread scheduling that occasionally caused the game loop to exit
- Different test conditions between failing and passing tests

**Successful Test Log (2026-01-22 06:58):**
```
06:58:13.946 - Latch released, game thread unblocked
06:58:13.947 - chooseSpellAbilityToPlay ENTRY for TestHost (MAIN1)
06:58:13.947 - TestHost returned null (skipPhase)
06:58:13.952 - AI controller installed
06:58:13.977 - Game continued with TestHost in COMBAT_BEGIN
```
Game proceeded correctly through all phases to END_OF_TURN.

**Key Difference from Failed Test:**
In failed test (21:48), no `chooseSpellAbilityToPlay` ENTRY appeared after AI conversion. In successful test, host's method was called immediately after latch release.

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
