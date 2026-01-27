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
