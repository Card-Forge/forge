---
name: batchtest
description: Runs comprehensive 100-game network test with monitoring, analysis, and documentation updates. Includes real-time failure detection, verified results reporting, and artifact archival.
argument-hint: [quick|full]
user-invocable: true
---

# Comprehensive Network Test

Run, monitor, and document a full network test with failure detection and verified reporting.

## Workflow Checklist

```
- [ ] Step 1: Pre-flight & Start test (IN BACKGROUND)
- [ ] Step 2: ACTIVE MONITORING LOOP (check every 5min, KILL if threshold exceeded)
- [ ] Step 3: Analyze & Report issues (only if test completed successfully)
- [ ] Step 4: Archive test artifacts
- [ ] Step 5: Update documentation (invoke /documentation)
```

**IMPORTANT:** Step 2 is MANDATORY. You must actively poll the output file during test execution, not just wait for completion.

## Variables

Set these after test starts:
- **TASK_ID**: Background task ID returned by Bash tool (e.g., `bbeb1b4`)
- **OUTPUT_FILE**: Path to background task output (e.g., `C:\Users\...\tasks\bbeb1b4.output`)
- **BATCH_ID**: From log filenames (e.g., `run20260125-094059`)
- **RESULTS_FILE**: `comprehensive-test-results-YYYYMMDD-HHMMSS.md`

---

## Step 1: Pre-flight & Start Test

### 1a. Verify Build

```bash
"C:\Program Files\Apache\maven\bin\mvn.cmd" -f "C:\Users\Angas\IdeaProjects\forge\pom.xml" -pl forge-gui-desktop -am compile -q
```

If build fails → fix errors before proceeding.

### 1b. Start Test IN BACKGROUND

**CRITICAL:** You MUST run the test in background mode to enable monitoring.

**Quick (10 games):**
```bash
# Use run_in_background: true
"C:\Program Files\Apache\maven\bin\mvn.cmd" -f "C:\Users\Angas\IdeaProjects\forge\pom.xml" -pl forge-gui-desktop -am verify -Dtest="ComprehensiveDeltaSyncTest#runQuickDeltaSyncTest" -Dsurefire.failIfNoSpecifiedTests=false 2>&1
```

**Full (100 games):**
```bash
# Use run_in_background: true
"C:\Program Files\Apache\maven\bin\mvn.cmd" -f "C:\Users\Angas\IdeaProjects\forge\pom.xml" -pl forge-gui-desktop -am verify -Dtest="ComprehensiveDeltaSyncTest#runComprehensiveDeltaSyncTest" -Dsurefire.failIfNoSpecifiedTests=false 2>&1
```

**Save the task ID and output file path** returned by the background command.

---

## Step 2: Active Monitoring Loop

**YOU MUST implement this monitoring loop. Do NOT just wait for completion.**

### 2a. Monitoring Process

After starting the background test, enter this monitoring loop:

1. **Wait 60 seconds** before first check (ensure test has initialized correctly.)
2. No need to report on progress at first check other than to confirm test is actually running
3. After first check, check output file for batch summaries **every 5 minutes**
4. **Parse each batch result** and evaluate against thresholds
5. **Kill immediately** if thresholds exceeded
6. **Continue until** BUILD SUCCESS/FAILURE appears or kill triggered

### 2b. Understanding Batch Output

**Test Structure:** 100 games = 10 batches of 10 games each

**During execution**, individual batch summaries appear progressively:
```
[MultiProcessGameExecutor] Execution complete: MultiProcess[games=10, success=X, failed=Y, ...]
```

**At completion**, the final summary appears:
```
[MultiProcessGameExecutor] All batches complete: MultiProcess[games=100, success=97, ...]
```

**IMPORTANT:** During monitoring, you may not see all batch summaries yet - they appear as each batch finishes. Count the number of "Execution complete:" lines to track progress (expect 10 for full test).

### 2c. Check Commands

**Check batch progress:**
```bash
grep -E "Execution complete:" <OUTPUT_FILE_PATH> | wc -l   # Count completed batches (expect 10)
grep -E "Execution complete:" <OUTPUT_FILE_PATH>           # See all batch results so far
```

**Check for completion:**
```bash
grep -E "All batches complete|BUILD SUCCESS|BUILD FAILURE" <OUTPUT_FILE_PATH>
```

Parse batch output format:
```
[MultiProcessGameExecutor] Execution complete: MultiProcess[games=10, success=X, failed=Y, ...]
```

### 2d. KILL Thresholds

| Condition | Threshold | Action |
|-----------|-----------|--------|
| Batch failure rate | >30% (failed > 3) | Kill immediately |
| Consecutive failures | 3+ games | Kill immediately |
| Checksum mismatches | >2 | Kill immediately |

**Kill command:**
```bash
taskkill /F /IM java.exe
```

### 2e. Decision Matrix Per Batch

| Result | Action |
|--------|--------|
| Batch ≥90% success | Log "Batch N: OK", continue monitoring |
| Batch 70-90% success | Log "Batch N: CONCERN", continue monitoring |
| Batch <70% success | **KILL** → report to user, do NOT proceed |
| Any checksum mismatch | **KILL** → report to user, do NOT proceed |

