package forge.gamemodes.net;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks actual bytes transmitted over the network.
 * This provides ground truth measurements for comparing with estimated sizes.
 *
 * Controlled by NetworkDebug.config:
 *   bandwidth.logging.enabled=true/false
 *
 * When disabled, network byte tracking has zero overhead.
 */
public class NetworkByteTracker {

    // Total bytes sent (including all protocol overhead, compression, etc.)
    private final AtomicLong totalBytesSent = new AtomicLong(0);

    // Bytes sent for delta packets
    private final AtomicLong deltaBytesSent = new AtomicLong(0);

    // Bytes sent for full state packets
    private final AtomicLong fullStateBytesSent = new AtomicLong(0);

    // Number of delta packets sent
    private final AtomicLong deltaPacketCount = new AtomicLong(0);

    // Number of full state packets sent
    private final AtomicLong fullStatePacketCount = new AtomicLong(0);

    // Whether tracking is enabled
    private volatile boolean enabled = true;

    /**
     * Record bytes sent for a network message.
     * @param bytes number of bytes actually transmitted (after compression, with all overhead)
     * @param messageType type of message (for categorization)
     */
    public void recordBytesSent(int bytes, String messageType) {
        if (!enabled) {
            return;
        }

        totalBytesSent.addAndGet(bytes);

        // Categorize by message type
        if (messageType != null) {
            if (messageType.contains("DeltaPacket") || messageType.contains("applyDelta")) {
                deltaBytesSent.addAndGet(bytes);
                deltaPacketCount.incrementAndGet();
            } else if (messageType.contains("FullStatePacket") || messageType.contains("fullStateSync")) {
                fullStateBytesSent.addAndGet(bytes);
                fullStatePacketCount.incrementAndGet();
            }
        }
    }

    /**
     * Get total bytes sent over the network.
     * @return total bytes including all overhead and compression
     */
    public long getTotalBytesSent() {
        return totalBytesSent.get();
    }

    /**
     * Get bytes sent for delta sync packets.
     * @return delta sync bytes
     */
    public long getDeltaBytesSent() {
        return deltaBytesSent.get();
    }

    /**
     * Get bytes sent for full state packets.
     * @return full state bytes
     */
    public long getFullStateBytesSent() {
        return fullStateBytesSent.get();
    }

    /**
     * Get number of delta packets sent.
     * @return delta packet count
     */
    public long getDeltaPacketCount() {
        return deltaPacketCount.get();
    }

    /**
     * Get number of full state packets sent.
     * @return full state packet count
     */
    public long getFullStatePacketCount() {
        return fullStatePacketCount.get();
    }

    /**
     * Reset all statistics.
     */
    public void reset() {
        totalBytesSent.set(0);
        deltaBytesSent.set(0);
        fullStateBytesSent.set(0);
        deltaPacketCount.set(0);
        fullStatePacketCount.set(0);
    }

    /**
     * Enable or disable byte tracking.
     * @param enabled true to enable tracking
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Check if tracking is enabled.
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get a formatted summary of network statistics.
     * @return human-readable statistics string
     */
    public String getStatsSummary() {
        return String.format(
            "Network Stats: Total=%d bytes, Delta=%d bytes (%d packets), FullState=%d bytes (%d packets)",
            totalBytesSent.get(),
            deltaBytesSent.get(),
            deltaPacketCount.get(),
            fullStateBytesSent.get(),
            fullStatePacketCount.get()
        );
    }
}
