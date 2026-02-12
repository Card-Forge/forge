package forge.gamemodes.net;

import forge.gamemodes.net.server.RemoteClient;
import forge.gamemodes.net.event.NetEvent;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Packet containing delta updates for TrackableObjects.
 * Contains only the changed properties for each object, plus a list of removed object IDs.
 * Also includes new objects that need to be created on the client (full serialization).
 */
public final class DeltaPacket implements NetEvent {
    private static final long serialVersionUID = 2L;

    private final long sequenceNumber;
    private final Map<Integer, byte[]> objectDeltas;      // Changed properties only
    private final Map<Integer, NewObjectData> newObjects; // Full object data for newly created objects
    private final Set<Integer> removedObjectIds;
    private final int checksum; // For periodic validation
    private final boolean checksumIncluded;

    // Object type constants (shared by NewObjectData and DeltaData)
    public static final int TYPE_CARD_VIEW = 0;
    public static final int TYPE_PLAYER_VIEW = 1;
    public static final int TYPE_STACK_ITEM_VIEW = 2;
    public static final int TYPE_COMBAT_VIEW = 3;
    public static final int TYPE_GAME_VIEW = 4;

    /**
     * Data for a newly created object that needs to be sent in full.
     */
    public static class NewObjectData implements Serializable {
        private static final long serialVersionUID = 1L;

        private final int objectId;
        private final int objectType;
        private final byte[] fullProperties; // ALL properties serialized

        public NewObjectData(int objectId, int objectType, byte[] fullProperties) {
            this.objectId = objectId;
            this.objectType = objectType;
            this.fullProperties = fullProperties;
        }

        public int getObjectId() {
            return objectId;
        }

        public int getObjectType() {
            return objectType;
        }

        public byte[] getFullProperties() {
            return fullProperties;
        }
    }

    /**
     * Create a new delta packet.
     * @param sequenceNumber monotonically increasing sequence number
     * @param objectDeltas map of object ID to serialized delta bytes
     * @param removedObjectIds set of object IDs that have been removed
     */
    public DeltaPacket(long sequenceNumber, Map<Integer, byte[]> objectDeltas, Set<Integer> removedObjectIds) {
        this(sequenceNumber, objectDeltas, null, removedObjectIds, 0, false);
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
        this(sequenceNumber, objectDeltas, null, removedObjectIds, checksum, true);
    }

    /**
     * Create a new delta packet with new objects and optional checksum.
     * @param sequenceNumber monotonically increasing sequence number
     * @param objectDeltas map of object ID to serialized delta bytes (changed properties only)
     * @param newObjects map of object ID to full object data (for new objects)
     * @param removedObjectIds set of object IDs that have been removed
     * @param checksum checksum of the full state for validation
     * @param checksumIncluded true if this packet includes a checksum for validation
     */
    public DeltaPacket(long sequenceNumber, Map<Integer, byte[]> objectDeltas,
                       Map<Integer, NewObjectData> newObjects, Set<Integer> removedObjectIds,
                       int checksum, boolean checksumIncluded) {
        this.sequenceNumber = sequenceNumber;
        this.objectDeltas = objectDeltas != null ? new HashMap<>(objectDeltas) : new HashMap<>();
        this.newObjects = newObjects != null ? new HashMap<>(newObjects) : new HashMap<>();
        this.removedObjectIds = removedObjectIds != null ? new HashSet<>(removedObjectIds) : new HashSet<>();
        this.checksum = checksum;
        this.checksumIncluded = checksumIncluded;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
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
     * Get the new objects map (full object data for newly created objects).
     * @return unmodifiable map of object ID to NewObjectData
     */
    public Map<Integer, NewObjectData> getNewObjects() {
        return Collections.unmodifiableMap(newObjects);
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
        return checksumIncluded;
    }

    /**
     * Check if this packet is empty (no changes).
     * @return true if there are no deltas, no new objects, and no removals
     */
    public boolean isEmpty() {
        return objectDeltas.isEmpty() && newObjects.isEmpty() && removedObjectIds.isEmpty();
    }

    /**
     * Get the approximate size of this packet in bytes.
     * Useful for monitoring bandwidth usage.
     * @return approximate size in bytes
     */
    public int getApproximateSize() {
        int size = 8 + 4; // sequenceNumber + checksum
        for (byte[] delta : objectDeltas.values()) {
            size += 4 + delta.length; // object ID + delta data
        }
        for (NewObjectData newObj : newObjects.values()) {
            size += 4 + 4 + newObj.getFullProperties().length; // objectId + objectType + data
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
        return String.format("DeltaPacket[seq=%d, newObjects=%d, deltas=%d, removed=%d, size~%d bytes]",
                sequenceNumber, newObjects.size(), objectDeltas.size(), removedObjectIds.size(), getApproximateSize());
    }
}
