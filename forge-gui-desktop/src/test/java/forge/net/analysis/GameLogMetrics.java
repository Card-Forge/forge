package forge.net.analysis;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores per-game metrics extracted from log file analysis.
 */
public class GameLogMetrics {

    /**
     * Classification of how a game failed (or NONE if it succeeded).
     */
    public enum FailureMode {
        NONE,              // Game completed successfully
        TIMEOUT,           // Game exceeded time limit
        CHECKSUM_MISMATCH, // Delta sync desync detected
        EXCEPTION,         // Exception/error logged
        INCOMPLETE         // Game didn't complete for unknown reason
    }

    // Failure tracking
    private FailureMode failureMode = FailureMode.NONE;
    private int firstErrorTurn = -1;  // Turn when first error occurred (-1 = no error)

    // Game identification
    private String logFileName;
    private int gameIndex = -1;
    private int playerCount = 2;

    // Game completion status
    private boolean gameCompleted;
    private int turnCount;
    private String winner;

    // Delta sync metrics
    private int deltaPacketCount;
    private long totalApproximateBytes;  // Estimated delta size from object diffs
    private long totalDeltaBytes;         // Actual bytes sent over network (ActualNetwork)
    private long totalFullStateBytes;     // Size if full state was sent
    // Deck tracking
    private List<String> deckNames = new ArrayList<>();

    // Error tracking
    private List<String> warnings = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
    private boolean hasChecksumMismatch;
    private NetworkLogAnalyzer.ErrorContext errorContext;


    /**
     * Calculate bandwidth savings percentage based on delta vs full state bytes.
     */
    public double calculateBandwidthSavings() {
        if (totalFullStateBytes == 0) return 0.0;
        return 100.0 * (1.0 - (double) totalDeltaBytes / totalFullStateBytes);
    }

    /**
     * Check if this game was successful (completed with no errors).
     */
    public boolean isSuccessful() {
        return gameCompleted && !hasChecksumMismatch && errors.isEmpty();
    }

    // Getters and setters

    public FailureMode getFailureMode() {
        return failureMode;
    }

    public void setFailureMode(FailureMode failureMode) {
        this.failureMode = failureMode;
    }

    public int getFirstErrorTurn() {
        return firstErrorTurn;
    }

    public void setFirstErrorTurn(int firstErrorTurn) {
        this.firstErrorTurn = firstErrorTurn;
    }

    public String getLogFileName() {
        return logFileName;
    }

    public void setLogFileName(String logFileName) {
        this.logFileName = logFileName;
    }

    public int getGameIndex() {
        return gameIndex;
    }

