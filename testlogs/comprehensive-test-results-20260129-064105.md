## Comprehensive Delta Sync Validation Results

**Analysis Date:** 2026-01-29 06:41:05

### Summary Results

| Metric | Value |
|--------|-------|
| Total Games Run | 100 |
| Successful Games | 97 |
| Failed Games | 3 |
| Overall Success Rate | 97.0% |
| Total Turns | 2290 |
| Average Turns per Game | 23.6 |
| Unique Decks Used | 207 |
| Average Bandwidth Savings | 99.6% |
| Total Network Traffic | 238.51 MB |
| Checksum Mismatches | 0 |
| Games with Errors | 0 |
| Games with Warnings | 4 |

### Bandwidth Usage Breakdown

| Metric | Total | Avg per Game | Description |
|--------|-------|--------------|-------------|
| Approximate | 51.20 MB | 524.3 KB | Estimated delta size from object diffs |
| ActualNetwork | 238.51 MB | 2.39 MB | Actual bytes sent over network |
| FullState | 56.50 GB | 578.58 MB | Size if full state was sent |

**Bandwidth Savings:**
- Approximate vs FullState: 99.9% savings
- ActualNetwork vs FullState: 99.6% savings
- ActualNetwork vs Approximate: 365.8% overhead (compression effect)

### Results by Player Count

| Players | Games | Success Rate | Avg Turns | Avg Savings |
|---------|-------|--------------|-----------|-------------|
| 2 | 50 | 98.0% | 15.6 | 99.4% |
| 3 | 30 | 100.0% | 27.2 | 99.5% |
| 4 | 20 | 90.0% | 39.5 | 99.6% |

### Bandwidth by Player Count

| Players | Approximate | ActualNetwork | FullState | Savings |
|---------|-------------|---------------|-----------|--------|
| 2 | 7.95 MB | 24.62 MB | 3.95 GB | 99.4% |
| 3 | 16.22 MB | 67.57 MB | 13.90 GB | 99.5% |
| 4 | 27.02 MB | 146.31 MB | 38.66 GB | 99.6% |

### Warning Analysis

**Games with Warnings:** 4

**Files with Warnings:**
- `network-debug-run20260129-060711-batch2-game9-3p-test.log`
- `network-debug-run20260129-060711-batch5-game2-2p-test.log`
- `network-debug-run20260129-060711-batch7-game6-4p-test.log`
- `network-debug-run20260129-060711-batch9-game0-4p-test.log`

**Unique Warnings:**
- `[06:16:53.890] [WARN] [NetworkDeserializer] Object not found in Tracker: type=CardView, id=184`
- `[06:16:53.892] [WARN] [NetworkDeserializer] Object not found in Tracker: type=CardView, id=184`
- `[06:16:53.900] [WARN] [NetworkDeserializer] Object not found in Tracker: type=CardView, id=183`
- `[06:16:53.901] [WARN] [NetworkDeserializer] Object not found in Tracker: type=CardView, id=183`
- `[06:16:53.910] [WARN] [NetworkDeserializer] Object not found in Tracker: type=CardView, id=182`
- `[06:16:53.913] [WARN] [NetworkDeserializer] Object not found in Tracker: type=CardView, id=182`
- `[06:16:53.920] [WARN] [NetworkDeserializer] Object not found in Tracker: type=CardView, id=181`
- `[06:16:53.923] [WARN] [NetworkDeserializer] Object not found in Tracker: type=CardView, id=181`
- `[06:27:34.372] [WARN] [NetworkDeserializer] Object not found in Tracker: type=CardView, id=160`
- `[06:27:35.195] [WARN] [NetworkDeserializer] Object not found in Tracker: type=CardView, id=161`
- ... and 9 more

### Turn Distribution

| Range | Count | % | Notes |
|-------|-------|---|-------|
| 1-5 | 0 | 0.0% |  |
| 6-10 | 4 | 4.0% |  |
| 11-20 | 44 | 44.0% |  |
| 21-30 | 25 | 25.0% |  |
| 30+ | 27 | 27.0% |  |

