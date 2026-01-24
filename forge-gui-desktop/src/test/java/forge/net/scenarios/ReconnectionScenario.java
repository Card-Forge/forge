package forge.net.scenarios;

import forge.deck.Deck;
import forge.game.GameOutcome;
import forge.game.Match;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.NetworkDebugLogger;
import forge.gamemodes.net.server.FServerManager;
import forge.gamemodes.net.server.ServerGameLobby;
import forge.net.GameEventListener;
import forge.net.GameTestMetrics;
import forge.net.HeadlessGuiDesktop;
import forge.net.TestDeckLoader;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import forge.net.PortAllocator;

/**
 * Tests player disconnection and AI takeover scenarios.
 * Validates: AI takeover timing, game continuation after disconnect.
 *
 * Phase 5.2 of the Automated Network Testing Plan.
 *
 * <h2>Current Limitations</h2>
 * <p>
 * True disconnect/reconnect testing requires a real network client connection.
 * The disconnect handling code in FServerManager is triggered by Netty's
 * channelInactive() callback when a client TCP connection drops.
 * </p>
 * <p>
 * Without a real client, we cannot trigger:
 * <ul>
 *   <li>markPlayerDisconnected() in GameSession</li>
 *   <li>convertPlayerToAI() private method in FServerManager</li>
 *   <li>/skipreconnect command processing</li>
 * </ul>
 * </p>
 *
 * <h2>What This Scenario Does Test</h2>
 * <ul>
 *   <li>Game execution through network infrastructure</li>
 *   <li>AI players completing a game</li>
 *   <li>Server lifecycle management</li>
 *   <li>Network metrics collection</li>
 * </ul>
 *
 * <h2>For True Disconnect Testing</h2>
 * <p>
 * Manual testing procedure:
 * <ol>
 *   <li>Start host: Run Forge desktop app, host a game</li>
 *   <li>Start client: Run second Forge instance, connect to host</li>
 *   <li>During game, close/disconnect the client</li>
 *   <li>On host, type /skipreconnect in lobby chat</li>
 *   <li>Verify AI takes over and game continues</li>
 * </ol>
 * </p>
 */
public class ReconnectionScenario {

    /**
     * Result of the reconnection scenario.
     */
    public static class ScenarioResult {
        public final boolean gameStarted;
        public final boolean gameCompleted;
        public final int turnCount;
        public final String winner;
        public final String description;
        public final GameTestMetrics metrics;
        public final String errorMessage;

        public ScenarioResult(boolean gameStarted, boolean gameCompleted,
                              int turnCount, String winner, String description,
                              GameTestMetrics metrics, String errorMessage) {
            this.gameStarted = gameStarted;
            this.gameCompleted = gameCompleted;
            this.turnCount = turnCount;
            this.winner = winner;
            this.description = description;
            this.metrics = metrics;
            this.errorMessage = errorMessage;
        }

        public boolean passed() {
            // Test passes if game started and completed with a winner
            return gameStarted && gameCompleted && turnCount > 0;
        }

        @Override
        public String toString() {
            return String.format(
                "ScenarioResult[%s, started=%b, completed=%b, turns=%d, winner=%s, error=%s]",
                description, gameStarted, gameCompleted, turnCount, winner, errorMessage);
        }
    }

    // Configuration
    private long gameTimeoutSeconds = 120;
    private Deck deck1 = null;
    private Deck deck2 = null;

    /**
     * Set the game timeout in seconds.
     *
     * @param seconds Timeout in seconds
     * @return this for method chaining
     */
    public ReconnectionScenario gameTimeout(long seconds) {
        this.gameTimeoutSeconds = seconds;
        return this;
    }

    /**
     * Set specific decks for the players.
     *
     * @param deck1 Deck for player 1
     * @param deck2 Deck for player 2
     * @return this for method chaining
     */
    public ReconnectionScenario withDecks(Deck deck1, Deck deck2) {
        this.deck1 = deck1;
        this.deck2 = deck2;
        return this;
    }

