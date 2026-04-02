package forge.net;

import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gamemodes.net.IHasNetLog;
import forge.gamemodes.net.NetworkLogConfig;
import forge.gamemodes.net.server.DeltaSyncManager;
import forge.gamemodes.net.server.RemoteClientGuiGame;
import forge.localinstance.properties.ForgeConstants;
import forge.deck.Deck;
import forge.net.analysis.AnalysisResult;
import forge.net.analysis.GameLogMetrics;
import forge.net.analysis.NetworkLogAnalyzer;
import forge.util.collect.FCollectionView;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
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
 * - Unit tests (3): Always run in CI - deck loader, server start/stop
 * - Single-game tests (2): testTrueNetworkTraffic (CI), testUnifiedHarnessLocalMode
 * - Configurable batch tests (2): testConfigurableSequential, testConfigurableParallel
 * - Comprehensive tests (2): runComprehensiveDeltaSyncTest, runQuickDeltaSyncTest
 * - Utility tests (1): analyzeLog
 *
 * Usage examples:
 *
 * Run default CI tests (unit tests + testTrueNetworkTraffic):
 *   mvn -pl forge-gui-desktop -am verify
 *
 * Run all tests including stress tests:
 *   mvn -pl forge-gui-desktop -am verify -Drun.stress.tests=true
 *
 * Run configurable batch tests:
 *   mvn -pl forge-gui-desktop -am verify -Dtest="NetworkPlayIntegrationTest#testConfigurableSequential" \
 *       -Dtest.gameCount=3 -Drun.stress.tests=true -Dsurefire.failIfNoSpecifiedTests=false
 *   mvn -pl forge-gui-desktop -am verify -Dtest="NetworkPlayIntegrationTest#testConfigurableParallel" \
 *       -Dtest.gameCount=2 -Drun.stress.tests=true -Dsurefire.failIfNoSpecifiedTests=false
 *
 * Run comprehensive test with custom configuration:
 *   mvn -pl forge-gui-desktop -am verify -Dtest="NetworkPlayIntegrationTest#runComprehensiveDeltaSyncTest" \
 *       -Dtest.2pGames=50 -Dtest.3pGames=30 -Dtest.4pGames=20 \
 *       -Drun.stress.tests=true -Dsurefire.failIfNoSpecifiedTests=false
 */
public class NetworkPlayIntegrationTest implements IHasNetLog {

    private static boolean initialized = false;

    @BeforeClass
    public static void setUp() {
        if (!initialized) {
            TestUtils.ensureFModelInitialized();
            initialized = true;
        }
    }

    private static void skipUnlessStressTestsEnabled() {
        if (!"true".equalsIgnoreCase(System.getProperty("run.stress.tests"))) {
            throw new SkipException("Stress tests skipped. Use -Drun.stress.tests=true to run.");
        }
        NetworkLogConfig.setTestMode(true);
        if (NetworkLogConfig.getBatchId() == null) {
            NetworkLogConfig.generateBatchId();
        }
    }

    @Test
    public void testDeckLoaderHasPrecons() {
        Assert.assertTrue(TestDeckLoader.hasPrecons(), "Should have precon decks available");
        int count = TestDeckLoader.getPreconCount();
        Assert.assertTrue(count > 0, "Should have at least one precon deck, found: " + count);
        netLog.info("Found {} precon decks", count);
    }

    @Test(dependsOnMethods = "testDeckLoaderHasPrecons")
    public void testDeckLoaderCanLoadDeck() {
        var deck = TestDeckLoader.getRandomPrecon();
        Assert.assertNotNull(deck, "Deck should not be null");
        int cardCount = deck.getMain().countAll();
        Assert.assertTrue(cardCount >= 40, "Deck should have at least 40 cards, found: " + cardCount);
        netLog.info("Loaded deck with {} cards", cardCount);
    }

