# Automated Network Testing Implementation Plan

## Executive Summary

This plan outlines automated network testing infrastructure for Forge's multiplayer functionality. The **primary focus is network play testing** for the NetworkPlay branch.

### Primary Objective

**Enable efficient debugging of network play without requiring a human to manually run two Forge instances.**

Currently, debugging network issues (delta sync, reconnection, desync) requires:
1. Manually starting two Forge instances
2. Manually hosting and joining a game
3. Manually playing through scenarios to reproduce bugs
4. Manually analyzing logs from both instances

This is time-consuming and error-prone. The automated test infrastructure aims to:
- **Simulate actual network traffic** between host and client(s)
- **Generate delta sync packets** that can be analyzed for debugging
- **Reproduce network scenarios** programmatically (disconnect, reconnect, desync)
- **Collect metrics** on bandwidth usage, packet counts, and timing
- **Enable rapid iteration** on network code without manual testing

### Current State vs Target State

| Aspect | Current State | Target State |
|--------|---------------|--------------|
| Player type | Local AI (`LobbySlotType.LOCAL`) | Remote AI clients (`LobbySlotType.REMOTE`) |
| Network traffic | None (0 bytes) | Real delta sync packets |
| Process model | Single process | Multi-process (host + clients) |
| Delta sync testing | Not possible | Full packet capture and analysis |
| Reconnection testing | Simulated only | Actual socket disconnect/reconnect |

### Why This Matters

Delta sync logging (`[DeltaSync]` prefix) **only occurs when there are actual remote clients**. The `NetGuiGame` class handles remote players and generates delta sync packets. With local AI players, this code path is never exercised, making it impossible to debug network synchronization issues through automated tests.

---

---

## Phase 8: True Network Client Simulation (COMPLETE)

This phase implements actual network traffic testing by connecting a headless client to the host server.

### 8.1 HeadlessNetworkClient

A headless network client that:
1. Connects to the host server as a remote client via `FGameClient`
2. Receives delta sync packets from `NetGuiGame`
3. Applies game state updates via the network protocol
4. Logs received packets for analysis with metrics collection

**Key classes:**
- `HeadlessNetworkClient` - Client connection and delta packet handling
- `DeltaLoggingGuiGame` - Inner class that extends `NoOpGuiGame` to capture delta/full state packets
- `FGameClient` - Existing Forge client networking

### 8.2 NetworkClientTestHarness

Test orchestration that:
1. Starts the host server with one local AI player
2. Spawns a `HeadlessNetworkClient` in a separate thread
3. Client sends deck and ready status via `UpdateLobbyPlayerEvent`
4. Coordinates game start across host and client
5. Monitors delta packet reception for success criteria
6. Clean shutdown of all components

### 8.3 Delta Sync Verification (Verified)

The test verifies:
- Delta packets are generated and received correctly (16+ packets in test run)
- Full state sync is sent on initial connection
- Cumulative byte tracking shows bandwidth savings (87-98% reduction vs full state)
- Client receives and processes all network events

**Test results (2026-01-23):**
- Delta packets received: 16
- Full state syncs: 1
- Total delta bytes: 11,063
- Test passed successfully in ~7 seconds

---

## Implementation Status

