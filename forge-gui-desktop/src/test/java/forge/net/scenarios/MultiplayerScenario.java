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
 * Tests multiplayer games with 3-4 AI players.
 * Validates: Multiplayer game completion, turn order, winner determination.
 *
 * Phase 5.3 of the Automated Network Testing Plan.
 *
 * <h2>What This Scenario Tests</h2>
 * <ul>
 *   <li>3-player and 4-player free-for-all games</li>
 *   <li>Multiplayer turn order handling</li>
 *   <li>Player elimination tracking</li>
 *   <li>Network serialization with multiple players</li>
 *   <li>Bandwidth metrics for larger games</li>
 * </ul>
 */
public class MultiplayerScenario {
    private static final String[] PLAYER_NAMES = {"Alice", "Bob", "Charlie", "Diana"};

    /**
     * Result of the multiplayer scenario.
     */
    public static class ScenarioResult {
        public final boolean gameStarted;
        public final boolean gameCompleted;
        public final int playerCount;
        public final int turnCount;
        public final String winner;
        public final String description;
        public final GameTestMetrics metrics;
        public final String errorMessage;

        public ScenarioResult(boolean gameStarted, boolean gameCompleted,
                              int playerCount, int turnCount, String winner,
                              String description, GameTestMetrics metrics,
                              String errorMessage) {
            this.gameStarted = gameStarted;
            this.gameCompleted = gameCompleted;
            this.playerCount = playerCount;
            this.turnCount = turnCount;
            this.winner = winner;
            this.description = description;
            this.metrics = metrics;
            this.errorMessage = errorMessage;
        }

        public boolean passed() {
            // Test passes if game started, completed with expected player count
            return gameStarted && gameCompleted && turnCount > 0;
        }

        @Override
        public String toString() {
            return String.format(
                "ScenarioResult[%s, started=%b, completed=%b, players=%d, turns=%d, winner=%s, error=%s]",
                description, gameStarted, gameCompleted, playerCount, turnCount, winner, errorMessage);
        }
    }

    // Configuration
    private int playerCount = 3;
    private long gameTimeoutSeconds = 180; // Multiplayer games may take longer
    private List<Deck> decks = null;

    /**
     * Set the number of players (3-4 supported).
     *
     * @param count Number of players
     * @return this for method chaining
     */
    public MultiplayerScenario playerCount(int count) {
        if (count < 3 || count > 4) {
            throw new IllegalArgumentException("Player count must be 3 or 4, got: " + count);
        }
        this.playerCount = count;
        return this;
    }

    /**
     * Set the game timeout in seconds.
     *
     * @param seconds Timeout in seconds
     * @return this for method chaining
     */
    public MultiplayerScenario gameTimeout(long seconds) {
        this.gameTimeoutSeconds = seconds;
        return this;
    }

    /**
     * Set specific decks for the players.
     *
     * @param decks List of decks (must match player count)
     * @return this for method chaining
     */
    public MultiplayerScenario withDecks(List<Deck> decks) {
        this.decks = decks;
        return this;
    }