    public void setGameIndex(int gameIndex) {
        this.gameIndex = gameIndex;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public boolean isGameCompleted() {
        return gameCompleted;
    }

    public void setGameCompleted(boolean gameCompleted) {
        this.gameCompleted = gameCompleted;
    }

    public int getTurnCount() {
        return turnCount;
    }

    public void setTurnCount(int turnCount) {
        this.turnCount = turnCount;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public int getDeltaPacketCount() {
        return deltaPacketCount;
    }

    public void setDeltaPacketCount(int deltaPacketCount) {
        this.deltaPacketCount = deltaPacketCount;
    }

    public long getTotalApproximateBytes() {
        return totalApproximateBytes;
    }

    public void setTotalApproximateBytes(long totalApproximateBytes) {
        this.totalApproximateBytes = totalApproximateBytes;
    }

    public long getTotalDeltaBytes() {
        return totalDeltaBytes;
    }

    public void setTotalDeltaBytes(long totalDeltaBytes) {
        this.totalDeltaBytes = totalDeltaBytes;
    }

    public long getTotalFullStateBytes() {
        return totalFullStateBytes;
    }

    public void setTotalFullStateBytes(long totalFullStateBytes) {
        this.totalFullStateBytes = totalFullStateBytes;
    }

    public List<String> getDeckNames() {
        return deckNames;
    }

    public void addDeckName(String deckName) {
        if (deckName != null && !deckName.isEmpty() && !deckNames.contains(deckName)) {
            deckNames.add(deckName);
        }
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void addWarning(String warning) {
        if (warnings.size() < 100) { // Cap at 100 to limit memory
            warnings.add(warning);
        }
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addError(String error) {
        if (errors.size() < 100) { // Cap at 100 to limit memory
            errors.add(error);
        }
    }

    public boolean hasChecksumMismatch() {
        return hasChecksumMismatch;
    }

    public void setHasChecksumMismatch(boolean hasChecksumMismatch) {
        this.hasChecksumMismatch = hasChecksumMismatch;
    }

    public NetworkLogAnalyzer.ErrorContext getErrorContext() {
        return errorContext;
    }

    public void setErrorContext(NetworkLogAnalyzer.ErrorContext errorContext) {
        this.errorContext = errorContext;
    }

    @Override
    public String toString() {
        return String.format(
                "GameLogMetrics[file=%s, players=%d, completed=%b, turns=%d, winner=%s, " +
                "packets=%d, deltaBytes=%d, fullStateBytes=%d, savings=%.1f%%, " +
                "warnings=%d, errors=%d, checksumMismatch=%b, failureMode=%s, firstErrorTurn=%d]",
                logFileName, playerCount, gameCompleted, turnCount, winner,
                deltaPacketCount, totalDeltaBytes, totalFullStateBytes,
                calculateBandwidthSavings(), warnings.size(), errors.size(), hasChecksumMismatch,
                failureMode, firstErrorTurn);
    }

    /**
     * Generate a concise summary report for single-game analysis.
     * Shows key validation metrics without overwhelming detail.
     */
    public String toSummaryReport() {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("\n");
        sb.append("-".repeat(60)).append("\n");
        sb.append("Delta Sync Analysis Report\n");
        sb.append("-".repeat(60)).append("\n");

        // Status
        String status = isSuccessful() ? "PASSED" : "FAILED";
        String statusReason = "";
        if (!isSuccessful()) {
            if (hasChecksumMismatch) {
                statusReason = " (checksum mismatch)";
            } else if (!gameCompleted) {
                statusReason = " (game incomplete)";
            } else if (!errors.isEmpty()) {
                statusReason = " (errors detected)";
            }
        }
        sb.append(String.format("Status: %s%s\n", status, statusReason));
        sb.append(String.format("Failure Mode: %s\n", failureMode));
        sb.append("\n");

        // Game metrics
        sb.append("Game Metrics:\n");
        sb.append(String.format("  Players: %d\n", playerCount));
        sb.append(String.format("  Turns: %d\n", turnCount));
        sb.append(String.format("  Winner: %s\n", winner != null ? winner : "none"));
        sb.append("\n");

        // Bandwidth metrics (the key validation data)
        sb.append("Bandwidth Metrics:\n");
        sb.append(String.format("  Delta Packets: %d\n", deltaPacketCount));
        sb.append(String.format("  Approximate Size: %s\n", formatBytes(totalApproximateBytes)));
        sb.append(String.format("  ActualNetwork Size: %s\n", formatBytes(totalDeltaBytes)));
        sb.append(String.format("  FullState Baseline: %s\n", formatBytes(totalFullStateBytes)));
        sb.append("\n");

        // Bandwidth savings (the primary validation metric)
        sb.append("Bandwidth Savings:\n");
        double actualVsFull = calculateBandwidthSavings();
        double approxVsFull = totalFullStateBytes > 0 ?
                100.0 * (1.0 - (double) totalApproximateBytes / totalFullStateBytes) : 0;
        sb.append(String.format("  ActualNetwork vs FullState: %.1f%% %s\n",
                actualVsFull, actualVsFull >= 90 ? "(PASS >= 90%)" : "(FAIL < 90%)"));
        sb.append(String.format("  Approximate vs FullState: %.1f%%\n", approxVsFull));
        if (deltaPacketCount > 0) {
            sb.append(String.format("  Avg bytes per packet: %.0f\n",
                    (double) totalDeltaBytes / deltaPacketCount));
        }
        sb.append("\n");

        // Validation summary
        sb.append("Validation:\n");
        sb.append(String.format("  Checksum Mismatches: %d %s\n",
                hasChecksumMismatch ? 1 : 0, hasChecksumMismatch ? "(FAIL)" : "(PASS)"));
        sb.append(String.format("  Errors: %d\n", errors.size()));
        sb.append(String.format("  Warnings: %d\n", warnings.size()));

        // Show first error if any
        if (!errors.isEmpty()) {
            sb.append("\n");
            sb.append("First Error:\n");
            sb.append("  ").append(errors.get(0)).append("\n");
            if (firstErrorTurn >= 0) {
                sb.append(String.format("  (occurred at turn %d)\n", firstErrorTurn));
            }
        }

        // Error context if available
        if (errorContext != null && errorContext.errorMessage() != null) {
            sb.append("\n");
            sb.append("Error Context:\n");
            for (String line : errorContext.linesBefore()) {
                sb.append("  ").append(line).append("\n");
            }
            sb.append(">>> ").append(errorContext.errorMessage()).append("\n");
            for (String line : errorContext.linesAfter()) {
                sb.append("  ").append(line).append("\n");
            }
        }

        sb.append("-".repeat(60)).append("\n");

        return sb.toString();
    }

    /**
     * Format bytes in human-readable form.
     */
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    /**
     * Represents metrics for a single delta sync packet.
     */
}