| Phase | Component | Status | Notes |
|-------|-----------|--------|-------|
| 0 | API Verification | ✅ Complete | Documented in PHASE0_API_FINDINGS.md |
| 0.5 | TestDeckLoader | ✅ Complete | Wraps existing DeckSerializer |
| 1 | ConsoleNetworkTestRunner | ✅ Complete | Main entry point with CLI args |
| 2 | AutomatedGameTestHarness | ✅ Complete | Core test orchestration (local AI only) |
| 3 | NetworkAIPlayerFactory | ✅ Complete | AI player configuration helper |
| 5.1 | BasicGameScenario | ✅ Complete | 2-player game test (local AI, no network traffic) |
| 5.2 | ReconnectionScenario | ✅ Complete | With GameEventListener (simulated, no real disconnect) |
| 6 | GameTestMetrics | ✅ Complete | Metrics aggregation |
| 7 | ParallelTestExecutor | ✅ Complete | Scale testing support |
| - | GameEventListener | ✅ Complete | Helper for subscribing to game events |
| - | NetworkGameEventListener | ✅ Complete | Production listener for network play game events |
| **8** | **Network Client Simulation** | ✅ **Complete** | **HeadlessNetworkClient + NetworkClientTestHarness** |
| **9** | **Full Game Completion** | ✅ **Complete** | EDT fix, username fix - games complete with winner |
| **10** | **Sequential Multi-Game** | ✅ **Complete** | SequentialGameExecutor with isolated log files |

**Status Legend**: ✅ Complete | 🟡 In Progress | 🔴 Not Started | ⚠️ Blocked

**Verification Status** (2026-01-24):
- ✅ All components compile successfully (0 checkstyle violations)
- ✅ TestNG tests pass (8/10): deck loading, metrics, AI profiles, server start/stop, 2-player, 4-player, true network
- ✅ Server infrastructure works: starts on port, accepts connections, stops cleanly
- ✅ HeadlessGuiDesktop enables full game execution without display server
- ✅ Game events logged via NetworkGameEventListener (`[GameEvent]` prefix)
- ✅ **True network traffic test passes** - HeadlessNetworkClient receives 610 delta sync packets
- ✅ **Full games complete** - 12 turns, winner determined, 99% bandwidth savings
- ✅ **Phase 9 Complete** - EDT fix enables input handling for cleanup discard
- ⚠️ 3-player and reconnection tests fail due to deck validation (pre-existing issue)

**Test Commands**:
```bash
# All network tests
mvn -pl forge-gui-desktop -am test -Dtest=forge.net.AutomatedNetworkTest

# True network traffic test only (Phase 8)
mvn -pl forge-gui-desktop -am test -Dtest=AutomatedNetworkTest#testTrueNetworkTraffic
```

**Phase 8 Achievement**: The `testTrueNetworkTraffic` test successfully:
- Connects a headless client to the server
- Exchanges deck/ready status via `UpdateLobbyPlayerEvent`
- Receives 16+ delta sync packets
- Verifies 87-98% bandwidth savings vs full state sync

---

## Known Limitations

### Network Traffic Gap (RESOLVED)

~~The test infrastructure previously did NOT generate network traffic because all players were local AI.~~

**RESOLVED with Phase 8**: The `HeadlessNetworkClient` connects as a true remote player, enabling:
- Delta sync packet generation and logging
- Full state sync on connection
- Bandwidth metrics collection
- Network protocol verification

**Remaining Limitation**: The headless client cannot make gameplay decisions (mulligan, priority choices). The test exits early once delta sync is verified (2+ packets received). For full game completion testing, additional AI input handling would be needed in `DeltaLoggingGuiGame`.

---

## Phase 9: Full Game Completion with Remote Client (COMPLETE)

Enable the HeadlessNetworkClient to complete full games by adding AI decision-making to `DeltaLoggingGuiGame`.

### 9.1 Implementation (Completed)

**Key Fixes Applied:**
1. **FGameClient username fix** - Constructor now stores username and uses it for LoginEvent instead of always reading from preferences
2. **HeadlessGuiDesktop EDT fix** - Override `invokeInEdtLater`/`invokeInEdtNow` to execute immediately since there's no Swing EDT in headless mode

**Input Handling in DeltaLoggingGuiGame:**
- `updateButtons()` - Auto-clicks OK/Cancel for priority passes, mulligan, etc.
- `setSelectables()` - Auto-selects first card for cleanup discard

### 9.2 Test Results (2026-01-24)

```
Game completed in 12 turns
Winner: Alice (Host AI)
Delta packets: 610
Full state syncs: 1
Bandwidth savings: 99%
Test duration: ~18 seconds
```

