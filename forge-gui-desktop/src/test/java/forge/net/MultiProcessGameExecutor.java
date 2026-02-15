package forge.net;

import forge.gamemodes.net.NetworkDebugLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Executes multiple network games in parallel using separate JVM processes.
 *
 * Each game runs in its own JVM process, avoiding the FServerManager singleton
 * limitation that prevents true parallelism within a single process.
 *
 * Each process:
 * - Has its own isolated FServerManager instance
 * - Uses a unique network port
 * - Writes to its own log file
 *
 * This enables true parallel execution for rapid log generation.
 *
 * Supports 2-4 player games with configurable player counts for comprehensive testing.
 */
public class MultiProcessGameExecutor {

    private static final String LOG_PREFIX = "[MultiProcessGameExecutor]";
    private static final int BASE_PORT = 58000;
    private static final long DEFAULT_TIMEOUT_MS = 300000; // 5 minutes per game

    private final long timeoutMs;
    private final int basePort;

    // Runner class for different game types
    private String runnerClass = "forge.net.ComprehensiveGameRunner"; // Supports 2-4 players

    public MultiProcessGameExecutor() {
        this(DEFAULT_TIMEOUT_MS);
    }

    public MultiProcessGameExecutor(long timeoutMs) {
        this(timeoutMs, BASE_PORT);
    }

    public MultiProcessGameExecutor(long timeoutMs, int basePort) {
        this.timeoutMs = timeoutMs;
        this.basePort = basePort;
    }

    /**
     * Set the runner class for game execution.
     * Use ComprehensiveGameRunner for multi-player support.
     */
    public MultiProcessGameExecutor withRunnerClass(String runnerClass) {
        this.runnerClass = runnerClass;
        return this;
    }

    /**
     * Run multiple games in parallel processes (2-player games).
     *
     * @param gameCount Number of games to run in parallel
     * @return Aggregated results from all games
     */
    public ExecutionResult runGames(int gameCount) {
        // Create array of all 2-player games
        int[] playerCounts = new int[gameCount];
        for (int i = 0; i < gameCount; i++) {
            playerCounts[i] = 2;
        }
        return runGamesWithPlayerCounts(playerCounts);
    }

    /**
     * Run multiple games with specified player counts.
     * Games are run in batches to respect parallel limits.
     *
     * @param playerCounts Array of player counts for each game (2, 3, or 4)
     * @return Aggregated results from all games
     */
    public ExecutionResult runGamesWithPlayerCounts(int[] playerCounts) {
        // Generate batch ID for correlating all logs from this run
        String batchId = NetworkDebugLogger.generateBatchId();
        return runGamesWithPlayerCounts(playerCounts, 0, batchId);
    }

    /**
     * Run multiple games with specified player counts and batch info.
     * Used by runGamesInBatches to maintain unique game indices across batches.
     *
     * @param playerCounts Array of player counts for each game (2, 3, or 4)
     * @param batchNumber Batch number (0-based) for unique log filenames
     * @param batchId Batch ID for correlating logs from the same test run
     * @return Aggregated results from all games
     */
    public ExecutionResult runGamesWithPlayerCounts(int[] playerCounts, int batchNumber, String batchId) {
        int gameCount = playerCounts.length;

        NetworkDebugLogger.log("%s Starting %d games in PARALLEL (batch: %s, batchNum=%d)",
                LOG_PREFIX, gameCount, batchId, batchNumber);

        List<ProcessInfo> processes = new ArrayList<>();
        List<ProcessMonitor> monitors = new ArrayList<>();
        ExecutionResult result = new ExecutionResult(gameCount);

        try {
            // Get classpath from current JVM
            String classpath = System.getProperty("java.class.path");
            String javaHome = System.getProperty("java.home");
            String javaBin = javaHome + File.separator + "bin" + File.separator + "java";

            // Start all processes AND their output readers immediately
            for (int i = 0; i < gameCount; i++) {
                int port = basePort + i;
                int playerCount = playerCounts[i];
                ProcessInfo info = startGameProcess(javaBin, classpath, port, i, playerCount, batchId, batchNumber);
                processes.add(info);

                // Start monitoring immediately to prevent output buffer deadlock
                ProcessMonitor monitor = new ProcessMonitor(info);
                monitor.startReading();
                monitors.add(monitor);

                NetworkDebugLogger.log("%s Started process for batch%d-game%d (%d players, port %d, pid %d)",
                        LOG_PREFIX, batchNumber, i, playerCount, port, info.process.pid());
            }

            // Wait for all processes to complete IN PARALLEL
            // Each monitor runs in its own thread, so we just wait for all to finish
            CountDownLatch allDone = new CountDownLatch(gameCount);

            for (int i = 0; i < gameCount; i++) {
                final int index = i;
                final ProcessMonitor monitor = monitors.get(i);
                new Thread(() -> {
                    try {
                        monitor.waitForCompletion(timeoutMs, result);
                    } finally {
                        allDone.countDown();
                    }
                }, "wait-batch" + batchNumber + "-game" + index).start();
            }

            // Wait for all games to complete
            allDone.await();

        } catch (Exception e) {
            NetworkDebugLogger.error("%s Error: %s", LOG_PREFIX, e.getMessage());
            e.printStackTrace();
        }

        NetworkDebugLogger.log("%s Execution complete: %s", LOG_PREFIX, result.toSummary());
        return result;
    }

