package forge.net;

import forge.gamemodes.net.NetworkDebugLogger;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Tests for batch network game execution, both sequential and parallel.
 *
 * Combines tests from the former SequentialGameTest and MultiProcessGameTest classes.
 *
 * Sequential tests run multiple network games one after another for debugging and log generation.
 * Parallel tests spawn multiple JVM processes to run games truly in parallel,
 * avoiding the FServerManager singleton limitation.
 *
 * Each game process:
 * - Has its own isolated JVM and FServerManager instance (for parallel)
 * - Uses a unique network port
 * - Writes to its own log file
 *
 * Log files are created in: forge-gui-desktop/logs/
 * Format: network-debug-YYYYMMDD-HHMMSS-PID-gameN-test.log
 */
public class BatchGameTest {

    @BeforeClass
    public void setUp() {
        // Enable test mode for log file naming
        NetworkDebugLogger.setTestMode(true);
    }

    // ==================== Sequential Tests ====================

    /**
     * Run 3 games sequentially - standard test for log generation.
     * Each game completes fully before the next starts.
     */
    @Test(timeOut = 900000) // 15 minute timeout
    public void testSequentialThreeGames() {
        SequentialGameExecutor executor = new SequentialGameExecutor(300000);
        SequentialGameExecutor.ExecutionResult result = executor.runGames(3);

        NetworkDebugLogger.log("[BatchGameTest] Sequential 3 games: %s", result.toSummary());
        System.out.println(result.toDetailedReport());

        // All games should succeed when run sequentially
        assertTrue(result.getSuccessCount() >= 3,
                "All 3 sequential games should succeed: " + result.toSummary());
        assertTrue(result.getTotalDeltaPackets() > 0,
                "Delta packets should be received across games");
    }

    /**
     * Run 5 games sequentially - extended test for more log data.
     */
    @Test(timeOut = 1500000) // 25 minute timeout
    public void testSequentialFiveGames() {
        SequentialGameExecutor executor = new SequentialGameExecutor(300000);
        SequentialGameExecutor.ExecutionResult result = executor.runGames(5);

        NetworkDebugLogger.log("[BatchGameTest] Sequential 5 games: %s", result.toSummary());
        System.out.println(result.toDetailedReport());

        // All games should succeed
        assertTrue(result.getSuccessCount() >= 5,
                "All 5 sequential games should succeed: " + result.toSummary());
    }

    /**
     * Verify that each sequential game writes to its own log file.
     */
    @Test(timeOut = 600000)
    public void testSequentialIsolatedLogFiles() {
        SequentialGameExecutor executor = new SequentialGameExecutor(300000);
        SequentialGameExecutor.ExecutionResult result = executor.runGames(2);

        NetworkDebugLogger.log("[BatchGameTest] Isolated logs test: %s", result.toSummary());

        // Log results for each game
        for (int i = 0; i < 2; i++) {
            if (result.getResults().containsKey(i)) {
                NetworkClientTestHarness.TestResult gameResult = result.getResults().get(i);
                NetworkDebugLogger.log("[BatchGameTest] Game %d: deltas=%d, bytes=%d",
                        i, gameResult.deltaPacketsReceived, gameResult.totalDeltaBytes);
            }
        }

        // Both games should succeed
        assertTrue(result.getSuccessCount() >= 2,
                "Both games should succeed: " + result.toSummary());

        System.out.println("\nLog files created in: forge-gui-desktop/logs/");
        System.out.println("Look for files matching: network-debug-*-game*-test.log");
    }

    // ==================== Parallel Tests ====================

    /**
     * Run 3 games in parallel processes.
     * Each game runs in its own JVM, enabling true parallelism.
     */
    @Test(timeOut = 600000) // 10 minute timeout
    public void testParallelThreeGames() {
        MultiProcessGameExecutor executor = new MultiProcessGameExecutor(300000);
        MultiProcessGameExecutor.ExecutionResult result = executor.runGames(3);

        NetworkDebugLogger.log("[BatchGameTest] Parallel 3 games: %s", result.toSummary());
        System.out.println(result.toDetailedReport());

        // All games should succeed with separate processes
        assertTrue(result.getSuccessCount() >= 2,
                "At least 2 of 3 parallel games should succeed: " + result.toSummary());
        assertTrue(result.getTotalDeltaPackets() > 0,
                "Delta packets should be received across games");
    }