### 9.3 Success Criteria (All Met)

- ✅ Remote client completes full game (not just delta sync verification)
- ✅ Winner determination works
- ✅ Game progresses through all phases (mulligan to game end)
- ✅ Network traffic logged throughout (610 delta packets)

---

## Phase 10: Sequential Multi-Game Execution (COMPLETE)

Run multiple network games sequentially for **rapid debugging and log generation**.

**Primary Purpose:** Generate multiple game logs quickly for analysis. Each game runs to completion before the next starts, ensuring 100% reliability while producing isolated log files for debugging.

```bash
# Run 3 games sequentially
mvn -pl forge-gui-desktop test -Dtest=SequentialGameTest#testThreeSequentialGames

# Run custom number of games
mvn -pl forge-gui-desktop test -Dtest=SequentialGameTest#testConfigurableGameCount -Dtest.gameCount=5
```

### 10.1 Architecture

```
SequentialGameExecutor
    ├── Configuration
    │   ├── basePort: int (default 58000)
    │   └── timeoutMs: long (default 300000 = 5 minutes per game)
    │
    ├── Game 0 (runs first)
    │   ├── Host (port 58000) + HeadlessClient
    │   └── Log: network-debug-YYYYMMDD-HHMMSS-PID-game0.log
    │
    ├── Game 1 (runs after Game 0 completes)
    │   ├── Host (port 58001) + HeadlessClient
    │   └── Log: network-debug-YYYYMMDD-HHMMSS-PID-game1.log
    │
    ├── ... (games run sequentially)
    │
    └── Aggregator
        ├── Collects results from all games
        ├── Generates summary report
        └── Returns combined metrics
```

### 10.2 Isolated Log Files

Each game writes to its own log file via `NetworkDebugLogger.setInstanceSuffix()`.

**Log naming convention**:
- Single game: `network-debug-20260123-153045-12345.log`
- Sequential game 0: `network-debug-20260123-153045-12345-game0.log`
- Sequential game 1: `network-debug-20260123-153045-12345-game1.log`
- Test environment: `network-debug-20260123-153045-12345-game0-test.log`

### 10.3 Test Class

**File**: `forge-gui-desktop/src/test/java/forge/net/SequentialGameTest.java`

```java
@Test
public void testThreeSequentialGames() {
    SequentialGameExecutor executor = new SequentialGameExecutor(300000);
    SequentialGameExecutor.ExecutionResult result = executor.runGames(3);

    assertTrue(result.getSuccessCount() >= 3, "All games should succeed");
}

@Test
public void testConfigurableGameCount() {
    int gameCount = Integer.getInteger("test.gameCount", 3);
    SequentialGameExecutor executor = new SequentialGameExecutor();
    SequentialGameExecutor.ExecutionResult result = executor.runGames(gameCount);

    assertEquals(result.getSuccessCount(), gameCount, "All games should succeed");
}
```

### 10.4 Configuration Options

| Property | Default | Description |
|----------|---------|-------------|
| `test.gameCount` | 3 | Total games to run |
| `test.timeout` | 300000 | Per-game timeout in milliseconds |

**Usage**:
```bash
# Run 3 games
mvn -pl forge-gui-desktop test -Dtest=SequentialGameTest#testThreeSequentialGames

# Run 10 games
mvn -pl forge-gui-desktop test -Dtest=SequentialGameTest#testConfigurableGameCount -Dtest.gameCount=10
```

### 10.5 Log Analysis

When analyzing test logs:

1. **Find all logs for a test run**:
   ```bash
   ls -la forge-gui-desktop/logs/network-debug-*-game*.log
   ```

2. **Search across all game logs**:
   ```bash
   grep -l "error\|ERROR" forge-gui-desktop/logs/network-debug-*-game*.log
   ```

### 10.6 Success Criteria

