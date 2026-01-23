package forge.net;

import forge.deck.Deck;
import forge.gamemodes.net.NetworkDebugLogger;

/**
 * Factory for unified test execution across different game test modes.
 *
 * Provides a single entry point for running tests in LOCAL, NETWORK_LOCAL,
 * or NETWORK_REMOTE modes. Automatically selects the appropriate harness
 * based on the specified mode.
 *
 * Benefits:
 * - Unified interface for all test modes
 * - Easy comparative testing (local vs network performance)
 * - Simplified test configuration
 * - Mode-specific optimizations handled automatically
 *
 * Part of the unified test configuration system for headless game testing.
 *
 * Example usage:
 * <pre>
 * // Run a local test (no network overhead)
 * GameTestMetrics local = GameTestHarnessFactory.runTest(
 *     GameTestMode.LOCAL,
 *     TestDeckLoader.getRandomPrecon(),
 *     TestDeckLoader.getRandomPrecon()
 * );
 *
 * // Run a network test with local players
 * GameTestMetrics network = GameTestHarnessFactory.runTest(
 *     GameTestMode.NETWORK_LOCAL,
 *     TestDeckLoader.getRandomPrecon(),
 *     TestDeckLoader.getRandomPrecon()
 * );
 *
 * // Compare performance
 * long overhead = network.getGameDurationMs() - local.getGameDurationMs();
 * </pre>
 */
public class GameTestHarnessFactory {

    /**
     * Run a 2-player AI test in the specified mode with random precon decks.
     *
     * @param mode the test mode (LOCAL, NETWORK_LOCAL, or NETWORK_REMOTE)
     * @return GameTestMetrics with results
     */
    public static GameTestMetrics runTest(GameTestMode mode) {
        return runTest(mode, TestDeckLoader.getRandomPrecon(), TestDeckLoader.getRandomPrecon());
    }

    /**
     * Run a 2-player AI test in the specified mode with specific decks.
     *
     * Note: NETWORK_REMOTE mode ignores deck parameters and uses decks
     * configured by NetworkClientTestHarness.
     *
     * @param mode the test mode (LOCAL, NETWORK_LOCAL, or NETWORK_REMOTE)
     * @param deck1 deck for player 1 (ignored for NETWORK_REMOTE)
     * @param deck2 deck for player 2 (ignored for NETWORK_REMOTE)
     * @return GameTestMetrics with results
     * @throws IllegalArgumentException if mode is null
     */
    public static GameTestMetrics runTest(GameTestMode mode, Deck deck1, Deck deck2) {
        if (mode == null) {
            throw new IllegalArgumentException("Test mode cannot be null");
        }

        NetworkDebugLogger.log(String.format("[GameTestHarnessFactory] Running test in %s mode", mode.name()));

        GameTestMetrics metrics;

        switch (mode) {
            case LOCAL:
                // Use LocalGameTestHarness for non-network testing
                LocalGameTestHarness localHarness = new LocalGameTestHarness();
                metrics = localHarness.runLocalTwoPlayerGame(deck1, deck2);
                break;

            case NETWORK_LOCAL:
                // Use AutomatedGameTestHarness for network testing with local players
                AutomatedGameTestHarness networkHarness = new AutomatedGameTestHarness();
                metrics = networkHarness.runBasicTwoPlayerGame(deck1, deck2);
                // Set mode if not already set
                if (metrics.getTestMode() == null) {
                    metrics.setTestMode(GameTestMode.NETWORK_LOCAL);
                }
                break;

            case NETWORK_REMOTE:
                // Use NetworkClientTestHarness for true remote client testing
                NetworkClientTestHarness remoteHarness = new NetworkClientTestHarness();
                NetworkClientTestHarness.TestResult result = remoteHarness.runTwoPlayerNetworkTest();

                // Convert TestResult to GameTestMetrics
                metrics = convertToMetrics(result);
                metrics.setTestMode(GameTestMode.NETWORK_REMOTE);
                break;

            default:
                throw new IllegalArgumentException("Unknown test mode: " + mode);
        }

        return metrics;
    }

