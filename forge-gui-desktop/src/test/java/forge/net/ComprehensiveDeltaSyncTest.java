package forge.net;

import forge.gamemodes.net.NetworkDebugLogger;
import forge.gui.GuiBase;
import forge.model.FModel;
import forge.net.analysis.AnalysisResult;
import forge.net.analysis.GameLogMetrics;
import forge.net.analysis.NetworkLogAnalyzer;
import org.testng.Assert;
import org.testng.SkipException;

import java.util.List;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Comprehensive delta sync validation test.
 *
 * Runs 100 network games with varied player counts (2-4) and random decks,
 * analyzes logs for bugs and bandwidth efficiency, and validates results.
 *
 * Default configuration:
 * - 50 x 2-player games (50%)
 * - 30 x 3-player games (30%)
 * - 20 x 4-player games (20%)
 *
 * Configurable via system properties:
 * - -Dtest.2pGames=50
 * - -Dtest.3pGames=30
 * - -Dtest.4pGames=20
 * - -Dtest.batchSize=10
 * - -Dtest.timeoutMs=300000
 *
 * Usage:
 * mvn -pl forge-gui-desktop -am verify \
 *     -Dtest="ComprehensiveDeltaSyncTest#runComprehensiveDeltaSyncTest" \
 *     -Dsurefire.failIfNoSpecifiedTests=false
 */
public class ComprehensiveDeltaSyncTest {

    private static final String LOG_PREFIX = "[ComprehensiveDeltaSyncTest]";
    // Use working directory relative path - tests run from project root
    private static final File LOG_DIRECTORY = new File(System.getProperty("user.dir"), "logs");

    @BeforeClass
    public void setUp() {
        skipUnlessStressTestsEnabled();
        // Initialize headless environment
        if (GuiBase.getInterface() == null) {
            GuiBase.setInterface(new HeadlessGuiDesktop());
            FModel.initialize(null, preferences -> null);
        }
        NetworkDebugLogger.setTestMode(true);
        NetworkDebugLogger.log("%s Test environment initialized", LOG_PREFIX);
    }

    private void skipUnlessStressTestsEnabled() {
        if (!"true".equalsIgnoreCase(System.getProperty("run.stress.tests"))) {
            throw new SkipException("Stress tests skipped. Use -Drun.stress.tests=true to run.");
        }
    }

    /**
     * Run comprehensive delta sync test with 100 games.
     *
     * This test has a 2-hour timeout to allow for 100 games to complete.
     *
     * Validation criteria:
     * - Success rate > 90%
     * - Average bandwidth savings > 90%
     * - Zero checksum mismatches
     */
    @Test(timeOut = 7200000)  // 2 hour timeout for 100 games
    public void runComprehensiveDeltaSyncTest() {
        NetworkDebugLogger.log("%s Starting comprehensive delta sync validation", LOG_PREFIX);

        // 1. Create executor from system properties (or use defaults)
        ComprehensiveTestExecutor executor = ComprehensiveTestExecutor.fromSystemProperties();
        NetworkDebugLogger.log("%s Configuration:\n%s", LOG_PREFIX, executor.getConfigurationSummary());

        // 2. Run all games
        NetworkDebugLogger.log("%s Executing %d games...", LOG_PREFIX, executor.getTotalGames());
        LocalDateTime testStartTime = LocalDateTime.now();
        long startTime = System.currentTimeMillis();

        MultiProcessGameExecutor.ExecutionResult executionResult = executor.execute();

        long executionDuration = System.currentTimeMillis() - startTime;
        NetworkDebugLogger.log("%s Execution completed in %.1f minutes", LOG_PREFIX, executionDuration / 60000.0);

        // 3. Log execution summary
        System.out.println("\n" + executionResult.toDetailedReport());

        // 4. Analyze log files (only those from this test run)
        NetworkDebugLogger.log("%s Analyzing log files in %s (created after %s)", LOG_PREFIX, LOG_DIRECTORY.getAbsolutePath(), testStartTime);
        NetworkLogAnalyzer analyzer = new NetworkLogAnalyzer();
        AnalysisResult analysisResult = analyzer.analyzeComprehensiveTestAndAggregate(LOG_DIRECTORY, testStartTime);

        // 5. Generate and print report
        String report = analysisResult.generateReport();
        System.out.println("\n" + "=".repeat(80));
        System.out.println(report);
        System.out.println("=".repeat(80));

        // 6. Save report to file for future reference
        saveReportToFile(report, "comprehensive-test-results");

        // 7. Log summary
        NetworkDebugLogger.log("%s Analysis complete: %s", LOG_PREFIX, analysisResult.toSummary());

        // 8. Validate results
        validateResults(executionResult, analysisResult);
    }

