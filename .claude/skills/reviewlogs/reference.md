# Log Analysis Reference

## Log Prefixes

| Prefix | Purpose |
|--------|---------|
| `[NetworkRole]` | Identifies instance as HOST or CLIENT (appears early) |
| `[DeltaSync]` | Delta synchronization operations |
| `[FullStateSync]` | Full state synchronization |
| `[AI Takeover]` | AI conversion process |
| `[Disconnect]` | Client disconnect handling |
| `[/skipreconnect]` | Skip reconnect command processing |
| `[HostCommand]` | Host command routing |
| `[GameSession]` | Game session lifecycle |
| `[InputQueue]` | Input stack management |
| `[InputSyncronizedBase]` | Latch operations |
| `[chooseSpellAbilityToPlay]` | Priority decisions |
| `[GameEvent]` | Game actions (turns, spells, combat, life) |
| `[Per-client tracking]` | Multi-client delta sync checksums |

## Test Infrastructure Prefixes

| Prefix | Purpose |
|--------|---------|
| `[AutomatedGameTestHarness]` | Test harness execution |
| `[ReconnectionScenario]` | Reconnection test scenario |
| `[MultiplayerScenario]` | Multiplayer test (3-4 players, local AI) |
| `[MultiplayerNetworkScenario]` | Multiplayer with HeadlessNetworkClient |
| `[ComprehensiveGameRunner]` | Standalone multi-process runner |

## Search Patterns

| Purpose | Pattern |
|---------|---------|
| Errors | `error\|warn\|exception\|fail` (case insensitive) |
| Desync | `checksum\|mismatch\|desync\|resync` |
| AI takeover | `\[AI Takeover\]` |
| Disconnect | `\[Disconnect\]\|\[/skipreconnect\]` |

## Key Constants

- **CHECKSUM_INTERVAL**: Checksum validation every 20 packets

## Log File Naming

- Format: `network-debug-YYYYMMDD-HHMMSS-PID.log`
- Test logs include `-test` suffix: `network-debug-YYYYMMDD-HHMMSS-PID-test.log`

## Identifying Log Types

**Host log indicators:**
- `[chooseSpellAbilityToPlay]` entries
- `Packet #` logs

**Client log indicators:**
- `=== START applyDelta seq=` entries
- Checksum validation messages

**Test log indicators:**
- `-test` in filename
- Header: "TESTING ENVIRONMENT - HeadlessGuiDesktop Active"
