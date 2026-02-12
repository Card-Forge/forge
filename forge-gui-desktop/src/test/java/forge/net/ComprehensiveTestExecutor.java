package forge.net;

import forge.gamemodes.net.NetworkDebugLogger;
import forge.gui.GuiBase;
import forge.model.FModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Orchestrates comprehensive delta sync testing with multiple player counts.
 *
 * Default configuration runs 100 games:
 * - 50 x 2-player games (50%)
 * - 30 x 3-player games (30%)
 * - 20 x 4-player games (20%)
 *
 * Supports both execution modes:
 * - Parallel: Games run in separate JVM processes (default)
 * - Sequential: Games run one-by-one in the same JVM (useful for debugging)
 */
public class ComprehensiveTestExecutor {

    private static final String LOG_PREFIX = "[ComprehensiveTestExecutor]";
    private static final int BASE_PORT = 58000;

    // Default game distribution
    private int twoPlayerGames = 50;
    private int threePlayerGames = 30;
    private int fourPlayerGames = 20;

    // Execution settings
    private int parallelBatchSize = 10; // Run 10 games at a time
    private long gameTimeoutMs = 300000; // 5 minutes per game
    private boolean sequential = false; // Run sequentially instead of parallel

    /**
     * Set the number of 2-player games.
     */
    public ComprehensiveTestExecutor twoPlayerGames(int count) {
        this.twoPlayerGames = count;
        return this;
    }

    /**
     * Set the number of 3-player games.
     */
    public ComprehensiveTestExecutor threePlayerGames(int count) {
        this.threePlayerGames = count;
        return this;
    }

    /**
     * Set the number of 4-player games.
     */
    public ComprehensiveTestExecutor fourPlayerGames(int count) {
        this.fourPlayerGames = count;
        return this;
    }

    /**
     * Set the parallel batch size.
     */
    public ComprehensiveTestExecutor parallelBatchSize(int size) {
        this.parallelBatchSize = size;
        return this;
    }

    /**
     * Set the game timeout in milliseconds.
     */
    public ComprehensiveTestExecutor gameTimeout(long timeoutMs) {
        this.gameTimeoutMs = timeoutMs;
        return this;
    }

    /**
     * Enable sequential execution (games run one-by-one in same JVM).
     * Default is parallel execution (separate JVM per game).
     */
    public ComprehensiveTestExecutor sequential(boolean seq) {
        this.sequential = seq;
        return this;
    }

    /**
     * Get total number of games to run.
     */
    public int getTotalGames() {
        return twoPlayerGames + threePlayerGames + fourPlayerGames;
    }

    /**
     * Execute the comprehensive test suite.
     *
     * @return Execution result with aggregated metrics
     */
    public MultiProcessGameExecutor.ExecutionResult execute() {
        int totalGames = getTotalGames();
        String mode = sequential ? "sequential" : "parallel";
        NetworkDebugLogger.log("%s Starting comprehensive test: %d total games (%s)", LOG_PREFIX, totalGames, mode);
        NetworkDebugLogger.log("%s Distribution: %d x 2-player, %d x 3-player, %d x 4-player",
                LOG_PREFIX, twoPlayerGames, threePlayerGames, fourPlayerGames);
        NetworkDebugLogger.log("%s Batch size: %d, Timeout: %dms", LOG_PREFIX, parallelBatchSize, gameTimeoutMs);

        long startTime = System.currentTimeMillis();

        // Build player count array with shuffled distribution
        int[] playerCounts = buildShuffledPlayerCounts();

        MultiProcessGameExecutor.ExecutionResult result;

        if (sequential) {
            // Run games sequentially in same JVM
            result = executeSequentially(playerCounts);
        } else {
            // Create executor with ComprehensiveGameRunner for parallel execution
            MultiProcessGameExecutor executor = new MultiProcessGameExecutor(gameTimeoutMs)
                    .withRunnerClass("forge.net.ComprehensiveGameRunner");

            // Run all games in batches
            result = executor.runGamesInBatches(playerCounts, parallelBatchSize);
        }

        long duration = System.currentTimeMillis() - startTime;
        NetworkDebugLogger.log("%s Comprehensive test completed in %d ms (%.1f minutes)",
                LOG_PREFIX, duration, duration / 60000.0);

        return result;
    }