- [x] Run multiple network games sequentially
- [x] Each game writes to its own log file
- [x] Logs are clearly identifiable by game index
- [x] Aggregated metrics are accurate
- [x] Port conflicts are avoided
- [x] Clean shutdown of all resources via `endGameSession()`
- [x] 100% success rate

### GUI Initialization (Resolved)

Previously, tests required a display server because `GuiDesktop.hostMatch()` needed `Singletons.getControl()`.

**Resolved**: `HeadlessGuiDesktop` provides headless GUI implementation that works without display server.

---

## Files Created

All files in `forge-gui-desktop/src/test/java/forge/net/`:

```
forge-gui-desktop/src/test/java/forge/net/
├── TestDeckLoader.java              (Phase 0.5) ✅
├── ConsoleNetworkTestRunner.java    (Phase 1) ✅
├── AutomatedGameTestHarness.java    (Phase 2) ✅
├── NetworkAIPlayerFactory.java      (Phase 3) ✅
├── GameTestMetrics.java             (Phase 6) ✅
├── ParallelTestExecutor.java        (Phase 7) ✅
├── GameEventListener.java           (Helper) ✅
├── AutomatedNetworkTest.java        (TestNG tests) ✅
├── HeadlessNetworkClient.java       (Phase 8) ✅   # Headless client with delta logging
├── NetworkClientTestHarness.java    (Phase 8) ✅   # Test orchestration for host+client
├── HeadlessGuiDesktop.java          (Headless) ✅  # Headless GUI for testing
├── NoOpGuiGame.java                 (Headless) ✅  # No-op IGuiGame implementation
├── SequentialGameExecutor.java      (Phase 10) ✅  # Sequential multi-game execution
├── SequentialGameTest.java          (Phase 10) ✅  # Sequential execution tests
└── scenarios/
    ├── BasicGameScenario.java       (Phase 5.1) ✅
    ├── ReconnectionScenario.java    (Phase 5.2) ✅
    └── MultiplayerScenario.java     (Phase 5.3) ✅
```

Production file in `forge-gui/src/main/java/forge/gamemodes/net/`:
```
forge-gui/src/main/java/forge/gamemodes/net/
└── NetworkGameEventListener.java    (Production) ✅  # Game event logging for network play
```

---

## Phase 0.5: TestDeckLoader (Simplified)

**File**: `forge-gui-desktop/src/test/java/forge/net/TestDeckLoader.java`

**Key Change**: Uses existing `DeckSerializer` infrastructure instead of reimplementing deck parsing.

```java
package forge.net;

import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.properties.ForgeConstants;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Thin wrapper around existing DeckSerializer for test deck loading.
 * Uses quest precons which are known-good balanced decks.
 */
public class TestDeckLoader {

    /**
     * Load a quest precon deck by name.
     * @param name Deck name without .dck extension
     * @return Deck with valid cards
     */
    public static Deck loadQuestPrecon(String name) {
        File deckFile = new File(ForgeConstants.QUEST_PRECON_DIR, name + ".dck");
        return DeckSerializer.fromFile(deckFile);
    }

    /**
     * List all available quest precon deck names.
     * @return List of deck names (without .dck extension)
     */
    public static List<String> listAvailablePrecons() {
        File preconDir = new File(ForgeConstants.QUEST_PRECON_DIR);
        File[] files = preconDir.listFiles((d, n) -> n.endsWith(".dck"));
        if (files == null) return Collections.emptyList();
        return Arrays.stream(files)
            .map(f -> f.getName().replace(".dck", ""))
            .collect(Collectors.toList());
    }

    /**
     * Get a random quest precon deck.
     * @return Random precon deck with valid cards
     */
    public static Deck getRandomPrecon() {
        List<String> precons = listAvailablePrecons();
        if (precons.isEmpty()) {
            throw new IllegalStateException("No quest precon decks found in " +
                ForgeConstants.QUEST_PRECON_DIR);
        }
        String randomName = precons.get(new Random().nextInt(precons.size()));
        return loadQuestPrecon(randomName);
    }
}
```

