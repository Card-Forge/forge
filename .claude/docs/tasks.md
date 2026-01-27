# NetworkPlay Branch - Action Items

This document tracks current tasks and issues to be addressed. Review at the start of each session and update as work progresses.
Proceed with tasks in sequential order.

## Active Tasks
---

### 1. Create plan for staged feature implementation

**Status:** COMPLETE
**Priority:** MEDIUM

**Task:**  NetworkPlay branch has been discussed with Master Forge developers. Developers have noted that Branch includes extensive new code (~10,000 lines) and that review may be challenging.
Developers have suggested their likely approach will be to incorporate in stages, with features such as reconnect support prioritised.
Create a plan document (.documentation/StagedPR.md) setting out a staged approach to implementation.

**Resolution:** Created comprehensive `.documentation/StagedPR.md` document covering:
- Executive summary with branch statistics and key achievements
- 5 feature categories with detailed file inventories and line counts
- Dependency analysis showing Chat Improvements and Testing Tools are fully independent
- Recommended PR sequence: Chat (200 lines) -> Protocol (4,500 lines) -> UI (500 lines) -> Reconnection (1,250 lines) -> Testing (9,650 lines)
- Per-feature review guidance with focus areas and test recommendations
- Dependency diagram showing Protocol as foundational for Reconnection Support
- Alternative submission strategies for upstream reviewers

---

### 2. Unique Decks Undercount

**Status:** COMPLETE
**Priority:** LOW

**Issue Found:** The unique decks reporting in the most recent comprehensive test review seems to significantly undercount decks across all games. Investigate counting methodology.

**Resolution:** Fixed two issues:
1. Updated `DECK_NAME_PATTERN` regex in `NetworkLogAnalyzer.java` to capture additional log formats: "deck pre-loaded:", "deck=", and "with deck:" patterns
2. Fixed truncation of deck names containing parentheses (e.g., "Core Set 2019 Welcome Deck (BG)")
3. Updated `NetworkClientTestHarness.configureHostPlayer()` to log the host deck name