    /**
     * Execute games sequentially in the same JVM.
     * Useful for debugging as all games run in same process.
     */
    private MultiProcessGameExecutor.ExecutionResult executeSequentially(int[] playerCounts) {
        ensureFModelInitialized();

        NetworkDebugLogger.log("%s Starting %d sequential games", LOG_PREFIX, playerCounts.length);

        MultiProcessGameExecutor.ExecutionResult result = new MultiProcessGameExecutor.ExecutionResult(playerCounts.length);
        int port = BASE_PORT;

        for (int i = 0; i < playerCounts.length; i++) {
            int players = playerCounts[i];
            UnifiedNetworkHarness.GameResult gameResult = runSingleGame(i, port++, players);

            if (gameResult.errorMessage != null && !gameResult.success) {
                result.addError(i, gameResult.errorMessage);
            } else {
                // Convert to ExecutionResult format using constructor
                MultiProcessGameExecutor.GameResult execResult = new MultiProcessGameExecutor.GameResult(
                        gameResult.success,
                        gameResult.playerCount,
                        gameResult.deltaPacketsReceived,
                        gameResult.turnCount,
                        gameResult.totalDeltaBytes,
                        gameResult.winner,
                        gameResult.deckNames
                );
                result.addResult(i, execResult);
            }

            String status = gameResult.success ? "SUCCESS" : "FAILED";
            NetworkDebugLogger.log("%s Game %d (%dp): %s (deltas=%d, turns=%d, winner=%s)",
                    LOG_PREFIX, i, players, status,
                    gameResult.deltaPacketsReceived,
                    gameResult.turnCount,
                    gameResult.winner);
        }

        NetworkDebugLogger.log("%s Sequential execution complete: %s", LOG_PREFIX, result.toSummary());
        return result;
    }

    /**
     * Run a single game with isolated logging using UnifiedNetworkHarness.
     */
    private UnifiedNetworkHarness.GameResult runSingleGame(int gameIndex, int port, int playerCount) {
        // Set instance-specific log suffix so this game writes to its own log file
        NetworkDebugLogger.setInstanceSuffix("game" + gameIndex + "-" + playerCount + "p");

        try {
            NetworkDebugLogger.log("%s Starting game %d (%d players) on port %d", LOG_PREFIX, gameIndex, playerCount, port);

            // Use UnifiedNetworkHarness for all player counts
            return new UnifiedNetworkHarness()
                    .playerCount(playerCount)
                    .remoteClients(playerCount - 1)  // All but host are remote
                    .port(port)
                    .gameTimeout(gameTimeoutMs)
                    .execute();
        } finally {
            // Close this game's log file
            NetworkDebugLogger.closeThreadLogger();
        }
    }

    /**
     * Ensure FModel is initialized before running games.
     */
    private static synchronized void ensureFModelInitialized() {
        if (GuiBase.getInterface() == null) {
            NetworkDebugLogger.log("%s Initializing FModel with HeadlessGuiDesktop", LOG_PREFIX);
            GuiBase.setInterface(new HeadlessGuiDesktop());
            FModel.initialize(null, preferences -> null);
        }
    }