**Success Criteria**:
- [ ] Can list available precon decks
- [ ] Can load precon deck by name
- [ ] Loaded deck has 40+ cards
- [ ] `lobby.startGame()` returns non-null with loaded deck

---

## Phase 1: ConsoleNetworkTestRunner

**File**: `forge-gui-desktop/src/test/java/forge/net/ConsoleNetworkTestRunner.java`

**Initialization Pattern**: Reuse AITest patterns for FModel initialization.

```java
package forge.net;

import forge.GuiBase;
import forge.GuiDesktop;
import forge.model.FModel;
import forge.properties.ForgePreferences;

public class ConsoleNetworkTestRunner {

    public static void main(String[] args) {
        // Parse arguments
        int gameCount = parseGameCount(args);

        // Initialize FModel (requires Xvfb on headless systems)
        initializeFModel();

        // Create and run test harness
        AutomatedGameTestHarness harness = new AutomatedGameTestHarness();

        for (int i = 0; i < gameCount; i++) {
            try {
                harness.runBasicTwoPlayerGame();
                System.out.println("Game " + (i + 1) + " completed successfully");
            } catch (Exception e) {
                System.err.println("Game " + (i + 1) + " failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void initializeFModel() {
        // Pattern from AITest - lazy loading disabled for test stability
        GuiBase.setInterface(new GuiDesktop());
        FModel.initialize(null, new ForgePreferences());
    }

    private static int parseGameCount(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--games".equals(args[i])) {
                return Integer.parseInt(args[i + 1]);
            }
        }
        return 1; // Default
    }
}
```

**Command-Line Interface**:
```bash
# Run single game
xvfb-run mvn exec:java -Dexec.mainClass="forge.net.ConsoleNetworkTestRunner" \
    -Dexec.classpathScope=test

# Run multiple games
xvfb-run mvn exec:java -Dexec.mainClass="forge.net.ConsoleNetworkTestRunner" \
    -Dexec.classpathScope=test -Dexec.args="--games 5"
```

**Success Criteria**:
- [ ] Runs from command line without GUI
- [ ] Completes at least one game
- [ ] Clean shutdown (no hanging processes)

---

## Phase 2: AutomatedGameTestHarness

**File**: `forge-gui-desktop/src/test/java/forge/net/AutomatedGameTestHarness.java`

**Core network test orchestration using validated Phase 0 patterns.**

