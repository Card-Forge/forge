# Network Debug Logger Optimization Plan

**Status:** Draft - pending decision on implementation approach
**Created:** 2026-01-27

## Problem Statement
Network debug log files can grow to ~10-20MB per game, making storage and analysis cumbersome. Analysis of a sample 15,579-line log shows:
- **DEBUG**: 9,698 lines (62%) - mostly per-object details
- **INFO**: 5,412 lines (35%) - operation milestones
- **WARN/ERROR**: ~469 lines (3%)

## Goal
Achieve **70-80% log file size reduction** while preserving debugging utility for network synchronization analysis.

---

## Approach Options Under Consideration

### Option 1: Add TRACE Level (Simple)
- Add new `TRACE` log level below `DEBUG` for per-object details
- Convert repetitive per-object logging from `DEBUG` to `TRACE`
- Keep `DEBUG` as default (smaller reduction ~50%, preserves debug capability)
- **Pros:** Simple to implement, backward compatible
- **Cons:** ~50% reduction only, or lose debug capability if default changed to INFO

### Option 2: Smart Conditional Logging (Recommended)
- Log summaries at INFO level by default
- Keep circular buffer (~1500 lines) of recent DEBUG/TRACE entries in memory
- On ERROR/WARN, automatically flush buffer to file for debugging context
- **Pros:** Small logs normally, full context when issues occur
- **Cons:** Moderate complexity, ~200KB memory overhead

### Option 3: Compression
- Keep current verbosity, add automatic gzip compression
- **Pros:** No information loss
- **Cons:** Doesn't reduce I/O, harder to tail logs in real-time

### Option 4: Sampling
- Log every Nth occurrence of repetitive patterns
- **Pros:** Representative data with less volume
- **Cons:** May miss critical detail at the wrong moment

---

## Option 2 Implementation Details (Smart Conditional Logging)

### Mechanism
```java
// NetworkDebugLogger.java additions:
private static final int BUFFER_SIZE = 1500;
private static final Deque<String> recentEntries = new ArrayDeque<>(BUFFER_SIZE);

private static void logAtLevel(LogLevel level, String message) {
    // Always add to circular buffer for context
    if (level.isLoggable(LogLevel.DEBUG)) {
        addToBuffer(message);
    }

    // Write to file based on configured level
    if (level.isLoggable(fileLevel)) {
        writeToFile(message);
    }

    // On error/warn, flush buffer for debugging context
    if (level == LogLevel.ERROR || level == LogLevel.WARN) {
        flushBufferToFile();
    }
}
```

### Files to Modify
| File | Changes |
|------|---------|
| `forge-gui/.../NetworkDebugLogger.java` | Add TRACE level, circular buffer, flush mechanism |
| `forge-gui/.../NetworkDebugConfig.java` | Add buffer size config, change default file level |
| `forge-gui/.../NetworkPropertySerializer.java` | Convert debugLog() to trace() |
| `forge-gui/.../server/DeltaSyncManager.java` | Convert ~8 debug() calls to trace() |
| `forge-gui/.../NetworkGuiGame.java` | Convert ~15 debug() calls to trace() |
| `forge-gui/.../NetworkTrackableDeserializer.java` | Convert collection logging to trace() |
| `forge-gui/NetworkDebug.config` | Add buffer settings, update defaults |

---

## Most Verbose DEBUG Patterns (For Reference)

1. **Per-card CSV serialization** - 2 lines per card (serialize + deserialize)
2. **Empty collection reads** - `Collection read: type=, size=0, found=0, notFound=0`
3. **PlayerView verification** - Every delta packet (2 lines per packet)
4. **Delta application details** - Per-object application logging
5. **collectDeltas logging** - Every sync cycle

---

## Expected Results

| Metric | Before | After (Option 2) |
|--------|--------|------------------|
| Normal log size | 10-20 MB | 2-4 MB |
| Debug capability | Full | Full (via buffer flush) |
| Memory overhead | None | ~200KB |

---

## Verification Plan

1. Run 2-player test game with default config
2. Verify normal log file is ~2-4MB
3. Inject an error condition, verify buffer flushes context
4. Run NetworkLogAnalyzer to confirm metrics extraction works
5. Run batch test to confirm no regressions
