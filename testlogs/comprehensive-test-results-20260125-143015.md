## Comprehensive Delta Sync Validation Results

**Analysis Date:** 2026-01-25 14:30:15

### Summary Results

| Metric | Value |
|--------|-------|
| Total Games Run | 100 |
| Successful Games | 98 |
| Failed Games | 2 |
| Overall Success Rate | 98.0% |
| Total Turns | 2355 |
| Average Turns per Game | 23.8 |
| Unique Decks Used | 42 |
| Average Bandwidth Savings | 99.4% |
| Total Network Traffic | 300.61 MB |
| Checksum Mismatches | 1 |
| Games with Errors | 2 |
| Games with Warnings | 24 |

### Bandwidth Usage Breakdown

| Metric | Total | Avg per Game | Description |
|--------|-------|--------------|-------------|
| Approximate | 45.65 MB | 467.4 KB | Estimated delta size from object diffs |
| ActualNetwork | 300.61 MB | 3.01 MB | Actual bytes sent over network |
| FullState | 48438.26 MB | 484.38 MB | Size if full state was sent |

**Bandwidth Savings:**
- Approximate vs FullState: 99.9% savings
- ActualNetwork vs FullState: 99.4% savings
- ActualNetwork vs Approximate: 558.5% overhead (compression effect)

### Results by Player Count

| Players | Games | Success Rate | Avg Turns | Avg Savings |
|---------|-------|--------------|-----------|-------------|
| 2 | 50 | 98.0% | 15.0 | 99.4% |
| 3 | 30 | 100.0% | 26.5 | 99.5% |
| 4 | 20 | 95.0% | 41.3 | 99.3% |

### Bandwidth by Player Count

| Players | Approximate | ActualNetwork | FullState | Savings |
|---------|-------------|---------------|-----------|--------|
| 2 | 7.54 MB | 21.39 MB | 3867.64 MB | 99.4% |
| 3 | 15.39 MB | 65.38 MB | 13717.01 MB | 99.5% |
| 4 | 22.71 MB | 213.84 MB | 30853.61 MB | 99.3% |

### Error Analysis

**Checksum Mismatches:** 1 games had desync issues

**Unique Errors:**
- `[14:09:26.294] [ERROR] [DeltaSync] Checksum details (client state):`
- `[14:09:26.294] [ERROR] [DeltaSync]   GameView ID: 3`
- `[14:09:26.294] [ERROR] [DeltaSync]   Turn: 10`
- `[14:09:26.294] [ERROR] [DeltaSync]   Phase: UNTAP`
- `[14:09:26.294] [ERROR] [DeltaSync]   Player 0 (Alice (Host AI)): Life=20, Hand=7, GY=2, BF=0`
- `[14:09:26.294] [ERROR] [DeltaSync]   Player 1 (Bob (Remote)): Life=20, Hand=7, GY=2, BF=0`
- `[14:09:26.294] [ERROR] [DeltaSync]   Player 2 (Charlie (Remote)): Life=20, Hand=7, GY=2, BF=0`
- `[14:09:26.294] [ERROR] [DeltaSync]   Player 3 (Diana (Remote)): Life=20, Hand=7, GY=0, BF=3`
- `[14:09:26.294] [ERROR] [DeltaSync] Compare with server state in host log at seq=1180`
- `[14:25:02.781] [ERROR] [NetworkClientTestHarness] Test failed: Address already in use: bind`

### Warning Analysis

**Games with Warnings:** 24

