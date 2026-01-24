package forge.net;

import forge.deck.Deck;
import forge.game.GameOutcome;
import forge.game.Match;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.NetworkByteTracker;
import forge.gamemodes.net.NetworkDebugLogger;
import forge.gamemodes.net.server.FServerManager;
import forge.gamemodes.net.server.ServerGameLobby;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Core network test orchestration using validated Phase 0 patterns.
 * Runs AI-controlled games over the real network infrastructure to test
 * network serialization, delta sync, and bandwidth optimization.
 *
 * Phase 2 of the Automated Network Testing Plan.
 */
public class AutomatedGameTestHarness {

    private FServerManager server;
    private ServerGameLobby lobby;
    private int port;
    private long gameStartTime;

    /**
     * Run a basic 2-player AI game over network infrastructure.
     *
     * @return GameTestMetrics with results and network statistics
     */
    public GameTestMetrics runBasicTwoPlayerGame() {
        return runBasicTwoPlayerGame(
            TestDeckLoader.getRandomPrecon(),
            TestDeckLoader.getRandomPrecon()
        );
    }

    /**
     * Run a basic 2-player AI game with specific decks.
     *
     * @param deck1 Deck for player 1
     * @param deck2 Deck for player 2
     * @return GameTestMetrics with results and network statistics
     */
    public GameTestMetrics runBasicTwoPlayerGame(Deck deck1, Deck deck2) {
        GameTestMetrics metrics = new GameTestMetrics();
        metrics.setTestMode(GameTestMode.NETWORK_LOCAL);

        try {
            // 1. Start server
            port = PortAllocator.allocatePort();
            server = FServerManager.getInstance();
            server.startServer(port);
            logServerInstanceBanner("BasicTwoPlayerGame", 2, port);

            // 2. Create lobby and set on server (no getter available - must keep reference)
            lobby = new ServerGameLobby();
            server.setLobby(lobby);

            // 3. Configure slot 0 as AI (replaces default LOCAL slot)
            LobbySlot alice = lobby.getSlot(0);
            alice.setType(LobbySlotType.AI);
            alice.setName("Alice (AI)");
            alice.setDeck(deck1);
            alice.setIsReady(true);
            NetworkDebugLogger.log("[AutomatedGameTestHarness] Configured Alice with deck: " +
                (deck1.getName() != null ? deck1.getName() : "unnamed"));

            // 4. Configure slot 1 as AI (replaces default OPEN slot)
            LobbySlot bob = lobby.getSlot(1);
            bob.setType(LobbySlotType.AI);
            bob.setName("Bob (AI)");
            bob.setDeck(deck2);
            bob.setIsReady(true);
            NetworkDebugLogger.log("[AutomatedGameTestHarness] Configured Bob with deck: " +
                (deck2.getName() != null ? deck2.getName() : "unnamed"));

            // 5. Start game (returns Runnable, must check null)
            NetworkDebugLogger.log("[AutomatedGameTestHarness] Starting game...");
            gameStartTime = System.currentTimeMillis();

            Runnable start = lobby.startGame();
            if (start == null) {
                throw new IllegalStateException(
                    "startGame() returned null - deck validation likely failed. " +
                    "Check that both decks have valid cards loaded.");
            }

            // Note: onGameStarted() in ServerGameLobby calls createGameSession()
            // so we don't need to call it manually here
            start.run();

            // 6. Register event listener for game action logging
            registerEventListener();

            // 7. Game runs asynchronously with AI - wait for completion
            waitForGameCompletion(metrics);

            // 7. Record success
            metrics.setGameCompleted(true);
            metrics.setGameDurationMs(System.currentTimeMillis() - gameStartTime);

            NetworkDebugLogger.log("[AutomatedGameTestHarness] Game completed successfully");

        } catch (Exception e) {
            metrics.setGameCompleted(false);
            metrics.setErrorMessage(e.getMessage());
            metrics.setException(e);
            NetworkDebugLogger.error("[AutomatedGameTestHarness] Game failed: " + e.getMessage());
            e.printStackTrace();

        } finally {
            collectNetworkMetrics(metrics);
            cleanup();
        }

        NetworkDebugLogger.log("[AutomatedGameTestHarness] " + metrics.toSummary());
        return metrics;
    }

    /**
     * Register a GameEventListener to log game actions.
     * Called after game starts to capture turn-by-turn play.
     */
    private void registerEventListener() {
        HostedMatch hostedMatch = HeadlessGuiDesktop.getLastMatch();
        if (hostedMatch == null) {
            NetworkDebugLogger.log("[AutomatedGameTestHarness] No hosted match found - event listener not registered");
            return;
        }

        if (hostedMatch.getGame() == null) {
            NetworkDebugLogger.log("[AutomatedGameTestHarness] Game not started yet - event listener not registered");
            return;
        }

        // Disable verbose logging since NetworkGameEventListener in production code now handles logging
        GameEventListener listener = new GameEventListener().setVerboseLogging(false);
        hostedMatch.getGame().subscribeToEvents(listener);
        NetworkDebugLogger.log("[AutomatedGameTestHarness] Event listener registered (verbose logging disabled - using production NetworkGameEventListener)");
    }