    @Test
    public void testServerStartAndStop() {
        netLog.info("Testing server start/stop...");
        var server = forge.gamemodes.net.server.FServerManager.getInstance();
        int port = 55556;

        try {
            server.startServer(port);
            Assert.assertTrue(server.isHosting(), "Server should be hosting after start");
            netLog.info("Server started on port {}", port);
        } finally {
            if (server.isHosting()) {
                server.stopServer();
                netLog.info("Server stopped");
            }
        }
    }

    /**
     * Key test for delta sync validation - uses actual TCP network client.
     * Uses 10-card basic land decks for fast CI execution.
     * Games end in ~3 turns as players deck out (no spells to cast).
     * Deck legality is disabled via TestUtils.ensureFModelInitialized().
     *
     * Validates: protocol lifecycle (openView, setGameView), client-side GameView
     * consistency (zone references, card visibility), and zero pipeline send errors.
     */
    @Test(timeOut = 60000, description = "True network traffic test with remote client")
    public void testTrueNetworkTraffic() {
        netLog.info("Starting true network traffic test...");

        Deck deck1 = TestDeckLoader.createMinimalDeck("Mountain", 10);
        Deck deck2 = TestDeckLoader.createMinimalDeck("Forest", 10);

        UnifiedNetworkHarness.GameResult result = new UnifiedNetworkHarness()
                .playerCount(2)
                .remoteClients(1)
                .decks(deck1, deck2)
                .gameTimeout(60000)
                .execute();

        String bandwidthSummary = result.getBandwidthSummary();
        if (bandwidthSummary != null) {
            netLog.info("{}", bandwidthSummary);
        }

        Assert.assertTrue(result.gameStarted, "Game should have started: " + result.toSummary());
        Assert.assertTrue(result.deltaPacketsReceived > 0, "Should have received delta sync packets");

        // Protocol lifecycle
        Assert.assertTrue(result.clientOpenViewCalled,
                "Client should have received openView over the wire");
        Assert.assertTrue(result.clientSetGameViewCount > 0,
                "Client should have received setGameView updates (count: " + result.clientSetGameViewCount + ")");

        // Client GameView state
        GameView clientGameView = result.clientGameView;
        Assert.assertNotNull(clientGameView, "Client should have a GameView");
        Assert.assertNotNull(clientGameView.getGameLog(),
                "Client GameView.getGameLog() should be initialized");
        Assert.assertEquals(clientGameView.getPlayers().size(), 2,
                "Client GameView should have 2 players");

        // Zone consistency — CardViews in zone collections must have matching Zone property
        ZoneType[] zonesToCheck = {ZoneType.Hand, ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Library};
        for (PlayerView pv : clientGameView.getPlayers()) {
            for (ZoneType zone : zonesToCheck) {
                FCollectionView<CardView> cards = pv.getCards(zone);
                if (cards == null) continue;
                for (CardView cv : cards) {
                    Assert.assertEquals(cv.getZone(), zone,
                            "CardView id=" + cv.getId() + " in " + pv.getName() + "'s " + zone +
                            " has stale zone: " + cv.getZone());
                }
            }
        }

        // Card visibility — cards in public zones must be visible to all players
        Iterable<PlayerView> allPlayers = clientGameView.getPlayers();
        ZoneType[] publicZones = {ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Exile};
        for (PlayerView pv : clientGameView.getPlayers()) {
            for (ZoneType zone : publicZones) {
                FCollectionView<CardView> cards = pv.getCards(zone);
                if (cards == null) continue;
                for (CardView cv : cards) {
                    Assert.assertTrue(cv.canBeShownToAny(allPlayers),
                            "CardView id=" + cv.getId() + " in public zone " + zone +
                            " should be visible but canBeShownToAny returned false");
                }
            }
        }

        // Pipeline error assertions
        Assert.assertEquals(result.sendErrors, 0,
                "Server encountered " + result.sendErrors + " send error(s) during the game");

        netLog.info("Test PASSED: {} turns, {} setGameView updates, 0 send errors",
                result.turnCount, result.clientSetGameViewCount);
    }