**Unique Warnings:**
- `[14:06:40.763] [WARN] [NetworkDeserializer] Collection lookup failed: type=, id=61 - NOT FOUND in tracker or oldValue`
- `[14:06:40.763] [WARN] [NetworkDeserializer] Collection lookup failed: type=, id=62 - NOT FOUND in tracker or oldValue`
- `[14:06:40.763] [WARN] [NetworkDeserializer] Collection lookup failed: type=, id=63 - NOT FOUND in tracker or oldValue`
- `[14:06:40.763] [WARN] [NetworkDeserializer] Collection lookup failed: type=, id=64 - NOT FOUND in tracker or oldValue`
- `[14:06:40.763] [WARN] [NetworkDeserializer] Collection lookup failed: type=, id=65 - NOT FOUND in tracker or oldValue`
- `[14:06:40.763] [WARN] [NetworkDeserializer] Collection lookup failed: type=, id=66 - NOT FOUND in tracker or oldValue`
- `[14:06:40.763] [WARN] [NetworkDeserializer] Collection lookup failed: type=, id=67 - NOT FOUND in tracker or oldValue`
- `[14:06:40.763] [WARN] [NetworkDeserializer] Collection lookup failed: type=, id=68 - NOT FOUND in tracker or oldValue`
- `[14:06:40.763] [WARN] [NetworkDeserializer] Collection lookup failed: type=, id=69 - NOT FOUND in tracker or oldValue`
- `[14:06:40.763] [WARN] [NetworkDeserializer] Collection lookup failed: type=, id=70 - NOT FOUND in tracker or oldValue`
- ... and 90 more

### Validation Status

**FAILED** - Validation criteria not met:
- [x] Success rate >= 90% (actual: 98.0%)
- [x] Average bandwidth savings >= 90% (actual: 99.4%)
- [ ] Zero checksum mismatches (actual: 1)

### Analyzed Log Files

**Total files analyzed:** 100

