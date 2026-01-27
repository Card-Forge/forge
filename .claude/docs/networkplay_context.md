# NetworkPlay Branch Context

This file contains branch-specific workflows, debugging procedures, and testing guidelines for the NetworkPlay branch. Consult this when working on network-related features.

## Branch Guidelines

- **Action Items**: Review `.claude/docs/todo.md` at session start for current tasks and priorities. Update it as issues arise or tasks complete.
- Read `.documentation/NetworkPlay.md` for details on delta synchronization, reconnection support, and recent changes.

### IMPORTANT: Minimize Core Code Changes

All changes on this branch should be network-specific. Avoid modifying core non-network classes (e.g., `AbstractGuiGame`, `PlayerControllerHuman`, forge-game modules) unless absolutely necessary.

**Network-specific logic belongs in:**
- `NetworkGuiGame` and its subclasses (network client game state)
- `FServerManager`, `NetGuiGame` (server-side network handling)
- `InputLockUI` (network-aware UI input, already has network-specific paths)
- Network protocol classes in `forge-gui/src/main/java/forge/gamemodes/net/`

When core changes seem required, first explore whether the functionality can be achieved through network-specific subclasses, hooks, or protocol messages.

## Testing Infrastructure

- **Location**: `forge-gui-desktop/src/test/java/forge/net/`
- **Documentation**: See `.documentation/Testing.md` for full details
- **Headless Testing**: Uses `HeadlessGuiDesktop` to run full games without display server. See `architectural_patterns.md` for details.

### Comprehensive Test Results

The `ComprehensiveDeltaSyncTest` generates detailed metrics via `AnalysisResult.generateReport()`. Results are saved to:
- `forge-gui-desktop/logs/comprehensive-test-results-YYYYMMDD-HHMMSS.md`
- `forge-gui-desktop/logs/quick-test-results-*.md` (for quick tests)

Key metrics include three-tier bandwidth (Approximate, ActualNetwork, FullState) and per-player-count breakdowns.

Individual packet metrics in log files: `[DeltaSync] Packet #N: Approximate=X bytes, ActualNetwork=Y bytes, FullState=Z bytes`

### Early Test Termination

**IMPORTANT:** When running comprehensive network tests (50+ games):
- Monitor the failure rate as games complete
- If **more than 25% of games fail after 20 games**, immediately terminate the test
- Use `TaskStop` to kill the background task
- Move directly to debugging and fixing the error
- Do not wait for all games to complete when there's a clear systemic failure

This prevents wasting time on tests that will clearly fail validation, and allows faster iteration on fixes.

### Updating Test Documentation

When running new comprehensive tests:
1. Update "Comprehensive Validation Results for NetworkPlay Branch" section in `.documentation/Testing.md`
2. Maintain the existing format (Summary Results, Bandwidth Usage Breakdown, Results by Player Count, etc.)
3. Check `.documentation/NetworkPlay.md` for quoted figures and ensure consistency with new results

## Debugging

Debugging for network play should print to log files through `NetworkDebugLogger`, not to console.
Network debug logs: `forge-gui-desktop/logs/`

**Path Sanitization:** Log file paths replace user home directory with `~` for privacy.

### Testing Environment Logs

Test logs are distinguished by:
1. **Filename suffix**: `-test` in filename: `network-debug-YYYYMMDD-HHMMSS-PID-test.log`
2. **Header banner**: Includes "TESTING ENVIRONMENT - HeadlessGuiDesktop Active"

### Module Logging Constraints

| Module | Can use NetworkDebugLogger |
|--------|---------------------------|
| forge-gui, forge-gui-desktop, forge-gui-mobile | Yes |
| forge-core, forge-game, forge-ai | No - use `System.out.println` temporarily |

## Known Bugs Workflow

See `.documentation/Debugging.md` for bug tracking. Workflow:
- **Proactive Discovery**: Add bugs to .documentation/Debugging.md and todo.md immediately when discovered
- Update "Steps Taken" section after each debugging attempt
- **Before marking resolved**: Test fix in live environment
- When verified, move to "Resolved Bugs" section with solution and commit hash

**Bug Status Values:**
- `Under Investigation` - Actively being debugged
- `Fix Applied - Pending Verification` - Code fix committed, not yet tested
- `Resolved` - Fix verified working

**Network Bug Verification**: Verify fixes cover BOTH server-side (DeltaSyncManager, HostedMatch) AND client-side (NetworkGuiGame, CMatchUI).

## Network Play Testing Procedure

1. Build: `mvn -pl forge-gui-desktop -am install -DskipTests`
2. Start host instance (from IDE or command line)
3. Start client instance (change player name in lobby)
4. Host creates game, client joins
5. Test the specific network feature
6. Review logs at `forge-gui-desktop/logs/` (separate files per instance, identified by PID)

**Delta Sync Metrics:** Use actual measured figures from logs, not estimates. Look for `[DeltaSync]` entries with byte counts.

**Host Commands:**
- `/skipreconnect` - Skip reconnection wait, convert disconnected player to AI
- `/skipreconnect PlayerName` - Target specific player

**Cleaning Test Logs:**
```bash
rm -f forge-gui-desktop/logs/network-debug-*.log
```

## Log Analysis

**Finding logs:** Use Glob: `forge-gui-desktop/logs/network-debug-*.log`

**Key log prefixes:**
| Prefix | Purpose |
|--------|---------|
| `[NetworkRole]` | HOST or CLIENT identifier |
| `[DeltaSync]` | Delta synchronization |
| `[FullStateSync]` | Full state synchronization |
| `[AI Takeover]` | AI conversion process |
| `[Disconnect]` | Client disconnect handling |
| `[GameEvent]` | Game actions (turns, spells, combat, life changes) |
| `[Per-client tracking]` | Multi-client delta sync checksums |

**Search patterns (Grep tool):**
- Errors: `error|warn|exception|fail` (case insensitive)
- Desync: `checksum|mismatch|desync|resync`
- AI takeover: `\[AI Takeover\]`
- Disconnect: `\[Disconnect\]|\[/skipreconnect\]`

**Analysis approach:**
1. Identify host vs client log (host has `chooseSpellAbilityToPlay` entries)
2. Look for ERROR/WARN level messages first
3. Trace sequence of events around failures using timestamps
4. Compare host and client logs for the same timeframe