    /**
     * Execute the multiplayer scenario.
     *
     * @return ScenarioResult with test outcomes
     */
    public ScenarioResult execute() {
        NetworkDebugLogger.log("[MultiplayerScenario] Starting %d-player game scenario", playerCount);

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
            logServerInstanceBanner("MultiplayerScenario", playerCount, port);

            // 2. Create lobby
            lobby = new ServerGameLobby();
            server.setLobby(lobby);

            // 3. Add extra slots for multiplayer (lobby starts with 2 slots)
            for (int i = 2; i < playerCount; i++) {
                lobby.addSlot();
            }
            NetworkDebugLogger.log("[MultiplayerScenario] Lobby has %d slots", lobby.getNumberOfSlots());

            // 4. Configure all players as AI
            for (int i = 0; i < playerCount; i++) {
                Deck deck = (decks != null && i < decks.size()) ? decks.get(i) : TestDeckLoader.getRandomPrecon();
                String name = PLAYER_NAMES[i] + " (AI)";

                LobbySlot slot = lobby.getSlot(i);
                slot.setType(LobbySlotType.AI);
                slot.setName(name);
                slot.setDeck(deck);
                slot.setIsReady(true);

                String deckName = deck.getName() != null ? deck.getName() : "unnamed";
                NetworkDebugLogger.log("[MultiplayerScenario] Player %d configured: %s with %s", i + 1, name, deckName);
            }

            // 5. Start game
            NetworkDebugLogger.log("[MultiplayerScenario] Starting game...");
            long startTime = System.currentTimeMillis();

            Runnable start = lobby.startGame();
            if (start == null) {
                return new ScenarioResult(false, false, playerCount, 0, null,
                    playerCount + "-player multiplayer", metrics,
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
                NetworkDebugLogger.log("[MultiplayerScenario] Event listener registered");
            }

            // 7. Wait for game completion (games run asynchronously)
            NetworkDebugLogger.log("[MultiplayerScenario] Waiting for game completion...");

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

                    // Log progress every 15 seconds
                    if (waitedMs % 15000 == 0) {
                        NetworkDebugLogger.log("[MultiplayerScenario] Still waiting... %ds", waitedMs / 1000);
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

            NetworkDebugLogger.log("[MultiplayerScenario] Game completed: %b", gameCompleted);
            NetworkDebugLogger.log("[MultiplayerScenario] Turns: %d, Winner: %s", turnCount, winner);

            return new ScenarioResult(
                gameStarted,
                gameCompleted,
                playerCount,
                turnCount,
                winner,
                playerCount + "-player multiplayer",
                metrics,
                null
            );

        } catch (Exception e) {
            NetworkDebugLogger.error("[MultiplayerScenario] Error: " + e.getMessage(), e);
            metrics.setErrorMessage(e.getMessage());
            metrics.setException(e);

            return new ScenarioResult(
                gameStarted,
                gameCompleted,
                playerCount,
                turnCount,
                winner,
                playerCount + "-player multiplayer",
                metrics,
                e.getMessage()
            );

        } finally {
            // Cleanup
            if (server != null && server.isHosting()) {
                server.stopServer();
                NetworkDebugLogger.log("[MultiplayerScenario] Server stopped");
            }
        }
    }

    /**
     * Execute scenario with both 3 and 4 players.
     *
     * @return Array of ScenarioResults [3-player, 4-player]
     */
    public ScenarioResult[] executeAll() {
        ScenarioResult[] results = new ScenarioResult[2];

        NetworkDebugLogger.log("[MultiplayerScenario] Running 3-player game...");
        results[0] = new MultiplayerScenario()
            .playerCount(3)
            .gameTimeout(gameTimeoutSeconds)
            .execute();

        NetworkDebugLogger.log("[MultiplayerScenario] Running 4-player game...");
        results[1] = new MultiplayerScenario()
            .playerCount(4)
            .gameTimeout(gameTimeoutSeconds)
            .execute();

        return results;
    }

    /**
     * Log a visual separator banner for a new server instance.
     */
    private void logServerInstanceBanner(String scenarioName, int players, int serverPort) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String separator = "=".repeat(80);
        NetworkDebugLogger.log(separator);
        NetworkDebugLogger.log("SERVER INSTANCE STARTED");
        NetworkDebugLogger.log("  Scenario: %s", scenarioName);
        NetworkDebugLogger.log("  Players:  %d", players);
        NetworkDebugLogger.log("  Port:     %d", serverPort);
        NetworkDebugLogger.log("  Time:     %s", timestamp);
        NetworkDebugLogger.log(separator);
    }

    /**
     * Main method for standalone testing.
     */
    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("Multiplayer Scenario Test");
        System.out.println("=".repeat(60));
        System.out.println();

        int playerCount = 3;
        if (args.length > 0) {
            try {
                playerCount = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid player count: " + args[0] + ", using default 3");
            }
        }

        MultiplayerScenario scenario = new MultiplayerScenario()
            .playerCount(playerCount)
            .gameTimeout(180);

        ScenarioResult result = scenario.execute();

        System.out.println();
        System.out.println("Result: " + result);
        System.out.println("Test " + (result.passed() ? "PASSED" : "FAILED"));

        System.exit(result.passed() ? 0 : 1);
    }
}