| # | Log File | Status | Players | Turns | Winner |
|---|----------|--------|---------|-------|--------|
| 0 | `network-debug-run20260125-140616-batch0-game0-2p-test.log` | OK | 2 | 16 | Alice (Host AI) |
| 0 | `network-debug-run20260125-140616-batch1-game0-3p-test.log` | OK | 3 | 23 | Alice (Host AI) |
| 0 | `network-debug-run20260125-140616-batch2-game0-2p-test.log` | OK | 2 | 14 | Alice (Host AI) |
| 0 | `network-debug-run20260125-140616-batch3-game0-2p-test.log` | OK | 2 | 23 | Alice (Host AI) |
| 0 | `network-debug-run20260125-140616-batch4-game0-2p-test.log` | OK | 2 | 13 | Alice (Host AI) |
| 0 | `network-debug-run20260125-140616-batch5-game0-2p-test.log` | OK | 2 | 12 | Alice (Host AI) |
| 0 | `network-debug-run20260125-140616-batch6-game0-4p-test.log` | OK | 4 | 37 | Alice (Host AI) |
| 0 | `network-debug-run20260125-140616-batch7-game0-3p-test.log` | OK | 3 | 22 | Alice (Host AI) |
| 0 | `network-debug-run20260125-140616-batch8-game0-3p-test.log` | OK | 3 | 26 | Alice (Host AI) |
| 0 | `network-debug-run20260125-140616-batch9-game0-4p-test.log` | OK | 4 | 33 | Alice (Host AI) |
| 1 | `network-debug-run20260125-140616-batch0-game1-2p-test.log` | OK | 2 | 10 | Alice (Host AI) |
| 1 | `network-debug-run20260125-140616-batch1-game1-3p-test.log` | OK | 3 | 27 | Alice (Host AI) |
| 1 | `network-debug-run20260125-140616-batch2-game1-3p-test.log` | OK | 3 | 21 | Alice (Host AI) |
| 1 | `network-debug-run20260125-140616-batch3-game1-2p-test.log` | OK | 2 | 14 | Alice (Host AI) |
| 1 | `network-debug-run20260125-140616-batch4-game1-4p-test.log` | OK | 4 | 51 | Alice (Host AI) |
| 1 | `network-debug-run20260125-140616-batch5-game1-4p-test.log` | OK | 4 | 38 | Alice (Host AI) |
| 1 | `network-debug-run20260125-140616-batch6-game1-4p-test.log` | OK | 4 | 30 | Alice (Host AI) |
| 1 | `network-debug-run20260125-140616-batch7-game1-3p-test.log` | OK | 3 | 36 | Alice (Host AI) |
| 1 | `network-debug-run20260125-140616-batch8-game1-2p-test.log` | OK | 2 | 13 | Alice (Host AI) |
| 1 | `network-debug-run20260125-140616-batch9-game1-2p-test.log` | OK | 2 | 14 | Alice (Host AI) |
| 2 | `network-debug-run20260125-140616-batch0-game2-4p-test.log` | OK | 4 | 29 | Alice (Host AI) |
| 2 | `network-debug-run20260125-140616-batch1-game2-4p-test.log` | DESYNC | 4 | 37 | Alice (Host AI) |
| 2 | `network-debug-run20260125-140616-batch2-game2-2p-test.log` | OK | 2 | 15 | Alice (Host AI) |
| 2 | `network-debug-run20260125-140616-batch3-game2-3p-test.log` | OK | 3 | 32 | Alice (Host AI) |
| 2 | `network-debug-run20260125-140616-batch4-game2-2p-test.log` | OK | 2 | 11 | Alice (Host AI) |
| 2 | `network-debug-run20260125-140616-batch5-game2-2p-test.log` | OK | 2 | 13 | Alice (Host AI) |
| 2 | `network-debug-run20260125-140616-batch6-game2-2p-test.log` | OK | 2 | 15 | Alice (Host AI) |
| 2 | `network-debug-run20260125-140616-batch7-game2-2p-test.log` | OK | 2 | 15 | Alice (Host AI) |
| 2 | `network-debug-run20260125-140616-batch8-game2-4p-test.log` | OK | 4 | 54 | Alice (Host AI) |
| 2 | `network-debug-run20260125-140616-batch9-game2-2p-test.log` | OK | 2 | 15 | Alice (Host AI) |
| 3 | `network-debug-run20260125-140616-batch0-game3-3p-test.log` | OK | 3 | 23 | Alice (Host AI) |
| 3 | `network-debug-run20260125-140616-batch1-game3-2p-test.log` | OK | 2 | 13 | Alice (Host AI) |
| 3 | `network-debug-run20260125-140616-batch2-game3-2p-test.log` | OK | 2 | 12 | Alice (Host AI) |
| 3 | `network-debug-run20260125-140616-batch3-game3-2p-test.log` | OK | 2 | 17 | Alice (Host AI) |
| 3 | `network-debug-run20260125-140616-batch4-game3-2p-test.log` | OK | 2 | 19 | Alice (Host AI) |
| 3 | `network-debug-run20260125-140616-batch5-game3-3p-test.log` | OK | 3 | 21 | Alice (Host AI) |
| 3 | `network-debug-run20260125-140616-batch6-game3-4p-test.log` | OK | 4 | 44 | Alice (Host AI) |
| 3 | `network-debug-run20260125-140616-batch7-game3-2p-test.log` | OK | 2 | 20 | Alice (Host AI) |
| 3 | `network-debug-run20260125-140616-batch8-game3-4p-test.log` | OK | 4 | 47 | Alice (Host AI) |
| 3 | `network-debug-run20260125-140616-batch9-game3-2p-test.log` | OK | 2 | 16 | Alice (Host AI) |
| 4 | `network-debug-run20260125-140616-batch0-game4-3p-test.log` | OK | 3 | 37 | Alice (Host AI) |
| 4 | `network-debug-run20260125-140616-batch1-game4-3p-test.log` | OK | 3 | 23 | Alice (Host AI) |
| 4 | `network-debug-run20260125-140616-batch2-game4-3p-test.log` | OK | 3 | 22 | Alice (Host AI) |
| 4 | `network-debug-run20260125-140616-batch3-game4-3p-test.log` | OK | 3 | 22 | Alice (Host AI) |
| 4 | `network-debug-run20260125-140616-batch4-game4-4p-test.log` | OK | 4 | 55 | Alice (Host AI) |
| 4 | `network-debug-run20260125-140616-batch5-game4-3p-test.log` | OK | 3 | 25 | Alice (Host AI) |
| 4 | `network-debug-run20260125-140616-batch6-game4-4p-test.log` | OK | 4 | 38 | Alice (Host AI) |
| 4 | `network-debug-run20260125-140616-batch7-game4-2p-test.log` | OK | 2 | 16 | Alice (Host AI) |
| 4 | `network-debug-run20260125-140616-batch8-game4-4p-test.log` | OK | 4 | 61 | Alice (Host AI) |
| 4 | `network-debug-run20260125-140616-batch9-game4-2p-test.log` | OK | 2 | 14 | Alice (Host AI) |
| 5 | `network-debug-run20260125-140616-batch0-game5-4p-test.log` | OK | 4 | 37 | Alice (Host AI) |
| 5 | `network-debug-run20260125-140616-batch1-game5-4p-test.log` | OK | 4 | 36 | Alice (Host AI) |
| 5 | `network-debug-run20260125-140616-batch2-game5-3p-test.log` | OK | 3 | 27 | Alice (Host AI) |
| 5 | `network-debug-run20260125-140616-batch3-game5-3p-test.log` | OK | 3 | 29 | Alice (Host AI) |
| 5 | `network-debug-run20260125-140616-batch4-game5-2p-test.log` | OK | 2 | 11 | Alice (Host AI) |
| 5 | `network-debug-run20260125-140616-batch5-game5-3p-test.log` | OK | 3 | 28 | Alice (Host AI) |
| 5 | `network-debug-run20260125-140616-batch6-game5-2p-test.log` | OK | 2 | 15 | Alice (Host AI) |
| 5 | `network-debug-run20260125-140616-batch7-game5-2p-test.log` | OK | 2 | 21 | Alice (Host AI) |
| 5 | `network-debug-run20260125-140616-batch8-game5-3p-test.log` | OK | 3 | 23 | Alice (Host AI) |
| 5 | `network-debug-run20260125-140616-batch9-game5-2p-test.log` | OK | 2 | 19 | Alice (Host AI) |
| 6 | `network-debug-run20260125-140616-batch0-game6-4p-test.log` | OK | 4 | 35 | Alice (Host AI) |
| 6 | `network-debug-run20260125-140616-batch1-game6-2p-test.log` | OK | 2 | 15 | Alice (Host AI) |
| 6 | `network-debug-run20260125-140616-batch2-game6-2p-test.log` | OK | 2 | 13 | Alice (Host AI) |
| 6 | `network-debug-run20260125-140616-batch3-game6-2p-test.log` | OK | 2 | 21 | Alice (Host AI) |
| 6 | `network-debug-run20260125-140616-batch4-game6-2p-test.log` | OK | 2 | 13 | Alice (Host AI) |
| 6 | `network-debug-run20260125-140616-batch5-game6-3p-test.log` | OK | 3 | 55 | Alice (Host AI) |
| 6 | `network-debug-run20260125-140616-batch6-game6-2p-test.log` | OK | 2 | 15 | Alice (Host AI) |
| 6 | `network-debug-run20260125-140616-batch7-game6-2p-test.log` | OK | 2 | 15 | Alice (Host AI) |
| 6 | `network-debug-run20260125-140616-batch8-game6-2p-test.log` | OK | 2 | 15 | Alice (Host AI) |
| 6 | `network-debug-run20260125-140616-batch9-game6-3p-test.log` | OK | 3 | 29 | Alice (Host AI) |
| 7 | `network-debug-run20260125-140616-batch0-game7-3p-test.log` | OK | 3 | 18 | Alice (Host AI) |
| 7 | `network-debug-run20260125-140616-batch1-game7-2p-test.log` | OK | 2 | 10 | Alice (Host AI) |
| 7 | `network-debug-run20260125-140616-batch2-game7-4p-test.log` | OK | 4 | 34 | Alice (Host AI) |
| 7 | `network-debug-run20260125-140616-batch3-game7-2p-test.log` | OK | 2 | 19 | Alice (Host AI) |
| 7 | `network-debug-run20260125-140616-batch4-game7-2p-test.log` | OK | 2 | 12 | Alice (Host AI) |
| 7 | `network-debug-run20260125-140616-batch5-game7-2p-test.log` | OK | 2 | 18 | Alice (Host AI) |
| 7 | `network-debug-run20260125-140616-batch6-game7-3p-test.log` | OK | 3 | 24 | Alice (Host AI) |
| 7 | `network-debug-run20260125-140616-batch7-game7-2p-test.log` | OK | 2 | 10 | Alice (Host AI) |
| 7 | `network-debug-run20260125-140616-batch8-game7-2p-test.log` | FAIL | 2 | 0 | - |
| 7 | `network-debug-run20260125-140616-batch9-game7-3p-test.log` | OK | 3 | 30 | Alice (Host AI) |
| 8 | `network-debug-run20260125-140616-batch0-game8-3p-test.log` | OK | 3 | 31 | Alice (Host AI) |
| 8 | `network-debug-run20260125-140616-batch1-game8-2p-test.log` | OK | 2 | 14 | Alice (Host AI) |
| 8 | `network-debug-run20260125-140616-batch2-game8-3p-test.log` | OK | 3 | 24 | Alice (Host AI) |
| 8 | `network-debug-run20260125-140616-batch3-game8-3p-test.log` | OK | 3 | 26 | Alice (Host AI) |
| 8 | `network-debug-run20260125-140616-batch4-game8-2p-test.log` | OK | 2 | 12 | Alice (Host AI) |
| 8 | `network-debug-run20260125-140616-batch5-game8-3p-test.log` | OK | 3 | 22 | Alice (Host AI) |
| 8 | `network-debug-run20260125-140616-batch6-game8-4p-test.log` | OK | 4 | 40 | Alice (Host AI) |
| 8 | `network-debug-run20260125-140616-batch7-game8-3p-test.log` | OK | 3 | 19 | Alice (Host AI) |
| 8 | `network-debug-run20260125-140616-batch8-game8-2p-test.log` | OK | 2 | 14 | Alice (Host AI) |
| 8 | `network-debug-run20260125-140616-batch9-game8-2p-test.log` | OK | 2 | 16 | Alice (Host AI) |
| 9 | `network-debug-run20260125-140616-batch0-game9-2p-test.log` | OK | 2 | 13 | Alice (Host AI) |
| 9 | `network-debug-run20260125-140616-batch1-game9-3p-test.log` | OK | 3 | 32 | Alice (Host AI) |
| 9 | `network-debug-run20260125-140616-batch2-game9-4p-test.log` | OK | 4 | 37 | Alice (Host AI) |
| 9 | `network-debug-run20260125-140616-batch3-game9-2p-test.log` | OK | 2 | 17 | Alice (Host AI) |
| 9 | `network-debug-run20260125-140616-batch4-game9-2p-test.log` | OK | 2 | 25 | Alice (Host AI) |
| 9 | `network-debug-run20260125-140616-batch5-game9-4p-test.log` | OK | 4 | 52 | Alice (Host AI) |
| 9 | `network-debug-run20260125-140616-batch6-game9-3p-test.log` | OK | 3 | 18 | Alice (Host AI) |
| 9 | `network-debug-run20260125-140616-batch7-game9-2p-test.log` | OK | 2 | 14 | Alice (Host AI) |
| 9 | `network-debug-run20260125-140616-batch8-game9-2p-test.log` | OK | 2 | 12 | Alice (Host AI) |
| 9 | `network-debug-run20260125-140616-batch9-game9-2p-test.log` | OK | 2 | 16 | Alice (Host AI) |