    /**
     * Monitors a single game process, reading output and waiting for completion.
     */
    private class ProcessMonitor {
        private final ProcessInfo info;
        private final StringBuilder output = new StringBuilder();
        private final AtomicReference<String> resultLine = new AtomicReference<>();
        private final CountDownLatch readerDone = new CountDownLatch(1);
        private Thread readerThread;

        ProcessMonitor(ProcessInfo info) {
            this.info = info;
        }

        void startReading() {
            readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(info.process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        System.out.println("[Game " + info.gameIndex + "] " + line);
                        if (line.startsWith("RESULT:")) {
                            resultLine.set(line.substring(7));
                        }
                    }
                } catch (Exception e) {
                    NetworkDebugLogger.error("%s Error reading output for game %d: %s",
                            LOG_PREFIX, info.gameIndex, e.getMessage());
                } finally {
                    readerDone.countDown();
                }
            }, "process-reader-" + info.gameIndex);
            readerThread.setDaemon(true);
            readerThread.start();
        }

        void waitForCompletion(long timeoutMs, ExecutionResult result) {
            try {
                boolean completed = info.process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);

                if (!completed) {
                    info.process.destroyForcibly();
                    result.addTimeout(info.gameIndex);
                    NetworkDebugLogger.error("%s Game %d timed out after %dms",
                            LOG_PREFIX, info.gameIndex, timeoutMs);
                    return;
                }

                // Wait for reader thread to finish
                readerDone.await(5, TimeUnit.SECONDS);

                int exitCode = info.process.exitValue();

                if (resultLine.get() != null) {
                    parseResult(resultLine.get(), result);
                }

                if (exitCode == 0) {
                    NetworkDebugLogger.log("%s Game %d completed successfully",
                            LOG_PREFIX, info.gameIndex);
                } else if (exitCode == 1) {
                    NetworkDebugLogger.log("%s Game %d failed (exit code 1)",
                            LOG_PREFIX, info.gameIndex);
                } else {
                    result.addError(info.gameIndex, "Process exited with code " + exitCode);
                    NetworkDebugLogger.error("%s Game %d error (exit code %d)",
                            LOG_PREFIX, info.gameIndex, exitCode);
                }
            } catch (Exception e) {
                result.addError(info.gameIndex, "Exception: " + e.getMessage());
                NetworkDebugLogger.error("%s Game %d exception: %s",
                        LOG_PREFIX, info.gameIndex, e.getMessage());
            }
        }
    }

    /**
     * Run games in sequential batches to avoid overwhelming the system.
     * Useful for running 100+ games with limited parallelism.
     *
     * @param playerCounts Array of player counts for each game
     * @param batchSize Maximum number of parallel processes per batch
     * @return Aggregated results from all games
     */
    public ExecutionResult runGamesInBatches(int[] playerCounts, int batchSize) {
        int totalGames = playerCounts.length;
        int totalBatches = (totalGames + batchSize - 1) / batchSize;

        // Generate a single batch ID for the entire test run
        String batchId = NetworkDebugLogger.generateBatchId();
        NetworkDebugLogger.log("%s Running %d games in %d batches of %d (run: %s)",
                LOG_PREFIX, totalGames, totalBatches, batchSize, batchId);

        ExecutionResult aggregatedResult = new ExecutionResult(totalGames);
        int currentPortOffset = 0;
        int batchNumber = 0;

        for (int batchStart = 0; batchStart < totalGames; batchStart += batchSize) {
            int batchEnd = Math.min(batchStart + batchSize, totalGames);
            int batchLength = batchEnd - batchStart;

            // Extract player counts for this batch
            int[] batchPlayerCounts = new int[batchLength];
            System.arraycopy(playerCounts, batchStart, batchPlayerCounts, 0, batchLength);

            NetworkDebugLogger.log("%s Starting batch %d (games %d-%d of %d)",
                    LOG_PREFIX, batchNumber, batchStart, batchEnd - 1, totalGames);

            // Create executor for this batch with offset ports
            MultiProcessGameExecutor batchExecutor = new MultiProcessGameExecutor(
                    this.timeoutMs, this.basePort + currentPortOffset);
            batchExecutor.withRunnerClass(this.runnerClass);

            // Pass batch number and shared batch ID for unique log filenames
            ExecutionResult batchResult = batchExecutor.runGamesWithPlayerCounts(batchPlayerCounts, batchNumber, batchId);

            // Merge batch results into aggregated result
            for (Map.Entry<Integer, GameResult> entry : batchResult.getResults().entrySet()) {
                int originalIndex = batchStart + entry.getKey();
                aggregatedResult.addResult(originalIndex, entry.getValue());
            }

            currentPortOffset += batchLength;
            batchNumber++;

            NetworkDebugLogger.log("%s Batch complete. Running total: %d/%d games",
                    LOG_PREFIX, aggregatedResult.getSuccessCount() + aggregatedResult.getFailureCount(), totalGames);

            // Brief delay between batches to allow port cleanup
            // This helps prevent "Address already in use" errors on rapid test execution
            if (batchNumber < totalBatches) {
                try {
                    Thread.sleep(500); // 500ms delay between batches
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        NetworkDebugLogger.log("%s All batches complete: %s", LOG_PREFIX, aggregatedResult.toSummary());
        return aggregatedResult;
    }

    private ProcessInfo startGameProcess(String javaBin, String classpath, int port, int gameIndex, int playerCount, String batchId, int batchNumber)
            throws Exception {
        List<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-cp");
        command.add(classpath);
        command.add("-Xmx512m");  // Limit memory per process

        // Pass through test configuration properties to child processes
        if (Boolean.getBoolean("test.useAiForRemote")) {
            command.add("-Dtest.useAiForRemote=true");
        }

        command.add(runnerClass);
        command.add(String.valueOf(port));
        command.add(String.valueOf(gameIndex));
        command.add(String.valueOf(playerCount));
        command.add(batchId);
        command.add(String.valueOf(batchNumber));

        ProcessBuilder pb = new ProcessBuilder(command);

        // Merge stderr into stdout for easier capture
        pb.redirectErrorStream(true);

        Process process = pb.start();
        return new ProcessInfo(gameIndex, port, playerCount, process);
    }

    private void parseResult(String resultLine, ExecutionResult result) {
        try {
            // Format for ComprehensiveGameRunner: gameIndex|success|playerCount|deltas|turns|bytes|winner|decks
            String[] parts = resultLine.split("\\|");
            if (parts.length >= 7) {
                int gameIndex = Integer.parseInt(parts[0]);
                boolean success = Boolean.parseBoolean(parts[1]);
                int playerCount = Integer.parseInt(parts[2]);
                long deltas = Long.parseLong(parts[3]);
                int turns = Integer.parseInt(parts[4]);
                long bytes = Long.parseLong(parts[5]);
                String winner = "null".equals(parts[6]) ? null : parts[6];

                // Parse deck names if present (part 7)
                List<String> deckNames = new ArrayList<>();
                if (parts.length >= 8 && !parts[7].isEmpty()) {
                    String[] decks = parts[7].split(",");
                    for (String deck : decks) {
                        if (!deck.trim().isEmpty()) {
                            deckNames.add(deck.trim());
                        }
                    }
                }

                GameResult gameResult = new GameResult(success, playerCount, deltas, turns, bytes, winner, deckNames);
                result.addResult(gameIndex, gameResult);
            }
        } catch (Exception e) {
            NetworkDebugLogger.error("%s Failed to parse result: %s", LOG_PREFIX, resultLine);
        }
    }

    private static class ProcessInfo {
        final int gameIndex;
        final int port;
        final int playerCount;
        final Process process;

        ProcessInfo(int gameIndex, int port, int playerCount, Process process) {
            this.gameIndex = gameIndex;
            this.port = port;
            this.playerCount = playerCount;
            this.process = process;
        }
    }

    /**
     * Result from a single game.
     */
    public static class GameResult {
        public final boolean success;
        public final int playerCount;
        public final long deltaPackets;
        public final int turns;
        public final long bytes;
        public final String winner;
        public final List<String> deckNames;

        public GameResult(boolean success, int playerCount, long deltaPackets, int turns, long bytes, String winner, List<String> deckNames) {
            this.success = success;
            this.playerCount = playerCount;
            this.deltaPackets = deltaPackets;
            this.turns = turns;
            this.bytes = bytes;
            this.winner = winner;
            this.deckNames = deckNames != null ? new ArrayList<>(deckNames) : Collections.emptyList();
        }
    }

    /**
     * Aggregated results from parallel execution.
     */
    public static class ExecutionResult {
        private final int totalGames;
        private final Map<Integer, GameResult> results = new ConcurrentHashMap<>();
        private final Map<Integer, String> errors = new ConcurrentHashMap<>();
        private final Map<Integer, Boolean> timeouts = new ConcurrentHashMap<>();

        public ExecutionResult(int totalGames) {
            this.totalGames = totalGames;
        }

        public void addResult(int idx, GameResult r) {
            results.put(idx, r);
        }

        public void addError(int idx, String msg) {
            errors.put(idx, msg);
        }

        public void addTimeout(int idx) {
            timeouts.put(idx, true);
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

        public int getErrorCount() {
            return errors.size();
        }

        public int getTimeoutCount() {
            return timeouts.size();
        }

        public long getTotalDeltaPackets() {
            return results.values().stream().mapToLong(r -> r.deltaPackets).sum();
        }

        public long getTotalBytes() {
            return results.values().stream().mapToLong(r -> r.bytes).sum();
        }

        /**
         * Get total turns across all successful games.
         */
        public int getTotalTurns() {
            return results.values().stream()
                    .filter(r -> r.success)
                    .mapToInt(r -> r.turns)
                    .sum();
        }

        /**
         * Get count of unique decks used across all games.
         */
        public int getUniqueDecksCount() {
            return (int) results.values().stream()
                    .flatMap(r -> r.deckNames.stream())
                    .distinct()
                    .count();
        }

        /**
         * Get set of unique deck names used across all games.
         */
        public java.util.Set<String> getUniqueDeckNames() {
            return results.values().stream()
                    .flatMap(r -> r.deckNames.stream())
                    .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));
        }

        /**
         * Get total number of deck usages (sum of decks across all games).
         */
        public int getTotalDeckUsages() {
            return results.values().stream()
                    .mapToInt(r -> r.deckNames.size())
                    .sum();
        }

        public Map<Integer, GameResult> getResults() {
            return results;
        }

        public double getSuccessRate() {
            if (totalGames == 0) return 0.0;
            return (double) getSuccessCount() / totalGames;
        }

        // Player count aggregation methods

        /**
         * Get count of games by player count.
         */
        public int getGameCountByPlayers(int playerCount) {
            return (int) results.values().stream()
                    .filter(r -> r.playerCount == playerCount)
                    .count();
        }

        /**
         * Get success count by player count.
         */
        public int getSuccessCountByPlayers(int playerCount) {
            return (int) results.values().stream()
                    .filter(r -> r.playerCount == playerCount && r.success)
                    .count();
        }

        /**
         * Get success rate by player count.
         */
        public double getSuccessRateByPlayers(int playerCount) {
            long total = results.values().stream()
                    .filter(r -> r.playerCount == playerCount)
                    .count();
            if (total == 0) return 0.0;
            long success = results.values().stream()
                    .filter(r -> r.playerCount == playerCount && r.success)
                    .count();
            return (double) success / total;
        }

        /**
         * Get total bytes by player count.
         */
        public long getTotalBytesByPlayers(int playerCount) {
            return results.values().stream()
                    .filter(r -> r.playerCount == playerCount)
                    .mapToLong(r -> r.bytes)
                    .sum();
        }

        /**
         * Get average turns by player count.
         */
        public double getAverageTurnsByPlayers(int playerCount) {
            return results.values().stream()
                    .filter(r -> r.playerCount == playerCount && r.success)
                    .mapToInt(r -> r.turns)
                    .average()
                    .orElse(0.0);
        }

        /**
         * Format bytes in human-readable form (B, KB, MB, GB).
         */
        private String formatBytes(long bytes) {
            if (bytes < 1024) {
                return bytes + " B";
            } else if (bytes < 1024 * 1024) {
                return String.format("%.1f KB", bytes / 1024.0);
            } else if (bytes < 1024L * 1024L * 1024L) {
                return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
            } else {
                return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
            }
        }

        public String toSummary() {
            return String.format(
                    "MultiProcess[games=%d, success=%d, failed=%d, errors=%d, timeouts=%d, " +
                            "totalTurns=%d, uniqueDecks=%d, totalDeltas=%d, totalBytes=%d, successRate=%.0f%%]",
                    totalGames, getSuccessCount(), getFailureCount(),
                    getErrorCount(), getTimeoutCount(),
                    getTotalTurns(), getUniqueDecksCount(), getTotalDeltaPackets(), getTotalBytes(), getSuccessRate() * 100);
        }

        public String toDetailedReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("=".repeat(80)).append("\n");
            sb.append("Multi-Process Parallel Game Execution Report\n");
            sb.append("=".repeat(80)).append("\n\n");

            // Overall metrics
            sb.append("Overall Metrics:\n");
            sb.append("-".repeat(40)).append("\n");
            sb.append(String.format("  Total Games:     %d\n", totalGames));
            sb.append(String.format("  Successful:      %d (%.0f%%)\n", getSuccessCount(), getSuccessRate() * 100));
            sb.append(String.format("  Failed:          %d\n", getFailureCount()));
            sb.append(String.format("  Errors:          %d\n", getErrorCount()));
            sb.append(String.format("  Timeouts:        %d\n", getTimeoutCount()));
            sb.append(String.format("  Total Turns:     %d\n", getTotalTurns()));
            sb.append(String.format("  Unique Decks:    %d (from %d total usages)\n", getUniqueDecksCount(), getTotalDeckUsages()));
            sb.append(String.format("  Delta Packets:   %d\n", getTotalDeltaPackets()));
            sb.append(String.format("  Total Bytes:     %d (%s)\n", getTotalBytes(), formatBytes(getTotalBytes())));
            double bytesPerPacket = getTotalDeltaPackets() > 0 ? (double) getTotalBytes() / getTotalDeltaPackets() : 0;
            sb.append(String.format("  Bytes/Packet:    %.1f\n", bytesPerPacket));
            sb.append("\n");

            // Breakdown by player count
            sb.append("Breakdown by Player Count:\n");
            sb.append("-".repeat(40)).append("\n");
            for (int p = 2; p <= 4; p++) {
                int count = getGameCountByPlayers(p);
                if (count > 0) {
                    final int playerCount = p; // Final copy for lambda
                    int successCount = getSuccessCountByPlayers(p);
                    double successRate = getSuccessRateByPlayers(p) * 100;
                    double avgTurns = getAverageTurnsByPlayers(p);
                    int totalTurnsForP = results.values().stream()
                            .filter(r -> r.playerCount == playerCount && r.success)
                            .mapToInt(r -> r.turns)
                            .sum();
                    sb.append(String.format("  %d-player: %d games, %d success (%.0f%%), %d total turns (avg %.1f)\n",
                            p, count, successCount, successRate, totalTurnsForP, avgTurns));
                }
            }
            sb.append("\n");

            sb.append("Individual Game Results:\n");
            sb.append("-".repeat(40)).append("\n");

            for (int i = 0; i < totalGames; i++) {
                sb.append(String.format("Game %d: ", i));
                if (timeouts.containsKey(i)) {
                    sb.append("TIMEOUT\n");
                } else if (errors.containsKey(i)) {
                    sb.append("ERROR - ").append(errors.get(i)).append("\n");
                } else if (results.containsKey(i)) {
                    GameResult r = results.get(i);
                    sb.append(r.success ? "SUCCESS" : "FAILED");
                    sb.append(String.format(" - %dp, deltas=%d, turns=%d, winner=%s\n",
                            r.playerCount, r.deltaPackets, r.turns, r.winner));
                } else {
                    sb.append("NO RESULT\n");
                }
            }

            return sb.toString();
        }
    }
}
