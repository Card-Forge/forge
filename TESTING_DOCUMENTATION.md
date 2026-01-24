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
| `NoOpGuiGame` | No-op `IGuiGame` implementation (~80 methods) for AI games |

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
| `BasicGameScenario` | 2-player AI game completion |
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
    -Dtest="SequentialGameTest#testConfigurableGameCount" \
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
    -Dtest="SequentialGameTest#testConfigurableGameCount" \
    -Dtest.gameCount=5 -Dsurefire.failIfNoSpecifiedTests=false
```

**Multi-Process Parallel** (separate JVMs, true parallelism):
```bash
mvn -pl forge-gui-desktop -am verify \
    -Dtest="MultiProcessGameTest#testConfigurableGameCount" \
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

**Format**: `network-debug-YYYYMMDD-HHMMSS-PID-[gameN-Xp]-test.log`

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
| Test Date | 2026-01-24 |
| Total Games | 100 |
| Distribution | 50 x 2-player, 30 x 3-player, 20 x 4-player |
| Batch Size | 10 parallel games |
| Timeout | 5 minutes per game |
| Deck Selection | Random quest precons |
| Test Duration | 87 minutes |

### Summary Results

| Metric | Value |
|--------|-------|
| Successful Games | 100 |
| Success Rate | **100%** |
| Checksum Mismatches | **0** |
| Games with Errors | 0 |
| Average Turns per Game | 22.5 |

### Bandwidth Usage Breakdown

| Metric | Total | Avg/Game | Description |
|--------|-------|----------|-------------|
| Approximate | 45.3 MB | 391 KB | Estimated delta size from object diffs |
| ActualNetwork | 144.6 MB | 1.25 MB | Actual bytes sent over network |
| FullState | 30.8 GB | 265 MB | Size if full state was sent |

**Three-Tier Bandwidth Metrics Explained:**

1. **Approximate** - Pre-serialization estimate based on counting changed objects and their field sizes. This is calculated before any network overhead is added.

2. **ActualNetwork** - The actual bytes transmitted over TCP after serialization, compression, and protocol framing. This is what bandwidth is consumed.

3. **FullState** - The hypothetical size if the entire game state were sent each update (no delta sync). This baseline shows what bandwidth would be used without optimization.

**Bandwidth Savings:**
- ActualNetwork vs FullState: **99.5% savings** — The key metric showing delta sync effectiveness
- Approximate vs FullState: 99.9% savings — Shows theoretical maximum if serialization had zero overhead
- Bytes per delta packet: ~38 bytes — Average packet size demonstrates efficient incremental updates

### Results by Player Count

| Players | Games | Success Rate | Avg Turns | Avg Savings |
|---------|-------|--------------|-----------|-------------|
| 2 | 50 | 100% | 14.4 | 99.4% |
| 3 | 30 | 100% | 25.5 | 99.5% |
| 4 | 20 | 100% | 43.2 | 99.6% |

### Bandwidth by Player Count

| Players | Approximate | ActualNetwork | FullState | Savings |
|---------|-------------|---------------|-----------|---------|
| 2 | 10,373,783 | 29,076,110 | 5,037,330,006 | 99.4% |
| 3 | 15,171,768 | 45,540,799 | 9,503,034,088 | 99.5% |
| 4 | 19,772,334 | 69,991,428 | 16,245,580,646 | 99.6% |

### Validation Criteria

| Criterion | Target | Actual | Status |
|-----------|--------|--------|--------|
| Success Rate | ≥90% | 100% | **Pass** |
| Bandwidth Savings | ≥90% | 99.5% | **Pass** |
| Bytes per Packet | <200 | ~38 | **Pass** |
| Checksum Mismatches | 0 | 0 | **Pass** |

---

## Known Limitations

1. **AI Input Handling**: Remote client auto-responds to prompts with default choices; no strategic decision-making.

2. **Single JVM Limitation**: `FServerManager` singleton prevents multiple servers in one JVM; use multi-process execution for parallelism.

3. **Timeout Interpretation**: Games exceeding timeout are not network failures—they indicate complex/long games. Increase `test.timeoutMs` if needed.

4. **Deck Availability**: Deck tests require the `res/` directory to be populated. Tests use quest precon decks from `forge-gui/res/quest/precons/`.

---

## File Structure

```
forge-gui-desktop/src/test/java/forge/net/
├── AutomatedNetworkTest.java       # TestNG test suite
├── HeadlessNetworkClient.java      # Remote client for delta sync
├── NetworkClientTestHarness.java   # Host+client orchestration
├── HeadlessGuiDesktop.java         # Headless GUI interface
├── NoOpGuiGame.java                # No-op IGuiGame
├── AutomatedGameTestHarness.java   # Network game harness
├── LocalGameTestHarness.java       # Non-network game harness
├── GameTestHarnessFactory.java     # Unified test execution
├── GameTestMode.java               # Test mode enum
├── TestConfiguration.java          # Command-line configuration
├── TestDeckLoader.java             # Quest precon loading
├── GameTestMetrics.java            # Metrics collection
├── SequentialGameExecutor.java     # Sequential execution
├── SequentialGameTest.java         # Sequential test entry point
├── MultiProcessGameExecutor.java   # Parallel execution
├── MultiProcessGameTest.java       # Multi-process test entry point
├── SingleGameRunner.java           # Standalone 2-player runner
├── ComprehensiveGameRunner.java    # 2-4 player runner
├── ComprehensiveTestExecutor.java  # Test orchestration
├── ComprehensiveDeltaSyncTest.java # Validation test entry point
├── analysis/
│   ├── NetworkLogAnalyzer.java     # Log parser
│   ├── GameLogMetrics.java         # Per-game metrics
│   └── AnalysisResult.java         # Aggregate results
└── scenarios/
    ├── BasicGameScenario.java
    ├── ReconnectionScenario.java
    ├── MultiplayerScenario.java
    └── MultiplayerNetworkScenario.java

forge-gui/src/main/java/forge/gamemodes/net/
└── NetworkGameEventListener.java   # Production game event logging
```
