# Known Bugs and Issues

This file tracks known bugs in the Forge codebase, particularly for the NetworkPlay branch. It serves as a reference for debugging sessions and tracks progress on fixes.

---

## Active Bugs

### 5. Checksum Mismatch Every 20 Packets

**Status:** Under Investigation
**Branch:** NetworkPlay
**Severity:** Medium

**Description:**
Client consistently detects checksum mismatches at every CHECKSUM_INTERVAL (20 packets), triggering automatic full state resyncs. The game continues to function due to the resync mechanism, but this causes unnecessary network traffic.

**Symptoms:**
- ERROR logs every 20 packets: "CHECKSUM MISMATCH! Server=X, Client=Y"
- Values are vastly different (not off-by-one), e.g., Server=-630165399, Client=828400217
- Full state resync requested and completed successfully
- Game continues normally after resync

**Log Evidence (2026-01-22):**
```
[19:26:21.559] [ERROR] [DeltaSync] CHECKSUM MISMATCH! Server=-630165399, Client=828400217 at seq=20
[19:26:32.681] [ERROR] [DeltaSync] CHECKSUM MISMATCH! Server=-601536248, Client=857029368 at seq=40
[19:26:39.170] [ERROR] [DeltaSync] CHECKSUM MISMATCH! Server=-572907097, Client=885658519 at seq=60
[19:26:48.924] [ERROR] [DeltaSync] CHECKSUM MISMATCH! Server=-544277946, Client=914287670 at seq=80
```

**Hypothesis:**
Host computes checksum from server-side GameView while client computes from its local GameView. The checksum algorithm may be accessing fields that differ between server and client representations, or there may be object identity differences affecting hash computation.

**Files Involved:**
- forge-gui/src/main/java/forge/gamemodes/net/server/DeltaSyncManager.java (host checksum)
- forge-gui/src/main/java/forge/gamemodes/match/AbstractGuiGame.java (client checksum)

**Steps Taken:**
1. Identified issue via /reviewlogs analysis (2026-01-22)
2. Enhanced checksum mismatch logging shows client state details
3. Added host-side checksum logging for comparison (2026-01-22)

---

## Resolved Bugs

| # | Bug | Branch | Resolution | Commit |
|---|-----|--------|------------|--------|
| 1 | /skipreconnect AI takeover not working | NetworkPlay | Race condition fix: reorder operations to replace controller before clearing inputs | ea49b699e4 |
| 2 | Phase marker not updating on client | NetworkPlay | Fixed delta sync to track phase changes | - |
| 3 | Client hand not visible during mulligan | NetworkPlay | Changed GAMEVIEW_DELTA_KEY from 0 to Integer.MIN_VALUE to avoid ID collision | f06d2da2a7 |
| 4 | Collection deserialization fails to find object id=1 | NetworkPlay | Implemented composite delta keys throughout + fixed client-side type-specific lookup in createObjectFromData | TBD |

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
