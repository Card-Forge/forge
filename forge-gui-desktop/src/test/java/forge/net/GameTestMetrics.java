package forge.net;

import forge.gamemodes.net.NetworkByteTracker;

/**
 * Aggregates metrics from automated test runs.
 *
 * Supports both network and non-network test modes with mode-aware metrics.
 * Network-specific metrics (bytes, packets) are only populated for network modes.
 *
 * Phase 6 of the Automated Network Testing Plan.
 */
public class GameTestMetrics {

    // Test mode configuration
    private GameTestMode testMode;

    // Game metrics (common to all modes)
    private int turnCount;
    private long gameDurationMs;
    private String winner;
    private boolean gameCompleted;

    // Network metrics (from NetworkByteTracker)
    private long totalBytesSent;
    private long deltaBytesSent;
    private long fullStateBytesSent;
    private long deltaPacketCount;
    private long fullStatePacketCount;

    // Reconnection metrics
    private int disconnectionCount;
    private int reconnectionSuccesses;
    private int reconnectionFailures;

    // Error tracking
    private String errorMessage;
    private Exception exception;

    /**
     * Collect metrics from NetworkByteTracker.
     */
    public void collectFromTracker(NetworkByteTracker tracker) {
        if (tracker != null) {
            this.totalBytesSent = tracker.getTotalBytesSent();
            this.deltaBytesSent = tracker.getDeltaBytesSent();
            this.fullStateBytesSent = tracker.getFullStateBytesSent();
            this.deltaPacketCount = tracker.getDeltaPacketCount();
            this.fullStatePacketCount = tracker.getFullStatePacketCount();
        }
    }

    /**
     * Check if this is a network test mode.
     *
     * @return true if NETWORK_LOCAL or NETWORK_REMOTE, false if LOCAL
     */
    public boolean isNetworkTest() {
        return testMode != null && testMode.isNetworkMode();
    }

    /**
     * Generate a summary string for logging.
     * Format varies based on test mode (network vs non-network).
     */
    public String toSummary() {
        StringBuilder sb = new StringBuilder();

        // Mode prefix
        if (testMode != null) {
            sb.append(String.format("[%s] ", testMode.name()));
        }

        sb.append(String.format("Game %s: ", gameCompleted ? "COMPLETED" : "FAILED"));

        if (gameCompleted) {
            sb.append(String.format("Turns=%d, Duration=%dms, Winner=%s",
                turnCount, gameDurationMs, winner != null ? winner : "N/A"));
        } else if (errorMessage != null) {
            sb.append("Error: " + errorMessage);
        }

        // Network metrics only for network modes
        if (isNetworkTest() && totalBytesSent > 0) {
            sb.append(String.format(", Network: %d bytes total (%d delta/%d pkts, %d fullstate/%d pkts)",
                totalBytesSent, deltaBytesSent, deltaPacketCount,
                fullStateBytesSent, fullStatePacketCount));
        } else if (!isNetworkTest() && gameCompleted) {
            sb.append(" (no network overhead)");
        }

        return sb.toString();
    }

    // Getters and setters

    public int getTurnCount() {
        return turnCount;
    }

    public void setTurnCount(int turnCount) {
        this.turnCount = turnCount;
    }

    public long getGameDurationMs() {
        return gameDurationMs;
    }

    public void setGameDurationMs(long gameDurationMs) {
        this.gameDurationMs = gameDurationMs;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public boolean isGameCompleted() {
        return gameCompleted;
    }

    public void setGameCompleted(boolean gameCompleted) {
        this.gameCompleted = gameCompleted;
    }

    public long getTotalBytesSent() {
        return totalBytesSent;
    }

    public void setTotalBytesSent(long totalBytesSent) {
        this.totalBytesSent = totalBytesSent;
    }

    public long getDeltaBytesSent() {
        return deltaBytesSent;
    }

    public void setDeltaBytesSent(long deltaBytesSent) {
        this.deltaBytesSent = deltaBytesSent;
    }

    public long getFullStateBytesSent() {
        return fullStateBytesSent;
    }

    public void setFullStateBytesSent(long fullStateBytesSent) {
        this.fullStateBytesSent = fullStateBytesSent;
    }

    public long getDeltaPacketCount() {
        return deltaPacketCount;
    }

    public void setDeltaPacketCount(long deltaPacketCount) {
        this.deltaPacketCount = deltaPacketCount;
    }

    public long getFullStatePacketCount() {
        return fullStatePacketCount;
    }

    public void setFullStatePacketCount(long fullStatePacketCount) {
        this.fullStatePacketCount = fullStatePacketCount;
    }

    public int getDisconnectionCount() {
        return disconnectionCount;
    }

    public void setDisconnectionCount(int disconnectionCount) {
        this.disconnectionCount = disconnectionCount;
    }

    public int getReconnectionSuccesses() {
        return reconnectionSuccesses;
    }

    public void setReconnectionSuccesses(int reconnectionSuccesses) {
        this.reconnectionSuccesses = reconnectionSuccesses;
    }

    public int getReconnectionFailures() {
        return reconnectionFailures;
    }

    public void setReconnectionFailures(int reconnectionFailures) {
        this.reconnectionFailures = reconnectionFailures;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public GameTestMode getTestMode() {
        return testMode;
    }

    public void setTestMode(GameTestMode testMode) {
        this.testMode = testMode;
    }
}
