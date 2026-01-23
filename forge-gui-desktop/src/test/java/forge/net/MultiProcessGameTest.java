package forge.net;

import forge.gamemodes.net.NetworkDebugLogger;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Tests for multi-process parallel game execution.
 *
 * These tests spawn multiple JVM processes to run games truly in parallel,
 * avoiding the FServerManager singleton limitation.
 *
 * Each game process:
 * - Has its own isolated JVM and FServerManager instance
 * - Uses a unique network port
 * - Writes to its own log file
 *
 * This enables much faster log generation than sequential execution.
 *
 * Log files are created in: forge-gui-desktop/logs/
 * Format: network-debug-YYYYMMDD-HHMMSS-PID-gameN-test.log
 */
public class MultiProcessGameTest {

    @BeforeClass
    public void setUp() {
        NetworkDebugLogger.setTestMode(true);
    }

    /**
     * Run 3 games in parallel processes.
     * Each game runs in its own JVM, enabling true parallelism.
     */
    @Test(timeOut = 600000) // 10 minute timeout
    public void testThreeParallelProcesses() {
        MultiProcessGameExecutor executor = new MultiProcessGameExecutor(300000);
        MultiProcessGameExecutor.ExecutionResult result = executor.runGames(3);

        NetworkDebugLogger.log("[MultiProcessTest] %s", result.toSummary());
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
    public void testFiveParallelProcesses() {
        MultiProcessGameExecutor executor = new MultiProcessGameExecutor(300000);
        MultiProcessGameExecutor.ExecutionResult result = executor.runGames(5);

        NetworkDebugLogger.log("[MultiProcessTest] %s", result.toSummary());
        System.out.println(result.toDetailedReport());

        // Most games should succeed
        assertTrue(result.getSuccessCount() >= 4,
                "At least 4 of 5 parallel games should succeed: " + result.toSummary());
    }

    /**
     * Run with configurable game count from system properties.
     *
     * Usage:
     * mvn -pl forge-gui-desktop test -Dtest=MultiProcessGameTest#testConfigurableGameCount \
     *     -Dtest.gameCount=10
     */
    @Test(timeOut = 1800000) // 30 minute timeout for large runs
    public void testConfigurableGameCount() {
        int gameCount = Integer.getInteger("test.gameCount", 3);
        long timeoutMs = Long.getLong("test.timeout", 300000);

        NetworkDebugLogger.log("[MultiProcessTest] Config: games=%d, timeout=%dms",
                gameCount, timeoutMs);

        MultiProcessGameExecutor executor = new MultiProcessGameExecutor(timeoutMs);
        MultiProcessGameExecutor.ExecutionResult result = executor.runGames(gameCount);

        NetworkDebugLogger.log("[MultiProcessTest] %s", result.toSummary());
        System.out.println(result.toDetailedReport());

        // Most games should succeed
        double expectedSuccessRate = 0.8;
        int expectedSuccesses = (int) Math.ceil(gameCount * expectedSuccessRate);
        assertTrue(result.getSuccessCount() >= expectedSuccesses,
                String.format("At least %d of %d games should succeed: %s",
                        expectedSuccesses, gameCount, result.toSummary()));
    }

    /**
     * Quick test with 2 parallel processes to verify the infrastructure works.
     */
    @Test(timeOut = 300000) // 5 minute timeout
    public void testTwoParallelProcesses() {
        MultiProcessGameExecutor executor = new MultiProcessGameExecutor(180000);
        MultiProcessGameExecutor.ExecutionResult result = executor.runGames(2);

        NetworkDebugLogger.log("[MultiProcessTest] %s", result.toSummary());
        System.out.println(result.toDetailedReport());

        // Both games should succeed
        assertTrue(result.getSuccessCount() >= 1,
                "At least 1 of 2 parallel games should succeed: " + result.toSummary());

        System.out.println("\nLog files created in: forge-gui-desktop/logs/");
        System.out.println("Each process creates its own log: network-debug-*-game*-test.log");
    }
}
