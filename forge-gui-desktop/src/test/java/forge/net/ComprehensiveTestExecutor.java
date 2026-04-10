package forge.net;

import forge.gamemodes.net.IHasNetLog;
import forge.gamemodes.net.NetworkLogConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Orchestrates batch testing with configurable player-count distribution.
 * Builds a shuffled schedule of 2/3/4-player games and executes them
 * them via {@link MultiProcessGameExecutor} (parallel, separate JVM
 * per game) or sequentially via {@link UnifiedNetworkHarness} (same JVM,
 * useful for debugging).
 */
public class ComprehensiveTestExecutor implements IHasNetLog {

    private static final int BASE_PORT = 58000;

    // Default game distribution (small; presets override for comprehensive/quick tests)
    private int twoPlayerGames = 3;
    private int threePlayerGames = 0;
    private int fourPlayerGames = 0;

    // Format distribution
    private int commanderPercentage = 30; // 30% of games use Commander format

    // Execution settings
    private int parallelBatchSize = 10; // Run 10 games at a time
    private long gameTimeoutMs = 300000; // 5 minutes per game
    private boolean sequential = false; // Run sequentially instead of parallel

    public ComprehensiveTestExecutor twoPlayerGames(int count) {
        this.twoPlayerGames = count;
        return this;
    }

    public ComprehensiveTestExecutor threePlayerGames(int count) {
        this.threePlayerGames = count;
        return this;
    }

    public ComprehensiveTestExecutor fourPlayerGames(int count) {
        this.fourPlayerGames = count;
        return this;
    }