```java
package forge.net;

import forge.deck.Deck;
import forge.gamemodes.net.server.FServerManager;
import forge.gamemodes.net.server.ServerGameLobby;
import forge.gamemodes.net.server.LobbySlot;
import forge.gamemodes.net.server.LobbySlotType;
import forge.gamemodes.net.NetworkByteTracker;
import forge.gamemodes.net.NetworkDebugLogger;

public class AutomatedGameTestHarness {
    private FServerManager server;
    private ServerGameLobby lobby;
    private int port = 55555;

    /**
     * Run a basic 2-player AI game over network infrastructure.
     */
    public GameTestResult runBasicTwoPlayerGame() {
        try {
            // 1. Start server
            server = FServerManager.getInstance();
            server.startServer(port++); // Increment port to avoid conflicts

            // 2. Create lobby and set on server (no getter available)
            lobby = new ServerGameLobby();
            server.setLobby(lobby);

            // 3. Configure slot 0 as AI
            LobbySlot alice = lobby.getSlot(0);
            alice.setType(LobbySlotType.AI);
            alice.setName("Alice (AI)");
            alice.setDeck(TestDeckLoader.getRandomPrecon());
            alice.setIsReady(true);

            // 4. Configure slot 1 as AI
            LobbySlot bob = lobby.getSlot(1);
            bob.setType(LobbySlotType.AI);
            bob.setName("Bob (AI)");
            bob.setDeck(TestDeckLoader.getRandomPrecon());
            bob.setIsReady(true);

            // 5. Start game (returns Runnable, must check null)
            Runnable start = lobby.startGame();
            if (start == null) {
                throw new IllegalStateException("startGame() returned null - deck validation likely failed");
            }
            server.createGameSession();
            start.run();

            // 6. Wait for game completion
            waitForGameCompletion();

            // 7. Collect metrics
            return collectMetrics();

        } finally {
            cleanup();
        }
    }

    private void waitForGameCompletion() {
        // TODO: Implement game completion detection via event listeners
        // For now, games run synchronously with AI
    }

    private GameTestResult collectMetrics() {
        GameTestResult result = new GameTestResult();

        NetworkByteTracker tracker = server.getNetworkByteTracker();
        if (tracker != null) {
            result.totalBytesSent = tracker.getTotalBytesSent();
            result.deltaBytesSent = tracker.getDeltaBytesSent();
            result.fullStateBytesSent = tracker.getFullStateBytesSent();
            NetworkDebugLogger.log("[AutomatedTest] %s", tracker.getStatsSummary());
        }

        return result;
    }

    private void cleanup() {
        if (server != null && server.isHosting()) {
            server.stopServer();
        }
        lobby = null;
    }

    public static class GameTestResult {
        public long totalBytesSent;
        public long deltaBytesSent;
        public long fullStateBytesSent;
        public String winner;
        public int turnCount;
    }
}
```

**Success Criteria**:
- [ ] Can configure 2-player server-side AI games
- [ ] Games complete with winner determination
- [ ] NetworkByteTracker shows non-zero bytes
- [ ] Clean shutdown after each game

---

## Phase 3: NetworkAIPlayerFactory

**File**: `forge-gui-desktop/src/test/java/forge/net/NetworkAIPlayerFactory.java`

**Convenience helper for configuring AI players.**

```java
package forge.net;

import forge.deck.Deck;
import forge.gamemodes.net.server.LobbySlot;
import forge.gamemodes.net.server.LobbySlotType;

public class NetworkAIPlayerFactory {

    public enum AIProfile {
        DEFAULT,
        AGGRESSIVE,
        CONTROL
    }

    /**
     * Configure a lobby slot as an AI player.
     */
    public static void configureAIPlayer(LobbySlot slot, String name, AIProfile profile) {
        slot.setType(LobbySlotType.AI);
        slot.setName(name);
        slot.setDeck(selectDeckForProfile(profile));
        slot.setIsReady(true);
    }

    private static Deck selectDeckForProfile(AIProfile profile) {
        // For now, just use random precon
        // Later: map profiles to specific deck archetypes
        return TestDeckLoader.getRandomPrecon();
    }
}
```

---

## Phase 5.1: BasicGameScenario

**File**: `forge-gui-desktop/src/test/java/forge/net/scenarios/BasicGameScenario.java`

```java
package forge.net.scenarios;

import forge.net.AutomatedGameTestHarness;
import forge.net.AutomatedGameTestHarness.GameTestResult;

/**
 * Tests basic 2-player game with no interruptions.
 * Validates: Game completion, winner determination, basic network sync.
 */
public class BasicGameScenario {

    public ScenarioResult execute() {
        AutomatedGameTestHarness harness = new AutomatedGameTestHarness();
        GameTestResult result = harness.runBasicTwoPlayerGame();

        return new ScenarioResult(
            result.winner != null,
            result.totalBytesSent > 0,
            "Basic 2-player game"
        );
    }

    public static class ScenarioResult {
        public final boolean gameCompleted;
        public final boolean networkTrafficRecorded;
        public final String description;

        public ScenarioResult(boolean gameCompleted, boolean networkTrafficRecorded, String description) {
            this.gameCompleted = gameCompleted;
            this.networkTrafficRecorded = networkTrafficRecorded;
            this.description = description;
        }

        public boolean passed() {
            return gameCompleted && networkTrafficRecorded;
        }
    }
}
```

