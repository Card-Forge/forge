package forge.gamemodes.net;

import forge.card.CardStateName;
import forge.gamemodes.net.server.RemoteClient;
import forge.gamemodes.net.event.NetEvent;
import forge.trackable.TrackableProperty;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Packet containing delta updates for TrackableObjects.
 * Contains only the changed properties for each object, plus a list of removed object IDs.
 * Properties are sent as Map<TrackableProperty, Object> with ID substitution for object references.
 * Standard Java serialization handles the maps natively via Netty's ObjectEncoder.
 */
public final class DeltaPacket implements NetEvent {
    private static final long serialVersionUID = 3L;

    private final long sequenceNumber;
    private final Map<Integer, Map<TrackableProperty, Object>> objectDeltas; // Changed properties only
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
     * Data holder for serialized CardStateView properties.
     * Travels inside property maps as the value for CardStateViewType properties.
     */
    public static class CardStateData implements Serializable {
        private static final long serialVersionUID = 1L;

        public final int id;
        public final CardStateName state;
        public final Map<TrackableProperty, Object> properties;

        public CardStateData(int id, CardStateName state, Map<TrackableProperty, Object> properties) {
            this.id = id;
            this.state = state;
            this.properties = properties;
        }
    }

    /**
     * Data for a newly created object that needs to be sent in full.
     */
    public static class NewObjectData implements Serializable {
        private static final long serialVersionUID = 2L;

        private final int objectId;
        private final int objectType;
        private final Map<TrackableProperty, Object> properties; // ALL properties

        public NewObjectData(int objectId, int objectType, Map<TrackableProperty, Object> properties) {
            this.objectId = objectId;
            this.objectType = objectType;
            this.properties = properties;
        }

        public int getObjectId() {
            return objectId;
        }

        public int getObjectType() {
            return objectType;
        }

        public Map<TrackableProperty, Object> getProperties() {
            return properties;
        }
    }

    /**
     * Create a new delta packet with new objects and optional checksum.
     * @param sequenceNumber monotonically increasing sequence number
     * @param objectDeltas map of composite delta key to property map (changed properties only)
     * @param newObjects map of composite delta key to full object data (for new objects)
     * @param removedObjectIds set of composite delta keys that have been removed
     * @param checksum checksum of the full state for validation
     * @param checksumIncluded true if this packet includes a checksum for validation
     */
    public DeltaPacket(long sequenceNumber, Map<Integer, Map<TrackableProperty, Object>> objectDeltas,
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
     * @return map of composite delta key to property map
     */
    public Map<Integer, Map<TrackableProperty, Object>> getObjectDeltas() {
        return Collections.unmodifiableMap(objectDeltas);
    }

    /**
     * Get the set of removed object IDs.
     * @return unmodifiable set of removed composite delta keys
     */
    public Set<Integer> getRemovedObjectIds() {
        return Collections.unmodifiableSet(removedObjectIds);
    }

    /**
     * Get the new objects map (full object data for newly created objects).
     * @return unmodifiable map of composite delta key to NewObjectData
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
     * Estimating Map size is less precise than counting bytes, but still useful for monitoring.
     * @return approximate size in bytes
     */
    public int getApproximateSize() {
        int size = 8 + 4; // sequenceNumber + checksum
        for (Map<TrackableProperty, Object> delta : objectDeltas.values()) {
            size += 4 + delta.size() * 50; // key + ~50 bytes per property estimate
        }
        for (NewObjectData newObj : newObjects.values()) {
            size += 4 + 4 + newObj.getProperties().size() * 50; // objectId + objectType + properties
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
