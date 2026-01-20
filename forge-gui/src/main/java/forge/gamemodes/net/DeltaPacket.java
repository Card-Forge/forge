package forge.gamemodes.net;

import forge.gamemodes.net.server.RemoteClient;
import forge.gamemodes.net.event.NetEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Packet containing delta updates for TrackableObjects.
 * Contains only the changed properties for each object, plus a list of removed object IDs.
 */
public final class DeltaPacket implements NetEvent {
    private static final long serialVersionUID = 1L;

    private final long sequenceNumber;
    private final long timestamp;
    private final Map<Integer, byte[]> objectDeltas;
    private final Set<Integer> removedObjectIds;
    private final int checksum; // For periodic validation

    /**
     * Create a new delta packet.
     * @param sequenceNumber monotonically increasing sequence number
     * @param objectDeltas map of object ID to serialized delta bytes
     * @param removedObjectIds set of object IDs that have been removed
     */
    public DeltaPacket(long sequenceNumber, Map<Integer, byte[]> objectDeltas, Set<Integer> removedObjectIds) {
        this(sequenceNumber, objectDeltas, removedObjectIds, 0);
    }

    /**
     * Create a new delta packet with optional checksum.
     * @param sequenceNumber monotonically increasing sequence number
     * @param objectDeltas map of object ID to serialized delta bytes
     * @param removedObjectIds set of object IDs that have been removed
     * @param checksum checksum of the full state for validation (0 if not included)
     */
    public DeltaPacket(long sequenceNumber, Map<Integer, byte[]> objectDeltas,
                       Set<Integer> removedObjectIds, int checksum) {
        this.sequenceNumber = sequenceNumber;
        this.timestamp = System.currentTimeMillis();
        this.objectDeltas = objectDeltas != null ? new HashMap<>(objectDeltas) : new HashMap<>();
        this.removedObjectIds = removedObjectIds != null ? new HashSet<>(removedObjectIds) : new HashSet<>();
        this.checksum = checksum;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Get the object deltas as an unmodifiable view.
     * @return map of object ID to serialized delta bytes
     */
    public Map<Integer, byte[]> getObjectDeltas() {
        return Collections.unmodifiableMap(objectDeltas);
    }

    /**
     * Get the set of removed object IDs.
     * @return unmodifiable set of removed object IDs
     */
    public Set<Integer> getRemovedObjectIds() {
        return Collections.unmodifiableSet(removedObjectIds);
    }

    /**
     * Get the checksum for state validation.
     * @return checksum value, or 0 if not included in this packet
     */
    public int getChecksum() {
        return checksum;
    }

    /**
     * Check if this packet contains a checksum for validation.
     * @return true if checksum is included
     */
    public boolean hasChecksum() {
        return checksum != 0;
    }

    /**
     * Check if this packet is empty (no changes).
     * @return true if there are no deltas and no removals
     */
    public boolean isEmpty() {
        return objectDeltas.isEmpty() && removedObjectIds.isEmpty();
    }

    /**
     * Get the approximate size of this packet in bytes.
     * Useful for monitoring bandwidth usage.
     * @return approximate size in bytes
     */
    public int getApproximateSize() {
        int size = 8 + 8 + 4; // sequenceNumber + timestamp + checksum
        for (byte[] delta : objectDeltas.values()) {
            size += 4 + delta.length; // object ID + delta data
        }
        size += removedObjectIds.size() * 4; // removed IDs
        return size;
    }

    @Override
    public void updateForClient(final RemoteClient client) {
        // No client-specific updates needed for delta packets
    }

    @Override
    public String toString() {
        return String.format("DeltaPacket[seq=%d, deltas=%d, removed=%d, size~%d bytes]",
                sequenceNumber, objectDeltas.size(), removedObjectIds.size(), getApproximateSize());
    }
}
