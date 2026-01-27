---
name: documentation
description: Reviews recent changes and updates .documentation/NetworkPlay.md, .documentation/Testing.md, .documentation/Debugging.md, and .claude/docs/scriptReferences.md for accuracy. Triggers automatically before git commits to ensure documentation reflects code changes. Also useful after feature completions.
argument-hint: [focus-area]
---

# Update Project Documentation

Review recent changes and ensure documentation accurately reflects the codebase.

## Target Files

| File | Purpose |
|------|---------|
| `.documentation/NetworkPlay.md` | Delta sync, reconnection, network features |
| `.documentation/Testing.md` | Test infrastructure, results, usage |
| `.documentation/Debugging.md` | Known bugs, debugging progress, resolutions |
| `.claude/docs/scriptReferences.md` | Class/file reference catalog for NetworkPlay |

### Content Separation Rules

**.documentation/Testing.md** focuses on **end-state** for merge evaluation:
- Test infrastructure and how to run tests
- Current validated test results
- Known limitations of the test framework
- **DO NOT include:** Outstanding issues, fixes applied, development progress, bug references

**.documentation/Debugging.md** handles **development progress**:
- Active bugs and investigation status
- Resolved bugs with fix details
- Current test status summary (brief)
- All "Outstanding Issues" and "Fixes Applied" content belongs here

**.claude/docs/scriptReferences.md** is the **class catalog**:
- Lists all NetworkPlay-related classes with descriptions and locations
- Organized by category (Core Network, Delta Sync, Test Infrastructure, etc.)
- **MUST stay synchronized** with actual files - remove deleted classes, add new ones
- Used as first reference when looking for NetworkPlay code

## Workflow Checklist

Copy and track progress:

```
- [ ] Step 1: Check if documentation was recently updated
- [ ] Step 2: Identify recent code changes
- [ ] Step 3: Read current documentation
- [ ] Step 4: VERIFY TEST RESULTS (MANDATORY for .documentation/Testing.md)
- [ ] Step 5: Check accuracy against code
- [ ] Step 6: Check completeness
- [ ] Step 7: Present findings
- [ ] Step 8: Implement approved updates
- [ ] Step 9: Verify changes
```

## Step 1: Pre-Check

Check if documentation was already updated recently:

```bash
git diff --name-only HEAD~3..HEAD | grep -E "(BRANCH_DOCUMENTATION|TESTING_DOCUMENTATION|BUGS)\.md"
git diff --name-only | grep -E "(BRANCH_DOCUMENTATION|TESTING_DOCUMENTATION|BUGS)\.md"
```

If docs were comprehensively updated in recent commits, report "Documentation was updated in recent commits. No additional updates needed."

## Step 2: Identify Recent Changes

```bash
git log --oneline -20
git diff --stat HEAD~5..HEAD
git branch --show-current
```

If $ARGUMENTS provided, focus on that area (testing, delta, reconnection, bugs, architecture, all).

## Step 3: Read Current Documentation

Read all target files to understand current state.

## Step 4: VERIFY AND ARCHIVE TEST RESULTS (MANDATORY)

**CRITICAL: This step is MANDATORY when updating .documentation/Testing.md with any test metrics.**

### 4a. Archive Test Artifacts for GitHub Verification

Before updating documentation, copy test artifacts to `testlogs/` for GitHub upload:

```bash
# Create testlogs directory if it doesn't exist
mkdir -p testlogs

# Identify the batch ID from the most recent results file
ls -la forge-gui-desktop/logs/comprehensive-test-results-*.md

# Copy the results analysis file (use actual timestamp from filename)
cp forge-gui-desktop/logs/comprehensive-test-results-YYYYMMDD-HHMMSS.md testlogs/

# Copy all log files from that batch (use actual batch ID from log filenames)
cp forge-gui-desktop/logs/network-debug-runYYYYMMDD-HHMMSS-*.log testlogs/
```

**Important:** The batch ID in log filenames (e.g., `run20260125-091914`) identifies which logs belong together.

### 4b. Verify Test Results Data

Before writing ANY test metrics, you MUST:

1. **Read the actual results file** from the archived copy in `testlogs/`

2. **Copy numbers EXACTLY** from the results file - never estimate, interpolate, or generate numbers

3. **Cite the specific results file** (filename with timestamp) in the documentation

4. **If data is not in the file**, state "Not recorded" - NEVER fabricate values

**Verification Checklist (MUST complete before updating test metrics):**
- [ ] Test artifacts archived to `testlogs/` directory
- [ ] Read actual `comprehensive-test-results-YYYYMMDD-HHMMSS.md` file
- [ ] Every number in documentation matches source file exactly
- [ ] Results file timestamp is cited in documentation
- [ ] Any discrepancies between expected and actual counts are explained

**NEVER:**
- Estimate or approximate test metrics
- Use numbers from memory or previous documentation
- Generate plausible-sounding statistics
- Round or adjust numbers for presentation

## Step 5: Check Accuracy

- Do code references point to existing files?
- Are class names and method signatures correct?
- Do architectures match actual code?
- Are metrics and test counts current? (Must be verified via Step 4 for test results)

## Step 6: Check Completeness

- Are recent features documented?
- Are new classes/files mentioned?
- Are removed features still documented (should be removed)?
- Are architectural changes reflected?

## Step 7: Present Findings

### Documentation Review Summary
- **Branch**: [current branch]
- **Recent Changes**: [summary]
- **Focus Area**: [if specified]

### Issues Found

| File | Type | Description | Severity |
|------|------|-------------|----------|
| file.md | Accuracy/Completeness/Clarity | Issue | High/Med/Low |

### Proposed Updates

For each update:
- **File**: [filename]
- **Section**: [section name]
- **Issue**: [what's wrong]
- **Change**: [specific update]

Wait for user approval.

## Step 8: Implement Updates

For approved changes:
1. State file and section
2. Use Edit tool
3. Explain improvement

Add changelog entry for significant updates:
```markdown
### YYYY-MM-DD
- [Brief description]
```

## Step 9: Verify

- Re-read modified sections for coherence
- Check cross-references work
- Verify no formatting issues

## Target Audience

Volunteer developers of Forge Master branch. Focus on end functionality for informed merge decisions, not development process details.

## Reference

See [focus-areas.md](focus-areas.md) for focus area mappings and source locations.