    public ComprehensiveTestExecutor commanderPercentage(int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Commander percentage must be 0-100, got: " + percentage);
        }
        this.commanderPercentage = percentage;
        return this;
    }

    public ComprehensiveTestExecutor parallelBatchSize(int size) {
        this.parallelBatchSize = size;
        return this;
    }

    public ComprehensiveTestExecutor gameTimeout(long timeoutMs) {
        this.gameTimeoutMs = timeoutMs;
        return this;
    }

    public ComprehensiveTestExecutor sequential(boolean seq) {
        this.sequential = seq;
        return this;
    }

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
        netLog.info("Starting comprehensive test: {} total games ({})", totalGames, mode);
        netLog.info("Distribution: {} x 2-player, {} x 3-player, {} x 4-player",
                twoPlayerGames, threePlayerGames, fourPlayerGames);
        netLog.info("Commander percentage: {}%", commanderPercentage);
        netLog.info("Batch size: {}, Timeout: {}ms", parallelBatchSize, gameTimeoutMs);

        long startTime = System.currentTimeMillis();

        int[] playerCounts = buildShuffledPlayerCounts();
        boolean[] commanderFlags = buildCommanderFlags(totalGames);

        int commanderCount = 0;
        for (boolean f : commanderFlags) {
            if (f) commanderCount++;
        }
        netLog.info("Commander games: {}/{}", commanderCount, totalGames);

        MultiProcessGameExecutor.ExecutionResult result;

        if (sequential) {
            result = executeSequentially(playerCounts, commanderFlags);
        } else {
            result = new MultiProcessGameExecutor(gameTimeoutMs).
                    runGamesInBatches(playerCounts, commanderFlags, parallelBatchSize);
        }

        long duration = System.currentTimeMillis() - startTime;
        netLog.info("Comprehensive test completed in {} ms ({} minutes)",
                duration, String.format("%.1f", duration / 60000.0));

        return result;
    }

    private MultiProcessGameExecutor.ExecutionResult executeSequentially(int[] playerCounts, boolean[] commanderFlags) {
        ensureFModelInitialized();

        netLog.info("Starting {} sequential games", playerCounts.length);

        MultiProcessGameExecutor.ExecutionResult result = new MultiProcessGameExecutor.ExecutionResult(playerCounts.length);
        int port = BASE_PORT;

        for (int i = 0; i < playerCounts.length; i++) {
            int players = playerCounts[i];
            boolean commander = commanderFlags != null && i < commanderFlags.length && commanderFlags[i];
            UnifiedNetworkHarness.GameResult gameResult = runSingleGame(i, port++, players, commander);

            if (gameResult.errorMessage != null && !gameResult.success) {
                result.addError(i, gameResult.errorMessage);
            } else {
                result.addResult(i, gameResult);
            }

            String status = gameResult.success ? "SUCCESS" : "FAILED";
            String formatLabel = commander ? " [Cmdr]" : "";
            netLog.info("Game {} ({}p{}): {} (deltas={}, turns={}, winner={})",
                    i, players, formatLabel, status,
                    gameResult.deltaPacketsReceived,
                    gameResult.turnCount,
                    gameResult.winner);
        }

        netLog.info("Sequential execution complete: {}", result.toSummary());
        return result;
    }

    /**
     * Run a single game with isolated logging using UnifiedNetworkHarness.
     */
    private UnifiedNetworkHarness.GameResult runSingleGame(int gameIndex, int port, int playerCount, boolean commander) {
        String formatSuffix = commander ? "-cmdr" : "";
        // Set instance-specific log suffix so this game writes to its own log file
        NetworkLogConfig.setInstanceSuffix("game" + gameIndex + "-" + playerCount + "p" + formatSuffix);

        try {
            String formatLabel = commander ? "Commander" : "Constructed";
            netLog.info("Starting game {} ({} players, {}) on port {}", gameIndex, playerCount, formatLabel, port);

            // Use UnifiedNetworkHarness for all player counts
            return new UnifiedNetworkHarness()
                    .playerCount(playerCount)
                    .remoteClients(playerCount - 1)  // All but host are remote
                    .commander(commander)
                    .port(port)
                    .gameTimeout(gameTimeoutMs)
                    .execute();
        } finally {
            // Close this game's log file
            NetworkLogConfig.closeThreadLogger();
        }
    }

    private static synchronized void ensureFModelInitialized() {
        TestUtils.ensureFModelInitialized();
    }

    /**
     * Build a shuffled array of player counts based on configuration.
     * Shuffling distributes different game types throughout the test run.
     */
    private int[] buildShuffledPlayerCounts() {
        List<Integer> counts = new ArrayList<>();
        for (int i = 0; i < twoPlayerGames; i++) {
            counts.add(2);
        }
        for (int i = 0; i < threePlayerGames; i++) {
            counts.add(3);
        }
        for (int i = 0; i < fourPlayerGames; i++) {
            counts.add(4);
        }
        Collections.shuffle(counts);
        return counts.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Build an array of commander flags for the given number of games.
     * Exactly (totalGames * commanderPercentage / 100) games are flagged as Commander,
     * distributed randomly across the array.
     */
    private boolean[] buildCommanderFlags(int totalGames) {
        boolean[] flags = new boolean[totalGames];
        if (commanderPercentage <= 0 || totalGames == 0) {
            return flags;
        }

        int commanderCount = Math.max(1, totalGames * commanderPercentage / 100);
        if (commanderPercentage >= 100) {
            commanderCount = totalGames;
        }

        // Pick exactly commanderCount indices to flag as commander
        List<Integer> indices = new ArrayList<>(totalGames);
        for (int i = 0; i < totalGames; i++) {
            indices.add(i);
        }
        Collections.shuffle(indices, new Random());

        for (int i = 0; i < commanderCount; i++) {
            flags[indices.get(i)] = true;
        }

        return flags;
    }

    public String getConfigurationSummary() {
        int total = getTotalGames();
        return String.format(
                "ComprehensiveTestExecutor Configuration:\n" +
                "  Total Games: %d\n" +
                "  Distribution:\n" +
                "    - 2-player: %d (%.0f%%)\n" +
                "    - 3-player: %d (%.0f%%)\n" +
                "    - 4-player: %d (%.0f%%)\n" +
                "  Commander: %d%% of games\n" +
                "  Execution Mode: %s\n" +
                "  Batch Size: %d\n" +
                "  Game Timeout: %d ms",
                total,
                twoPlayerGames, total > 0 ? (100.0 * twoPlayerGames / total) : 0,
                threePlayerGames, total > 0 ? (100.0 * threePlayerGames / total) : 0,
                fourPlayerGames, total > 0 ? (100.0 * fourPlayerGames / total) : 0,
                commanderPercentage,
                sequential ? "Sequential (same JVM)" : "Parallel (multi-process)",
                parallelBatchSize,
                gameTimeoutMs
        );
    }

    /**
     * Create an executor with comprehensive test defaults (50/30/20),
     * then apply any system property overrides.
     */
    public static ComprehensiveTestExecutor comprehensive() {
        return new ComprehensiveTestExecutor()
                .twoPlayerGames(50)
                .threePlayerGames(30)
                .fourPlayerGames(20)
                .applySystemProperties();
    }

    /**
     * Create an executor from system properties.
     * Defaults to 3 x 2-player games if no properties are set.
     * Properties:
     *   -Dtest.2pGames=3
     *   -Dtest.3pGames=0
     *   -Dtest.4pGames=0
     *   -Dtest.commanderPct=30
     *   -Dtest.batchSize=10
     *   -Dtest.timeoutMs=300000
     */
    public static ComprehensiveTestExecutor fromSystemProperties() {
        return new ComprehensiveTestExecutor().applySystemProperties();
    }

    private ComprehensiveTestExecutor applySystemProperties() {
        ComprehensiveTestExecutor executor = this;

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

        String cmdrPct = System.getProperty("test.commanderPct");
        if (cmdrPct != null) {
            try {
                executor.commanderPercentage(Integer.parseInt(cmdrPct));
            } catch (NumberFormatException e) {
                System.err.println("Invalid test.commanderPct value: " + cmdrPct + ", using default");
            }
        }

        return executor;
    }

}
