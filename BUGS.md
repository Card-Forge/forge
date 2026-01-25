# Known Bugs and Issues

This file tracks known bugs in the Forge codebase, particularly for the NetworkPlay branch. It serves as a reference for debugging sessions and tracks progress on fixes.

---

## Active Bugs

### Bug #9: Log messages appear duplicated in single-JVM network tests

**Status:** Fixed
**Discovered:** 2026-01-25 via comprehensive test observation
**Severity:** Low (cosmetic - does not affect functionality)

**Description:**
In single-JVM network tests (where server and client run in the same process), log messages appeared duplicated because both NetGuiGame (server) and HeadlessNetworkGuiGame (client) extend NetworkGuiGame and log to the same file.

**Root Cause:**
Both server and client instances call methods like setGameView(), which log via NetworkDebugLogger. In single-JVM tests, both write to the same log file with the same instance suffix, causing apparent duplicates.

**Fix:**
Added `isServerSide()` and `getLogPrefix()` methods to NetworkGuiGame:
- NetGuiGame overrides `isServerSide()` to return `true`
- Log messages now include `[Server]` or `[Client]` prefix to distinguish the source

**Files changed:**
- `NetworkGuiGame.java` - Added isServerSide(), getLogPrefix(), updated log format
- `NetGuiGame.java` - Override isServerSide() to return true

### Bug #10: Intermittent checksum mismatch in 4-player games

**Status:** Open - Root Cause Identified
**Discovered:** 2026-01-25 via comprehensive test (run20260125-140616)
**Frequency:** 1 in 100 games (batch1-game2-4p)
**Severity:** Medium

**Details:**
- Game: `network-debug-run20260125-140616-batch1-game2-4p-test.log`
- Status: DESYNC at Turn 10, seq=1180
- 4-player game

**Root Cause Analysis:**

Client and server are computing checksums on **different GameView objects**:

| Property | Server State | Client State |
|----------|-------------|--------------|
| GameView ID | **1** | **3** (wrong!) |
| Turn | 10 | 10 |
| Phase | **UPKEEP** | **UNTAP** (wrong!) |
| Player 0 | Bob (Remote) | Alice (Host AI) |
| Player 1 | Charlie (Remote) | Bob (Remote) |
| Player 2 | Diana (Remote) | Charlie (Remote) |
| Player 3 | Alice (Host AI) | Diana (Remote) |

**The client has a completely different GameView object** with:
1. Wrong GameView ID (3 vs 1)
2. Wrong phase (UNTAP vs UPKEEP)
3. Wrong player ordering (rotated)

**Additional Log Analysis:**

Looking at the log more carefully, there are MULTIPLE server-side checksum computations for seq=1180:
1. First server logs checksum=-41803267 at END_OF_TURN
2. Second server logs checksum=-1831154159 at UNTAP
3. Third server logs checksum=656358674 at UPKEEP

This is because each remote client has its own DeltaSyncManager with its own sequence counter. All three reach seq=1180 at different game phases.

The mismatch occurs because:
- Client receives packet with checksum=656358674 (from UPKEEP)
- Client's state after applying delta has checksum=-1831154159 (UNTAP state)

**Revised Hypothesis:** The client is computing checksum BEFORE fully applying all phase transition deltas, OR the deltas for the phase change are not being applied correctly to this particular client. The per-client property tracking may be incorrectly filtering out some phase change deltas.

**Fix Direction:**
1. Verify delta application completes before checksum validation
2. Check if phase property changes are correctly tracked per-client
3. Ensure `lastSentPropertyChecksums` in DeltaSyncManager is not causing phase updates to be skipped

**Files to investigate:**
- `NetworkGuiGame.java:applyDelta()` - Client-side delta application, line ~186-346
- `DeltaSyncManager.java` - Per-client tracking at line 50-54
- `HeadlessNetworkGuiGame.java` - Client-side game view

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