    /**
     * Wait for game completion and extract results.
     * Games run asynchronously in a thread pool, so we need to poll for completion.
     */
    private void waitForGameCompletion(GameTestMetrics metrics) {
        NetworkDebugLogger.log("[AutomatedGameTestHarness] Waiting for game completion...");

        // Games run asynchronously - poll for completion
        HostedMatch hostedMatch = HeadlessGuiDesktop.getLastMatch();
        if (hostedMatch == null) {
            NetworkDebugLogger.error("[AutomatedGameTestHarness] No hosted match found");
            return;
        }

        // Wait for the match to have outcomes (game finished)
        int maxWaitSeconds = 120;
        int waitedMs = 0;
        int pollIntervalMs = 100;

        while (waitedMs < maxWaitSeconds * 1000) {
            Match match = hostedMatch.getMatch();
            if (match != null) {
                Collection<GameOutcome> outcomes = match.getOutcomes();
                if (outcomes != null && !outcomes.isEmpty()) {
                    NetworkDebugLogger.log("[AutomatedGameTestHarness] Game completed after " + waitedMs + "ms");
                    break;
                }
            }

            try {
                Thread.sleep(pollIntervalMs);
                waitedMs += pollIntervalMs;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                NetworkDebugLogger.error("[AutomatedGameTestHarness] Wait interrupted");
                break;
            }

            // Log progress every 5 seconds
            if (waitedMs % 5000 == 0) {
                NetworkDebugLogger.log("[AutomatedGameTestHarness] Still waiting... " + (waitedMs / 1000) + "s");
            }
        }

        if (waitedMs >= maxWaitSeconds * 1000) {
            NetworkDebugLogger.error("[AutomatedGameTestHarness] Game did not complete within " + maxWaitSeconds + " seconds");
        }

        // Extract results from the HostedMatch
        extractGameResults(metrics);
    }

    /**
     * Extract game results from the HostedMatch stored in HeadlessGuiDesktop.
     */
    private void extractGameResults(GameTestMetrics metrics) {
        HostedMatch hostedMatch = HeadlessGuiDesktop.getLastMatch();
        if (hostedMatch == null) {
            NetworkDebugLogger.error("[AutomatedGameTestHarness] No hosted match found - cannot extract results");
            return;
        }

        Match match = hostedMatch.getMatch();
        if (match == null) {
            NetworkDebugLogger.error("[AutomatedGameTestHarness] No match found in hosted match");
            return;
        }

        Collection<GameOutcome> outcomesCollection = match.getOutcomes();
        if (outcomesCollection == null || outcomesCollection.isEmpty()) {
            NetworkDebugLogger.error("[AutomatedGameTestHarness] No game outcomes found");
            return;
        }

        // Get the most recent game outcome
        List<GameOutcome> outcomes = new ArrayList<>(outcomesCollection);
        GameOutcome outcome = outcomes.get(outcomes.size() - 1);

        // Extract turn count
        int turnCount = outcome.getLastTurnNumber();
        metrics.setTurnCount(turnCount);
        NetworkDebugLogger.log("[AutomatedGameTestHarness] Game lasted " + turnCount + " turns");

        // Extract winner
        RegisteredPlayer winner = outcome.getWinningPlayer();
        if (winner != null) {
            String winnerName = winner.getPlayer().getName();
            metrics.setWinner(winnerName);
            NetworkDebugLogger.log("[AutomatedGameTestHarness] Winner: " + winnerName);
        } else {
            NetworkDebugLogger.log("[AutomatedGameTestHarness] No winner (draw or incomplete)");
        }

        // Log win condition
        if (outcome.getWinCondition() != null) {
            NetworkDebugLogger.log("[AutomatedGameTestHarness] Win condition: " + outcome.getWinCondition());
        }
    }

    /**
     * Collect network metrics from the server's NetworkByteTracker.
     */
    private void collectNetworkMetrics(GameTestMetrics metrics) {
        if (server == null) {
            return;
        }

        NetworkByteTracker tracker = server.getNetworkByteTracker();
        if (tracker != null) {
            metrics.collectFromTracker(tracker);
            NetworkDebugLogger.log("[AutomatedGameTestHarness] Network stats: " + tracker.getStatsSummary());
        } else {
            NetworkDebugLogger.log("[AutomatedGameTestHarness] NetworkByteTracker not available " +
                "(bandwidth logging may be disabled)");
        }
    }

    /**
     * Clean up server resources.
     */
    private void cleanup() {
        if (server != null && server.isHosting()) {
            server.stopServer();
            NetworkDebugLogger.log("[AutomatedGameTestHarness] Server stopped");
        }
        lobby = null;
    }

    /**
     * Get the port used by this harness instance.
     */
    public int getPort() {
        return port;
    }

    /**
     * Get the server instance (for advanced testing scenarios).
     */
    public FServerManager getServer() {
        return server;
    }

    /**
     * Get the lobby instance (for advanced testing scenarios).
     */
    public ServerGameLobby getLobby() {
        return lobby;
    }

    /**
     * Log a visual separator banner for a new server instance.
     * Helps distinguish between different games in the same log file.
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
}
