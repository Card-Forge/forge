# Known Bugs and Issues

This file tracks known bugs in the Forge codebase, particularly for the NetworkPlay branch. It serves as a reference for debugging sessions and tracks progress on fixes.

---

## Active Bugs

### Bug #9: HeadlessNetworkClient prints log messages twice

**Status:** Open - Needs Investigation
**Discovered:** 2026-01-25 via comprehensive test observation
**Severity:** Low (cosmetic - does not affect functionality)

**Description:**
The HeadlessNetworkClient appears to be printing log messages twice during test execution. This may be due to:
- Duplicate logger configuration (both parent and child loggers active)
- Console handler attached at multiple points in the logging hierarchy
- NetworkDebugLogger and standard Java logging both writing to console

**Impact:**
- Log files are larger than necessary
- Makes log analysis more difficult due to duplicate entries

**Investigation:** TODO - Check logger configuration in HeadlessNetworkClient and NetworkDebugLogger

### Bug #10: Intermittent checksum mismatch in 4-player games

**Status:** Open - Needs Investigation
**Discovered:** 2026-01-25 via comprehensive test (run20260125-140616)
**Frequency:** 1 in 100 games (batch1-game2-4p)
**Severity:** Medium

**Details:**
- Game: `network-debug-run20260125-140616-batch1-game2-4p-test.log`
- Status: DESYNC at Turn 10, UNTAP phase
- 4-player game

**Error:**
```
[14:09:26.294] [ERROR] [DeltaSync] Checksum details (client state):
  GameView ID: 3
  Turn: 10
  Phase: UNTAP
  Player 0 (Alice (Host AI)): Life=20, Hand=7, GY=2, BF=0
  Player 1 (Bob (Remote)): Life=20, Hand=7, GY=2, BF=0
  Player 2 (Charlie (Remote)): Life=20, Hand=7, GY=2, BF=0
  Player 3 (Diana (Remote)): Life=20, Hand=7, GY=0, BF=3
```

**Investigation:** TODO - Compare server state at seq=1180 to identify divergence source

### Bug #11: "Address already in use: bind" port conflict during tests

**Status:** Open - Needs Investigation
**Discovered:** 2026-01-25 via comprehensive test (run20260125-140616)
**Frequency:** 1 in 100 games (batch8-game7-2p)
**Severity:** Low (test infrastructure issue)

**Details:**
- Game: `network-debug-run20260125-140616-batch8-game7-2p-test.log`
- Status: FAIL - 0 turns completed
- Error occurred during game setup

**Error:**
```
[14:25:02.781] [ERROR] [NetworkClientTestHarness] Test failed: Address already in use: bind
```

**Likely Cause:**
Previous game's port not fully released when new game started. May need longer delay between games or better port cleanup.

**Investigation:** TODO - Review port allocation in MultiProcessGameExecutor

---

## Current Test Status

**Comprehensive Test Results (2026-01-25 14:30:15):**
*Source: comprehensive-test-results-20260125-143015.md (archived to testlogs/)*

| Metric | Value |
|--------|-------|
| Total Games | 100 |
| Success Rate | **98%** (98/100) |
| Bandwidth Savings | **99.4%** |
| Checksum Mismatches | 1 |
| Games with Warnings | 24 |

**By Player Count:**
| Players | Success Rate |
|---------|-------------|
| 2-player | 98% (49/50) - 1 port bind failure |
| 3-player | 100% (30/30) |
| 4-player | 95% (19/20) - 1 checksum mismatch |

**2 Failures Explained:**
1. **Checksum mismatch** (1 game): 4-player game desync at Turn 10 (protocol bug - needs investigation)
2. **Port bind failure** (1 game): "Address already in use" error (test infrastructure issue)

---

## Resolved Bugs

| # | Bug | Branch | Resolution | Commit |
|---|-----|--------|------------|--------|
| 1 | /skipreconnect AI takeover not working | NetworkPlay | Race condition fix: reorder operations to replace controller before clearing inputs | ea49b699e4 |
| 2 | Phase marker not updating on client | NetworkPlay | Fixed delta sync to track phase changes | - |
| 3 | Client hand not visible during mulligan | NetworkPlay | Changed GAMEVIEW_DELTA_KEY from 0 to Integer.MIN_VALUE to avoid ID collision | f06d2da2a7 |
| 4 | Collection deserialization fails to find object id=1 | NetworkPlay | Implemented composite delta keys throughout + fixed client-side type-specific lookup in createObjectFromData | 1d564ab2d8 |
| 5 | Checksum mismatch every 20 packets | NetworkPlay | Changed `getPhase().hashCode()` to `getPhase().ordinal()` - hashCode differs between JVMs, ordinal is consistent | 12aeccaac4 |
| 6 | GameView ID in checksum causing mismatch | NetworkPlay | Removed `gameView.getId()` from checksum in both DeltaSyncManager and NetworkGuiGame - GameView ID is a local JVM identifier that differs between server and client | - |
| 7 | Multiplayer (3-4 player) games failing with 0% success | NetworkPlay | Per-client property tracking: Multiple clients share GameView; first client's clearAllChanges() cleared state for all. Added independent checksum tracking per client in DeltaSyncManager. Comprehensive test: 97% success (97/100 games) | 715cc4da68 |
| 8 | HeadlessNetworkClient auto-response race condition causing game timeouts | NetworkPlay | Replaced unsynchronized `new Thread()` calls with single-threaded `ScheduledExecutorService` to serialize all auto-responses. Each new prompt cancels pending responses to prevent stale actions. Comprehensive test: 96% success (96/100 games) | 2aad2f9938 |

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

### Log Prefixes

Key log prefixes used by NetworkDebugLogger:
- `[chooseSpellAbilityToPlay]` - Priority decisions (PlayerControllerHuman)
- `[InputQueue]` - Input stack management
- `[InputSyncronizedBase]` - Latch operations
- `[AI Takeover]` - AI conversion process
- `[DeltaSync]` - Delta synchronization
