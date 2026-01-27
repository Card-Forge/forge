---
name: reviewlogs
description: Reviews recent network debug logs, identifies errors and desync events, and proposes fixes. Analyzes host/client log pairs for checksum mismatches, timing issues, and synchronization problems.
argument-hint: [log-count]
disable-model-invocation: true
---

# Review Network Debug Logs

Systematically analyze recent network debug logs and identify issues.

## Workflow Checklist

Copy and track progress:

```
- [ ] Step 1: Find recent log files
- [ ] Step 2: Check log times against recent commits
- [ ] Step 3: Identify host vs client logs
- [ ] Step 4: Search for critical issues
- [ ] Step 5: Analyze patterns
- [ ] Step 6: Trace specific failures
- [ ] Step 7: Present findings
- [ ] Step 8: Evaluate for .documentation/Debugging.md
- [ ] Step 9: Implement approved changes
```

## Step 1: Find Recent Logs

```bash
ls -lt forge-gui-desktop/logs/network-debug-*.log | head -${ARGUMENTS:-4}
```

Default: 4 logs (2 host+client pairs). Use $ARGUMENTS to specify different count.

Match logs from the same session by timestamp in filenames.

## Step 2: Check Against Recent Commits

```bash
git log --oneline --since="1 day ago" --format="%h %ci %s" -- forge-gui/ forge-game/
```

Compare log timestamps (YYYYMMDD-HHMMSS) against commit times:
- **Logs BEFORE fix**: Issues may already be resolved
- **Logs AFTER fix**: Can verify fix effectiveness

## Step 3: Identify Host vs Client

- Search for `[NetworkRole]` near top of log (explicitly states HOST or CLIENT)
- **Host**: Has `[chooseSpellAbilityToPlay]` entries
- **Client**: Has `=== START applyDelta seq=` entries

## Step 4: Search for Critical Issues

```bash
# Errors and exceptions
grep -i "error\|exception\|fail" <logfile>

# Checksum/desync issues
grep -i "checksum\|mismatch\|desync\|resync" <logfile>

# Warnings
grep "\[WARN\]" <logfile>

# AI takeover issues
grep "\[AI Takeover\]" <logfile>
```

## Step 5: Analyze Patterns

Look for:
- **Checksum mismatches**: Frequency, sequence numbers, preceding events
- **Full state resyncs**: Count, necessity
- **Timing issues**: Large timestamp gaps
- **Object NOT FOUND**: False positives vs real issues
- **Delta sync efficiency**: Bandwidth savings percentages

## Step 6: Trace Failures

For errors found, get context:
```bash
grep -B5 -A10 "<error pattern>" <logfile>
```

## Step 7: Present Findings

### Log Session Summary
- **Session timestamp**: [from filename]
- **Host PID / Client PID**: [numbers]

### Issues Found

| Severity | Issue | Count | Log Lines |
|----------|-------|-------|-----------|
| ERROR/WARN/INFO | Description | N | line numbers |

### Detailed Analysis

For significant issues:
1. **What happened**: Sequence of events
2. **Root cause hypothesis**: Likely cause
3. **Evidence**: Log excerpts with timestamps
4. **Impact**: Effect on gameplay/performance

### Proposed Fixes

| Issue | File(s) | Change | Risk |
|-------|---------|--------|------|
| Brief description | Source files | Specific change | Low/Med/High |

## Step 8: .documentation/Debugging.md Evaluation

Add bugs that are:
- Reproducible (occurs consistently in logs)
- Impactful (affects gameplay or user experience)
- Not already documented
- Requires investigation

Format for .documentation/Debugging.md:
```markdown
### [N]. [Title]
**Status:** Under Investigation
**Severity:** [High/Medium/Low]
**Description:** [What the bug is]
**Log Evidence:** [Relevant excerpts]
**Hypothesis:** [Initial theory]
```

## Step 9: Implement Approved Changes

Wait for user approval, then:
1. Implement approved fixes
2. Add approved bugs to .documentation/Debugging.md
3. Explain each change

## Reference

See [reference.md](reference.md) for log prefixes and search patterns.
