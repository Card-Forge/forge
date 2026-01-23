package forge.net;

import forge.gamemodes.net.NetworkDebugLogger;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Tests for sequential network game execution.
 *
 * These tests run multiple network games sequentially for rapid debugging and log generation.
 * Each game writes to its own log file, enabling easy analysis of individual games.
 *
 * Part of Phase 10 of the automated network testing infrastructure.
 *
 * Log files are created in: forge-gui-desktop/logs/
 * Format: network-debug-YYYYMMDD-HHMMSS-PID-gameN-test.log
 */
public class SequentialGameTest {

    @BeforeClass
    public void setUp() {
        // Enable test mode for log file naming
        NetworkDebugLogger.setTestMode(true);
    }

    /**
     * Run 3 games sequentially - standard test for log generation.
     * Each game completes fully before the next starts.
     */
    @Test(timeOut = 900000) // 15 minute timeout
    public void testThreeSequentialGames() {
        SequentialGameExecutor executor = new SequentialGameExecutor(300000);
        SequentialGameExecutor.ExecutionResult result = executor.runGames(3);

        NetworkDebugLogger.log("[SequentialGameTest] %s", result.toSummary());
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
    public void testFiveSequentialGames() {
        SequentialGameExecutor executor = new SequentialGameExecutor(300000);
        SequentialGameExecutor.ExecutionResult result = executor.runGames(5);

        NetworkDebugLogger.log("[SequentialGameTest] %s", result.toSummary());
        System.out.println(result.toDetailedReport());

        // All games should succeed
        assertTrue(result.getSuccessCount() >= 5,
                "All 5 sequential games should succeed: " + result.toSummary());
    }

    /**
     * Run with configurable game count from system properties.
     *
     * Usage:
     * mvn -pl forge-gui-desktop test -Dtest=SequentialGameTest#testConfigurableGameCount \
     *     -Dtest.gameCount=10
     */
    @Test(timeOut = 3600000) // 60 minute timeout for large runs
    public void testConfigurableGameCount() {
        int gameCount = Integer.getInteger("test.gameCount", 3);
        long timeoutMs = Long.getLong("test.timeout", 300000);

        NetworkDebugLogger.log("[SequentialGameTest] Config: games=%d, timeout=%dms",
                gameCount, timeoutMs);

        SequentialGameExecutor executor = new SequentialGameExecutor(timeoutMs);
        SequentialGameExecutor.ExecutionResult result = executor.runGames(gameCount);

        NetworkDebugLogger.log("[SequentialGameTest] %s", result.toSummary());
        System.out.println(result.toDetailedReport());

        // All games should succeed
        assertEquals(result.getSuccessCount(), gameCount,
                "All games should succeed: " + result.toSummary());
    }

    /**
     * Verify that each game writes to its own log file.
     */
    @Test(timeOut = 600000)
    public void testIsolatedLogFiles() {
        SequentialGameExecutor executor = new SequentialGameExecutor(300000);
        SequentialGameExecutor.ExecutionResult result = executor.runGames(2);

        NetworkDebugLogger.log("[SequentialGameTest] Isolated logs test: %s", result.toSummary());

        // Log results for each game
        for (int i = 0; i < 2; i++) {
            if (result.getResults().containsKey(i)) {
                NetworkClientTestHarness.TestResult gameResult = result.getResults().get(i);
                NetworkDebugLogger.log("[SequentialGameTest] Game %d: deltas=%d, bytes=%d",
                        i, gameResult.deltaPacketsReceived, gameResult.totalDeltaBytes);
            }
        }

        // Both games should succeed
        assertTrue(result.getSuccessCount() >= 2,
                "Both games should succeed: " + result.toSummary());

        System.out.println("\nLog files created in: forge-gui-desktop/logs/");
        System.out.println("Look for files matching: network-debug-*-game*-test.log");
    }
}
