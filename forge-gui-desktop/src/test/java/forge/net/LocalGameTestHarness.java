package forge.net;

import forge.deck.Deck;
import forge.game.GameOutcome;
import forge.game.Match;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.match.LocalLobby;
import forge.gamemodes.net.NetworkDebugLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Test harness for local (non-network) AI-vs-AI games.
 *
 * This harness provides a non-network equivalent to AutomatedGameTestHarness.
 * It uses LocalLobby instead of ServerGameLobby, running games without any
 * network stack involvement: no FServerManager, no sockets, no serialization,
 * no delta sync.
 *
 * Benefits:
 * - Pure game engine testing without network overhead
 * - Faster execution (no network serialization/deserialization)
 * - Simpler debugging (single process, no network complexity)
 * - Ideal for game rules regression testing and AI behavior validation
 *
 * Part of the unified test configuration system for headless game testing.
 */
public class LocalGameTestHarness {

    private LocalLobby lobby;
    private long gameStartTime;

    /**
     * Run a basic 2-player AI game using LocalLobby (non-network).
     *
     * @return GameTestMetrics with results (no network statistics)
     */
    public GameTestMetrics runLocalTwoPlayerGame() {
        return runLocalTwoPlayerGame(
            TestDeckLoader.getRandomPrecon(),
            TestDeckLoader.getRandomPrecon()
        );
    }

    /**
     * Run a local 2-player AI game with specific decks.
     * No server, no sockets, no delta sync - pure game engine.
     *
     * @param deck1 Deck for player 1
     * @param deck2 Deck for player 2
     * @return GameTestMetrics with results (no network statistics)
     */
    public GameTestMetrics runLocalTwoPlayerGame(Deck deck1, Deck deck2) {
        GameTestMetrics metrics = new GameTestMetrics();
        metrics.setTestMode(GameTestMode.LOCAL);

        try {
            NetworkDebugLogger.log("[LocalGameTestHarness] Starting local 2-player game (non-network)");

            // 1. Create LocalLobby (non-network lobby)
            lobby = new LocalLobby();

            // 2. Configure AI players
            LobbySlot slot0 = lobby.getSlot(0);
            slot0.setType(LobbySlotType.AI);
            slot0.setName("Alice (Local AI)");
            slot0.setDeck(deck1);
            slot0.setIsReady(true);
            NetworkDebugLogger.log("[LocalGameTestHarness] Configured Alice with deck: " +
                (deck1.getName() != null ? deck1.getName() : "unnamed"));

            LobbySlot slot1 = lobby.getSlot(1);
            slot1.setType(LobbySlotType.AI);
            slot1.setName("Bob (Local AI)");
            slot1.setDeck(deck2);
            slot1.setIsReady(true);
            NetworkDebugLogger.log("[LocalGameTestHarness] Configured Bob with deck: " +
                (deck2.getName() != null ? deck2.getName() : "unnamed"));

            // 3. Start game
            NetworkDebugLogger.log("[LocalGameTestHarness] Starting game...");
            gameStartTime = System.currentTimeMillis();

            Runnable start = lobby.startGame();
            if (start == null) {
                throw new IllegalStateException(
                    "startGame() returned null - deck validation likely failed. " +
                    "Check that both decks have valid cards loaded.");
            }

            start.run();

            // 4. Register event listener for game action logging
            registerEventListener();

            // 5. Wait for game completion
            waitForGameCompletion(metrics);

            // 6. Record success
            metrics.setGameCompleted(true);
            metrics.setGameDurationMs(System.currentTimeMillis() - gameStartTime);

            NetworkDebugLogger.log("[LocalGameTestHarness] Game completed successfully");

        } catch (Exception e) {
            metrics.setGameCompleted(false);
            metrics.setErrorMessage(e.getMessage());
            metrics.setException(e);
            NetworkDebugLogger.error("[LocalGameTestHarness] Game failed: " + e.getMessage());
            e.printStackTrace();

        } finally {
            cleanup();
        }

        NetworkDebugLogger.log("[LocalGameTestHarness] " + metrics.toSummary());
        return metrics;
    }

