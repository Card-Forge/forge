package forge.net;

import forge.gamemodes.net.NetworkDebugLogger;
import forge.gui.GuiBase;
import forge.model.FModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Executes multiple network game pairs in parallel for rapid debugging and log generation.
 *
 * Primary purpose: Generate multiple game logs quickly for analysis. In real play you
 * won't run more than one game at a time, so this is not stress testing - it's about
 * rapid iteration when debugging network issues by generating many logs in parallel.
 *
 * Each game instance:
 * - Runs in its own thread with unique thread name
 * - Uses a unique network port to avoid conflicts
 * - Writes to its own log file (via NetworkDebugLogger.setInstanceSuffix)
 * - Returns independent test results
 *
 * Part of Phase 10 of the automated network testing infrastructure.
 */
public class ParallelNetworkGameExecutor {

    private static final String LOG_PREFIX = "[ParallelNetworkGameExecutor]";
    private static final int DEFAULT_PARALLELISM = 5;
    private static final int MAX_PARALLELISM = 10;
    private static final int BASE_PORT = 58000;
    private static final long DEFAULT_TIMEOUT_MS = 600000; // 10 minutes

    private final int parallelism;
    private final long timeoutMs;
    private final int basePort;

    /**
     * Create executor with default settings (5 parallel games, 10 minute timeout).
     */
    public ParallelNetworkGameExecutor() {
        this(DEFAULT_PARALLELISM, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Create executor with custom parallelism and timeout.
     *
     * @param parallelism Number of concurrent game instances (1-10)
     * @param timeoutMs Per-game timeout in milliseconds
     */
    public ParallelNetworkGameExecutor(int parallelism, long timeoutMs) {
        this(parallelism, timeoutMs, BASE_PORT);
    }

    /**
     * Create executor with full customization.
     *
     * @param parallelism Number of concurrent game instances (1-10)
     * @param timeoutMs Per-game timeout in milliseconds
     * @param basePort Starting port number for game servers
     */
    public ParallelNetworkGameExecutor(int parallelism, long timeoutMs, int basePort) {
        this.parallelism = Math.min(Math.max(1, parallelism), MAX_PARALLELISM);
        this.timeoutMs = timeoutMs;
        this.basePort = basePort;
    }

    /**
     * Ensure FModel is initialized before parallel execution.
     * FModel.initialize() is not thread-safe, so we must call it once
     * before starting any parallel games.
     */
    private static synchronized void ensureFModelInitialized() {
        if (GuiBase.getInterface() == null) {
            NetworkDebugLogger.log("%s Initializing FModel with HeadlessGuiDesktop (one-time)", LOG_PREFIX);
            GuiBase.setInterface(new HeadlessGuiDesktop());
            FModel.initialize(null, preferences -> null);
        }
    }

    /**
     * Run multiple network games in parallel.
     *
     * Each game gets:
     * - Its own thread (named "NetworkGame-N")
     * - Its own network port (basePort + N)
     * - Its own log file (network-debug-...-gameN.log)
     *
     * @param gameCount Total number of games to run
     * @return Aggregated results from all games
     */
    public ParallelTestResult runParallel(int gameCount) {
        // Initialize FModel once before parallel execution - not thread-safe
        ensureFModelInitialized();

        NetworkDebugLogger.log("%s Starting %d parallel games with parallelism=%d",
                LOG_PREFIX, gameCount, parallelism);

        ExecutorService executor = Executors.newFixedThreadPool(parallelism, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

        List<Future<NetworkClientTestHarness.TestResult>> futures = new ArrayList<>();
        AtomicInteger portCounter = new AtomicInteger(basePort);

        // Submit all games
        for (int i = 0; i < gameCount; i++) {
            final int gameIndex = i;
            final int port = portCounter.getAndIncrement();
            futures.add(executor.submit(() -> runSingleGame(gameIndex, port)));
        }

        // Collect results
        ParallelTestResult result = new ParallelTestResult(gameCount);
        for (int i = 0; i < futures.size(); i++) {
            try {
                NetworkClientTestHarness.TestResult gameResult =
                        futures.get(i).get(timeoutMs, TimeUnit.MILLISECONDS);
                result.addResult(i, gameResult);
                NetworkDebugLogger.log("%s Game %d completed: %s",
                        LOG_PREFIX, i, gameResult.success ? "SUCCESS" : "FAILED");
            } catch (TimeoutException e) {
                result.addTimeout(i);
                NetworkDebugLogger.error("%s Game %d timed out after %dms",
                        LOG_PREFIX, i, timeoutMs);
            } catch (Exception e) {
                result.addError(i, e.getMessage());
                NetworkDebugLogger.error("%s Game %d error: %s",
                        LOG_PREFIX, i, e.getMessage());
            }
        }

        // Shutdown executor
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        NetworkDebugLogger.log("%s Parallel execution complete: %s",
                LOG_PREFIX, result.toSummary());
        return result;
    }

    /**
     * Run a single game with isolated logging.
     */
    private NetworkClientTestHarness.TestResult runSingleGame(int gameIndex, int port) {
        // Set thread name for identification
        Thread.currentThread().setName("NetworkGame-" + gameIndex);

        // Set instance-specific log suffix so this game writes to its own log file
        NetworkDebugLogger.setInstanceSuffix("game" + gameIndex);

        try {
            NetworkDebugLogger.log("%s Starting game %d on port %d",
                    LOG_PREFIX, gameIndex, port);

            NetworkClientTestHarness harness = new NetworkClientTestHarness();
            harness.setPort(port);
            return harness.runTwoPlayerNetworkTest();

        } finally {
            // Close this thread's log file
            NetworkDebugLogger.closeThreadLogger();
        }
    }

    /**
     * Aggregated results from parallel test execution.
     */
    public static class ParallelTestResult {
        private final int totalGames;
        private final Map<Integer, NetworkClientTestHarness.TestResult> results = new ConcurrentHashMap<>();
        private final Map<Integer, String> errors = new ConcurrentHashMap<>();
        private final Set<Integer> timeouts = ConcurrentHashMap.newKeySet();

        public ParallelTestResult(int totalGames) {
            this.totalGames = totalGames;
        }

        public void addResult(int idx, NetworkClientTestHarness.TestResult r) {
            results.put(idx, r);
        }

        public void addError(int idx, String msg) {
            errors.put(idx, msg);
        }

        public void addTimeout(int idx) {
            timeouts.add(idx);
        }

        public int getTotalGames() {
            return totalGames;
        }

        public int getSuccessCount() {
            return (int) results.values().stream().filter(r -> r.success).count();
        }

        public int getFailureCount() {
            return (int) results.values().stream().filter(r -> !r.success).count() + errors.size();
        }

        public int getTimeoutCount() {
            return timeouts.size();
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

        public Map<Integer, String> getErrors() {
            return errors;
        }

        public Set<Integer> getTimeouts() {
            return timeouts;
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
                    "ParallelTest[games=%d, success=%d, failed=%d, timeout=%d, " +
                            "totalDeltas=%d, totalBytes=%d, successRate=%.0f%%]",
                    totalGames, getSuccessCount(), getFailureCount(), getTimeoutCount(),
                    getTotalDeltaPackets(), getTotalBytes(), getSuccessRate() * 100);
        }

        /**
         * Generate detailed report of all game results.
         */
        public String toDetailedReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("=".repeat(80)).append("\n");
            sb.append("Parallel Network Game Execution Report\n");
            sb.append("=".repeat(80)).append("\n\n");

            sb.append("Summary: ").append(toSummary()).append("\n\n");

            sb.append("Individual Game Results:\n");
            sb.append("-".repeat(40)).append("\n");

            for (int i = 0; i < totalGames; i++) {
                sb.append(String.format("Game %d: ", i));
                if (timeouts.contains(i)) {
                    sb.append("TIMEOUT\n");
                } else if (errors.containsKey(i)) {
                    sb.append("ERROR - ").append(errors.get(i)).append("\n");
                } else if (results.containsKey(i)) {
                    NetworkClientTestHarness.TestResult r = results.get(i);
                    sb.append(r.success ? "SUCCESS" : "FAILED");
                    sb.append(String.format(" - deltas=%d, turns=%d, winner=%s\n",
                            r.deltaPacketsReceived, r.turns, r.winner));
                } else {
                    sb.append("UNKNOWN\n");
                }
            }

            return sb.toString();
        }
    }
}