---

## Phase 5.2: ReconnectionScenario

**File**: `forge-gui-desktop/src/test/java/forge/net/scenarios/ReconnectionScenario.java`

```java
package forge.net.scenarios;

/**
 * Tests player disconnection and reconnection.
 * Validates: AI takeover, player reconnection, session restoration.
 */
public class ReconnectionScenario {

    public ScenarioResult execute() {
        // TODO: Implement after basic scenario works
        // 1. Start 2-player game
        // 2. Simulate disconnect on turn 3
        // 3. Verify AI takeover
        // 4. Simulate reconnect after 30 seconds
        // 5. Verify session restored
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

---

## Phase 6: GameTestMetrics

**File**: `forge-gui-desktop/src/test/java/forge/net/GameTestMetrics.java`

```java
package forge.net;

import forge.gamemodes.net.NetworkByteTracker;

/**
 * Aggregates metrics from test runs.
 */
public class GameTestMetrics {
    // Game metrics
    public int turnCount;
    public long gameDurationMs;
    public String winner;

    // Network metrics (from NetworkByteTracker)
    public long totalBytesSent;
    public long deltaBytesSent;
    public long fullStateBytesSent;
    public long deltaPacketCount;
    public long fullStatePacketCount;

    // Reconnection metrics
    public int disconnectionCount;
    public int reconnectionSuccesses;
    public int reconnectionFailures;

    /**
     * Collect metrics from NetworkByteTracker.
     */
    public void collectFromTracker(NetworkByteTracker tracker) {
        if (tracker != null) {
            this.totalBytesSent = tracker.getTotalBytesSent();
            this.deltaBytesSent = tracker.getDeltaBytesSent();
            this.fullStateBytesSent = tracker.getFullStateBytesSent();
            this.deltaPacketCount = tracker.getDeltaPacketCount();
            this.fullStatePacketCount = tracker.getFullStatePacketCount();
        }
    }

    public String toSummary() {
        return String.format(
            "Turns: %d, Duration: %dms, Winner: %s, " +
            "Network: %d bytes total (%d delta, %d full state)",
            turnCount, gameDurationMs, winner,
            totalBytesSent, deltaBytesSent, fullStateBytesSent
        );
    }
}
```

---

## Phase 7: ParallelTestExecutor

**File**: `forge-gui-desktop/src/test/java/forge/net/ParallelTestExecutor.java`

```java
package forge.net;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Executes test scenarios in parallel for scale testing.
 */
public class ParallelTestExecutor {

