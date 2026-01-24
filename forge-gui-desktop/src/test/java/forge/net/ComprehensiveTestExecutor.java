package forge.net;

import forge.gamemodes.net.NetworkDebugLogger;

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
 * Games are executed in parallel batches using MultiProcessGameExecutor.
 */
public class ComprehensiveTestExecutor {

    private static final String LOG_PREFIX = "[ComprehensiveTestExecutor]";

    // Default game distribution
    private int twoPlayerGames = 50;
    private int threePlayerGames = 30;
    private int fourPlayerGames = 20;

    // Execution settings
    private int parallelBatchSize = 5; // Run 5 games at a time
    private long gameTimeoutMs = 300000; // 5 minutes per game

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
        NetworkDebugLogger.log("%s Starting comprehensive test: %d total games", LOG_PREFIX, totalGames);
        NetworkDebugLogger.log("%s Distribution: %d x 2-player, %d x 3-player, %d x 4-player",
                LOG_PREFIX, twoPlayerGames, threePlayerGames, fourPlayerGames);
        NetworkDebugLogger.log("%s Batch size: %d, Timeout: %dms", LOG_PREFIX, parallelBatchSize, gameTimeoutMs);

        long startTime = System.currentTimeMillis();

        // Build player count array with shuffled distribution
        int[] playerCounts = buildShuffledPlayerCounts();

        // Create executor with ComprehensiveGameRunner
        MultiProcessGameExecutor executor = new MultiProcessGameExecutor(gameTimeoutMs)
                .withRunnerClass("forge.net.ComprehensiveGameRunner");

        // Run all games in batches
        MultiProcessGameExecutor.ExecutionResult result = executor.runGamesInBatches(playerCounts, parallelBatchSize);

        long duration = System.currentTimeMillis() - startTime;
        NetworkDebugLogger.log("%s Comprehensive test completed in %d ms (%.1f minutes)",
                LOG_PREFIX, duration, duration / 60000.0);

        return result;
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
        return String.format(
                "ComprehensiveTestExecutor Configuration:\n" +
                "  Total Games: %d\n" +
                "  Distribution:\n" +
                "    - 2-player: %d (%.0f%%)\n" +
                "    - 3-player: %d (%.0f%%)\n" +
                "    - 4-player: %d (%.0f%%)\n" +
                "  Batch Size: %d\n" +
                "  Game Timeout: %d ms",
                getTotalGames(),
                twoPlayerGames, (100.0 * twoPlayerGames / getTotalGames()),
                threePlayerGames, (100.0 * threePlayerGames / getTotalGames()),
                fourPlayerGames, (100.0 * fourPlayerGames / getTotalGames()),
                parallelBatchSize,
                gameTimeoutMs
        );
    }

    /**
     * Create a default executor with standard configuration.
     * 100 games: 50 x 2p, 30 x 3p, 20 x 4p
     */
    public static ComprehensiveTestExecutor defaultConfiguration() {
        return new ComprehensiveTestExecutor()
                .twoPlayerGames(50)
                .threePlayerGames(30)
                .fourPlayerGames(20)
                .parallelBatchSize(5)
                .gameTimeout(300000);
    }

    /**
     * Create an executor from system properties.
     * Properties:
     *   -Dtest.2pGames=50
     *   -Dtest.3pGames=30
     *   -Dtest.4pGames=20
     *   -Dtest.batchSize=5
     *   -Dtest.timeoutMs=300000
     */
    public static ComprehensiveTestExecutor fromSystemProperties() {
        ComprehensiveTestExecutor executor = new ComprehensiveTestExecutor();

        String twoP = System.getProperty("test.2pGames");
        if (twoP != null) {
            executor.twoPlayerGames(Integer.parseInt(twoP));
        }

        String threeP = System.getProperty("test.3pGames");
        if (threeP != null) {
            executor.threePlayerGames(Integer.parseInt(threeP));
        }

        String fourP = System.getProperty("test.4pGames");
        if (fourP != null) {
            executor.fourPlayerGames(Integer.parseInt(fourP));
        }

        String batchSize = System.getProperty("test.batchSize");
        if (batchSize != null) {
            executor.parallelBatchSize(Integer.parseInt(batchSize));
        }

        String timeout = System.getProperty("test.timeoutMs");
        if (timeout != null) {
            executor.gameTimeout(Long.parseLong(timeout));
        }

        return executor;
    }

    /**
     * Main method for standalone execution.
     */
    public static void main(String[] args) {
        System.out.println("Starting Comprehensive Delta Sync Test");
        System.out.println("=".repeat(60));

        ComprehensiveTestExecutor executor = fromSystemProperties();
        System.out.println(executor.getConfigurationSummary());
        System.out.println();

        MultiProcessGameExecutor.ExecutionResult result = executor.execute();

        System.out.println();
        System.out.println(result.toDetailedReport());
    }
}
