# Known Bugs and Issues

This file tracks known bugs in the Forge codebase, particularly for the NetworkPlay branch. It serves as a reference for debugging sessions and tracks progress on fixes.

---

## Active Bugs

### 4. Collection deserialization fails to find object id=1

**Status:** Under Investigation
**Branch:** NetworkPlay
**Severity:** Medium

**Description:**
When deserializing collections (Library, Hand) on the client, object with id=1 is not found in the tracker, causing missing cards.

**Symptoms:**
- Warnings: `Collection lookup failed: type=, id=1 - NOT FOUND in tracker or oldValue`
- Library shows 59 cards instead of 60
- Occurs consistently during initial sync

**Log Evidence (2026-01-22 08:15):**
```
[08:15:19.141] Collection lookup failed: type=, id=1 - NOT FOUND in tracker or oldValue
[08:15:19.141] Collection read: type=, size=60, found=59, notFound=1
[08:15:19.141] PlayerView 0: setting Library = Collection[59]
```

**Hypothesis:**
Possible ID collision - PlayerViews use id=0 and id=1, which may conflict with CardView id=1. Similar to the GAMEVIEW_DELTA_KEY collision fixed in bug #3.

**Files Involved:**
- `forge-gui/src/main/java/forge/gamemodes/net/NetworkDeserializer.java`
- `forge-gui/src/main/java/forge/gamemodes/match/AbstractGuiGame.java`

**Steps Taken:**
1. Identified issue via /reviewlogs analysis (2026-01-22)

---

## Resolved Bugs

| # | Bug | Branch | Resolution | Commit |
|---|-----|--------|------------|--------|
| 1 | /skipreconnect AI takeover not working | NetworkPlay | Race condition fix: reorder operations to replace controller before clearing inputs | ea49b699e4 |
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

### Log Prefixes

Key log prefixes used by NetworkDebugLogger:
- `[chooseSpellAbilityToPlay]` - Priority decisions (PlayerControllerHuman)
- `[InputQueue]` - Input stack management
- `[InputSyncronizedBase]` - Latch operations
- `[AI Takeover]` - AI conversion process
- `[DeltaSync]` - Delta synchronization