    /**
     * Register a GameEventListener to log game actions.
     * Called after game starts to capture turn-by-turn play.
     */
    private void registerEventListener() {
        HostedMatch hostedMatch = HeadlessGuiDesktop.getLastMatch();
        if (hostedMatch == null) {
            NetworkDebugLogger.log("[LocalGameTestHarness] No hosted match found - event listener not registered");
            return;
        }

        if (hostedMatch.getGame() == null) {
            NetworkDebugLogger.log("[LocalGameTestHarness] Game not started yet - event listener not registered");
            return;
        }

        // Enable verbose logging for local mode since NetworkGameEventListener only registers for network games
        GameEventListener listener = new GameEventListener().setVerboseLogging(true);
        hostedMatch.getGame().subscribeToEvents(listener);
        NetworkDebugLogger.log("[LocalGameTestHarness] Event listener registered");
    }

    /**
     * Wait for game completion and extract results.
     * Games run asynchronously in a thread pool, so we need to poll for completion.
     */
    private void waitForGameCompletion(GameTestMetrics metrics) {
        NetworkDebugLogger.log("[LocalGameTestHarness] Waiting for game completion...");

        // Games run asynchronously - poll for completion
        HostedMatch hostedMatch = HeadlessGuiDesktop.getLastMatch();
        if (hostedMatch == null) {
            NetworkDebugLogger.error("[LocalGameTestHarness] No hosted match found");
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
                    NetworkDebugLogger.log("[LocalGameTestHarness] Game completed after " + waitedMs + "ms");
                    break;
                }
            }

            try {
                Thread.sleep(pollIntervalMs);
                waitedMs += pollIntervalMs;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                NetworkDebugLogger.error("[LocalGameTestHarness] Wait interrupted");
                break;
            }

            // Log progress every 5 seconds
            if (waitedMs % 5000 == 0) {
                NetworkDebugLogger.log("[LocalGameTestHarness] Still waiting... " + (waitedMs / 1000) + "s");
            }
        }

        if (waitedMs >= maxWaitSeconds * 1000) {
            NetworkDebugLogger.error("[LocalGameTestHarness] Game did not complete within " + maxWaitSeconds + " seconds");
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
            NetworkDebugLogger.error("[LocalGameTestHarness] No hosted match found - cannot extract results");
            return;
        }

        Match match = hostedMatch.getMatch();
        if (match == null) {
            NetworkDebugLogger.error("[LocalGameTestHarness] No match found in hosted match");
            return;
        }

        Collection<GameOutcome> outcomesCollection = match.getOutcomes();
        if (outcomesCollection == null || outcomesCollection.isEmpty()) {
            NetworkDebugLogger.error("[LocalGameTestHarness] No game outcomes found");
            return;
        }

        // Get the most recent game outcome
        List<GameOutcome> outcomes = new ArrayList<>(outcomesCollection);
        GameOutcome outcome = outcomes.get(outcomes.size() - 1);

        // Extract turn count
        int turnCount = outcome.getLastTurnNumber();
        metrics.setTurnCount(turnCount);
        NetworkDebugLogger.log("[LocalGameTestHarness] Game lasted " + turnCount + " turns");

        // Extract winner
        RegisteredPlayer winner = outcome.getWinningPlayer();
        if (winner != null) {
            String winnerName = winner.getPlayer().getName();
            metrics.setWinner(winnerName);
            NetworkDebugLogger.log("[LocalGameTestHarness] Winner: " + winnerName);
        } else {
            NetworkDebugLogger.log("[LocalGameTestHarness] No winner (draw or incomplete)");
        }

        // Log win condition
        if (outcome.getWinCondition() != null) {
            NetworkDebugLogger.log("[LocalGameTestHarness] Win condition: " + outcome.getWinCondition());
        }
    }

    /**
     * Clean up resources.
     */
    private void cleanup() {
        lobby = null;
    }

    /**
     * Get the lobby instance (for advanced testing scenarios).
     */
    public LocalLobby getLobby() {
        return lobby;
    }
}