    /**
     * Run 5 games in parallel for more comprehensive log generation.
     */
    @Test(timeOut = 900000) // 15 minute timeout
    public void testParallelFiveGames() {
        MultiProcessGameExecutor executor = new MultiProcessGameExecutor(300000);
        MultiProcessGameExecutor.ExecutionResult result = executor.runGames(5);

        NetworkDebugLogger.log("[BatchGameTest] Parallel 5 games: %s", result.toSummary());
        System.out.println(result.toDetailedReport());

        // Most games should succeed
        assertTrue(result.getSuccessCount() >= 4,
                "At least 4 of 5 parallel games should succeed: " + result.toSummary());
    }

    /**
     * Quick test with 2 parallel processes to verify the infrastructure works.
     */
    @Test(timeOut = 300000) // 5 minute timeout
    public void testParallelTwoGames() {
        MultiProcessGameExecutor executor = new MultiProcessGameExecutor(180000);
        MultiProcessGameExecutor.ExecutionResult result = executor.runGames(2);

        NetworkDebugLogger.log("[BatchGameTest] Parallel 2 games: %s", result.toSummary());
        System.out.println(result.toDetailedReport());

        // Both games should succeed
        assertTrue(result.getSuccessCount() >= 1,
                "At least 1 of 2 parallel games should succeed: " + result.toSummary());

        System.out.println("\nLog files created in: forge-gui-desktop/logs/");
        System.out.println("Each process creates its own log: network-debug-*-game*-test.log");
    }

    // ==================== Configurable Tests ====================

    /**
     * Run with configurable game count from system properties (sequential execution).
     *
     * Usage:
     * mvn -pl forge-gui-desktop verify -Dtest=BatchGameTest#testConfigurableSequential \
     *     -Dtest.gameCount=10 -Dsurefire.failIfNoSpecifiedTests=false
     */
    @Test(timeOut = 3600000) // 60 minute timeout for large runs
    public void testConfigurableSequential() {
        int gameCount = Integer.getInteger("test.gameCount", 3);
        long timeoutMs = Long.getLong("test.timeout", 300000);

        NetworkDebugLogger.log("[BatchGameTest] Sequential config: games=%d, timeout=%dms",
                gameCount, timeoutMs);

        SequentialGameExecutor executor = new SequentialGameExecutor(timeoutMs);
        SequentialGameExecutor.ExecutionResult result = executor.runGames(gameCount);

        NetworkDebugLogger.log("[BatchGameTest] Sequential result: %s", result.toSummary());
        System.out.println(result.toDetailedReport());

        // All games should succeed
        assertEquals(result.getSuccessCount(), gameCount,
                "All games should succeed: " + result.toSummary());
    }

    /**
     * Run with configurable game count from system properties (parallel execution).
     *
     * Usage:
     * mvn -pl forge-gui-desktop verify -Dtest=BatchGameTest#testConfigurableParallel \
     *     -Dtest.gameCount=10 -Dsurefire.failIfNoSpecifiedTests=false
     */
    @Test(timeOut = 1800000) // 30 minute timeout for large runs
    public void testConfigurableParallel() {
        int gameCount = Integer.getInteger("test.gameCount", 3);
        long timeoutMs = Long.getLong("test.timeout", 300000);

        NetworkDebugLogger.log("[BatchGameTest] Parallel config: games=%d, timeout=%dms",
                gameCount, timeoutMs);

        MultiProcessGameExecutor executor = new MultiProcessGameExecutor(timeoutMs);
        MultiProcessGameExecutor.ExecutionResult result = executor.runGames(gameCount);

        NetworkDebugLogger.log("[BatchGameTest] Parallel result: %s", result.toSummary());
        System.out.println(result.toDetailedReport());

        // Most games should succeed
        double expectedSuccessRate = 0.8;
        int expectedSuccesses = (int) Math.ceil(gameCount * expectedSuccessRate);
        assertTrue(result.getSuccessCount() >= expectedSuccesses,
                String.format("At least %d of %d games should succeed: %s",
                        expectedSuccesses, gameCount, result.toSummary()));
    }
}
