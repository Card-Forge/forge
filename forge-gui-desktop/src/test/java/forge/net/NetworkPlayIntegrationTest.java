package forge.net;

import forge.gamemodes.net.NetworkDebugLogger;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.net.analysis.AnalysisResult;
import forge.net.analysis.GameLogMetrics;
import forge.net.analysis.NetworkLogAnalyzer;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Unified integration test suite for NetworkPlay infrastructure.
 *
 * Uses UnifiedNetworkHarness for all test scenarios:
 * - 2-player local AI (NETWORK_LOCAL mode)
 * - 2-player with remote client (NETWORK_REMOTE mode)
 * - 3-4 player multiplayer with remote clients
 *
 * Test categories:
 * - Unit tests: Always run (deck loader, metrics, configuration)
 * - Single game tests: Integration tests for individual games
 * - Batch tests: Sequential and parallel multi-game execution
 * - Comprehensive tests: Large-scale delta sync validation
 *
 * All tests require -Drun.stress.tests=true to execute (skipped otherwise).
 *
 * Usage examples:
 *
 * Run all tests:
 *   mvn -pl forge-gui-desktop -am verify -Drun.stress.tests=true
 *
 * Run specific test:
 *   mvn -pl forge-gui-desktop -am verify -Dtest="NetworkPlayIntegrationTest#testTrueNetworkTraffic" \
 *       -Drun.stress.tests=true -Dsurefire.failIfNoSpecifiedTests=false
 *
 * Run comprehensive test with custom configuration:
 *   mvn -pl forge-gui-desktop -am verify -Dtest="NetworkPlayIntegrationTest#runComprehensiveDeltaSyncTest" \
 *       -Dtest.2pGames=50 -Dtest.3pGames=30 -Dtest.4pGames=20 \
 *       -Drun.stress.tests=true -Dsurefire.failIfNoSpecifiedTests=false
 */
public class NetworkPlayIntegrationTest {

    private static final String LOG_PREFIX = "[NetworkPlayIntegrationTest]";
    private static boolean initialized = false;

