# NetworkPlay Branch Automated Testing Documentation

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Core Components](#core-components)
3. [Test Configuration](#test-configuration)
4. [Running Tests](#running-tests)
5. [Testing Use Cases](#testing-use-cases)
6. [Comprehensive Validation Results of NetworkPlay Branch](#comprehensive-validation-results-for-networkplay-branch)
7. [Known Limitations](#known-limitations)

---

## Executive Summary

This branch provides automated testing infrastructure for headless execution of full AI-vs-AI Forge games from mulligan to completion. The infrastructure supports both network and non-network testing modes.

The conclusion of this document sets out comprehensive validation results of the NetworkPlay Branch using this testing infrastructure.

### Key Capabilities

- **Headless Execution**: Full games run without display server (no X11/Wayland required)
- **Network Testing**: Real TCP connections with delta sync packet validation
- **Non-Network Testing**: Pure game engine testing via `LocalLobby` for rules validation
- **2-4 Player Support**: Multiplayer games with multiple remote clients
- **Batch Testing**: 100+ games via multi-process parallel execution
- **Production Code Paths**: Uses real `HostedMatch`/`ServerGameLobby` code, no mocking
- **Log Analysis**: Automated parsing of network debug logs for bandwidth and error metrics

All test code resides in `forge-gui-desktop/src/test/java/forge/net/`.

---

## Core Components

### Headless Execution

| Component | Purpose |
|-----------|---------|
| `HeadlessGuiDesktop` | Extends `GuiDesktop` to bypass display requirements |
| `NoOpGuiGame` | No-op `IGuiGame` implementation (693 lines) for AI games |

**HeadlessGuiDesktop** enables games to run without Swing EDT by executing runnables immediately rather than queuing them on the Event Dispatch Thread.

### Network Testing

| Component | Purpose |
|-----------|---------|
| `HeadlessNetworkClient` | Remote TCP client that receives delta sync packets |
| `NetworkClientTestHarness` | Orchestrates host server + remote client testing |
| `NetworkGameEventListener` | Production logging of game events during network play |

**HeadlessNetworkClient** connects via `FGameClient`, sends deck/ready status, receives delta packets, and auto-responds to prompts (mulligan, priority, cleanup discard, player selection).

### Game Execution

| Component | Purpose |
|-----------|---------|
| `AutomatedGameTestHarness` | Network game testing via `ServerGameLobby` |
| `LocalGameTestHarness` | Non-network testing via `LocalLobby` |
| `GameTestHarnessFactory` | Unified entry point for all test modes |
| `SequentialGameExecutor` | Sequential multi-game execution in single JVM |
| `MultiProcessGameExecutor` | Parallel execution via separate JVM processes |
| `ComprehensiveTestExecutor` | Orchestrates mixed 2-4 player game batches |

### Test Scenarios

| Scenario | Description |
|----------|-------------|
| `MultiplayerScenario` | 3-4 player games with local AI |
| `MultiplayerNetworkScenario` | 3-4 player games with remote HeadlessNetworkClient connections |
| `ReconnectionScenario` | Game with disconnect/AI takeover |

### Support Classes

| Class | Purpose |
|-------|---------|
| `TestDeckLoader` | Loads any of 424 quest precon decks |
| `GameTestMetrics` | Collects game and network metrics |
| `TestConfiguration` | Loads configuration from system properties |
| `GameTestMode` | Enum: `LOCAL`, `NETWORK_LOCAL`, `NETWORK_REMOTE` |

### Log Analysis

| Component | Purpose |
|-----------|---------|
| `analysis/NetworkLogAnalyzer` | Parses log files for bandwidth metrics |
| `analysis/GameLogMetrics` | Per-game metrics storage |
| `analysis/AnalysisResult` | Aggregates results and generates reports |

---

## Test Configuration

### Test Modes

| Mode | Description | Use Case |
|------|-------------|----------|
| `LOCAL` | Pure game engine via `LocalLobby` | Fast rules testing, game engine validation |
| `NETWORK_LOCAL` | Network stack via `ServerGameLobby`, host-only | Network overhead measurement |
| `NETWORK_REMOTE` | Server + TCP client connections | Delta sync validation, bandwidth testing |

### System Properties Reference

All properties are passed via Maven's `-D` flag.

#### General Test Configuration

| Property | Default | Type | Description |
|----------|---------|------|-------------|
| `testMode` | `LOCAL` | String | Test mode: `LOCAL`, `NETWORK_LOCAL`, or `NETWORK_REMOTE` |
| `iterations` | 1 | Integer | Number of test iterations for single-game tests |

#### Deck Configuration

| Property | Default | Type | Description |
|----------|---------|------|-------------|
| `deck1` | - | Path | Absolute path to deck file for player 1 |
| `deck2` | - | Path | Absolute path to deck file for player 2 |
| `precon1` | - | String | Quest precon name for player 1 (e.g., `"Quest Precon - Elves"`) |
| `precon2` | - | String | Quest precon name for player 2 (e.g., `"Quest Precon - Goblins"`) |

**Deck Priority**: `deck1`/`deck2` (file path) > `precon1`/`precon2` (precon name) > random precon

#### Comprehensive Test Configuration

| Property | Default | Type | Description |
|----------|---------|------|-------------|
| `test.2pGames` | 50 | Integer | Number of 2-player games in comprehensive test |
| `test.3pGames` | 30 | Integer | Number of 3-player games in comprehensive test |
| `test.4pGames` | 20 | Integer | Number of 4-player games in comprehensive test |
| `test.batchSize` | 10 | Integer | Number of parallel games per batch |
| `test.timeoutMs` | 300000 | Long | Timeout per game in milliseconds (default 5 minutes) |

#### Sequential/Multi-Game Configuration

| Property | Default | Type | Description |
|----------|---------|------|-------------|
| `test.gameCount` | 5 | Integer | Number of games for sequential or multi-process tests |

### Configuration Examples

```bash
# Basic: Random decks, local mode
mvn -pl forge-gui-desktop test -Dtest=AutomatedNetworkTest#testLocalTwoPlayerGame

# Named precons
mvn -pl forge-gui-desktop test -Dtest=AutomatedNetworkTest#testWithSystemProperties \
    -Dprecon1="Quest Precon - Elves" -Dprecon2="Quest Precon - Goblins"

# Custom deck files
mvn -pl forge-gui-desktop test -Dtest=AutomatedNetworkTest#testWithSystemProperties \
    -Ddeck1=/path/to/deck1.dck -Ddeck2=/path/to/deck2.dck

# Network mode with specific decks
mvn -pl forge-gui-desktop test -Dtest=AutomatedNetworkTest#testTrueNetworkTraffic \
    -DtestMode=NETWORK_REMOTE -Dprecon1="Quest Precon - Burn" -Dprecon2="Quest Precon - Control"

# Custom comprehensive test distribution
mvn -pl forge-gui-desktop -am verify \
    -Dtest="ComprehensiveDeltaSyncTest#runComprehensiveDeltaSyncTest" \
    -Dtest.2pGames=20 -Dtest.3pGames=15 -Dtest.4pGames=10 \
    -Dtest.batchSize=5 -Dtest.timeoutMs=600000 \
    -Dsurefire.failIfNoSpecifiedTests=false

# Sequential games with increased timeout
mvn -pl forge-gui-desktop -am verify \
    -Dtest="BatchGameTest#testConfigurableSequential" \
    -Dtest.gameCount=10 -Dtest.timeoutMs=600000 \
    -Dsurefire.failIfNoSpecifiedTests=false
```

---

## Running Tests

### Build Prerequisites

```bash
mvn -pl forge-gui-desktop -am install -DskipTests
```

### Single Game Tests

```bash
# Local 2-player game (no network stack)
mvn -pl forge-gui-desktop test -Dtest=AutomatedNetworkTest#testLocalTwoPlayerGame

# Network 2-player with remote client (delta sync)
mvn -pl forge-gui-desktop test -Dtest=AutomatedNetworkTest#testTrueNetworkTraffic

# Multiplayer network games
mvn -pl forge-gui-desktop test -Dtest=AutomatedNetworkTest#testMultiplayer3Player
mvn -pl forge-gui-desktop test -Dtest=AutomatedNetworkTest#testMultiplayer4Player

# Reconnection scenario
mvn -pl forge-gui-desktop test -Dtest=AutomatedNetworkTest#testReconnection
```

### Multi-Game Execution

**Sequential** (same JVM, one game at a time):
```bash
mvn -pl forge-gui-desktop -am verify \
    -Dtest="BatchGameTest#testConfigurableSequential" \
    -Dtest.gameCount=5 -Dsurefire.failIfNoSpecifiedTests=false
```

**Multi-Process Parallel** (separate JVMs, true parallelism):
```bash
mvn -pl forge-gui-desktop -am verify \
    -Dtest="BatchGameTest#testConfigurableParallel" \
    -Dtest.gameCount=10 -Dsurefire.failIfNoSpecifiedTests=false
```

### Comprehensive Validation

```bash
# Full test (100 games by default)
mvn -pl forge-gui-desktop -am verify \
    -Dtest="ComprehensiveDeltaSyncTest#runComprehensiveDeltaSyncTest" \
    -Dsurefire.failIfNoSpecifiedTests=false

# Quick validation (reduced game counts)
mvn -pl forge-gui-desktop -am verify \
    -Dtest="ComprehensiveDeltaSyncTest#runQuickDeltaSyncTest" \
    -Dsurefire.failIfNoSpecifiedTests=false

# 2-player only test
mvn -pl forge-gui-desktop -am verify \
    -Dtest="ComprehensiveDeltaSyncTest#runTwoPlayerOnlyTest" \
    -Dtest.2pGames=5 -Dsurefire.failIfNoSpecifiedTests=false
```

### Log Files

**Location**: `forge-gui-desktop/logs/`

**Format**: `network-debug-BATCHID-gameN-Pp-test.log`

Where:
- `BATCHID` = `runYYYYMMDD-HHMMSS` (timestamp when batch started, shared across all games in same run)
- `N` = game index (0, 1, 2, ...)
- `P` = player count (2, 3, 4)

**Example**: `network-debug-run20260125-091914-game0-2p-test.log`

All logs from the same batch test share the same `runYYYYMMDD-HHMMSS` prefix, making them easily groupable and sortable.

**Key prefixes**:
- `[DeltaSync]` - Delta synchronization with bandwidth metrics
- `[HeadlessClient]` - Remote client events
- `[GameEvent]` - Gameplay events (turns, spells, combat, life changes)
- `[MultiplayerNetworkScenario]` - Multiplayer test orchestration

---

## Testing Use Cases

The testing infrastructure supports multiple use cases beyond network validation.

### Game Engine Validation

Test the core game rules engine without network overhead using `LOCAL` mode:

```bash
# Fast rules testing - runs games through LocalLobby
mvn -pl forge-gui-desktop test -Dtest=AutomatedNetworkTest#testLocalTwoPlayerGame

# Multiple iterations to stress-test rules
mvn -pl forge-gui-desktop test -Dtest=AutomatedNetworkTest#testWithSystemProperties \
    -DtestMode=LOCAL -Diterations=10
```

**Use cases**:
- Validating new card implementations
- Testing rules interactions
- Identifying game engine crashes or infinite loops
- Performance baseline without network stack

### Deck Testing

Test specific decks for AI playability and rules correctness:

```bash
# Test specific precon decks
mvn -pl forge-gui-desktop test -Dtest=AutomatedNetworkTest#testWithSystemProperties \
    -Dprecon1="Quest Precon - Affinity" -Dprecon2="Quest Precon - Tokens"

# Test custom constructed decks
mvn -pl forge-gui-desktop test -Dtest=AutomatedNetworkTest#testWithSystemProperties \
    -Ddeck1=/path/to/custom_deck.dck -Ddeck2=/path/to/opponent.dck
```

**Use cases**:
- Validating quest precon deck functionality
- Testing AI compatibility with specific archetypes
- Identifying problematic card interactions in specific decks

### Network Bandwidth Analysis

Measure delta sync efficiency and bandwidth usage:

```bash
# Single game with detailed logging
mvn -pl forge-gui-desktop test -Dtest=AutomatedNetworkTest#testTrueNetworkTraffic

# Batch analysis with log aggregation
mvn -pl forge-gui-desktop -am verify \
    -Dtest="ComprehensiveDeltaSyncTest#runComprehensiveDeltaSyncTest" \
    -Dtest.2pGames=50 -Dtest.3pGames=0 -Dtest.4pGames=0 \
    -Dsurefire.failIfNoSpecifiedTests=false
```

**Log output includes**:
```
[DeltaSync] Packet #N: Approximate=X bytes, ActualNetwork=Y bytes, FullState=Z bytes
[DeltaSync]   Savings: Approximate=A%, Actual=B%
```

**Use cases**:
- Measuring bandwidth savings from delta sync optimization
- Identifying state changes that cause large delta packets
- Comparing approximate vs actual network overhead

### Multiplayer Stress Testing

Test 3-4 player games with multiple concurrent network clients:

```bash
# 3-player network game
mvn -pl forge-gui-desktop test -Dtest=AutomatedNetworkTest#testMultiplayer3Player

# 4-player network game
mvn -pl forge-gui-desktop test -Dtest=AutomatedNetworkTest#testMultiplayer4Player

# Batch multiplayer testing
mvn -pl forge-gui-desktop -am verify \
    -Dtest="ComprehensiveDeltaSyncTest#runComprehensiveDeltaSyncTest" \
    -Dtest.2pGames=0 -Dtest.3pGames=20 -Dtest.4pGames=10 \
    -Dsurefire.failIfNoSpecifiedTests=false
```

**Use cases**:
- Testing concurrent client handling
- Validating turn order in multiplayer
- Stress-testing state synchronization with multiple observers

### Reconnection Testing

Test client disconnect and AI takeover scenarios:

```bash
mvn -pl forge-gui-desktop test -Dtest=AutomatedNetworkTest#testReconnection
```

**Use cases**:
- Validating AI takeover when player disconnects
- Testing `/skipreconnect` command functionality
- Verifying game state preservation during disconnection

### Regression Testing

Run large batches to catch intermittent issues:

```bash
# 100 games across all player counts
mvn -pl forge-gui-desktop -am verify \
    -Dtest="ComprehensiveDeltaSyncTest#runComprehensiveDeltaSyncTest" \
    -Dsurefire.failIfNoSpecifiedTests=false

# Extended timeout for complex games
mvn -pl forge-gui-desktop -am verify \
    -Dtest="ComprehensiveDeltaSyncTest#runComprehensiveDeltaSyncTest" \
    -Dtest.timeoutMs=600000 \
    -Dsurefire.failIfNoSpecifiedTests=false
```

**Use cases**:
- Pre-release validation of network features
- Catching rare race conditions
- Validating fixes for intermittent bugs

---

## Comprehensive Validation Results for NetworkPlay Branch

> **Source:** `comprehensive-test-results-20260125-143015.md`
>
> **Verification:** Test artifacts (results file and 100 game logs) are archived in [`testlogs/`](testlogs/) for independent verification.
>
> **IMPORTANT:** All metrics below are copied directly from the verified test results file. No values are estimated or interpolated.

### Objective

To conduct comprehensive testing of the delta synchronisation network protocol in the NetworkPlay Branch via large scale AI-vs-AI Forge games played from mulligan to completion using complete network infrastructure, using a variety of different player counts and unique decks. This is intended to validate effectiveness of network code changes across a wide range of use cases.

### How Testing Simulates Actual Network Play

The testing architecture exercises the complete production network stack:

1. **Real TCP Connections**: Each remote player connects via `FGameClient` over actual TCP sockets, identical to human players joining a game.

2. **Network Serialization**: All game state updates flow through the production serialization pipeline (`DeltaPacket.serialize()` → network transmission → `DeltaPacket.deserialize()`). No game state is passed in-memory.

3. **Production Server Code**: Games run through `HostedMatch` and `ServerGameLobby`, the same classes used in live network play. No mocking or simulation.

4. **Delta Sync Protocol**: The host broadcasts delta packets containing only state changes, and remote clients apply these deltas to reconstruct the full game state—exercising the exact code path human players use.

5. **Multi-Client Coordination**: In 3-4 player games, multiple `HeadlessNetworkClient` instances connect as independent TCP clients, each receiving and processing delta packets independently.

The only difference from live play is that AI makes decisions instantly rather than waiting for human input. The network infrastructure—connection handling, packet serialization, state synchronization, and bandwidth optimization—is identical to production.

### Test Configuration

| Parameter | Value |
|-----------|-------|
| Test Date | 2026-01-25 |
| Results File | `comprehensive-test-results-20260125-143015.md` |
| Configured Games | 100 (50 x 2p, 30 x 3p, 20 x 4p) |
| Games Analyzed | 100 |
| Batch Size | 10 parallel games |
| Timeout | 5 minutes per game |

### Summary Results

| Metric | Value |
|--------|-------|
| Total Games Run | 100 |
| Successful Games | 98 |
| Failed Games | 2 |
| Success Rate | **98.0%** |
| Checksum Mismatches | 1 |
| Games with Errors | 2 |
| Games with Warnings | 24 |
| Total Turns | 2355 |
| Average Turns per Game | 23.8 |
| Unique Decks Used | 42* |

*\* Deck count is an undercount due to log parsing limitations in archived results. Log analyzer regex has since been fixed to capture all deck name formats.*

### Key Finding: High Success Rate with Rare Desync

The test achieved **98% success rate** across all player counts. One checksum mismatch occurred in a 4-player game (batch1-game2-4p at Turn 10), caused by PlayerView ID mismatch (now fixed).

### Bandwidth Usage Breakdown

| Metric | Total | Avg per Game | Description |
|--------|-------|--------------|-------------|
| Approximate | 45.65 MB | 467.4 KB | Estimated delta size from object diffs |
| ActualNetwork | 300.61 MB | 3.01 MB | Actual bytes sent over network |
| FullState | 48438.26 MB | 484.38 MB | Size if full state was sent |

**Bandwidth Savings:**
- Approximate vs FullState: **99.9%** savings
- ActualNetwork vs FullState: **99.4%** savings

### Results by Player Count

| Players | Games | Success Rate | Avg Turns | Avg Savings |
|---------|-------|--------------|-----------|-------------|
| 2 | 50 | **98.0%** | 15.0 | 99.4% |
| 3 | 30 | **100.0%** | 26.5 | 99.5% |
| 4 | 20 | **95.0%** | 41.3 | 99.3% |

### Bandwidth by Player Count

| Players | Approximate | ActualNetwork | FullState | Savings |
|---------|-------------|---------------|-----------|---------|
| 2 | 7.54 MB | 21.39 MB | 3867.64 MB | 99.4% |
| 3 | 15.39 MB | 65.38 MB | 13717.01 MB | 99.5% |
| 4 | 22.71 MB | 213.84 MB | 30853.61 MB | 99.3% |

### Validation Criteria

| Criterion | Target | Actual | Status |
|-----------|--------|--------|--------|
| Success Rate | ≥90% | 98.0% | **PASS** |
| Bandwidth Savings | ≥90% | 99.4% | **PASS** |
| Checksum Mismatches | 0 | 1 | **INVESTIGATE** |

### Error Analysis

**Unique Errors from Results File:**
- `[14:09:26.294] [ERROR] [DeltaSync] Checksum details (client state):` - Checksum mismatch in 4-player game
- `[14:25:02.781] [ERROR] [NetworkClientTestHarness] Test failed: Address already in use: bind`

**2 Game Failures Explained:**

The comprehensive test had 2 failures out of 100 games:

**1. Checksum Mismatch (batch1-game2-4p):**
A 4-player game experienced a desync at Turn 10, UNTAP phase. Client state diverged from server:
```
Player 3 (Diana): GY=0, BF=3 (client) vs server state (needs investigation)
```
This was a **protocol bug** caused by PlayerView ID mismatch between server and client - now fixed (see Debugging.md #10).

**2. Port Binding Error (batch8-game7-2p):**
This occurs during rapid parallel test execution when port cleanup is incomplete between batches. This is a **test infrastructure issue**, not a protocol bug.

**Warning Analysis:**
24 games showed `NetworkDeserializer` collection lookup warnings. These are non-fatal warnings where collection IDs were not found in the tracker - games completed successfully despite these warnings.

---

## Known Limitations

### Test Infrastructure Limitations

1. **AI Input Handling**: Remote client auto-responds to prompts with default choices; no strategic decision-making.

2. **Single JVM Limitation**: `FServerManager` singleton prevents multiple servers in one JVM; use multi-process execution for parallelism.

3. **Timeout Interpretation**: Games exceeding timeout are not network failures—they indicate complex/long games. Increase `test.timeoutMs` if needed.

4. **Deck Availability**: Deck tests require the `res/` directory to be populated. Tests use quest precon decks from `forge-gui/res/quest/precons/`.

### Network Simulation Gaps

While the testing infrastructure exercises production network code, some real-world conditions are not fully simulated:

5. **Single-Machine Latency**: Tests run on localhost without real network latency. Packet transmission is effectively instantaneous, which may not reveal timing-sensitive bugs that occur with 50-200ms latency.

6. **No Packet Loss Simulation**: Tests do not simulate dropped, duplicated, or out-of-order packets. The delta sync protocol's resilience to packet loss is not validated.

7. **Fixed Response Timing**: Auto-responses use fixed delays (50-100ms) rather than variable human response times. This may not exercise timeout handling or race conditions that occur with unpredictable input timing.

8. **AI Decision Paths Only**: Tests exercise AI decision-making code paths, which may differ from human player interactions (e.g., target selection order, mana payment preferences).

9. **Host-Only Game Logic**: The `Game` object runs exclusively on the host. Tests verify that clients correctly receive and apply delta packets, but do not validate scenarios where the client's local representation would be used for gameplay decisions beyond display.

10. **UI Rendering Validation**: Tests verify that data arrives correctly via delta sync, but do not validate that the data would render correctly in the actual UI (CardView properties, zone layouts, card positioning, etc.).

11. **Reconnection Stress Testing**: The reconnection test uses a controlled scenario with a clean disconnect. It does not stress-test mid-combat reconnects, rapid disconnect/reconnect cycles, or reconnection during complex stack resolution.

12. **Client Input Edge Cases**: Auto-responses always succeed immediately. Tests do not exercise:
    - Input timeout handling (what happens if client doesn't respond)
    - Invalid input rejection (malformed or illegal game actions)
    - Input queue overflow or race conditions
    - Concurrent input from multiple clients during multiplayer priority

13. **State Divergence Detection**: While checksum validation catches major desyncs, subtle state divergences (e.g., incorrect card ordering within a zone, missing UI-only properties) may not be detected if they don't affect the checksum.

---

## File Structure

```
forge-gui-desktop/src/test/java/forge/net/
├── AutomatedNetworkTest.java       # TestNG test suite
├── ComprehensiveDeltaSyncTest.java # 100-game validation test entry point
├── BatchGameTest.java              # Sequential and parallel batch test entry point
│
├── # Headless Execution
├── HeadlessGuiDesktop.java         # Headless GUI interface (bypasses display)
├── HeadlessNetworkClient.java      # Remote TCP client for delta sync
├── HeadlessNetworkGuiGame.java     # NetworkGuiGame for headless client
├── NoOpGuiGame.java                # No-op IGuiGame implementation
│
├── # Game Harnesses
├── AutomatedGameTestHarness.java   # Network game harness (ServerGameLobby)
├── LocalGameTestHarness.java       # Non-network harness (LocalLobby)
├── NetworkClientTestHarness.java   # Host+client orchestration
├── GameTestHarnessFactory.java     # Unified entry point for all modes
│
├── # Executors
├── SequentialGameExecutor.java     # Sequential multi-game execution
├── MultiProcessGameExecutor.java   # Parallel execution via JVM processes
├── ComprehensiveTestExecutor.java  # Orchestrates mixed 2-4 player batches
├── ComprehensiveGameRunner.java    # 2-4 player runner (subprocess entry)
│
├── # Utilities
├── GameTestMode.java               # Enum: LOCAL, NETWORK_LOCAL, NETWORK_REMOTE
├── TestConfiguration.java          # System properties configuration
├── TestDeckLoader.java             # Quest precon deck loading
├── GameTestMetrics.java            # Game and network metrics collection
├── GameEventListener.java          # Game event logging interface
├── PortAllocator.java              # Network port management
├── NetworkAIPlayerFactory.java     # AI player creation for tests
├── ConsoleNetworkTestRunner.java   # Standalone CI/CD entry point
│
├── analysis/
│   ├── NetworkLogAnalyzer.java     # Log file parsing
│   ├── GameLogMetrics.java         # Per-game metrics storage
│   └── AnalysisResult.java         # Aggregate results and reporting
│
└── scenarios/
    ├── ReconnectionScenario.java       # Disconnect/AI takeover test
    ├── MultiplayerScenario.java        # 3-4 player with local AI
    └── MultiplayerNetworkScenario.java # 3-4 player with remote clients

forge-gui/src/main/java/forge/gamemodes/net/
└── NetworkGameEventListener.java   # Production game event logging
```

**File Count:** 29 test files (23 main + 3 scenarios + 3 analysis)