    /**
     * Convert NetworkClientTestHarness.TestResult to GameTestMetrics.
     *
     * @param result the test result from NetworkClientTestHarness
     * @return GameTestMetrics populated from result
     */
    private static GameTestMetrics convertToMetrics(NetworkClientTestHarness.TestResult result) {
        GameTestMetrics metrics = new GameTestMetrics();

        metrics.setGameCompleted(result.gameCompleted);
        metrics.setTurnCount(result.turns);
        metrics.setWinner(result.winner);

        // Network metrics from remote client
        metrics.setDeltaPacketCount(result.deltaPacketsReceived);
        metrics.setFullStatePacketCount(result.fullStateSyncsReceived);
        metrics.setTotalBytesSent(result.totalDeltaBytes);

        if (!result.success) {
            metrics.setErrorMessage(result.errorMessage);
        }

        return metrics;
    }

    /**
     * Run comparative tests across all modes with the same deck configurations.
     *
     * Useful for performance comparison and validation that all modes
     * produce consistent results.
     *
     * @param deck1 deck for player 1
     * @param deck2 deck for player 2
     * @return array of GameTestMetrics, one for each mode
     */
    public static GameTestMetrics[] runComparativeTests(Deck deck1, Deck deck2) {
        NetworkDebugLogger.log("[GameTestHarnessFactory] Running comparative tests across all modes");

        GameTestMetrics[] results = new GameTestMetrics[GameTestMode.values().length];

        for (int i = 0; i < GameTestMode.values().length; i++) {
            GameTestMode mode = GameTestMode.values()[i];
            NetworkDebugLogger.log(String.format("[GameTestHarnessFactory] Testing mode: %s", mode.name()));
            results[i] = runTest(mode, deck1, deck2);
        }

        return results;
    }

    /**
     * Run batch tests in the specified mode.
     *
     * @param mode the test mode
     * @param iterations number of iterations to run
     * @return array of GameTestMetrics, one for each iteration
     */
    public static GameTestMetrics[] runBatchTests(GameTestMode mode, int iterations) {
        if (iterations <= 0) {
            throw new IllegalArgumentException("Iterations must be positive");
        }

        NetworkDebugLogger.log(String.format(
            "[GameTestHarnessFactory] Running batch test: %d iterations in %s mode",
            iterations, mode.name()
        ));

        GameTestMetrics[] results = new GameTestMetrics[iterations];

        for (int i = 0; i < iterations; i++) {
            NetworkDebugLogger.log(String.format(
                "[GameTestHarnessFactory] Batch iteration %d/%d", i + 1, iterations
            ));
            results[i] = runTest(mode);
        }

        return results;
    }

    /**
     * Calculate aggregate statistics from multiple test runs.
     *
     * @param results array of test results
     * @return string summary of aggregated statistics
     */
    public static String aggregateStats(GameTestMetrics[] results) {
        if (results == null || results.length == 0) {
            return "No results to aggregate";
        }

        int completed = 0;
        int failed = 0;
        long totalTurns = 0;
        long totalDuration = 0;
        long totalBytes = 0;

        for (GameTestMetrics result : results) {
            if (result.isGameCompleted()) {
                completed++;
                totalTurns += result.getTurnCount();
                totalDuration += result.getGameDurationMs();
                totalBytes += result.getTotalBytesSent();
            } else {
                failed++;
            }
        }

        double avgTurns = completed > 0 ? (double) totalTurns / completed : 0;
        double avgDuration = completed > 0 ? (double) totalDuration / completed : 0;
        double avgBytes = completed > 0 ? (double) totalBytes / completed : 0;

        return String.format(
            "Aggregate Stats: %d completed, %d failed | Avg: %.1f turns, %.0f ms, %.0f bytes",
            completed, failed, avgTurns, avgDuration, avgBytes
        );
    }
}