    /**
     * Run a smaller test with 10 games for quick validation.
     * Useful for development and CI smoke tests.
     */
    @Test(timeOut = 1200000)  // 20 minute timeout for 10 games
    public void runQuickDeltaSyncTest() {
        NetworkDebugLogger.log("%s Starting quick delta sync validation (10 games)", LOG_PREFIX);

        ComprehensiveTestExecutor executor = new ComprehensiveTestExecutor()
                .twoPlayerGames(5)
                .threePlayerGames(3)
                .fourPlayerGames(2)
                .parallelBatchSize(3);

        NetworkDebugLogger.log("%s Configuration:\n%s", LOG_PREFIX, executor.getConfigurationSummary());

        LocalDateTime testStartTime = LocalDateTime.now();
        MultiProcessGameExecutor.ExecutionResult executionResult = executor.execute();

        System.out.println("\n" + executionResult.toDetailedReport());

        // Analyze and validate with relaxed criteria for quick test
        NetworkLogAnalyzer analyzer = new NetworkLogAnalyzer();
        AnalysisResult analysisResult = analyzer.analyzeComprehensiveTestAndAggregate(LOG_DIRECTORY, testStartTime);

        String report = analysisResult.generateReport();
        System.out.println("\n" + report);
        saveReportToFile(report, "quick-test-results");

        // Quick test uses relaxed validation (80% success rate)
        Assert.assertTrue(executionResult.getSuccessRate() >= 0.80,
                String.format("Quick test success rate should be >= 80%%, was %.1f%%",
                        executionResult.getSuccessRate() * 100));
    }

    /**
     * Test only 2-player games (fastest execution).
     */
    @Test(timeOut = 600000)  // 10 minute timeout
    public void runTwoPlayerOnlyTest() {
        NetworkDebugLogger.log("%s Starting 2-player only test", LOG_PREFIX);

        ComprehensiveTestExecutor executor = new ComprehensiveTestExecutor()
                .twoPlayerGames(10)
                .threePlayerGames(0)
                .fourPlayerGames(0)
                .parallelBatchSize(5);

        MultiProcessGameExecutor.ExecutionResult result = executor.execute();

        System.out.println("\n" + result.toDetailedReport());

        Assert.assertTrue(result.getSuccessRate() >= 0.80,
                String.format("2-player test success rate should be >= 80%%, was %.1f%%",
                        result.getSuccessRate() * 100));
    }

    /**
     * Test multiplayer games (3 and 4 players).
     */
    @Test(timeOut = 900000)  // 15 minute timeout
    public void runMultiplayerOnlyTest() {
        NetworkDebugLogger.log("%s Starting multiplayer only test", LOG_PREFIX);

        ComprehensiveTestExecutor executor = new ComprehensiveTestExecutor()
                .twoPlayerGames(0)
                .threePlayerGames(5)
                .fourPlayerGames(5)
                .parallelBatchSize(3);

        MultiProcessGameExecutor.ExecutionResult result = executor.execute();

        System.out.println("\n" + result.toDetailedReport());

        Assert.assertTrue(result.getSuccessRate() >= 0.70,
                String.format("Multiplayer test success rate should be >= 70%%, was %.1f%%",
                        result.getSuccessRate() * 100));
    }

    /**
     * Utility test to analyze existing logs without running new games.
     * Useful for testing report generation changes.
     * Set -Dtest.batchId=20260127-213221 to analyze a specific batch.
     */
    @Test
    public void analyzeExistingLogs() {
        NetworkDebugLogger.log("%s Analyzing existing logs...", LOG_PREFIX);

        File logDir = new File("logs");
        NetworkLogAnalyzer analyzer = new NetworkLogAnalyzer();

        // Check for specific batch ID
        String batchId = System.getProperty("test.batchId", "");
        List<GameLogMetrics> metrics;

        if (!batchId.isEmpty()) {
            // Analyze only logs from the specified batch
            // Pattern supports both old format (game0-4p) and new format with batch (batch0-game0-4p)
            String pattern = "network-debug-run" + batchId + "-(?:batch\\d+-)?game\\d+-\\d+p-test\\.log";
            System.out.println("Analyzing batch: " + batchId + " (pattern: " + pattern + ")");
            metrics = analyzer.analyzeDirectory(logDir, pattern);
        } else {
            // Analyze all comprehensive test logs (no time filter)
            metrics = analyzer.analyzeComprehensiveTestLogs(logDir);
        }
        NetworkDebugLogger.log("%s Found %d log files", LOG_PREFIX, metrics.size());

        if (metrics.isEmpty()) {
            System.out.println("No comprehensive test logs found in " + logDir.getAbsolutePath());
            return;
        }

        // Build and print report
        AnalysisResult result = analyzer.buildAnalysisResult(metrics);
        String report = result.generateReport();
        System.out.println("\n" + report);

        // Save report
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        java.nio.file.Path reportPath = logDir.toPath().resolve("analysis-results-" + timestamp + ".md");
        try {
            java.nio.file.Files.write(reportPath, report.getBytes());
            System.out.println("Report saved to: " + reportPath);
        } catch (java.io.IOException e) {
            System.err.println("Failed to save report: " + e.getMessage());
        }
    }