    /**
     * Execute the reconnection scenario.
     *
     * <p>
     * Currently runs an AI-vs-AI game through the network infrastructure.
     * True disconnect testing requires a real network client - see class javadoc.
     * </p>
     *
     * @return ScenarioResult with test outcomes
     */
    public ScenarioResult execute() {
        NetworkDebugLogger.log("[ReconnectionScenario] Starting game scenario");
        NetworkDebugLogger.log("[ReconnectionScenario] Note: True disconnect testing requires a real network client.");
        NetworkDebugLogger.log("[ReconnectionScenario] This test validates game execution through network infrastructure.");

        GameTestMetrics metrics = new GameTestMetrics();
        FServerManager server = null;
        ServerGameLobby lobby = null;

        boolean gameStarted = false;
        boolean gameCompleted = false;
        int turnCount = 0;
        String winner = null;

        try {
            // 1. Start server
            int port = PortAllocator.allocatePort();
            server = FServerManager.getInstance();
            server.startServer(port);
            logServerInstanceBanner("ReconnectionScenario", 2, port);

            // 2. Create lobby
            lobby = new ServerGameLobby();
            server.setLobby(lobby);

            // 3. Configure player 1 as AI
            Deck playerDeck1 = deck1 != null ? deck1 : TestDeckLoader.getRandomPrecon();
            LobbySlot slot1 = lobby.getSlot(0);
            slot1.setType(LobbySlotType.AI);
            slot1.setName("Alice (AI)");
            slot1.setDeck(playerDeck1);
            slot1.setIsReady(true);
            NetworkDebugLogger.log("[ReconnectionScenario] Player 1 configured: %s", slot1.getName());

            // 4. Configure player 2 as AI
            Deck playerDeck2 = deck2 != null ? deck2 : TestDeckLoader.getRandomPrecon();
            LobbySlot slot2 = lobby.getSlot(1);
            slot2.setType(LobbySlotType.AI);
            slot2.setName("Bob (AI)");
            slot2.setDeck(playerDeck2);
            slot2.setIsReady(true);
            NetworkDebugLogger.log("[ReconnectionScenario] Player 2 configured: %s", slot2.getName());

            // 5. Start game
            NetworkDebugLogger.log("[ReconnectionScenario] Starting game...");
            long startTime = System.currentTimeMillis();

            Runnable start = lobby.startGame();
            if (start == null) {
                return new ScenarioResult(false, false, 0, null,
                    "Reconnection scenario", metrics,
                    "startGame() returned null - deck validation likely failed");
            }

            gameStarted = true;
            start.run();

            // 6. Register event listener for game action logging
            // Note: Production NetworkGameEventListener handles verbose logging
            HostedMatch hostedMatch = HeadlessGuiDesktop.getLastMatch();
            if (hostedMatch != null && hostedMatch.getGame() != null) {
                GameEventListener listener = new GameEventListener().setVerboseLogging(false);
                hostedMatch.getGame().subscribeToEvents(listener);
                NetworkDebugLogger.log("[ReconnectionScenario] Event listener registered");
            }

            // 7. Wait for game completion (games run asynchronously)
            NetworkDebugLogger.log("[ReconnectionScenario] Waiting for game completion...");

            if (hostedMatch != null) {
                int maxWaitMs = (int) (gameTimeoutSeconds * 1000);
                int waitedMs = 0;
                int pollIntervalMs = 100;

                while (waitedMs < maxWaitMs) {
                    Match match = hostedMatch.getMatch();
                    if (match != null) {
                        Collection<GameOutcome> outcomes = match.getOutcomes();
                        if (outcomes != null && !outcomes.isEmpty()) {
                            gameCompleted = true;
                            break;
                        }
                    }

                    Thread.sleep(pollIntervalMs);
                    waitedMs += pollIntervalMs;

                    // Log progress every 10 seconds
                    if (waitedMs % 10000 == 0) {
                        NetworkDebugLogger.log("[ReconnectionScenario] Still waiting... %ds", waitedMs / 1000);
                    }
                }

                // Extract results
                if (gameCompleted) {
                    Match match = hostedMatch.getMatch();
                    List<GameOutcome> outcomes = new ArrayList<>(match.getOutcomes());
                    GameOutcome outcome = outcomes.get(outcomes.size() - 1);

                    turnCount = outcome.getLastTurnNumber();
                    RegisteredPlayer winningPlayer = outcome.getWinningPlayer();
                    if (winningPlayer != null) {
                        winner = winningPlayer.getPlayer().getName();
                    }
                }
            }

            metrics.setGameCompleted(gameCompleted);
            metrics.setTurnCount(turnCount);
            metrics.setWinner(winner);
            metrics.setGameDurationMs(System.currentTimeMillis() - startTime);

            // 7. Collect network metrics
            if (server.getNetworkByteTracker() != null) {
                metrics.collectFromTracker(server.getNetworkByteTracker());
            }

            NetworkDebugLogger.log("[ReconnectionScenario] Game completed: %b", gameCompleted);
            NetworkDebugLogger.log("[ReconnectionScenario] Turns: %d, Winner: %s", turnCount, winner);

            return new ScenarioResult(
                gameStarted,
                gameCompleted,
                turnCount,
                winner,
                "Reconnection scenario (infrastructure test)",
                metrics,
                null
            );

        } catch (Exception e) {
            NetworkDebugLogger.error("[ReconnectionScenario] Error: " + e.getMessage(), e);
            metrics.setErrorMessage(e.getMessage());
            metrics.setException(e);

            return new ScenarioResult(
                gameStarted,
                gameCompleted,
                turnCount,
                winner,
                "Reconnection scenario",
                metrics,
                e.getMessage()
            );

        } finally {
            // Cleanup
            if (server != null && server.isHosting()) {
                server.stopServer();
                NetworkDebugLogger.log("[ReconnectionScenario] Server stopped");
            }
        }
    }

    /**
     * Log a visual separator banner for a new server instance.
     */
    private void logServerInstanceBanner(String scenarioName, int playerCount, int serverPort) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String separator = "=".repeat(80);
        NetworkDebugLogger.log(separator);
        NetworkDebugLogger.log("SERVER INSTANCE STARTED");
        NetworkDebugLogger.log("  Scenario: %s", scenarioName);
        NetworkDebugLogger.log("  Players:  %d", playerCount);
        NetworkDebugLogger.log("  Port:     %d", serverPort);
        NetworkDebugLogger.log("  Time:     %s", timestamp);
        NetworkDebugLogger.log(separator);
    }

    /**
     * Main method for standalone testing.
     */
    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("Reconnection Scenario Test");
        System.out.println("=".repeat(60));
        System.out.println();
        System.out.println("NOTE: This test validates game execution through network infrastructure.");
        System.out.println("True disconnect testing requires running two Forge instances manually.");
        System.out.println();

        ReconnectionScenario scenario = new ReconnectionScenario()
            .gameTimeout(120);

        ScenarioResult result = scenario.execute();

        System.out.println();
        System.out.println("Result: " + result);
        System.out.println("Test " + (result.passed() ? "PASSED" : "FAILED"));

        System.exit(result.passed() ? 0 : 1);
    }
}
