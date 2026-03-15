package forge.net;

import forge.gamemodes.net.IHasNetLog;
import forge.gamemodes.net.NetworkLogConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
public class MultiProcessGameExecutor implements IHasNetLog {

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
        return runGamesWithPlayerCounts(playerCounts, new boolean[playerCounts.length]);
    }

    /**
     * Run multiple games with specified player counts and commander flags.
     *
     * @param playerCounts Array of player counts for each game (2, 3, or 4)
     * @param commanderFlags Array of booleans indicating Commander format per game
     * @return Aggregated results from all games
     */
    public ExecutionResult runGamesWithPlayerCounts(int[] playerCounts, boolean[] commanderFlags) {
        // Generate batch ID for correlating all logs from this run
        String batchId = NetworkLogConfig.generateBatchId();
        return runGamesWithPlayerCounts(playerCounts, commanderFlags, 0, batchId);
    }

    /**
     * Run multiple games with specified player counts, commander flags, and batch info.
     * Used by runGamesInBatches to maintain unique game indices across batches.
     *
     * @param playerCounts Array of player counts for each game (2, 3, or 4)
     * @param commanderFlags Array of booleans indicating Commander format per game
     * @param batchNumber Batch number (0-based) for unique log filenames
     * @param batchId Batch ID for correlating logs from the same test run
     * @return Aggregated results from all games
     */
    public ExecutionResult runGamesWithPlayerCounts(int[] playerCounts, boolean[] commanderFlags, int batchNumber, String batchId) {
        int gameCount = playerCounts.length;

        netLog.info("Starting {} games in PARALLEL (batch: {}, batchNum={})",
                gameCount, batchId, batchNumber);

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
                boolean commander = commanderFlags != null && i < commanderFlags.length && commanderFlags[i];
                ProcessInfo info = startGameProcess(javaBin, classpath, port, i, playerCount, batchId, batchNumber, commander);
                processes.add(info);

                // Start monitoring immediately to prevent output buffer deadlock
                ProcessMonitor monitor = new ProcessMonitor(info);
                monitor.startReading();
                monitors.add(monitor);

                netLog.info("Started process for batch{}-game{} ({} players, port {}, pid {})",
                        batchNumber, i, playerCount, port, info.process.pid());
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
            netLog.error("Error: {}", e.getMessage());
            e.printStackTrace();
        }

        netLog.info("Execution complete: {}", result.toSummary());
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
                    netLog.error("Error reading output for game {}: {}",
                            info.gameIndex, e.getMessage());
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
                    netLog.error("Game {} timed out after {}ms",
                            info.gameIndex, timeoutMs);
                    return;
                }

                // Wait for reader thread to finish
                readerDone.await(5, TimeUnit.SECONDS);

                int exitCode = info.process.exitValue();

                if (resultLine.get() != null) {
                    parseResult(resultLine.get(), result);
                }

                if (exitCode == 0) {
                    netLog.info("Game {} completed successfully",
                            info.gameIndex);
                } else if (exitCode == 1) {
                    netLog.info("Game {} failed (exit code 1)",
                            info.gameIndex);
                } else {
                    result.addError(info.gameIndex, "Process exited with code " + exitCode);
                    netLog.error("Game {} error (exit code {})",
                            info.gameIndex, exitCode);
                }
            } catch (Exception e) {
                result.addError(info.gameIndex, "Exception: " + e.getMessage());
                netLog.error("Game {} exception: {}",
                        info.gameIndex, e.getMessage());
            }
        }
    }

    /**
     * Run games in sequential batches (all Constructed format).
     */
    public ExecutionResult runGamesInBatches(int[] playerCounts, int batchSize) {
        return runGamesInBatches(playerCounts, new boolean[playerCounts.length], batchSize);
    }

    /**
     * Run games in sequential batches to avoid overwhelming the system.
     * Useful for running 100+ games with limited parallelism.
     *
     * @param playerCounts Array of player counts for each game
     * @param commanderFlags Array of booleans indicating Commander format per game
     * @param batchSize Maximum number of parallel processes per batch
     * @return Aggregated results from all games
     */
    public ExecutionResult runGamesInBatches(int[] playerCounts, boolean[] commanderFlags, int batchSize) {
        int totalGames = playerCounts.length;
        int totalBatches = (totalGames + batchSize - 1) / batchSize;

        // Generate a single batch ID for the entire test run
        String batchId = NetworkLogConfig.generateBatchId();
        netLog.info("Running {} games in {} batches of {} (run: {})",
                totalGames, totalBatches, batchSize, batchId);

        ExecutionResult aggregatedResult = new ExecutionResult(totalGames);
        int currentPortOffset = 0;
        int batchNumber = 0;

        for (int batchStart = 0; batchStart < totalGames; batchStart += batchSize) {
            int batchEnd = Math.min(batchStart + batchSize, totalGames);
            int batchLength = batchEnd - batchStart;

            // Extract player counts and commander flags for this batch
            int[] batchPlayerCounts = new int[batchLength];
            boolean[] batchCommanderFlags = new boolean[batchLength];
            System.arraycopy(playerCounts, batchStart, batchPlayerCounts, 0, batchLength);
            if (commanderFlags != null) {
                System.arraycopy(commanderFlags, batchStart, batchCommanderFlags, 0, batchLength);
            }

            netLog.info("Starting batch {} (games {}-{} of {})",
                    batchNumber, batchStart, batchEnd - 1, totalGames);

            // Create executor for this batch with offset ports
            MultiProcessGameExecutor batchExecutor = new MultiProcessGameExecutor(
                    this.timeoutMs, this.basePort + currentPortOffset);
            batchExecutor.withRunnerClass(this.runnerClass);

            // Pass batch number and shared batch ID for unique log filenames
            ExecutionResult batchResult = batchExecutor.runGamesWithPlayerCounts(batchPlayerCounts, batchCommanderFlags, batchNumber, batchId);

            // Merge batch results into aggregated result
            for (Map.Entry<Integer, UnifiedNetworkHarness.GameResult> entry : batchResult.getResults().entrySet()) {
                int originalIndex = batchStart + entry.getKey();
                aggregatedResult.addResult(originalIndex, entry.getValue());
            }

            currentPortOffset += batchLength;
            batchNumber++;

            netLog.info("Batch complete. Running total: {}/{} games",
                    aggregatedResult.getSuccessCount() + aggregatedResult.getFailureCount(), totalGames);

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

        netLog.info("All batches complete: {}", aggregatedResult.toSummary());
        return aggregatedResult;
    }

    private ProcessInfo startGameProcess(String javaBin, String classpath, int port, int gameIndex, int playerCount,
                                          String batchId, int batchNumber, boolean commander)
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
        command.add(String.valueOf(commander));

        ProcessBuilder pb = new ProcessBuilder(command);

        // Merge stderr into stdout for easier capture
        pb.redirectErrorStream(true);

        Process process = pb.start();
        return new ProcessInfo(gameIndex, port, playerCount, process);
    }

    private void parseResult(String resultLine, ExecutionResult result) {
        try {
            // Format: gameIndex|success|playerCount|deltas|turns|bytes|winner|decks|format|eventMismatches
            String[] parts = resultLine.split("\\|");
            if (parts.length >= 7) {
                int gameIndex = Integer.parseInt(parts[0]);

                UnifiedNetworkHarness.GameResult gameResult = new UnifiedNetworkHarness.GameResult();
                gameResult.success = Boolean.parseBoolean(parts[1]);
                gameResult.playerCount = Integer.parseInt(parts[2]);
                gameResult.deltaPacketsReceived = Long.parseLong(parts[3]);
                gameResult.turnCount = Integer.parseInt(parts[4]);
                gameResult.totalDeltaBytes = Long.parseLong(parts[5]);
                gameResult.winner = "null".equals(parts[6]) ? null : parts[6];

                // Parse deck names if present (part 7)
                if (parts.length >= 8 && !parts[7].isEmpty()) {
                    String[] decks = parts[7].split(",");
                    for (String deck : decks) {
                        if (!deck.trim().isEmpty()) {
                            gameResult.deckNames.add(deck.trim());
                        }
                    }
                }

                // Parse game format if present (part 8)
                if (parts.length >= 9 && !parts[8].isEmpty()) {
                    gameResult.gameFormat = parts[8];
                }

                // Parse event-state mismatches if present (part 9)
                if (parts.length >= 10) {
                    gameResult.eventStateMismatches = Long.parseLong(parts[9]);
                }

                result.addResult(gameIndex, gameResult);
            }
        } catch (Exception e) {
            netLog.error("Failed to parse result: {}", resultLine);
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
     * Aggregated results from parallel execution.
     */
    public static class ExecutionResult {
        private final int totalGames;
        private final Map<Integer, UnifiedNetworkHarness.GameResult> results = new ConcurrentHashMap<>();
        private final Map<Integer, String> errors = new ConcurrentHashMap<>();
        private final Map<Integer, Boolean> timeouts = new ConcurrentHashMap<>();

        public ExecutionResult(int totalGames) {
            this.totalGames = totalGames;
        }

        public void addResult(int idx, UnifiedNetworkHarness.GameResult r) {
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
            return results.values().stream().mapToLong(r -> r.deltaPacketsReceived).sum();
        }

        public long getTotalBytes() {
            return results.values().stream().mapToLong(r -> r.totalDeltaBytes).sum();
        }

        public long getTotalEventStateMismatches() {
            return results.values().stream().mapToLong(r -> r.eventStateMismatches).sum();
        }

        /**
         * Get total turns across all successful games.
         */
        public int getTotalTurns() {
            return results.values().stream()
                    .filter(r -> r.success)
                    .mapToInt(r -> r.turnCount)
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

        public Map<Integer, UnifiedNetworkHarness.GameResult> getResults() {
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
                    .mapToLong(r -> r.totalDeltaBytes)
                    .sum();
        }

        /**
         * Get average turns by player count.
         */
        public double getAverageTurnsByPlayers(int playerCount) {
            return results.values().stream()
                    .filter(r -> r.playerCount == playerCount && r.success)
                    .mapToInt(r -> r.turnCount)
                    .average()
                    .orElse(0.0);
        }

        // Format aggregation methods

        /**
         * Get count of Commander format games.
         */
        public int getCommanderGameCount() {
            return (int) results.values().stream()
                    .filter(r -> "Commander".equals(r.gameFormat))
                    .count();
        }

        /**
         * Get success count of Commander format games.
         */
        public int getCommanderSuccessCount() {
            return (int) results.values().stream()
                    .filter(r -> "Commander".equals(r.gameFormat) && r.success)
                    .count();
        }

        /**
         * Get success rate of Commander format games.
         */
        public double getCommanderSuccessRate() {
            long total = getCommanderGameCount();
            if (total == 0) return 0.0;
            return (double) getCommanderSuccessCount() / total;
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
            sb.append(String.format("  Total Bytes:     %d (%s)\n", getTotalBytes(), TestUtils.formatBytes(getTotalBytes())));
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
                            .mapToInt(r -> r.turnCount)
                            .sum();
                    sb.append(String.format("  %d-player: %d games, %d success (%.0f%%), %d total turns (avg %.1f)\n",
                            p, count, successCount, successRate, totalTurnsForP, avgTurns));
                }
            }
            sb.append("\n");

            // Breakdown by format
            int commanderCount = getCommanderGameCount();
            if (commanderCount > 0) {
                int constructedCount = totalGames - commanderCount - getErrorCount() - getTimeoutCount();
                sb.append("Breakdown by Format:\n");
                sb.append("-".repeat(40)).append("\n");
                sb.append(String.format("  Constructed: %d games, %d success\n",
                        constructedCount > 0 ? constructedCount : totalGames - commanderCount,
                        getSuccessCount() - getCommanderSuccessCount()));
                sb.append(String.format("  Commander:   %d games, %d success (%.0f%%)\n",
                        commanderCount, getCommanderSuccessCount(), getCommanderSuccessRate() * 100));
                sb.append("\n");
            }

            sb.append("Individual Game Results:\n");
            sb.append("-".repeat(40)).append("\n");

            for (int i = 0; i < totalGames; i++) {
                sb.append(String.format("Game %d: ", i));
                if (timeouts.containsKey(i)) {
                    sb.append("TIMEOUT\n");
                } else if (errors.containsKey(i)) {
                    sb.append("ERROR - ").append(errors.get(i)).append("\n");
                } else if (results.containsKey(i)) {
                    UnifiedNetworkHarness.GameResult r = results.get(i);
                    sb.append(r.success ? "SUCCESS" : "FAILED");
                    String fmtTag = "Commander".equals(r.gameFormat) ? " [Cmdr]" : "";
                    sb.append(String.format(" - %dp%s, deltas=%d, turns=%d, winner=%s\n",
                            r.playerCount, fmtTag, r.deltaPacketsReceived, r.turnCount, r.winner));
                } else {
                    sb.append("NO RESULT\n");
                }
            }

            return sb.toString();
        }
    }
}