### Failure Mode Analysis

| Mode | Count | % | Affected Games |
|------|-------|---|----------------|
| INCOMPLETE | 3 | 3.0% | game0, game8, game0 |

### Batch Performance

| Batch | Games | Success Rate | Failures |
|-------|-------|--------------|----------|
| 0 | 10 | 80% | network-debug-run20260129-060711-batch1-game0-4p-test.log (incomplete), network-debug-run20260129-060711-batch9-game0-4p-test.log (incomplete) |
| 1 | 10 | 100% | - |
| 2 | 10 | 100% | - |
| 3 | 10 | 100% | - |
| 4 | 10 | 100% | - |
| 5 | 10 | 100% | - |
| 6 | 10 | 100% | - |
| 7 | 10 | 100% | - |
| 8 | 10 | 90% | network-debug-run20260129-060711-batch3-game8-2p-test.log (incomplete) |
| 9 | 10 | 100% | - |

### Failure Patterns

| Pattern | Value | Status |
|---------|-------|--------|
| Max Consecutive Failures | 1 | OK |
| First Half Success (0-49) | 96.0% | |
| Second Half Success (50-99) | 98.0% |  |
| Warnings -> Failures | 1/4 (25.0%) | |

**Stability Trend:** STABLE

### Validation Status

**PASSED** - All validation criteria met:
- [x] Success rate >= 90% (actual: 97.0%)
- [x] Average bandwidth savings >= 90% (actual: 99.6%)
- [x] Zero checksum mismatches (actual: 0)

### Analyzed Log Files

**Total files analyzed:** 100

