package forge.net.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores per-game metrics extracted from log file analysis.
 * Supports both delta sync structured logs and generic game logs.
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

    // Delta sync metrics — both measured via serialize+compress for apples-to-apples comparison
    private int deltaPacketCount;
    private long totalDeltaBytes;         // Serialized+compressed delta size (including events)
    private long totalFullStateBytes;     // Serialized+compressed full GameView + events size
    private long stateOnlyDeltaBytes;    // Delta size excluding events
    private long stateOnlyFullBytes;     // Full GameView size excluding events

    // Deck tracking
    private List<String> deckNames = new ArrayList<>();

    // Network performance metrics (from Encoded/send() blocked lines)
    private long totalEncodedBytes;
    private int encodedMessageCount;
    private long minEncodedBytes = Long.MAX_VALUE;
    private long maxEncodedBytes;
    private int sendBlockedCount;
    private long totalBlockedMs;
    private long minBlockedMs = Long.MAX_VALUE;
    private long maxBlockedMs;

    // Game timing
    private String firstTimestamp;
    private String lastTimestamp;

    // Error tracking
    private List<String> warnings = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
    private Map<String, Integer> errorCounts = new HashMap<>();
    private int sendErrors;
    private boolean hasChecksumMismatch;
    private Map<String, NetworkLogAnalyzer.ErrorContext> errorContexts = new LinkedHashMap<>();


    /**
     * Calculate bandwidth savings percentage based on delta vs full state bytes.
     */
    public double calculateBandwidthSavings() {
        if (totalFullStateBytes == 0) return 0.0;
        return 100.0 * (1.0 - (double) totalDeltaBytes / totalFullStateBytes);
    }

    /**
     * Check if this game was successful (completed with no critical errors).
     * Send errors and checksum mismatches are critical failures.
     * Log-detected errors (e.g. caught exceptions) are tracked but do not affect success.
     */
    public boolean isSuccessful() {
        return gameCompleted && !hasChecksumMismatch && sendErrors == 0;
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

    public long getStateOnlyDeltaBytes() {
        return stateOnlyDeltaBytes;
    }

    public void setStateOnlyDeltaBytes(long stateOnlyDeltaBytes) {
        this.stateOnlyDeltaBytes = stateOnlyDeltaBytes;
    }

    public long getStateOnlyFullBytes() {
        return stateOnlyFullBytes;
    }

    public void setStateOnlyFullBytes(long stateOnlyFullBytes) {
        this.stateOnlyFullBytes = stateOnlyFullBytes;
    }

    // Network performance getters

    public void recordEncodedMessage(long bytes) {
        encodedMessageCount++;
        totalEncodedBytes += bytes;
        if (bytes < minEncodedBytes) minEncodedBytes = bytes;
        if (bytes > maxEncodedBytes) maxEncodedBytes = bytes;
    }

    public void recordSendBlocked(long ms) {
        sendBlockedCount++;
        totalBlockedMs += ms;
        if (ms < minBlockedMs) minBlockedMs = ms;
        if (ms > maxBlockedMs) maxBlockedMs = ms;
    }

    public long getTotalEncodedBytes() { return totalEncodedBytes; }
    public int getEncodedMessageCount() { return encodedMessageCount; }
    public long getMinEncodedBytes() { return encodedMessageCount > 0 ? minEncodedBytes : 0; }
    public long getMaxEncodedBytes() { return maxEncodedBytes; }
    public double getAvgEncodedBytes() { return encodedMessageCount > 0 ? (double) totalEncodedBytes / encodedMessageCount : 0; }

    public int getSendBlockedCount() { return sendBlockedCount; }
    public long getTotalBlockedMs() { return totalBlockedMs; }
    public long getMinBlockedMs() { return sendBlockedCount > 0 ? minBlockedMs : 0; }
    public long getMaxBlockedMs() { return maxBlockedMs; }
    public double getAvgBlockedMs() { return sendBlockedCount > 0 ? (double) totalBlockedMs / sendBlockedCount : 0; }

    public String getFirstTimestamp() { return firstTimestamp; }
    public void setFirstTimestamp(String ts) { this.firstTimestamp = ts; }
    public String getLastTimestamp() { return lastTimestamp; }
    public void setLastTimestamp(String ts) { this.lastTimestamp = ts; }

    public List<String> getDeckNames() {
        return deckNames;
    }

    public void addDeckName(String deckName) {
        if (deckName != null && !deckName.isEmpty() && !deckNames.contains(deckName)) {
            deckNames.add(deckName);
        }
    }

    public int getSendErrors() {
        return sendErrors;
    }

    public void setSendErrors(int sendErrors) {
        this.sendErrors = sendErrors;
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

    /** Increment the occurrence count for a normalized error pattern. No cap. */
    public void incrementErrorCount(String normalizedError) {
        errorCounts.merge(normalizedError, 1, Integer::sum);
    }

    /** Get occurrence counts by normalized error pattern. */
    public Map<String, Integer> getErrorCounts() {
        return errorCounts;
    }

    public boolean hasChecksumMismatch() {
        return hasChecksumMismatch;
    }

    public void setHasChecksumMismatch(boolean hasChecksumMismatch) {
        this.hasChecksumMismatch = hasChecksumMismatch;
    }

    /** Get error contexts keyed by normalized error pattern. */
    public Map<String, NetworkLogAnalyzer.ErrorContext> getErrorContexts() {
        return errorContexts;
    }

    public void setErrorContexts(Map<String, NetworkLogAnalyzer.ErrorContext> errorContexts) {
        this.errorContexts = errorContexts;
    }

    /** Get the first error context (convenience for backward compatibility). */
    public NetworkLogAnalyzer.ErrorContext getErrorContext() {
        return errorContexts.isEmpty() ? null : errorContexts.values().iterator().next();
    }

    @Override
    public String toString() {
        return String.format(
                "GameLogMetrics[file=%s, players=%d, completed=%b, turns=%d, winner=%s, " +
                "packets=%d, deltaBytes=%d, fullStateBytes=%d, savings=%.1f%%, " +
                "sendErrors=%d, warnings=%d, errors=%d, checksumMismatch=%b, failureMode=%s, firstErrorTurn=%d]",
                logFileName, playerCount, gameCompleted, turnCount, winner,
                deltaPacketCount, totalDeltaBytes, totalFullStateBytes,
                calculateBandwidthSavings(), sendErrors, warnings.size(), errors.size(), hasChecksumMismatch,
                failureMode, firstErrorTurn);
    }

    /**
     * Generate a concise summary report for single-game analysis.
     * Adapts content based on whether delta sync data is present.
     */
    public String toSummaryReport() {
        StringBuilder sb = new StringBuilder();

        sb.append("\n");
        sb.append("-".repeat(60)).append("\n");
        sb.append("Game Analysis Report\n");
        sb.append("-".repeat(60)).append("\n");

        // Status
        String status = isSuccessful() ? "PASSED" : "FAILED";
        String statusReason = "";
        if (!isSuccessful()) {
            if (hasChecksumMismatch) {
                statusReason = " (checksum mismatch)";
            } else if (!gameCompleted) {
                statusReason = " (game incomplete)";
            } else if (sendErrors > 0) {
                statusReason = " (send errors)";
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

        // Bandwidth metrics (only if delta sync data present)
        if (totalFullStateBytes > 0) {
            sb.append("Bandwidth Metrics:\n");
            sb.append(String.format("  Delta Packets: %d\n", deltaPacketCount));
            sb.append(String.format("  Delta Size: %d bytes\n", totalDeltaBytes));
            sb.append(String.format("  FullState Baseline: %d bytes\n", totalFullStateBytes));
            sb.append("\n");

            sb.append("Bandwidth Savings:\n");
            double savings = calculateBandwidthSavings();
            sb.append(String.format("  Delta vs FullState: %.1f%% %s\n",
                    savings, savings >= 90 ? "(PASS >= 90%)" : "(FAIL < 90%)"));
            if (deltaPacketCount > 0) {
                sb.append(String.format("  Avg bytes per packet: %.0f\n",
                        (double) totalDeltaBytes / deltaPacketCount));
            }
            sb.append("\n");
        }

        // Validation summary
        sb.append("Validation:\n");
        sb.append(String.format("  Checksum Mismatches: %d %s\n",
                hasChecksumMismatch ? 1 : 0, hasChecksumMismatch ? "(FAIL)" : "(PASS)"));
        sb.append(String.format("  Send Errors: %d\n", sendErrors));
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
        NetworkLogAnalyzer.ErrorContext errorContext = getErrorContext();
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
}