    /**
     * Build a shuffled array of player counts based on configuration.
     * Shuffling distributes different game types throughout the test run.
     */
    private int[] buildShuffledPlayerCounts() {
        List<Integer> counts = new ArrayList<>();

        // Add 2-player games
        for (int i = 0; i < twoPlayerGames; i++) {
            counts.add(2);
        }

        // Add 3-player games
        for (int i = 0; i < threePlayerGames; i++) {
            counts.add(3);
        }

        // Add 4-player games
        for (int i = 0; i < fourPlayerGames; i++) {
            counts.add(4);
        }

        // Shuffle to distribute game types throughout the run
        Collections.shuffle(counts);

        // Convert to array
        return counts.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Generate a summary report of the test configuration.
     */
    public String getConfigurationSummary() {
        int total = getTotalGames();
        return String.format(
                "ComprehensiveTestExecutor Configuration:\n" +
                "  Total Games: %d\n" +
                "  Distribution:\n" +
                "    - 2-player: %d (%.0f%%)\n" +
                "    - 3-player: %d (%.0f%%)\n" +
                "    - 4-player: %d (%.0f%%)\n" +
                "  Execution Mode: %s\n" +
                "  Batch Size: %d\n" +
                "  Game Timeout: %d ms",
                total,
                twoPlayerGames, total > 0 ? (100.0 * twoPlayerGames / total) : 0,
                threePlayerGames, total > 0 ? (100.0 * threePlayerGames / total) : 0,
                fourPlayerGames, total > 0 ? (100.0 * fourPlayerGames / total) : 0,
                sequential ? "Sequential (same JVM)" : "Parallel (multi-process)",
                parallelBatchSize,
                gameTimeoutMs
        );
    }

    /**
     * Run N sequential 2-player games.
     * Convenience method replacing SequentialGameExecutor.runGames().
     *
     * @param gameCount Number of 2-player games to run
     * @return Execution result
     */
    public static MultiProcessGameExecutor.ExecutionResult runSequentialGames(int gameCount, long timeoutMs) {
        return new ComprehensiveTestExecutor()
                .twoPlayerGames(gameCount)
                .threePlayerGames(0)
                .fourPlayerGames(0)
                .gameTimeout(timeoutMs)
                .sequential(true)
                .execute();
    }

    /**
     * Create an executor from system properties.
     * Properties:
     *   -Dtest.2pGames=50
     *   -Dtest.3pGames=30
     *   -Dtest.4pGames=20
     *   -Dtest.batchSize=10
     *   -Dtest.timeoutMs=300000
     */
    public static ComprehensiveTestExecutor fromSystemProperties() {
        ComprehensiveTestExecutor executor = new ComprehensiveTestExecutor();

        String twoP = System.getProperty("test.2pGames");
        if (twoP != null) {
            try {
                executor.twoPlayerGames(Integer.parseInt(twoP));
            } catch (NumberFormatException e) {
                System.err.println("Invalid test.2pGames value: " + twoP + ", using default");
            }
        }

        String threeP = System.getProperty("test.3pGames");
        if (threeP != null) {
            try {
                executor.threePlayerGames(Integer.parseInt(threeP));
            } catch (NumberFormatException e) {
                System.err.println("Invalid test.3pGames value: " + threeP + ", using default");
            }
        }

        String fourP = System.getProperty("test.4pGames");
        if (fourP != null) {
            try {
                executor.fourPlayerGames(Integer.parseInt(fourP));
            } catch (NumberFormatException e) {
                System.err.println("Invalid test.4pGames value: " + fourP + ", using default");
            }
        }

        String batchSize = System.getProperty("test.batchSize");
        if (batchSize != null) {
            try {
                executor.parallelBatchSize(Integer.parseInt(batchSize));
            } catch (NumberFormatException e) {
                System.err.println("Invalid test.batchSize value: " + batchSize + ", using default");
            }
        }

        String timeout = System.getProperty("test.timeoutMs");
        if (timeout != null) {
            try {
                executor.gameTimeout(Long.parseLong(timeout));
            } catch (NumberFormatException e) {
                System.err.println("Invalid test.timeoutMs value: " + timeout + ", using default");
            }
        }

        return executor;
    }

}
