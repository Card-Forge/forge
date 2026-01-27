# Known Bugs and Issues

This file tracks known bugs in the Forge codebase, particularly for the NetworkPlay branch. It serves as a reference for debugging sessions and tracks progress on fixes.

---

## Active Bugs

### Bug #12: Multiplayer (3+ player) Desync - Collection Lookup Failures

**Status:** INVESTIGATING
**Severity:** HIGH
**Affected:** 3-player and 4-player network games

**Symptoms:**
- All 2-player games pass (100% success rate)
- All 3+ player games fail with CHECKSUM_MISMATCH
- Desync occurs early (often Turn 0-1)

**Key Error Pattern:**
```
[WARN] [NetworkDeserializer] Collection lookup failed: type=, id=220 - NOT FOUND in tracker or oldValue
[ERROR] [DeltaSync] Checksum details (client state):
[ERROR] [DeltaSync]   GameView ID: 4
[ERROR] [DeltaSync]   Turn: 0
[ERROR] [DeltaSync]   Phase: null
```

**Analysis:**
- The "Collection lookup failed" warnings indicate delta sync is referencing object IDs that don't exist in the client's tracker
- Empty `type=` field suggests the type information is missing or not being transmitted
- Multiple sequential IDs (220, 221, 222...) failing suggests a batch of objects not being tracked
- This may be related to how multiple clients share GameView state

**Investigation Notes:**
- Compare with Bug #7 (previously fixed multiplayer issue) - similar symptoms but different root cause
- Check if recent changes to per-client property tracking introduced regression
- Review `NetworkDeserializer.createObjectFromData()` for type-specific lookup logic
- Check if client registration order affects object ID assignment

**Relevant Files:**
- `forge-game/src/main/java/forge/game/GameView.java`
- `forge-gui/src/main/java/forge/gamemodes/net/server/DeltaSyncManager.java`
- `forge-gui/src/main/java/forge/gamemodes/net/client/NetworkDeserializer.java`

**Test Command:**
```bash
mvn -pl forge-gui-desktop -am verify -Dtest="ComprehensiveDeltaSyncTest#analyzeExistingLogs" -Dtest.batchId=20260127-213221 -Dsurefire.failIfNoSpecifiedTests=false -Dcheckstyle.skip=true
```

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
| 9 | Log messages appear duplicated in single-JVM network tests | NetworkPlay | Added `isServerSide()` and `getLogPrefix()` methods to NetworkGuiGame. Log messages now include `[Server]` or `[Client]` prefix to distinguish the source. | 4b0ea811a3 |
| 10 | Intermittent checksum mismatch in 4-player games (PlayerView ID mismatch) | NetworkPlay | Modified GameClientHandler.java to extract server-assigned PlayerView IDs from GameView and apply them to RegisteredPlayers before Game creation, ensuring consistent IDs between server and client. | - |
| 11 | "Address already in use: bind" port conflict during tests | NetworkPlay | Added `SO_REUSEADDR` option to server socket in FServerManager.java; added 500ms delay between batches in MultiProcessGameExecutor.java | ebc9a8823e |

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