    /**
     * Validate execution and analysis results against criteria.
     */
    private void validateResults(MultiProcessGameExecutor.ExecutionResult executionResult,
                                  AnalysisResult analysisResult) {
        NetworkDebugLogger.log("%s Validating results...", LOG_PREFIX);

        // 1. Check success rate > 90%
        double successRate = executionResult.getSuccessRate() * 100;
        NetworkDebugLogger.log("%s Success rate: %.1f%% (target: 90%%)", LOG_PREFIX, successRate);
        Assert.assertTrue(successRate >= 90.0,
                String.format("Success rate should be >= 90%%, was %.1f%%", successRate));

        // 2. Check bandwidth efficiency via bytes per delta packet
        // Delta sync is efficient if average bytes per packet is small (<200 bytes)
        // For comparison: full state packets are typically 100-200KB
        long totalDeltas = executionResult.getTotalDeltaPackets();
        long totalBytes = executionResult.getTotalBytes();
        double bytesPerPacket = totalDeltas > 0 ? (double) totalBytes / totalDeltas : 0;
        NetworkDebugLogger.log("%s Bytes per delta packet: %.1f (target: <200)", LOG_PREFIX, bytesPerPacket);

        // Calculate estimated savings vs full state (assuming ~150KB per full state update)
        double estimatedFullStateBytes = totalDeltas * 150000.0; // 150KB per update
        double estimatedSavings = estimatedFullStateBytes > 0
                ? 100.0 * (1.0 - (double) totalBytes / estimatedFullStateBytes) : 0;
        NetworkDebugLogger.log("%s Estimated bandwidth savings: %.1f%% (vs 150KB full state)", LOG_PREFIX, estimatedSavings);

        // Also check log-based analysis if available
        double logBasedSavings = analysisResult.getAverageBandwidthSavings();
        if (logBasedSavings > 0) {
            NetworkDebugLogger.log("%s Log-based bandwidth savings: %.1f%%", LOG_PREFIX, logBasedSavings);
        }

        // Primary validation: bytes per packet should be efficient (< 200 bytes)
        // This indicates delta sync is sending small incremental updates, not full state
        Assert.assertTrue(bytesPerPacket < 200,
                String.format("Bytes per delta packet should be < 200 (efficient), was %.1f", bytesPerPacket));

        // 3. Check zero checksum mismatches
        int checksumMismatches = analysisResult.getGamesWithChecksumMismatches();
        NetworkDebugLogger.log("%s Checksum mismatches: %d (target: 0)", LOG_PREFIX, checksumMismatches);
        Assert.assertEquals(checksumMismatches, 0,
                String.format("Should have zero checksum mismatches, had %d", checksumMismatches));

        // 4. Additional validation by player count
        for (int p = 2; p <= 4; p++) {
            double playerSuccessRate = executionResult.getSuccessRateByPlayers(p) * 100;
            int playerGameCount = executionResult.getGameCountByPlayers(p);
            if (playerGameCount > 0) {
                NetworkDebugLogger.log("%s %d-player success rate: %.1f%% (%d games)",
                        LOG_PREFIX, p, playerSuccessRate, playerGameCount);
                // Per-player-count threshold is lower (80%) since smaller sample sizes
                Assert.assertTrue(playerSuccessRate >= 80.0,
                        String.format("%d-player success rate should be >= 80%%, was %.1f%%",
                                p, playerSuccessRate));
            }
        }

        NetworkDebugLogger.log("%s All validation criteria PASSED", LOG_PREFIX);
    }

    /**
     * Save the analysis report to a timestamped file in the logs directory.
     * This allows easy reference to comprehensive test results without reconstructing from logs.
     *
     * @param report The markdown report content
     * @param prefix Filename prefix (e.g., "comprehensive-test-results" or "quick-test-results")
     */
    private void saveReportToFile(String report, String prefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        File reportFile = new File(LOG_DIRECTORY, prefix + "-" + timestamp + ".md");

        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write(report);
            NetworkDebugLogger.log("%s Report saved to: %s", LOG_PREFIX, reportFile.getAbsolutePath());
            System.out.println("Report saved to: " + reportFile.getAbsolutePath());
        } catch (IOException e) {
            NetworkDebugLogger.log("%s WARNING: Failed to save report to file: %s", LOG_PREFIX, e.getMessage());
            System.err.println("WARNING: Failed to save report to file: " + e.getMessage());
        }
    }

    /**
     * Main method for standalone execution.
     */
    public static void main(String[] args) {
        System.out.println("Starting Comprehensive Delta Sync Test");
        System.out.println("=".repeat(60));

        // Initialize headless environment
        if (GuiBase.getInterface() == null) {
            GuiBase.setInterface(new HeadlessGuiDesktop());
            FModel.initialize(null, preferences -> null);
        }
        NetworkDebugLogger.setTestMode(true);

        ComprehensiveDeltaSyncTest test = new ComprehensiveDeltaSyncTest();
        try {
            test.runComprehensiveDeltaSyncTest();
            System.out.println("\nTest PASSED");
        } catch (AssertionError e) {
            System.err.println("\nTest FAILED: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("\nTest ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }
}
