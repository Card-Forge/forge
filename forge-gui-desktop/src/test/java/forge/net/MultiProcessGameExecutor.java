package forge.net;

import forge.gamemodes.net.NetworkDebugLogger;

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
 */
public class MultiProcessGameExecutor {

    private static final String LOG_PREFIX = "[MultiProcessGameExecutor]";
    private static final int BASE_PORT = 58000;
    private static final long DEFAULT_TIMEOUT_MS = 300000; // 5 minutes per game

    private final long timeoutMs;
    private final int basePort;

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
     * Run multiple games in parallel processes.
     *
     * @param gameCount Number of games to run in parallel
     * @return Aggregated results from all games
     */
    public ExecutionResult runGames(int gameCount) {
        NetworkDebugLogger.log("%s Starting %d parallel processes", LOG_PREFIX, gameCount);

        List<ProcessInfo> processes = new ArrayList<>();
        ExecutionResult result = new ExecutionResult(gameCount);

        try {
            // Get classpath from current JVM
            String classpath = System.getProperty("java.class.path");
            String javaHome = System.getProperty("java.home");
            String javaBin = javaHome + File.separator + "bin" + File.separator + "java";

            // Start all processes
            for (int i = 0; i < gameCount; i++) {
                int port = basePort + i;
                ProcessInfo info = startGameProcess(javaBin, classpath, port, i);
                processes.add(info);
                NetworkDebugLogger.log("%s Started process for game %d (port %d, pid %d)",
                        LOG_PREFIX, i, port, info.process.pid());
            }

            // Wait for all processes to complete
            for (ProcessInfo info : processes) {
                waitForProcess(info, result);
            }

        } catch (Exception e) {
            NetworkDebugLogger.error("%s Error: %s", LOG_PREFIX, e.getMessage());
            e.printStackTrace();
        }

        NetworkDebugLogger.log("%s Execution complete: %s", LOG_PREFIX, result.toSummary());
        return result;
    }

    private ProcessInfo startGameProcess(String javaBin, String classpath, int port, int gameIndex)
            throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                javaBin,
                "-cp", classpath,
                "-Xmx512m",  // Limit memory per process
                "forge.net.SingleGameRunner",
                String.valueOf(port),
                String.valueOf(gameIndex)
        );

        // Merge stderr into stdout for easier capture
        pb.redirectErrorStream(true);

        Process process = pb.start();
        return new ProcessInfo(gameIndex, port, process);
    }

    private void waitForProcess(ProcessInfo info, ExecutionResult result) {
        // Start a thread to read output concurrently (prevents buffer deadlock)
        StringBuilder output = new StringBuilder();
        AtomicReference<String> resultLine = new AtomicReference<>();
        CountDownLatch readerDone = new CountDownLatch(1);

        Thread readerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(info.process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    // Log output to help debug process issues
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

        try {
            boolean completed = info.process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);

            if (!completed) {
                info.process.destroyForcibly();
                result.addTimeout(info.gameIndex);
                NetworkDebugLogger.error("%s Game %d timed out after %dms",
                        LOG_PREFIX, info.gameIndex, timeoutMs);
                return;
            }

            // Wait for reader thread to finish (should be quick after process exits)
            readerDone.await(5, TimeUnit.SECONDS);

            int exitCode = info.process.exitValue();

            // Parse the result line if we got one
            if (resultLine.get() != null) {
                parseResult(resultLine.get(), result);
            }

            if (exitCode == 0) {
                NetworkDebugLogger.log("%s Game %d completed successfully (exit code 0)",
                        LOG_PREFIX, info.gameIndex);
            } else if (exitCode == 1) {
                NetworkDebugLogger.log("%s Game %d failed (exit code 1)",
                        LOG_PREFIX, info.gameIndex);
            } else {
                result.addError(info.gameIndex, "Process exited with code " + exitCode);
                NetworkDebugLogger.error("%s Game %d error (exit code %d)\nOutput:\n%s",
                        LOG_PREFIX, info.gameIndex, exitCode, output.toString());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.addError(info.gameIndex, "Interrupted");
        } catch (Exception e) {
            result.addError(info.gameIndex, e.getMessage());
        }
    }

    private void parseResult(String resultLine, ExecutionResult result) {
        try {
            // Format: gameIndex|success|deltas|turns|bytes|winner
            String[] parts = resultLine.split("\\|");
            if (parts.length >= 6) {
                int gameIndex = Integer.parseInt(parts[0]);
                boolean success = Boolean.parseBoolean(parts[1]);
                long deltas = Long.parseLong(parts[2]);
                int turns = Integer.parseInt(parts[3]);
                long bytes = Long.parseLong(parts[4]);
                String winner = "null".equals(parts[5]) ? null : parts[5];

                GameResult gameResult = new GameResult(success, deltas, turns, bytes, winner);
                result.addResult(gameIndex, gameResult);
            }
        } catch (Exception e) {
            NetworkDebugLogger.error("%s Failed to parse result: %s", LOG_PREFIX, resultLine);
        }
    }

    private static class ProcessInfo {
        final int gameIndex;
        final int port;
        final Process process;

        ProcessInfo(int gameIndex, int port, Process process) {
            this.gameIndex = gameIndex;
            this.port = port;
            this.process = process;
        }
    }

    /**
     * Result from a single game.
     */
    public static class GameResult {
        public final boolean success;
        public final long deltaPackets;
        public final int turns;
        public final long bytes;
        public final String winner;

        public GameResult(boolean success, long deltaPackets, int turns, long bytes, String winner) {
            this.success = success;
            this.deltaPackets = deltaPackets;
            this.turns = turns;
            this.bytes = bytes;
            this.winner = winner;
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

        public Map<Integer, GameResult> getResults() {
            return results;
        }

        public double getSuccessRate() {
            if (totalGames == 0) return 0.0;
            return (double) getSuccessCount() / totalGames;
        }

        public String toSummary() {
            return String.format(
                    "MultiProcess[games=%d, success=%d, failed=%d, errors=%d, timeouts=%d, " +
                            "totalDeltas=%d, totalBytes=%d, successRate=%.0f%%]",
                    totalGames, getSuccessCount(), getFailureCount(),
                    getErrorCount(), getTimeoutCount(),
                    getTotalDeltaPackets(), getTotalBytes(), getSuccessRate() * 100);
        }

        public String toDetailedReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("=".repeat(80)).append("\n");
            sb.append("Multi-Process Parallel Game Execution Report\n");
            sb.append("=".repeat(80)).append("\n\n");

            sb.append("Summary: ").append(toSummary()).append("\n\n");

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
                    sb.append(String.format(" - deltas=%d, turns=%d, winner=%s\n",
                            r.deltaPackets, r.turns, r.winner));
                } else {
                    sb.append("NO RESULT\n");
                }
            }

            return sb.toString();
        }
    }
}