| # | Log File | Status | Players | Turns | Winner |
|---|----------|--------|---------|-------|--------|
| 0 | `network-debug-run20260129-060711-batch0-game0-2p-test.log` | OK | 2 | 12 | Alice (Host AI) |
| 0 | `network-debug-run20260129-060711-batch1-game0-4p-test.log` | FAIL | 4 | 17 | - |
| 0 | `network-debug-run20260129-060711-batch2-game0-4p-test.log` | OK | 4 | 55 | Diana (Remote) |
| 0 | `network-debug-run20260129-060711-batch3-game0-3p-test.log` | OK | 3 | 18 | Charlie (Remote) |
| 0 | `network-debug-run20260129-060711-batch4-game0-2p-test.log` | OK | 2 | 25 | Alice (Host AI) |
| 0 | `network-debug-run20260129-060711-batch5-game0-2p-test.log` | OK | 2 | 14 | Alice (Host AI) |
| 0 | `network-debug-run20260129-060711-batch6-game0-3p-test.log` | OK | 3 | 19 | Charlie (Remote) |
| 0 | `network-debug-run20260129-060711-batch7-game0-3p-test.log` | OK | 3 | 31 | Charlie (Remote) |
| 0 | `network-debug-run20260129-060711-batch8-game0-3p-test.log` | OK | 3 | 23 | Charlie (Remote) |
| 0 | `network-debug-run20260129-060711-batch9-game0-4p-test.log` | FAIL | 4 | 39 | - |
| 1 | `network-debug-run20260129-060711-batch0-game1-2p-test.log` | OK | 2 | 10 | Alice (Host AI) |
| 1 | `network-debug-run20260129-060711-batch1-game1-2p-test.log` | OK | 2 | 12 | Alice (Host AI) |
| 1 | `network-debug-run20260129-060711-batch2-game1-2p-test.log` | OK | 2 | 15 | Alice (Host AI) |
| 1 | `network-debug-run20260129-060711-batch3-game1-2p-test.log` | OK | 2 | 16 | Alice (Host AI) |
| 1 | `network-debug-run20260129-060711-batch4-game1-4p-test.log` | OK | 4 | 34 | Diana (Remote) |
| 1 | `network-debug-run20260129-060711-batch5-game1-4p-test.log` | OK | 4 | 31 | Alice (Host AI) |
| 1 | `network-debug-run20260129-060711-batch6-game1-2p-test.log` | OK | 2 | 15 | Alice (Host AI) |
| 1 | `network-debug-run20260129-060711-batch7-game1-2p-test.log` | OK | 2 | 11 | Alice (Host AI) |
| 1 | `network-debug-run20260129-060711-batch8-game1-3p-test.log` | OK | 3 | 32 | Charlie (Remote) |
| 1 | `network-debug-run20260129-060711-batch9-game1-2p-test.log` | OK | 2 | 15 | Alice (Host AI) |
| 2 | `network-debug-run20260129-060711-batch0-game2-3p-test.log` | OK | 3 | 28 | Alice (Host AI) |
| 2 | `network-debug-run20260129-060711-batch1-game2-2p-test.log` | OK | 2 | 12 | Alice (Host AI) |
| 2 | `network-debug-run20260129-060711-batch2-game2-4p-test.log` | OK | 4 | 38 | Alice (Host AI) |
| 2 | `network-debug-run20260129-060711-batch3-game2-3p-test.log` | OK | 3 | 26 | Charlie (Remote) |
| 2 | `network-debug-run20260129-060711-batch4-game2-4p-test.log` | OK | 4 | 26 | Diana (Remote) |
| 2 | `network-debug-run20260129-060711-batch5-game2-2p-test.log` | OK | 2 | 17 | Alice (Host AI) |
| 2 | `network-debug-run20260129-060711-batch6-game2-2p-test.log` | OK | 2 | 15 | Alice (Host AI) |
| 2 | `network-debug-run20260129-060711-batch7-game2-2p-test.log` | OK | 2 | 14 | Alice (Host AI) |
| 2 | `network-debug-run20260129-060711-batch8-game2-3p-test.log` | OK | 3 | 27 | Charlie (Remote) |
| 2 | `network-debug-run20260129-060711-batch9-game2-3p-test.log` | OK | 3 | 25 | Charlie (Remote) |
| 3 | `network-debug-run20260129-060711-batch0-game3-3p-test.log` | OK | 3 | 47 | Charlie (Remote) |
| 3 | `network-debug-run20260129-060711-batch1-game3-3p-test.log` | OK | 3 | 20 | Charlie (Remote) |
| 3 | `network-debug-run20260129-060711-batch2-game3-2p-test.log` | OK | 2 | 12 | Alice (Host AI) |
| 3 | `network-debug-run20260129-060711-batch3-game3-2p-test.log` | OK | 2 | 14 | Alice (Host AI) |
| 3 | `network-debug-run20260129-060711-batch4-game3-3p-test.log` | OK | 3 | 27 | Alice (Host AI) |
| 3 | `network-debug-run20260129-060711-batch5-game3-2p-test.log` | OK | 2 | 18 | Alice (Host AI) |
| 3 | `network-debug-run20260129-060711-batch6-game3-3p-test.log` | OK | 3 | 24 | Alice (Host AI) |
| 3 | `network-debug-run20260129-060711-batch7-game3-2p-test.log` | OK | 2 | 13 | Alice (Host AI) |
| 3 | `network-debug-run20260129-060711-batch8-game3-3p-test.log` | OK | 3 | 28 | Alice (Host AI) |
| 3 | `network-debug-run20260129-060711-batch9-game3-4p-test.log` | OK | 4 | 47 | Diana (Remote) |
| 4 | `network-debug-run20260129-060711-batch0-game4-4p-test.log` | OK | 4 | 38 | Diana (Remote) |
| 4 | `network-debug-run20260129-060711-batch1-game4-2p-test.log` | OK | 2 | 12 | Alice (Host AI) |
| 4 | `network-debug-run20260129-060711-batch2-game4-3p-test.log` | OK | 3 | 28 | Alice (Host AI) |
| 4 | `network-debug-run20260129-060711-batch3-game4-3p-test.log` | OK | 3 | 25 | Charlie (Remote) |
| 4 | `network-debug-run20260129-060711-batch4-game4-2p-test.log` | OK | 2 | 14 | Alice (Host AI) |
| 4 | `network-debug-run20260129-060711-batch5-game4-3p-test.log` | OK | 3 | 23 | Charlie (Remote) |
| 4 | `network-debug-run20260129-060711-batch6-game4-3p-test.log` | OK | 3 | 24 | Charlie (Remote) |
| 4 | `network-debug-run20260129-060711-batch7-game4-2p-test.log` | OK | 2 | 13 | Alice (Host AI) |
| 4 | `network-debug-run20260129-060711-batch8-game4-2p-test.log` | OK | 2 | 23 | Alice (Host AI) |
| 4 | `network-debug-run20260129-060711-batch9-game4-4p-test.log` | OK | 4 | 32 | Alice (Host AI) |
| 5 | `network-debug-run20260129-060711-batch0-game5-3p-test.log` | OK | 3 | 25 | Charlie (Remote) |
| 5 | `network-debug-run20260129-060711-batch1-game5-3p-test.log` | OK | 3 | 24 | Charlie (Remote) |
| 5 | `network-debug-run20260129-060711-batch2-game5-2p-test.log` | OK | 2 | 11 | Alice (Host AI) |
| 5 | `network-debug-run20260129-060711-batch3-game5-2p-test.log` | OK | 2 | 20 | Alice (Host AI) |
| 5 | `network-debug-run20260129-060711-batch4-game5-2p-test.log` | OK | 2 | 24 | Alice (Host AI) |
| 5 | `network-debug-run20260129-060711-batch5-game5-4p-test.log` | OK | 4 | 38 | Alice (Host AI) |
| 5 | `network-debug-run20260129-060711-batch6-game5-2p-test.log` | OK | 2 | 13 | Alice (Host AI) |
| 5 | `network-debug-run20260129-060711-batch7-game5-4p-test.log` | OK | 4 | 33 | Alice (Host AI) |
| 5 | `network-debug-run20260129-060711-batch8-game5-4p-test.log` | OK | 4 | 33 | Diana (Remote) |
| 5 | `network-debug-run20260129-060711-batch9-game5-2p-test.log` | OK | 2 | 17 | Alice (Host AI) |
| 6 | `network-debug-run20260129-060711-batch0-game6-2p-test.log` | OK | 2 | 20 | Alice (Host AI) |
| 6 | `network-debug-run20260129-060711-batch1-game6-2p-test.log` | OK | 2 | 14 | Alice (Host AI) |
| 6 | `network-debug-run20260129-060711-batch2-game6-2p-test.log` | OK | 2 | 15 | Alice (Host AI) |
| 6 | `network-debug-run20260129-060711-batch3-game6-3p-test.log` | OK | 3 | 23 | Alice (Host AI) |
| 6 | `network-debug-run20260129-060711-batch4-game6-2p-test.log` | OK | 2 | 15 | Alice (Host AI) |
| 6 | `network-debug-run20260129-060711-batch5-game6-2p-test.log` | OK | 2 | 16 | Alice (Host AI) |
| 6 | `network-debug-run20260129-060711-batch6-game6-3p-test.log` | OK | 3 | 24 | Alice (Host AI) |
| 6 | `network-debug-run20260129-060711-batch7-game6-4p-test.log` | OK | 4 | 31 | Charlie (Remote) |
| 6 | `network-debug-run20260129-060711-batch8-game6-2p-test.log` | OK | 2 | 10 | Alice (Host AI) |
| 6 | `network-debug-run20260129-060711-batch9-game6-4p-test.log` | OK | 4 | 44 | Diana (Remote) |
| 7 | `network-debug-run20260129-060711-batch0-game7-2p-test.log` | OK | 2 | 12 | Alice (Host AI) |
| 7 | `network-debug-run20260129-060711-batch1-game7-2p-test.log` | OK | 2 | 17 | Alice (Host AI) |
| 7 | `network-debug-run20260129-060711-batch2-game7-4p-test.log` | OK | 4 | 45 | Alice (Host AI) |
| 7 | `network-debug-run20260129-060711-batch3-game7-4p-test.log` | OK | 4 | 57 | Charlie (Remote) |
| 7 | `network-debug-run20260129-060711-batch4-game7-2p-test.log` | OK | 2 | 17 | Alice (Host AI) |
| 7 | `network-debug-run20260129-060711-batch5-game7-2p-test.log` | OK | 2 | 14 | Alice (Host AI) |
| 7 | `network-debug-run20260129-060711-batch6-game7-2p-test.log` | OK | 2 | 20 | Alice (Host AI) |
| 7 | `network-debug-run20260129-060711-batch7-game7-2p-test.log` | OK | 2 | 9 | Alice (Host AI) |
| 7 | `network-debug-run20260129-060711-batch8-game7-2p-test.log` | OK | 2 | 17 | Alice (Host AI) |
| 7 | `network-debug-run20260129-060711-batch9-game7-2p-test.log` | OK | 2 | 14 | Alice (Host AI) |
| 8 | `network-debug-run20260129-060711-batch0-game8-2p-test.log` | OK | 2 | 11 | Alice (Host AI) |
| 8 | `network-debug-run20260129-060711-batch1-game8-3p-test.log` | OK | 3 | 36 | Charlie (Remote) |
| 8 | `network-debug-run20260129-060711-batch2-game8-3p-test.log` | OK | 3 | 34 | Charlie (Remote) |
| 8 | `network-debug-run20260129-060711-batch3-game8-2p-test.log` | FAIL | 2 | 10 | - |
| 8 | `network-debug-run20260129-060711-batch4-game8-3p-test.log` | OK | 3 | 21 | Charlie (Remote) |
| 8 | `network-debug-run20260129-060711-batch5-game8-2p-test.log` | OK | 2 | 21 | Alice (Host AI) |
| 8 | `network-debug-run20260129-060711-batch6-game8-2p-test.log` | OK | 2 | 21 | Alice (Host AI) |
| 8 | `network-debug-run20260129-060711-batch7-game8-2p-test.log` | OK | 2 | 17 | Alice (Host AI) |
| 8 | `network-debug-run20260129-060711-batch8-game8-2p-test.log` | OK | 2 | 13 | Alice (Host AI) |
| 8 | `network-debug-run20260129-060711-batch9-game8-2p-test.log` | OK | 2 | 37 | Alice (Host AI) |
| 9 | `network-debug-run20260129-060711-batch0-game9-4p-test.log` | OK | 4 | 41 | Diana (Remote) |
| 9 | `network-debug-run20260129-060711-batch1-game9-3p-test.log` | OK | 3 | 34 | Charlie (Remote) |
| 9 | `network-debug-run20260129-060711-batch2-game9-3p-test.log` | OK | 3 | 35 | Alice (Host AI) |
| 9 | `network-debug-run20260129-060711-batch3-game9-3p-test.log` | OK | 3 | 26 | Alice (Host AI) |
| 9 | `network-debug-run20260129-060711-batch4-game9-3p-test.log` | OK | 3 | 31 | Charlie (Remote) |
| 9 | `network-debug-run20260129-060711-batch5-game9-2p-test.log` | OK | 2 | 12 | Alice (Host AI) |
| 9 | `network-debug-run20260129-060711-batch6-game9-2p-test.log` | OK | 2 | 13 | Alice (Host AI) |
| 9 | `network-debug-run20260129-060711-batch7-game9-4p-test.log` | OK | 4 | 52 | Diana (Remote) |
| 9 | `network-debug-run20260129-060711-batch8-game9-3p-test.log` | OK | 3 | 29 | Charlie (Remote) |
| 9 | `network-debug-run20260129-060711-batch9-game9-4p-test.log` | OK | 4 | 36 | Diana (Remote) |