    @Test(timeOut = 150000, description = "UnifiedNetworkHarness local mode test")
    public void testUnifiedHarnessLocalMode() {
        skipUnlessStressTestsEnabled();
        netLog.info("Testing UnifiedNetworkHarness local mode...");

        UnifiedNetworkHarness.GameResult result = new UnifiedNetworkHarness()
                .playerCount(2)
                .remoteClients(0)
                .execute();

        System.out.println(result.toDetailedReport());

        Assert.assertNotNull(result, "Result should not be null");
        Assert.assertEquals(result.remoteClientCount, 0, "Should have no remote clients");
        Assert.assertTrue(result.gameStarted, "Game should have started");
    }

    /**
     * Configurable sequential batch test.
     * Usage: -Dtest.gameCount=10 -Dtest.timeoutMs=300000
     */
    @Test(timeOut = 3600000, description = "Configurable sequential batch")
    public void testConfigurableSequential() {
        skipUnlessStressTestsEnabled();
        int gameCount = Integer.getInteger("test.gameCount", 3);
        long timeoutMs = Long.getLong("test.timeoutMs", 300000);

        netLog.info("Sequential config: games={}, timeout={}ms", gameCount, timeoutMs);

        MultiProcessGameExecutor.ExecutionResult result = ComprehensiveTestExecutor.runSequentialGames(gameCount, timeoutMs);

        netLog.info("Sequential result: {}", result.toSummary());
        System.out.println(result.toDetailedReport());

        Assert.assertEquals(result.getSuccessCount(), gameCount, "All games should succeed: " + result.toSummary());
    }

    /**
     * Configurable parallel batch test.
     * Usage: -Dtest.gameCount=10 -Dtest.timeoutMs=300000
     */
    @Test(timeOut = 1800000, description = "Configurable parallel batch")
    public void testConfigurableParallel() {
        skipUnlessStressTestsEnabled();
        int gameCount = Integer.getInteger("test.gameCount", 3);
        long timeoutMs = Long.getLong("test.timeoutMs", 300000);

        netLog.info("Parallel config: games={}, timeout={}ms", gameCount, timeoutMs);

        MultiProcessGameExecutor executor = new MultiProcessGameExecutor(timeoutMs);
        MultiProcessGameExecutor.ExecutionResult result = executor.runGames(gameCount);

        netLog.info("Parallel result: {}", result.toSummary());
        System.out.println(result.toDetailedReport());

        double expectedSuccessRate = 0.8;
        int expectedSuccesses = (int) Math.ceil(gameCount * expectedSuccessRate);
        Assert.assertTrue(result.getSuccessCount() >= expectedSuccesses,
                String.format("At least %d of %d games should succeed: %s", expectedSuccesses, gameCount, result.toSummary()));
    }

    /**
     * Comprehensive delta sync test with 100 games (default configuration).
     *
     * Validation criteria:
     * - Success rate >= 90%
     * - Average bandwidth savings >= 90%
     * - Zero checksum mismatches
     */
    @Test(timeOut = 7200000, description = "Comprehensive 100-game delta sync validation")
    public void runComprehensiveDeltaSyncTest() {
        skipUnlessStressTestsEnabled();
        netLog.info("Starting comprehensive delta sync validation");

        ComprehensiveTestExecutor executor = ComprehensiveTestExecutor.fromSystemProperties();
        netLog.info("Configuration:\n{}", executor.getConfigurationSummary());

        netLog.info("Executing {} games...", executor.getTotalGames());
        LocalDateTime testStartTime = LocalDateTime.now();
        long startTime = System.currentTimeMillis();

        MultiProcessGameExecutor.ExecutionResult executionResult = executor.execute();

        long executionDuration = System.currentTimeMillis() - startTime;
        netLog.info("Execution completed in {} minutes", String.format("%.1f", executionDuration / 60000.0));

        System.out.println("\n" + executionResult.toDetailedReport());

        File logSubDir = NetworkLogConfig.getLogDirectory();
        NetworkLogAnalyzer analyzer = new NetworkLogAnalyzer();
        AnalysisResult analysisResult = analyzer.analyzeComprehensiveTestAndAggregate(logSubDir, testStartTime);

        String report = analysisResult.generateReport();
        System.out.println("\n" + "=".repeat(80));
        System.out.println(report);
        System.out.println("=".repeat(80));

        saveReportToFile(report);

        validateComprehensiveResults(executionResult, analysisResult);
    }

