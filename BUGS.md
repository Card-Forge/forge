# Known Bugs and Issues

This file tracks known bugs in the Forge codebase, particularly for the NetworkPlay branch. It serves as a reference for debugging sessions and tracks progress on fixes.

---

## Active Bugs

### Bug #7: Multiplayer (3-4 Player) Network Games Failing - CRITICAL REGRESSION

**Status:** Under Investigation
**Priority:** CRITICAL
**Branch:** NetworkPlay
**Date Identified:** 2026-01-24

**Symptoms:**
- 2-player network games: 100% success rate (40/40 games)
- 3-player network games: 0% success rate (0/26 games)
- 4-player network games: 0% success rate (0/15 games)
- 9 checksum mismatches detected across failed games

**Error Pattern (from logs):**
```
[DeltaSync] Checksum details (client state):
  GameView ID: 4
  Turn: 0
  Phase: null
  Player 0 (Alice (Host AI)): Life=20, Hand=5, GY=0, BF=0
  Player 1 (Bob (Remote)): Life=20, Hand=0, GY=0, BF=0
  Player 2 (Charlie (Remote)): Life=20, Hand=0, GY=0, BF=0
  Player 3 (Diana (Remote)): Life=20, Hand=0, GY=0, BF=0
```

**Key Observations:**
1. Failures occur at Turn 0, Phase null (very early in game initialization)
2. Remote players show Hand=0 on client while server has Hand=5 for host AI
3. This suggests initial hand state is not being synchronized to clients in 3-4 player games
4. 2-player games are completely unaffected (100% success)

**Potential Root Causes to Investigate:**
1. Initial state transmission differs between 2-player and multiplayer
2. Delta sync serialization issue specific to >2 players
3. Client initialization order/timing issue with multiple connections
4. Hand visibility rules may differ in multiplayer context

**Files Likely Involved:**
- `forge-gui/src/main/java/forge/gamemodes/net/DeltaSyncManager.java` - Delta packet creation
- `forge-gui/src/main/java/forge/gamemodes/net/server/HostedMatch.java` - Game hosting
- `forge-gui-desktop/src/test/java/forge/net/HeadlessNetworkClient.java` - Test client
- `forge-gui-desktop/src/test/java/forge/net/scenarios/MultiplayerNetworkScenario.java` - Multiplayer test

**Steps Taken:**
1. Ran comprehensive 100-game test (2026-01-24)
2. Identified pattern: 2-player=100% success, 3-4 player=0% success
3. Documented in TESTING_DOCUMENTATION.md
4. Added to BUGS.md for tracking

**Next Steps:**
1. Review logs from failed 3-player games to find exact failure point
2. Compare delta sync initialization between 2-player and 3-player games
3. Check if this is a regression from recent changes or pre-existing issue
4. Add targeted debug logging to initial game state transmission

---

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
