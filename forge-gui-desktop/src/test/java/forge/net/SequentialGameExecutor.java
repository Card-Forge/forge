package forge.net;

import forge.gamemodes.net.NetworkDebugLogger;
import forge.gui.GuiBase;
import forge.model.FModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Executes multiple network games sequentially for rapid debugging and log generation.
 *
 * Each game instance:
 * - Runs to completion before the next game starts
 * - Uses a unique network port to avoid conflicts
 * - Writes to its own log file (via NetworkDebugLogger.setInstanceSuffix)
 * - Returns independent test results
 *
 * Part of Phase 10 of the automated network testing infrastructure.
 */
public class SequentialGameExecutor {

    private static final String LOG_PREFIX = "[SequentialGameExecutor]";
    private static final int BASE_PORT = 58000;
    private static final long DEFAULT_TIMEOUT_MS = 300000; // 5 minutes per game

    private final long timeoutMs;
    private final int basePort;

    /**
     * Create executor with default settings (5 minute timeout per game).
     */
    public SequentialGameExecutor() {
        this(DEFAULT_TIMEOUT_MS);
    }

    /**
     * Create executor with custom timeout.
     *
     * @param timeoutMs Per-game timeout in milliseconds
     */
    public SequentialGameExecutor(long timeoutMs) {
        this(timeoutMs, BASE_PORT);
    }

    /**
     * Create executor with full customization.
     *
     * @param timeoutMs Per-game timeout in milliseconds
     * @param basePort Starting port number for game servers
     */
    public SequentialGameExecutor(long timeoutMs, int basePort) {
        this.timeoutMs = timeoutMs;
        this.basePort = basePort;
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
     * Run multiple network games sequentially.
     *
     * Each game gets:
     * - Its own network port (basePort + N)
     * - Its own log file (network-debug-...-gameN.log)
     *
     * @param gameCount Total number of games to run
     * @return Aggregated results from all games
     */
    public ExecutionResult runGames(int gameCount) {
        ensureFModelInitialized();

        NetworkDebugLogger.log("%s Starting %d sequential games", LOG_PREFIX, gameCount);

        ExecutionResult result = new ExecutionResult(gameCount);
        int port = basePort;

        for (int i = 0; i < gameCount; i++) {
            NetworkClientTestHarness.TestResult gameResult = runSingleGame(i, port++);
            result.addResult(i, gameResult);

            String status = gameResult.success ? "SUCCESS" : "FAILED";
            NetworkDebugLogger.log("%s Game %d: %s (deltas=%d, turns=%d, winner=%s)",
                    LOG_PREFIX, i, status,
                    gameResult.deltaPacketsReceived,
                    gameResult.turns,
                    gameResult.winner);
        }

        NetworkDebugLogger.log("%s Execution complete: %s", LOG_PREFIX, result.toSummary());
        return result;
    }

    /**
     * Run a single game with isolated logging.
     */
    private NetworkClientTestHarness.TestResult runSingleGame(int gameIndex, int port) {
        // Set instance-specific log suffix so this game writes to its own log file
        NetworkDebugLogger.setInstanceSuffix("game" + gameIndex);

        try {
            NetworkDebugLogger.log("%s Starting game %d on port %d", LOG_PREFIX, gameIndex, port);

            NetworkClientTestHarness harness = new NetworkClientTestHarness();
            harness.setPort(port);
            return harness.runTwoPlayerNetworkTest();

        } finally {
            // Close this game's log file
            NetworkDebugLogger.closeThreadLogger();
        }
    }

    /**
     * Aggregated results from sequential game execution.
     */
    public static class ExecutionResult {
        private final int totalGames;
        private final Map<Integer, NetworkClientTestHarness.TestResult> results = new ConcurrentHashMap<>();

        public ExecutionResult(int totalGames) {
            this.totalGames = totalGames;
        }

        public void addResult(int idx, NetworkClientTestHarness.TestResult r) {
            results.put(idx, r);
        }

        public int getTotalGames() {
            return totalGames;
        }

        public int getSuccessCount() {
            return (int) results.values().stream().filter(r -> r.success).count();
        }

        public int getFailureCount() {
            return (int) results.values().stream().filter(r -> !r.success).count();
        }

        public long getTotalDeltaPackets() {
            return results.values().stream().mapToLong(r -> r.deltaPacketsReceived).sum();
        }

        public long getTotalBytes() {
            return results.values().stream().mapToLong(r -> r.totalDeltaBytes).sum();
        }

        public Map<Integer, NetworkClientTestHarness.TestResult> getResults() {
            return results;
        }

        /**
         * Get success rate as a percentage (0.0 - 1.0).
         */
        public double getSuccessRate() {
            if (totalGames == 0) return 0.0;
            return (double) getSuccessCount() / totalGames;
        }

        public String toSummary() {
            return String.format(
                    "SequentialExecution[games=%d, success=%d, failed=%d, " +
                            "totalDeltas=%d, totalBytes=%d, successRate=%.0f%%]",
                    totalGames, getSuccessCount(), getFailureCount(),
                    getTotalDeltaPackets(), getTotalBytes(), getSuccessRate() * 100);
        }

        /**
         * Generate detailed report of all game results.
         */
        public String toDetailedReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("=".repeat(80)).append("\n");
            sb.append("Sequential Network Game Execution Report\n");
            sb.append("=".repeat(80)).append("\n\n");

            sb.append("Summary: ").append(toSummary()).append("\n\n");

            sb.append("Individual Game Results:\n");
            sb.append("-".repeat(40)).append("\n");

            for (int i = 0; i < totalGames; i++) {
                sb.append(String.format("Game %d: ", i));
                if (results.containsKey(i)) {
                    NetworkClientTestHarness.TestResult r = results.get(i);
                    sb.append(r.success ? "SUCCESS" : "FAILED");
                    sb.append(String.format(" - deltas=%d, turns=%d, winner=%s\n",
                            r.deltaPacketsReceived, r.turns, r.winner));
                } else {
                    sb.append("NOT RUN\n");
                }
            }

            return sb.toString();
        }
    }
}