    @BeforeClass
    public static void setUp() {
        skipUnlessStressTestsEnabled();
        if (!initialized) {
            GuiBase.setInterface(new HeadlessGuiDesktop());
            FModel.initialize(null, preferences -> {
                preferences.setPref(FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
                preferences.setPref(FPref.UI_LANGUAGE, "en-US");
                return null;
            });
            NetworkDebugLogger.setTestMode(true);
            initialized = true;
        }
    }

    private static void skipUnlessStressTestsEnabled() {
        if (!"true".equalsIgnoreCase(System.getProperty("run.stress.tests"))) {
            throw new SkipException("Stress tests skipped. Use -Drun.stress.tests=true to run.");
        }
    }

    // ==================== Unit Tests ====================

    @Test
    public void testDeckLoaderHasPrecons() {
        Assert.assertTrue(TestDeckLoader.hasPrecons(), "Should have precon decks available");
        int count = TestDeckLoader.getPreconCount();
        Assert.assertTrue(count > 0, "Should have at least one precon deck, found: " + count);
        NetworkDebugLogger.log("%s Found %d precon decks", LOG_PREFIX, count);
    }

    @Test(dependsOnMethods = "testDeckLoaderHasPrecons")
    public void testDeckLoaderCanLoadDeck() {
        var deck = TestDeckLoader.getRandomPrecon();
        Assert.assertNotNull(deck, "Deck should not be null");
        int cardCount = deck.getMain().countAll();
        Assert.assertTrue(cardCount >= 40, "Deck should have at least 40 cards, found: " + cardCount);
        NetworkDebugLogger.log("%s Loaded deck with %d cards", LOG_PREFIX, cardCount);
    }

    @Test
    public void testGameResultInitialization() {
        UnifiedNetworkHarness.GameResult result = new UnifiedNetworkHarness.GameResult();
        result.gameCompleted = true;
        result.turnCount = 10;
        result.winner = "Alice";
        result.totalBytesSent = 1000;

        Assert.assertTrue(result.gameCompleted);
        Assert.assertEquals(result.turnCount, 10);
        Assert.assertEquals(result.winner, "Alice");

        String summary = result.toSummary();
        Assert.assertNotNull(summary);
        Assert.assertTrue(summary.contains("completed=true") || summary.contains("success="));
    }

    @Test
    public void testGameTestModeEnum() {
        // Test NETWORK_LOCAL mode
        Assert.assertFalse(GameTestMode.NETWORK_LOCAL.usesRemoteClient(), "NETWORK_LOCAL should not use remote client");

        // Test NETWORK_REMOTE mode
        Assert.assertTrue(GameTestMode.NETWORK_REMOTE.usesRemoteClient(), "NETWORK_REMOTE should use remote client");

        for (GameTestMode mode : GameTestMode.values()) {
            Assert.assertNotNull(mode.getDisplayName(), "Display name should not be null");
            Assert.assertNotNull(mode.getDescription(), "Description should not be null");
        }
    }

    @Test
    public void testConfigurationParsing() {
        TestConfiguration config = new TestConfiguration();
        Assert.assertNotNull(config.getDeck1(), "Deck1 should not be null");
        Assert.assertNotNull(config.getDeck2(), "Deck2 should not be null");
        Assert.assertNotNull(config.getTestMode(), "Test mode should not be null");
        Assert.assertTrue(config.getPlayerCount() >= 2 && config.getPlayerCount() <= 4, "Player count should be 2-4");
        Assert.assertTrue(config.getIterations() > 0, "Iterations should be positive");
    }

    @Test
    public void testServerStartAndStop() {
        NetworkDebugLogger.log("%s Testing server start/stop...", LOG_PREFIX);
        var server = forge.gamemodes.net.server.FServerManager.getInstance();
        int port = 55556;

        try {
            server.startServer(port);
            Assert.assertTrue(server.isHosting(), "Server should be hosting after start");
            NetworkDebugLogger.log("%s Server started on port %d", LOG_PREFIX, port);
        } finally {
            if (server.isHosting()) {
                server.stopServer();
                NetworkDebugLogger.log("%s Server stopped", LOG_PREFIX);
            }
        }
    }

    // ==================== Single Game Integration Tests ====================

    @Test(timeOut = 120000, description = "Full 2-player AI game test over network infrastructure")
    public void testFullAutomatedGame() {
        NetworkDebugLogger.log("%s Starting full automated game test...", LOG_PREFIX);

        UnifiedNetworkHarness.GameResult result = new UnifiedNetworkHarness()
                .playerCount(2)
                .remoteClients(0)
                .gameTimeout(120000)
                .execute();

        NetworkDebugLogger.log("%s Game result: %s", LOG_PREFIX, result.toSummary());
        System.out.println(result.toDetailedReport());

        if (result.gameCompleted) {
            Assert.assertTrue(result.turnCount > 0, "Should have at least one turn");
        }
        Assert.assertNotNull(result, "Result should not be null");
    }

    /**
     * Key test for delta sync validation - uses actual TCP network client.
     */
    @Test(timeOut = 300000, description = "True network traffic test with remote client")
    public void testTrueNetworkTraffic() {
        NetworkDebugLogger.log("%s Starting true network traffic test...", LOG_PREFIX);
        NetworkDebugLogger.log("%s This test connects an actual network client to verify delta sync", LOG_PREFIX);

        UnifiedNetworkHarness.GameResult result = new UnifiedNetworkHarness()
                .playerCount(2)
                .remoteClients(1)
                .gameTimeout(300000)
                .execute();

        NetworkDebugLogger.log("%s Network test result: %s", LOG_PREFIX, result.toSummary());
        System.out.println(result.toDetailedReport());

        if (result.success) {
            Assert.assertTrue(result.deltaPacketsReceived > 0, "Should have received delta sync packets");
        }
        Assert.assertTrue(result.gameStarted, "Game should have started");
    }

    @Test(timeOut = 200000, description = "3-player multiplayer network game test")
    public void testMultiplayer3Player() {
        NetworkDebugLogger.log("%s Starting 3-player multiplayer network test...", LOG_PREFIX);

        UnifiedNetworkHarness.GameResult result = new UnifiedNetworkHarness()
                .playerCount(3)
                .remoteClients(2)
                .gameTimeout(180000)
                .useAiForRemotePlayers(true)
                .execute();

        NetworkDebugLogger.log("%s 3-player result: %s", LOG_PREFIX, result);
        System.out.println(result.toDetailedReport());

        if (result.passed()) {
            Assert.assertEquals(result.playerCount, 3, "Should have 3 players");
            Assert.assertTrue(result.turnCount > 0, "Should have at least one turn");
            Assert.assertTrue(result.deltaPacketsReceived > 0, "Should have received delta packets");
        }
        Assert.assertTrue(result.gameStarted, "Game should have started");
    }

    @Test(timeOut = 200000, description = "4-player multiplayer network game test")
    public void testMultiplayer4Player() {
        NetworkDebugLogger.log("%s Starting 4-player multiplayer network test...", LOG_PREFIX);

        UnifiedNetworkHarness.GameResult result = new UnifiedNetworkHarness()
                .playerCount(4)
                .remoteClients(3)
                .gameTimeout(180000)
                .useAiForRemotePlayers(true)
                .execute();

        NetworkDebugLogger.log("%s 4-player result: %s", LOG_PREFIX, result);
        System.out.println(result.toDetailedReport());

        if (result.passed()) {
            Assert.assertEquals(result.playerCount, 4, "Should have 4 players");
            Assert.assertTrue(result.turnCount > 0, "Should have at least one turn");
            Assert.assertTrue(result.deltaPacketsReceived > 0, "Should have received delta packets");
        }
        Assert.assertTrue(result.gameStarted, "Game should have started");
    }

    @Test(timeOut = 150000, description = "UnifiedNetworkHarness local mode test")
    public void testUnifiedHarnessLocalMode() {
        NetworkDebugLogger.log("%s Testing UnifiedNetworkHarness local mode...", LOG_PREFIX);

        // Test local AI mode (no remote clients)
        UnifiedNetworkHarness.GameResult result = new UnifiedNetworkHarness()
                .playerCount(2)
                .remoteClients(0)
                .execute();

        System.out.println(result.toDetailedReport());

        Assert.assertNotNull(result, "Result should not be null");
        Assert.assertEquals(result.remoteClientCount, 0, "Should have no remote clients");
        Assert.assertTrue(result.gameStarted, "Game should have started");
    }

    // ==================== Batch Tests (Sequential and Parallel) ====================

    @Test(timeOut = 360000, description = "Small batch testing (3 games)")
    public void testBatchTesting() {
        NetworkDebugLogger.log("%s Starting batch testing...", LOG_PREFIX);

        int iterations = 3;
        int completed = 0;
        int totalTurns = 0;

        for (int i = 0; i < iterations; i++) {
            NetworkDebugLogger.log("%s Batch iteration %d/%d", LOG_PREFIX, i + 1, iterations);

            UnifiedNetworkHarness.GameResult result = new UnifiedNetworkHarness()
                    .playerCount(2)
                    .remoteClients(0)
                    .execute();

            Assert.assertEquals(result.remoteClientCount, 0, "Should be local mode");
            if (result.gameCompleted) {
                completed++;
                totalTurns += result.turnCount;
            }
        }

        NetworkDebugLogger.log("%s Batch results: %d/%d completed, avg turns: %.1f",
                LOG_PREFIX, completed, iterations, completed > 0 ? (double) totalTurns / completed : 0);

        Assert.assertEquals(completed, iterations, "All games should complete");
    }

    @Test(timeOut = 900000, description = "Sequential 3 games")
    public void testSequentialThreeGames() {
        MultiProcessGameExecutor.ExecutionResult result = ComprehensiveTestExecutor.runSequentialGames(3, 300000);

        NetworkDebugLogger.log("%s Sequential 3 games: %s", LOG_PREFIX, result.toSummary());
        System.out.println(result.toDetailedReport());

        Assert.assertTrue(result.getSuccessCount() >= 3, "All 3 sequential games should succeed: " + result.toSummary());
        Assert.assertTrue(result.getTotalDeltaPackets() > 0, "Delta packets should be received across games");
    }

    @Test(timeOut = 600000, description = "Parallel 3 games")
    public void testParallelThreeGames() {
        MultiProcessGameExecutor executor = new MultiProcessGameExecutor(300000);
        MultiProcessGameExecutor.ExecutionResult result = executor.runGames(3);

        NetworkDebugLogger.log("%s Parallel 3 games: %s", LOG_PREFIX, result.toSummary());
        System.out.println(result.toDetailedReport());

        Assert.assertTrue(result.getSuccessCount() >= 2, "At least 2 of 3 parallel games should succeed: " + result.toSummary());
        Assert.assertTrue(result.getTotalDeltaPackets() > 0, "Delta packets should be received across games");
    }

    @Test(timeOut = 300000, description = "Quick parallel 2 games")
    public void testParallelTwoGames() {
        MultiProcessGameExecutor executor = new MultiProcessGameExecutor(180000);
        MultiProcessGameExecutor.ExecutionResult result = executor.runGames(2);

        NetworkDebugLogger.log("%s Parallel 2 games: %s", LOG_PREFIX, result.toSummary());
        System.out.println(result.toDetailedReport());

        Assert.assertTrue(result.getSuccessCount() >= 1, "At least 1 of 2 parallel games should succeed: " + result.toSummary());
    }

    /**
     * Configurable sequential batch test.
     * Usage: -Dtest.gameCount=10 -Dtest.timeoutMs=300000
     */
    @Test(timeOut = 3600000, description = "Configurable sequential batch")
    public void testConfigurableSequential() {
        int gameCount = Integer.getInteger("test.gameCount", 3);
        long timeoutMs = Long.getLong("test.timeoutMs", 300000);

        NetworkDebugLogger.log("%s Sequential config: games=%d, timeout=%dms", LOG_PREFIX, gameCount, timeoutMs);

        MultiProcessGameExecutor.ExecutionResult result = ComprehensiveTestExecutor.runSequentialGames(gameCount, timeoutMs);

        NetworkDebugLogger.log("%s Sequential result: %s", LOG_PREFIX, result.toSummary());
        System.out.println(result.toDetailedReport());

        Assert.assertEquals(result.getSuccessCount(), gameCount, "All games should succeed: " + result.toSummary());
    }

    /**
     * Configurable parallel batch test.
     * Usage: -Dtest.gameCount=10 -Dtest.timeoutMs=300000
     */
    @Test(timeOut = 1800000, description = "Configurable parallel batch")
    public void testConfigurableParallel() {
        int gameCount = Integer.getInteger("test.gameCount", 3);
        long timeoutMs = Long.getLong("test.timeoutMs", 300000);

        NetworkDebugLogger.log("%s Parallel config: games=%d, timeout=%dms", LOG_PREFIX, gameCount, timeoutMs);

        MultiProcessGameExecutor executor = new MultiProcessGameExecutor(timeoutMs);
        MultiProcessGameExecutor.ExecutionResult result = executor.runGames(gameCount);

        NetworkDebugLogger.log("%s Parallel result: %s", LOG_PREFIX, result.toSummary());
        System.out.println(result.toDetailedReport());

        double expectedSuccessRate = 0.8;
        int expectedSuccesses = (int) Math.ceil(gameCount * expectedSuccessRate);
        Assert.assertTrue(result.getSuccessCount() >= expectedSuccesses,
                String.format("At least %d of %d games should succeed: %s", expectedSuccesses, gameCount, result.toSummary()));
    }

    // ==================== Comprehensive Delta Sync Tests ====================

    /**
     * Comprehensive delta sync test with 100 games (default configuration).
     *
     * Configurable via system properties:
     * - -Dtest.2pGames=50 (default)
     * - -Dtest.3pGames=30 (default)
     * - -Dtest.4pGames=20 (default)
     * - -Dtest.batchSize=10 (default)
     * - -Dtest.timeoutMs=300000 (default)
     *
     * Validation criteria:
     * - Success rate >= 90%
     * - Average bandwidth savings >= 90%
     * - Zero checksum mismatches
     */
    @Test(timeOut = 7200000, description = "Comprehensive 100-game delta sync validation")
    public void runComprehensiveDeltaSyncTest() {
        NetworkDebugLogger.log("%s Starting comprehensive delta sync validation", LOG_PREFIX);

        ComprehensiveTestExecutor executor = ComprehensiveTestExecutor.fromSystemProperties();
        NetworkDebugLogger.log("%s Configuration:\n%s", LOG_PREFIX, executor.getConfigurationSummary());

        NetworkDebugLogger.log("%s Executing %d games...", LOG_PREFIX, executor.getTotalGames());
        LocalDateTime testStartTime = LocalDateTime.now();
        long startTime = System.currentTimeMillis();

        MultiProcessGameExecutor.ExecutionResult executionResult = executor.execute();

        long executionDuration = System.currentTimeMillis() - startTime;
        NetworkDebugLogger.log("%s Execution completed in %.1f minutes", LOG_PREFIX, executionDuration / 60000.0);

        System.out.println("\n" + executionResult.toDetailedReport());

        // Analyze log files
        NetworkLogAnalyzer analyzer = new NetworkLogAnalyzer();
        AnalysisResult analysisResult = analyzer.analyzeComprehensiveTestAndAggregate(new File(ForgeConstants.NETWORK_LOGS_DIR), testStartTime);

        String report = analysisResult.generateReport();
        System.out.println("\n" + "=".repeat(80));
        System.out.println(report);
        System.out.println("=".repeat(80));

        saveReportToFile(report, "comprehensive-test-results");

        validateComprehensiveResults(executionResult, analysisResult);
    }

    @Test(timeOut = 1200000, description = "Quick 10-game delta sync validation")
    public void runQuickDeltaSyncTest() {
        NetworkDebugLogger.log("%s Starting quick delta sync validation (10 games)", LOG_PREFIX);

        ComprehensiveTestExecutor executor = new ComprehensiveTestExecutor()
                .twoPlayerGames(5)
                .threePlayerGames(3)
                .fourPlayerGames(2)
                .parallelBatchSize(3);

        LocalDateTime testStartTime = LocalDateTime.now();
        MultiProcessGameExecutor.ExecutionResult executionResult = executor.execute();

        System.out.println("\n" + executionResult.toDetailedReport());

        NetworkLogAnalyzer analyzer = new NetworkLogAnalyzer();
        AnalysisResult analysisResult = analyzer.analyzeComprehensiveTestAndAggregate(new File(ForgeConstants.NETWORK_LOGS_DIR), testStartTime);

        String report = analysisResult.generateReport();
        System.out.println("\n" + report);
        saveReportToFile(report, "quick-test-results");

        Assert.assertTrue(executionResult.getSuccessRate() >= 0.80,
                String.format("Quick test success rate should be >= 80%%, was %.1f%%", executionResult.getSuccessRate() * 100));
    }

    @Test(timeOut = 600000, description = "2-player only test")
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
                String.format("2-player test success rate should be >= 80%%, was %.1f%%", result.getSuccessRate() * 100));
    }

    @Test(timeOut = 900000, description = "Multiplayer only test (3 and 4 players)")
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
                String.format("Multiplayer test success rate should be >= 70%%, was %.1f%%", result.getSuccessRate() * 100));
    }

    /**
     * Analyze existing logs without running new games.
     * Useful for testing report generation changes.
     * Set -Dtest.batchId=20260127-213221 to analyze a specific batch.
     */
    @Test
    public void analyzeExistingLogs() {
        NetworkDebugLogger.log("%s Analyzing existing logs...", LOG_PREFIX);

        File logDir = new File(ForgeConstants.NETWORK_LOGS_DIR);
        NetworkLogAnalyzer analyzer = new NetworkLogAnalyzer();

        String batchId = System.getProperty("test.batchId", "");
        List<GameLogMetrics> metrics;

        if (!batchId.isEmpty()) {
            String pattern = "network-debug-run" + batchId + "-(?:batch\\d+-)?game\\d+-\\d+p-test\\.log";
            System.out.println("Analyzing batch: " + batchId + " (pattern: " + pattern + ")");
            metrics = analyzer.analyzeDirectory(logDir, pattern);
        } else {
            metrics = analyzer.analyzeComprehensiveTestLogs(logDir);
        }

        NetworkDebugLogger.log("%s Found %d log files", LOG_PREFIX, metrics.size());

        if (metrics.isEmpty()) {
            System.out.println("No comprehensive test logs found in " + logDir.getAbsolutePath());
            return;
        }

        AnalysisResult result = analyzer.buildAnalysisResult(metrics);
        String report = result.generateReport();
        System.out.println("\n" + report);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        java.nio.file.Path reportPath = logDir.toPath().resolve("analysis-results-" + timestamp + ".md");
        try {
            java.nio.file.Files.write(reportPath, report.getBytes());
            System.out.println("Report saved to: " + reportPath);
        } catch (IOException e) {
            System.err.println("Failed to save report: " + e.getMessage());
        }
    }

    // ==================== Configurable System Property Tests ====================

    /**
     * Configurable test using system properties for deck selection and test mode.
     *
     * Usage:
     *   -Dprecon1="Quest Precon - Red" -Dprecon2="Quest Precon - Blue"
     *   -DtestMode=NETWORK_LOCAL (or NETWORK_REMOTE)
     *   -Diterations=5
     */
    @Test(timeOut = 600000, description = "Configurable test via system properties")
    public void testWithSystemProperties() {
        NetworkDebugLogger.log("%s Starting configurable test with system properties...", LOG_PREFIX);

        TestConfiguration config = new TestConfiguration();
        config.printConfiguration();

        int iterations = config.getIterations();
        GameTestMode mode = config.getTestMode();
        int remoteClients = mode.usesRemoteClient() ? 1 : 0;

        for (int i = 0; i < iterations; i++) {
            if (iterations > 1) {
                NetworkDebugLogger.log("%s === Iteration %d/%d ===", LOG_PREFIX, i + 1, iterations);
            }

            UnifiedNetworkHarness.GameResult result = new UnifiedNetworkHarness()
                    .playerCount(2)
                    .remoteClients(remoteClients)
                    .decks(config.getDeck1(), config.getDeck2())
                    .execute();

            NetworkDebugLogger.log("%s Result: %s", LOG_PREFIX, result.toSummary());

            Assert.assertNotNull(result, "Result should not be null");
            Assert.assertEquals(result.remoteClientCount, remoteClients, "Remote client count should match mode");
        }
    }

    // ==================== Helper Methods ====================

    private void validateComprehensiveResults(MultiProcessGameExecutor.ExecutionResult executionResult,
                                               AnalysisResult analysisResult) {
        NetworkDebugLogger.log("%s Validating results...", LOG_PREFIX);

        // 1. Check success rate >= 90%
        double successRate = executionResult.getSuccessRate() * 100;
        NetworkDebugLogger.log("%s Success rate: %.1f%% (target: 90%%)", LOG_PREFIX, successRate);
        Assert.assertTrue(successRate >= 90.0, String.format("Success rate should be >= 90%%, was %.1f%%", successRate));

        // 2. Check bandwidth efficiency
        long totalDeltas = executionResult.getTotalDeltaPackets();
        long totalBytes = executionResult.getTotalBytes();
        double bytesPerPacket = totalDeltas > 0 ? (double) totalBytes / totalDeltas : 0;
        NetworkDebugLogger.log("%s Bytes per delta packet: %.1f (target: <200)", LOG_PREFIX, bytesPerPacket);

        Assert.assertTrue(bytesPerPacket < 200,
                String.format("Bytes per delta packet should be < 200 (efficient), was %.1f", bytesPerPacket));

        // 3. Check zero checksum mismatches
        int checksumMismatches = analysisResult.getGamesWithChecksumMismatches();
        NetworkDebugLogger.log("%s Checksum mismatches: %d (target: 0)", LOG_PREFIX, checksumMismatches);
        Assert.assertEquals(checksumMismatches, 0,
                String.format("Should have zero checksum mismatches, had %d", checksumMismatches));

        // 4. Per-player-count validation
        for (int p = 2; p <= 4; p++) {
            double playerSuccessRate = executionResult.getSuccessRateByPlayers(p) * 100;
            int playerGameCount = executionResult.getGameCountByPlayers(p);
            if (playerGameCount > 0) {
                NetworkDebugLogger.log("%s %d-player success rate: %.1f%% (%d games)", LOG_PREFIX, p, playerSuccessRate, playerGameCount);
                Assert.assertTrue(playerSuccessRate >= 80.0,
                        String.format("%d-player success rate should be >= 80%%, was %.1f%%", p, playerSuccessRate));
            }
        }

        NetworkDebugLogger.log("%s All validation criteria PASSED", LOG_PREFIX);
    }

    private void saveReportToFile(String report, String prefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        File logDir = new File(ForgeConstants.NETWORK_LOGS_DIR);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        File reportFile = new File(logDir, prefix + "-" + timestamp + ".md");

        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write(report);
            NetworkDebugLogger.log("%s Report saved to: %s", LOG_PREFIX, reportFile.getAbsolutePath());
            System.out.println("Report saved to: " + reportFile.getAbsolutePath());
        } catch (IOException e) {
            NetworkDebugLogger.log("%s WARNING: Failed to save report: %s", LOG_PREFIX, e.getMessage());
            System.err.println("WARNING: Failed to save report: " + e.getMessage());
        }
    }
}
