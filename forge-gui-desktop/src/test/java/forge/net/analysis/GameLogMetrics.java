package forge.net.analysis;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores per-game metrics extracted from log file analysis.
 */
public class GameLogMetrics {

    // Game identification
    private String logFileName;
    private int batchNumber = -1;  // -1 means not from a batch run
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
    private double averageBandwidthSavingsApproximate;
    private double averageBandwidthSavingsActual;

    // Deck tracking
    private List<String> deckNames = new ArrayList<>();

    // Error tracking
    private List<String> warnings = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
    private boolean hasChecksumMismatch;

    // Timing
    private long gameDurationMs;

    // Raw packet data for detailed analysis
    private List<PacketMetrics> packets = new ArrayList<>();

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

    public String getLogFileName() {
        return logFileName;
    }

    public void setLogFileName(String logFileName) {
        this.logFileName = logFileName;
    }

    public int getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(int batchNumber) {
        this.batchNumber = batchNumber;
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

    public double getAverageBandwidthSavingsApproximate() {
        return averageBandwidthSavingsApproximate;
    }

    public void setAverageBandwidthSavingsApproximate(double averageBandwidthSavingsApproximate) {
        this.averageBandwidthSavingsApproximate = averageBandwidthSavingsApproximate;
    }

    public double getAverageBandwidthSavingsActual() {
        return averageBandwidthSavingsActual;
    }

    public void setAverageBandwidthSavingsActual(double averageBandwidthSavingsActual) {
        this.averageBandwidthSavingsActual = averageBandwidthSavingsActual;
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

    public long getGameDurationMs() {
        return gameDurationMs;
    }

    public void setGameDurationMs(long gameDurationMs) {
        this.gameDurationMs = gameDurationMs;
    }

    public List<PacketMetrics> getPackets() {
        return packets;
    }

    public void addPacket(PacketMetrics packet) {
        packets.add(packet);
    }

    @Override
    public String toString() {
        String batchStr = batchNumber >= 0 ? "batch" + batchNumber + "-" : "";
        return String.format(
                "GameLogMetrics[file=%s, %sgame%d, players=%d, completed=%b, turns=%d, winner=%s, " +
                "packets=%d, deltaBytes=%d, fullStateBytes=%d, savings=%.1f%%, " +
                "warnings=%d, errors=%d, checksumMismatch=%b]",
                logFileName, batchStr, gameIndex, playerCount, gameCompleted, turnCount, winner,
                deltaPacketCount, totalDeltaBytes, totalFullStateBytes,
                calculateBandwidthSavings(), warnings.size(), errors.size(), hasChecksumMismatch);
    }

    /**
     * Represents metrics for a single delta sync packet.
     */
    public static class PacketMetrics {
        public final int packetNumber;
        public final long approximateBytes;
        public final long actualNetworkBytes;
        public final long fullStateBytes;
        public final double approximateSavings;
        public final double actualSavings;

        public PacketMetrics(int packetNumber, long approximateBytes, long actualNetworkBytes,
                             long fullStateBytes, double approximateSavings, double actualSavings) {
            this.packetNumber = packetNumber;
            this.approximateBytes = approximateBytes;
            this.actualNetworkBytes = actualNetworkBytes;
            this.fullStateBytes = fullStateBytes;
            this.approximateSavings = approximateSavings;
            this.actualSavings = actualSavings;
        }
    }
}