Verified with test run: 80.7% capture rate on archived logs (expected, since old logs didn't have host deck logging). New logs will capture 100%.

---

### 3. Improve bandwidth reporting

**Status:** COMPLETE
**Priority:** LOW

**Issue Found:** Bandwidth reporting in test results should use GB once file sizes reach that size for ease of reading.

**Resolution:** Updated `formatBytes()` methods in `AnalysisResult.java` and `MultiProcessGameExecutor.java` to support GB formatting when sizes exceed 1GB.

---

### 4. Update .documentation/Debugging.md

**Status:** COMPLETE
**Priority:** LOW

**Issue Found:** .documentation/Debugging.md refers to resolved issues in detail - these should be moved to the table at the end. Only active bugs should be outlined in detail. CLAUDE.md should be updated to make this clear.

**Resolution:** Moved bugs #9, #10, #11 from detailed descriptions to the Resolved Bugs summary table. Updated CLAUDE.md to clarify .documentation/Debugging.md structure convention.

---

### 5. Update CLAUDE.md to provide context on Forge development

**Status:** COMPLETE
**Priority:** LOW

**Task:**  CLAUDE.md should note context that Forge is developed by a small group of volunteers on an intermittent and part-time basis.

**Resolution:** Added development context note to Project Overview section in CLAUDE.md.

---

### 6. Batch Test Analyzer Improvements

**Status:** COMPLETE
**Priority:** MEDIUM

**Task:** Enhance batch test analyzer with error analysis, failure classification, and pattern detection capabilities.

**Resolution:** Implemented in 3 files:
1. `GameLogMetrics.java`: Added `FailureMode` enum (NONE, TIMEOUT, CHECKSUM_MISMATCH, EXCEPTION, INCOMPLETE), `failureMode` field, and `firstErrorTurn` tracking
2. `NetworkLogAnalyzer.java`: Added `determineFailureMode()` method, timeout pattern detection, first error turn tracking during parsing
3. `AnalysisResult.java`: Added aggregation fields (failureModeCounts, errorFrequency, batchStats, turnHistogram, maxConsecutiveFailures, firstHalfSuccessRate, secondHalfSuccessRate, warningsLeadingToFailure) and new report sections (Turn Distribution, Failure Mode Analysis, Batch Performance, Top Errors, Failure Patterns, Stability Trend)

**Additional Fixes (same session):**
- Fixed timestamp extraction pattern to handle new `run` prefix in log filenames (`network-debug-runYYYYMMDD-...`)
- Fixed timestamp filtering to use `!isBefore()` instead of `isAfter()` to include same-second logs (millisecond precision issue)
- Added `analyzeExistingLogs` test method with `-Dtest.batchId=YYYYMMDD-HHMMSS` parameter for analyzing specific batches

---

### 7. Checkstyle Violations (Low Priority)

**Status:** NOT STARTED
**Priority:** LOW

**Context:** During compilation of batch test analyzer changes, pre-existing checkstyle violations were detected. These are NOT in NetworkPlay code but in core Forge files. Total violations: ~9250 across the codebase.

**Notable Files with Violations:**
- `forge-core/src/main/java/forge/util/ThreadUtil.java`: Missing Javadoc comments, FinalParameters violations, line length, brace style issues
- `forge-core/src/main/java/forge/util/Visitor.java`: Javadoc style issues

**Notes:**
- These violations exist in the main Forge codebase and predate NetworkPlay changes
- The `-Dcheckstyle.skip=true` flag can be used to bypass during builds
- Fixing these is outside NetworkPlay scope and should be coordinated with main Forge maintainers

---

### 8. Investigate Multiplayer Desync (Bug #12)

**Status:** NOT STARTED
**Priority:** HIGH

**Issue:** All 3+ player network games fail with CHECKSUM_MISMATCH while 2-player games pass 100%.

**Key Symptoms:**
- Desync occurs early (Turn 0-1)
- Warning: `[NetworkDeserializer] Collection lookup failed: type=, id=XXX - NOT FOUND in tracker or oldValue`
- Empty `type=` field in warning suggests missing type information
- Sequential IDs failing (220, 221, 222...) suggests batch of untracked objects

**Investigation Steps:**
1. Run fresh quick test to confirm issue persists after recent changes
2. Compare logs from successful 2-player game vs failed 3-player game
3. Check `NetworkDeserializer.createObjectFromData()` for how type-specific lookups work
4. Review `DeltaSyncManager` per-client tracking for multi-client scenarios
5. Check if object ID assignment differs based on client registration order

**Relevant Files:**
- `forge-gui/src/main/java/forge/gamemodes/net/client/NetworkDeserializer.java`
- `forge-gui/src/main/java/forge/gamemodes/net/server/DeltaSyncManager.java`
- `forge-game/src/main/java/forge/game/GameView.java`

**Test Commands:**
```bash
# Run quick test (10 games)
mvn -pl forge-gui-desktop -am verify -Dtest="ComprehensiveDeltaSyncTest#runQuickDeltaSyncTest" -Dsurefire.failIfNoSpecifiedTests=false -Dcheckstyle.skip=true

# Analyze specific batch
mvn -pl forge-gui-desktop -am verify -Dtest="ComprehensiveDeltaSyncTest#analyzeExistingLogs" -Dtest.batchId=YYYYMMDD-HHMMSS -Dsurefire.failIfNoSpecifiedTests=false -Dcheckstyle.skip=true
```

**See Also:** `.documentation/Debugging.md` Bug #12 for full details

---

### 9. Implement Remote AI Actions for Network Testing

**Status:** NOT STARTED
**Priority:** MEDIUM

**Issue:** Remote clients in network tests (`HeadlessNetworkClient`) auto-pass priority every time instead of making actual gameplay decisions. This results in:
- Host AI (Alice) winning 100% of games
- Limited network protocol coverage (mostly server→client, not client→server actions)
- Less diverse game states for delta sync testing
- Logs that don't show realistic bidirectional gameplay

**Root Cause Analysis:**
- Host (Slot 0): Uses `LobbySlotType.AI` → gets full `PlayerControllerAi` decision-making
- Remote clients (Slots 1-3): Use `LobbySlotType.OPEN` → `DeltaLoggingGuiGame` just auto-clicks OK
- In `HeadlessNetworkClient.java:492-496`, `updateButtons()` immediately schedules `selectButtonOk()` when OK is enabled

**Technical Challenge:**
- `PlayerControllerAi` requires the actual `Game` object (server-side only)
- Remote clients only have `GameView` (read-only view layer)

**Potential Solutions:**
1. **Server-side AI for remote slots** - Change remote slots to `LobbySlotType.AI` while keeping network clients connected for delta verification. AI decisions made server-side, clients just observe/verify.
2. **Client-side simplified AI** - Implement basic decision logic using `GameView` (limited but tests full round-trip)
3. **Hybrid approach** - Server runs AI, clients verify state matches their view

**Implementation Steps:**
1. Research how `LobbySlotType.AI` slots interact with connected network clients
2. Test if changing remote slots to AI type breaks client connectivity
3. If option 1 works, update `MultiplayerNetworkScenario` to use AI for all slots
4. Verify delta sync still functions with server-side AI for remote players
5. Update documentation to reflect that all players are AI-controlled

**Relevant Files:**
- `forge-gui-desktop/src/test/java/forge/net/HeadlessNetworkClient.java` (DeltaLoggingGuiGame class)
- `forge-gui-desktop/src/test/java/forge/net/scenarios/MultiplayerNetworkScenario.java` (slot configuration)
- `forge-gui/src/main/java/forge/gamemodes/match/LobbySlot.java`
- `forge-gui/src/main/java/forge/gamemodes/match/LobbySlotType.java`

**Benefits of Fix:**
- More comprehensive network protocol testing
- Diverse game states reveal edge cases in delta sync
- Bidirectional traffic testing (client actions → server → broadcast)
- More useful debug logs showing actual gameplay from all players

---

### 10. Verify Error Context Extraction Feature

**Status:** NOT STARTED
**Priority:** LOW

**Background:** Implemented error context extraction feature to help Claude debug test failures. The feature adds:
- `LogContextExtractor.java`: New class that extracts game state and log lines around errors
- `GameLogMetrics.java`: Added `errorContext` field to store extracted context
- `NetworkLogAnalyzer.java`: Calls extractor after detecting errors
- `AnalysisResult.java`: New "Error Context for Failed Games" section in reports

**Verification Steps:**
1. Run quick test to generate logs with potential errors:
   ```bash
   mvn -pl forge-gui-desktop -am verify -Dtest="ComprehensiveDeltaSyncTest#runQuickDeltaSyncTest" -Dsurefire.failIfNoSpecifiedTests=false -Dcheckstyle.skip=true
   ```
2. Review generated `comprehensive-test-results-*.md` for new "Error Context for Failed Games" section
3. Verify the section shows:
   - Turn and phase at error
   - Player states table (Life, Hand, GY, Battlefield)
   - Warnings before error
   - Lines around error with context

**Files Created/Modified:**
- `forge-gui-desktop/src/test/java/forge/net/analysis/LogContextExtractor.java` (NEW)
- `forge-gui-desktop/src/test/java/forge/net/analysis/GameLogMetrics.java` (modified)
- `forge-gui-desktop/src/test/java/forge/net/analysis/NetworkLogAnalyzer.java` (modified)
- `forge-gui-desktop/src/test/java/forge/net/analysis/AnalysisResult.java` (modified)