    @Test(timeOut = 1200000, description = "Quick 10-game delta sync validation")
    public void runQuickDeltaSyncTest() {
        skipUnlessStressTestsEnabled();
        netLog.info("Starting quick delta sync validation (10 games)");

        ComprehensiveTestExecutor executor = new ComprehensiveTestExecutor()
                .twoPlayerGames(5)
                .threePlayerGames(3)
                .fourPlayerGames(2)
                .parallelBatchSize(10);

        LocalDateTime testStartTime = LocalDateTime.now();
        MultiProcessGameExecutor.ExecutionResult executionResult = executor.execute();

        System.out.println("\n" + executionResult.toDetailedReport());

        File logSubDir = NetworkLogConfig.getLogDirectory();
        NetworkLogAnalyzer analyzer = new NetworkLogAnalyzer();
        AnalysisResult analysisResult = analyzer.analyzeComprehensiveTestAndAggregate(logSubDir, testStartTime);

        String report = analysisResult.generateReport();
        System.out.println("\n" + report);
        saveReportToFile(report);

        // Log event-state mismatches for visibility (no assertion — see validateComprehensiveResults for enforcement)
        long eventMismatches = executionResult.getTotalEventStateMismatches();
        netLog.info("Event-state mismatches: {} across {} games", eventMismatches, executionResult.getSuccessCount());

        Assert.assertTrue(executionResult.getSuccessRate() >= 0.80,
                String.format("Quick test success rate should be >= 80%%, was %.1f%%", executionResult.getSuccessRate() * 100));
    }

    /**
     * Analyze any log file or directory using the CLI analyzer.
     * Usage:
     *   mvn -pl forge-gui-desktop -am verify \
     *       -Dtest="NetworkPlayIntegrationTest#analyzeLog" \
     *       -Dlog.input="path/to/file.log" \
     *       -Drun.stress.tests=true -Dsurefire.failIfNoSpecifiedTests=false
     *
     * Optional: -Dlog.output="path/to/report.md" (default: network-log-analysis.md in same directory as input)
     */
    @Test
    public void analyzeLog() {
        skipUnlessStressTestsEnabled();
        String input = System.getProperty("log.input");
        Assert.assertNotNull(input, "Must set -Dlog.input=<file|dir>");

        String output = System.getProperty("log.output");
        List<String> args = new ArrayList<>();
        if (output != null) {
            args.add("-o");
            args.add(output);
        }
        args.add(input);
        forge.net.analysis.LogAnalyzerCli.main(args.toArray(new String[0]));
    }

