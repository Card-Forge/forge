package forge.net;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Executes test scenarios in parallel for scale testing.
 * Allows running multiple automated games concurrently to stress test
 * the network infrastructure.
 *
 * Phase 7 of the Automated Network Testing Plan.
 */
public class ParallelTestExecutor {

    private static final int DEFAULT_TIMEOUT_MINUTES = 10;

    /**
     * Execute multiple games in parallel.
     *
     * @param iterations Total number of games to run
     * @param parallelism Number of concurrent games
     * @return List of GameTestMetrics from all completed games
     */
    public List<GameTestMetrics> executeParallel(int iterations, int parallelism) {
        return executeParallel(iterations, parallelism, DEFAULT_TIMEOUT_MINUTES);
    }

    /**
     * Execute multiple games in parallel with custom timeout.
     *
     * @param iterations Total number of games to run
     * @param parallelism Number of concurrent games
     * @param timeoutMinutes Maximum time to wait for each game
     * @return List of GameTestMetrics from all completed games
     */
    public List<GameTestMetrics> executeParallel(int iterations, int parallelism, int timeoutMinutes) {
        System.out.println("[ParallelTestExecutor] Starting parallel execution:");
        System.out.println("  Iterations: " + iterations);
        System.out.println("  Parallelism: " + parallelism);
        System.out.println("  Timeout: " + timeoutMinutes + " minutes per game");

        ExecutorService executor = Executors.newFixedThreadPool(parallelism);
        List<Future<GameTestMetrics>> futures = new ArrayList<>();

        // Submit all game tasks
        for (int i = 0; i < iterations; i++) {
            final int gameNumber = i + 1;
            futures.add(executor.submit(() -> {
                System.out.println("[ParallelTestExecutor] Starting game " + gameNumber);
                AutomatedGameTestHarness harness = new AutomatedGameTestHarness();
                GameTestMetrics result = harness.runBasicTwoPlayerGame();
                System.out.println("[ParallelTestExecutor] Finished game " + gameNumber +
                    " - " + (result.isGameCompleted() ? "PASSED" : "FAILED"));
                return result;
            }));
        }

        // Collect results
        List<GameTestMetrics> results = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                GameTestMetrics metrics = futures.get(i).get(timeoutMinutes, TimeUnit.MINUTES);
                results.add(metrics);
            } catch (Exception e) {
                System.err.println("[ParallelTestExecutor] Game " + (i + 1) + " failed: " + e.getMessage());
                // Create a failure metrics entry
                GameTestMetrics failedMetrics = new GameTestMetrics();
                failedMetrics.setGameCompleted(false);
                failedMetrics.setErrorMessage("Execution failed: " + e.getMessage());
                failedMetrics.setException(e instanceof Exception ? (Exception) e : null);
                results.add(failedMetrics);
            }
        }

        // Shutdown executor
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Print summary
        printSummary(results);

        return results;
    }

    /**
     * Print a summary of parallel test execution.
     */
    private void printSummary(List<GameTestMetrics> results) {
        int passed = 0;
        int failed = 0;
        long totalBytes = 0;
        long totalDuration = 0;

        for (GameTestMetrics m : results) {
            if (m.isGameCompleted()) {
                passed++;
                totalBytes += m.getTotalBytesSent();
                totalDuration += m.getGameDurationMs();
            } else {
                failed++;
            }
        }

        System.out.println();
        System.out.println("[ParallelTestExecutor] Summary:");
        System.out.println("  Total games: " + results.size());
        System.out.println("  Passed: " + passed);
        System.out.println("  Failed: " + failed);

        if (passed > 0) {
            System.out.println("  Avg bytes/game: " + (totalBytes / passed));
            System.out.println("  Avg duration: " + (totalDuration / passed) + "ms");
        }
    }

    /**
     * Aggregate statistics from multiple test runs.
     */
    public static class AggregateStats {
        public final int totalGames;
        public final int passed;
        public final int failed;
        public final long totalBytes;
        public final long avgBytesPerGame;
        public final long avgDurationMs;

        public AggregateStats(List<GameTestMetrics> results) {
            int p = 0, f = 0;
            long bytes = 0, duration = 0;

            for (GameTestMetrics m : results) {
                if (m.isGameCompleted()) {
                    p++;
                    bytes += m.getTotalBytesSent();
                    duration += m.getGameDurationMs();
                } else {
                    f++;
                }
            }

            this.totalGames = results.size();
            this.passed = p;
            this.failed = f;
            this.totalBytes = bytes;
            this.avgBytesPerGame = p > 0 ? bytes / p : 0;
            this.avgDurationMs = p > 0 ? duration / p : 0;
        }

        @Override
        public String toString() {
            return String.format(
                "AggregateStats[total=%d, passed=%d, failed=%d, avgBytes=%d, avgDuration=%dms]",
                totalGames, passed, failed, avgBytesPerGame, avgDurationMs);
        }
    }

    /**
     * Main method for standalone parallel testing.
     *
     * Usage: java ParallelTestExecutor [iterations] [parallelism]
     */
    public static void main(String[] args) {
        int iterations = args.length > 0 ? Integer.parseInt(args[0]) : 5;
        int parallelism = args.length > 1 ? Integer.parseInt(args[1]) : 2;

        System.out.println("Running parallel test with " + iterations +
            " iterations and " + parallelism + " concurrent games");

        // Note: FModel must be initialized before running
        ParallelTestExecutor executor = new ParallelTestExecutor();
        List<GameTestMetrics> results = executor.executeParallel(iterations, parallelism);

        AggregateStats stats = new AggregateStats(results);
        System.out.println();
        System.out.println("Final stats: " + stats);

        System.exit(stats.failed > 0 ? 1 : 0);
    }
}