### 2f. Monitoring Output Template

Report to user during monitoring:
```
Monitoring test run [TASK_ID]...
  Batches complete: 3/10
  Batch 0: 10/10 success (100%) ✓
  Batch 1: 9/10 success (90%) ✓
  Batch 2: 10/10 success (100%) ✓
  Waiting for remaining batches...
```

If applicable include:
```
 Cause of test failure:
 -[INSERT UNIQUE FAILURE REASON]
```

### 2g. Completion Check

Test is complete when you see ONE of:
- `All batches complete: MultiProcess[games=100, ...]` → Final summary with totals
- `BUILD SUCCESS` → Maven test passed
- `BUILD FAILURE` → Maven test failed

**After seeing "All batches complete"**, verify the totals match expectations:
- `games=100` (or expected count)
- `success=X` + `failed=Y` should equal `games`

**If no output for 5+ minutes:** Check if process is hung.

**If killed or failed:** Do NOT proceed to documentation. Report failure pattern to user and investigate.

---

## Step 3: Analyze & Report Issues

After successful completion, analyze results.

### 3a. Locate Results

```bash
ls -la forge-gui-desktop/logs/comprehensive-test-results-*.md | tail -1
```

Read the results file using the Read tool.

### 3b. Find Failures, Errors, Warnings

Use Grep tool to search logs:

**Failures/Timeouts:**
```
Pattern: "failed|timed out|exit code"
Path: forge-gui-desktop/logs/
```

**Errors:**
```
Pattern: "\[ERROR\]"
Path: forge-gui-desktop/logs/network-debug-run*-game*-test.log
```

**Warnings:**
```
Pattern: "\[WARN\]"
Path: forge-gui-desktop/logs/network-debug-run*-game*-test.log
```

### 3c. Present Summary to User

| Category | Count | Details |
|----------|-------|---------|
| Game Failures | | [timeouts, setup failures, crashes] |
| Games with Errors | | [unique error types] |
| Games with Warnings | | [unique warning types] |

### 3d. Add Issues to .documentation/Debugging.md and Todo List

For each unique failure/error:
1. Add entry to `.documentation/Debugging.md` (see Appendix A for template)
2. Create investigation task using TaskCreate:
   - High priority: Game failures, checksum mismatches
   - Medium priority: Errors (games completed)
   - Low priority: Warnings

---

## Step 4: Archive Test Artifacts

**MANDATORY before updating documentation.**

```bash
mkdir -p testlogs

# Copy results file (use actual filename)
cp forge-gui-desktop/logs/comprehensive-test-results-YYYYMMDD-HHMMSS.md testlogs/

# Copy game logs (use actual batch ID)
cp forge-gui-desktop/logs/network-debug-runYYYYMMDD-HHMMSS-*.log testlogs/

# Verify
ls testlogs/ | wc -l
```

---

## Step 5: Update Documentation

Invoke the documentation skill to update .documentation/Testing.md with verified results:

```
/documentation testing
```

The documentation skill will:
- Read archived results file from `testlogs/`
- Verify all metrics against source
- Propose updates for approval
- Ensure source citation is included

---

## Error Recovery

| Scenario | Action |
|----------|--------|
| Test fails mid-run | Note batch/error, report to user, do NOT update docs |
| Results inconsistent | Re-read file, check log count, report discrepancy |
| Fewer logs than expected | Investigate filtering, check for crashes |

---

## Appendix A: .documentation/Debugging.md Templates

### Game Failure Template
```markdown
## Bug #N: [Failure Type] - [Brief Description]

**Status:** Open - Needs Investigation
**Discovered:** [Date] via comprehensive test
**Frequency:** [X failures in Y games]
**Severity:** High

**Details:**
- Type: [Timeout / Setup Failure / Crash]
- Player count: [2p/3p/4p]
- Batch ID: [runYYYYMMDD-HHMMSS]

**Error:**
`[exact message]`

**Investigation:** TODO
```

### Error Template (Non-Fatal)
```markdown
## Bug #N: [Error Type]

**Status:** Open - Needs Investigation
**Discovered:** [Date] via comprehensive test
**Frequency:** [X occurrences in Y games]
**Severity:** Low (games completed)

**Error:** `[exact message]`

**Investigation:** TODO
```

---

## Quick Reference

| Test | Command | Duration |
|------|---------|----------|
| Quick | `...#runQuickDeltaSyncTest` | ~10 min |
| Full | `...#runComprehensiveDeltaSyncTest` | ~30 min |
| 2P only | `...#runTwoPlayerOnlyTest` | ~5 min |

| File | Location |
|------|----------|
| Results | `forge-gui-desktop/logs/comprehensive-test-results-*.md` |
| Game logs | `forge-gui-desktop/logs/network-debug-run*-game*-test.log` |
| Archive | `testlogs/` |
| Documentation | `.documentation/Testing.md` |