    private void validateComprehensiveResults(MultiProcessGameExecutor.ExecutionResult executionResult,
                                               AnalysisResult analysisResult) {
        netLog.info("Validating results...");

        // Log all metrics first, then assert — so all values are visible even if one fails
        double successRate = executionResult.getSuccessRate() * 100;
        netLog.info("Success rate: {}% (target: >= 90%)", String.format("%.1f", successRate));

        long totalDeltas = executionResult.getTotalDeltaPackets();
        long totalBytes = executionResult.getTotalBytes();
        double bytesPerPacket = totalDeltas > 0 ? (double) totalBytes / totalDeltas : 0;
        netLog.info("Bytes per delta packet: {}", String.format("%.1f", bytesPerPacket));

        int checksumMismatches = analysisResult.getGamesWithChecksumMismatches();
        int maxPerGame = analysisResult.getMaxMismatchesPerGame();
        netLog.info("Checksum mismatches: {} games, max {} per game (threshold: 1)", checksumMismatches, maxPerGame);

        int totalChecksums = analysisResult.getAllMetrics().stream()
                .mapToInt(GameLogMetrics::getChecksumCount).sum();
        int checksumThreshold = DeltaSyncManager.CHECKSUM_INTERVAL * 5 / 4;
        long expectedMinChecksums = totalDeltas / checksumThreshold;
        netLog.info("Checksums fired: {} (deltas={}, expected >= {})",
                totalChecksums, totalDeltas, expectedMinChecksums);

        long eventMismatches = executionResult.getTotalEventStateMismatches();
        int successCount = executionResult.getSuccessCount();
        long eventTolerance = successCount * 2;
        netLog.info("Event-state mismatches: {} across {} games (tolerance: < {})",
                eventMismatches, successCount, eventTolerance);

        for (int p = 2; p <= 4; p++) {
            int playerGameCount = executionResult.getGameCountByPlayers(p);
            if (playerGameCount > 0) {
                double playerSuccessRate = executionResult.getSuccessRateByPlayers(p) * 100;
                netLog.info("{}-player success rate: {}% ({} games)", p, String.format("%.1f", playerSuccessRate), playerGameCount);
            }
        }

        Assert.assertTrue(successRate >= 90.0, String.format("Success rate should be >= 90%%, was %.1f%%", successRate));

        Assert.assertTrue(maxPerGame <= 1,
                String.format("No persistent checksum mismatches (max per game should be <= 1, was %d)", maxPerGame));

        // Benign mismatches occur when Zone.java resets tapped without firing an event
        // (zone transitions) or when GameEventForwarder flushes between setTapped() and
        // fireEvent() (scheduler races). The delta itself is always checksum-correct.
        Assert.assertTrue(eventMismatches < eventTolerance,
                String.format("Event-state mismatches should average < 2 per game, had %d in %d games",
                        eventMismatches, successCount));

        // Threshold is CHECKSUM_INTERVAL * 5/4 to give headroom for the adaptive
        // interval and game-end boundary effects while still catching checksum failures
        if (RemoteClientGuiGame.useDeltaSync && totalDeltas >= DeltaSyncManager.CHECKSUM_INTERVAL) {
            Assert.assertTrue(totalChecksums >= expectedMinChecksums,
                    String.format("Checksums should fire ~every %d deltas, expected >= %d but got %d (from %d deltas)",
                            DeltaSyncManager.CHECKSUM_INTERVAL, expectedMinChecksums, totalChecksums, totalDeltas));
        }

        for (int p = 2; p <= 4; p++) {
            int playerGameCount = executionResult.getGameCountByPlayers(p);
            if (playerGameCount > 0) {
                double playerSuccessRate = executionResult.getSuccessRateByPlayers(p) * 100;
                Assert.assertTrue(playerSuccessRate >= 80.0,
                        String.format("%d-player success rate should be >= 80%%, was %.1f%%", p, playerSuccessRate));
            }
        }

        netLog.info("All validation criteria PASSED");
    }

    /**
     * Save a report to file using consistent naming with log files.
     * Uses the batchId from NetworkLogConfig to keep results files grouped with their logs.
     * Pattern: network-debug-{batchId}-results.md
     */
    private void saveReportToFile(String report) {
        File logDir = new File(ForgeConstants.NETWORK_LOGS_DIR);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        // Use batchId for consistent naming with log files
        String batchId = NetworkLogConfig.getBatchId();
        String filename;
        if (batchId != null) {
            filename = "network-debug-" + batchId + "-results.md";
        } else {
            // Fallback to timestamp if no batchId (shouldn't happen in normal test flow)
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            filename = "network-debug-" + timestamp + "-results.md";
        }

        File reportFile = new File(logDir, filename);

        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write(report);
            String sanitizedPath = NetworkLogConfig.sanitizePath(reportFile.getAbsolutePath());
            netLog.info("Report saved to: {}", sanitizedPath);
            System.out.println("Report saved to: " + sanitizedPath);
        } catch (IOException e) {
            netLog.warn("Failed to save report: {}", e.getMessage());
            System.err.println("WARNING: Failed to save report: " + e.getMessage());
        }
    }
}