    public List<AutomatedGameTestHarness.GameTestResult> executeParallel(
            int iterations, int parallelism) {

        ExecutorService executor = Executors.newFixedThreadPool(parallelism);
        List<Future<AutomatedGameTestHarness.GameTestResult>> futures = new ArrayList<>();

        for (int i = 0; i < iterations; i++) {
            futures.add(executor.submit(() -> {
                AutomatedGameTestHarness harness = new AutomatedGameTestHarness();
                return harness.runBasicTwoPlayerGame();
            }));
        }

        List<AutomatedGameTestHarness.GameTestResult> results = new ArrayList<>();
        for (Future<AutomatedGameTestHarness.GameTestResult> future : futures) {
            try {
                results.add(future.get(10, TimeUnit.MINUTES));
            } catch (Exception e) {
                System.err.println("Test failed: " + e.getMessage());
            }
        }

        executor.shutdown();
        return results;
    }
}
```

---

## Implementation Order

1. **TestDeckLoader** - Verify deck loading works (unblocks everything)
2. **ConsoleNetworkTestRunner** - Basic CLI skeleton
3. **AutomatedGameTestHarness** - Core network test infrastructure
4. **BasicGameScenario** - Validate end-to-end network flow
5. **Additional scenarios** - Reconnection, multiplayer, stress tests
6. **Metrics & Parallel** - Enhancement phases

---

## Comprehensive Test Reporting Requirements

When running the 100-game comprehensive test and reporting results in `.documentation/Testing.md`, the following metrics **must be included**:

### Required Metrics

1. **Overall Results**
   - Total games / successful games / failed games
   - Success rate percentage
   - Total test duration
   - Unique decks tested (from total deck usages)

2. **Breakdown by Player Count**
   - Number of games at each player count (2p, 3p, 4p)
   - Success rate per player count
   - Average turns per player count

3. **Bandwidth Efficiency Breakdown**

   The bandwidth reporting must include the **three-tier breakdown** matching the `[DeltaSync]` log format:

   | Metric | Description |
   |--------|-------------|
   | **Approximate** | Estimated delta size based on object diffs (calculated, not network) |
   | **ActualNetwork** | Actual bytes transmitted over the network (serialized + protocol overhead) |
   | **FullState** | Size if full game state was sent (for comparison/savings calculation) |

   Report these as:
   - Total bytes for each category across all games
   - Average bytes per packet for each category
   - Savings percentages: `Approximate vs FullState` and `Actual vs FullState`

   **Example format in .documentation/Testing.md:**
   ```markdown
   ### Bandwidth Efficiency

   | Metric | Total | Per Packet | Savings vs Full State |
   |--------|-------|------------|----------------------|
   | Approximate | 1,234,567 bytes | 42 bytes | 99.2% |
   | Actual Network | 2,345,678 bytes | 80 bytes | 98.4% |
   | Full State | 145,678,901 bytes | 4,982 bytes | - |
   ```

4. **Delta Sync Statistics**
   - Total delta packets sent
   - Total bytes transferred
   - Checksum mismatches (should be 0)

### Log Analysis Source

The `NetworkLogAnalyzer` parses log files for these metrics by extracting:
```
[DeltaSync] Packet #N: Approximate=X bytes, ActualNetwork=Y bytes, FullState=Z bytes
[DeltaSync]   Savings: Approximate=A%, Actual=B%
```

The comprehensive test executor should aggregate these from all game logs and include them in the final report.

---

## Verification Plan

1. **Unit test**: `TestDeckLoader` loads valid decks with 40+ cards
2. **Integration test**: `AutomatedGameTestHarness` completes a 2-player AI game over network
3. **CLI test**: `ConsoleNetworkTestRunner --games 1` completes successfully
4. **Network validation**: `NetworkByteTracker` shows expected delta/full-state packet counts
5. **Log validation**: `NetworkDebugLogger` captures game events in expected format

---

## Key API Corrections (from Phase 0)

| Assumption | Reality | Impact |
|------------|---------|--------|
| `lobby.addAIPlayer(name)` exists | No such method | Use `slot.setType(AI)` pattern |
| `server.getLobby()` exists | No getter | Create lobby and call `server.setLobby()` |
| `lobby.startGame()` is void | Returns `Runnable` | Must check null and call `run()` |
| NetworkByteTracker always available | Can be null | Check `!= null` before using |

---

## Environment Requirements

- **Java**: 17+
- **Display**: Xvfb required for headless operation
- **Build**: `mvn -pl forge-gui-desktop -am install -DskipTests`
- **Run**: `xvfb-run mvn exec:java -Dexec.mainClass="forge.net.ConsoleNetworkTestRunner" -Dexec.classpathScope=test`

---

## Future Local Testing Support

If local (non-network) testing becomes a priority later, the architecture supports adding:

```java
public class LocalGameTestHarness implements GameTestHarness {
    // Uses Match.createGame() directly
    // No FServerManager overhead
    // Could share TestDeckLoader and test scenarios
}
```

This would enable faster test execution for game logic testing (no serialization overhead).

**This is documented for future reference, not current implementation.**
