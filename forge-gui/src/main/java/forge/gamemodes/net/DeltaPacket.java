package forge.gamemodes.net;

import forge.card.CardStateName;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.StackItemView;
import forge.gamemodes.net.server.RemoteClient;
import forge.gamemodes.net.event.NetEvent;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;
import forge.trackable.TrackableTypes;
import forge.trackable.TrackableTypes.TrackableType;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Packet containing delta updates for TrackableObjects.
 * Contains changed properties for existing objects and full property maps for new objects.
 * Properties are sent as Map<TrackableProperty, Object> with ID substitution for object references.
 * Object type and ID are encoded in the composite delta key (upper 4 bits = type, lower 28 bits = ID).
 * Standard Java serialization handles the maps natively via Netty's ObjectEncoder.
 */
public final class DeltaPacket implements NetEvent {
    private static final long serialVersionUID = 5L;

    private final long sequenceNumber;
    private final Map<Integer, Map<TrackableProperty, Object>> objectDeltas; // Changed properties only
    private final Map<Integer, Map<TrackableProperty, Object>> newObjects; // Full property maps for new objects
    private final int checksum; // For periodic validation
    private final boolean checksumIncluded;

    // Object type constants
    public static final int TYPE_CARD_VIEW = 0;
    public static final int TYPE_PLAYER_VIEW = 1;
    public static final int TYPE_STACK_ITEM_VIEW = 2;
    public static final int TYPE_COMBAT_VIEW = 3;
    public static final int TYPE_GAME_VIEW = 4;

    /** Returns the type tag for the given TrackableObject, or -1 if unsupported. */
    public static int typeTagFor(TrackableObject obj) {
        if (obj instanceof CardView) return TYPE_CARD_VIEW;
        if (obj instanceof PlayerView) return TYPE_PLAYER_VIEW;
        if (obj instanceof StackItemView) return TYPE_STACK_ITEM_VIEW;
        if (obj instanceof GameView) return TYPE_GAME_VIEW;
        return -1;
    }

    /** Returns the TrackableType for the given type tag, or null if unknown. */
    public static TrackableType<?> trackableTypeFor(int typeTag) {
        switch (typeTag) {
            case TYPE_CARD_VIEW: return TrackableTypes.CardViewType;
            case TYPE_PLAYER_VIEW: return TrackableTypes.PlayerViewType;
            case TYPE_STACK_ITEM_VIEW: return TrackableTypes.StackItemViewType;
            default: return null;
        }
    }

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
     * Data holder for serialized CombatView state.
     * Travels inside property maps as the value for CombatViewType properties.
     * Each entry represents one attacking band with its defender, blockers, and planned blockers.
     */
    public static class CombatData implements Serializable {
        private static final long serialVersionUID = 1L;

        /** Attacker IDs per band. */
        public final List<List<Integer>> bandAttackerIds;
        /** Defender reference per band: {typeMarker, id} (0=CardView, 1=PlayerView). */
        public final List<int[]> bandDefenderRefs;
        /** Blocker IDs per band (null entry = no blockers). */
        public final List<List<Integer>> bandBlockerIds;
        /** Planned blocker IDs per band (null entry = no planned blockers). */
        public final List<List<Integer>> bandPlannedBlockerIds;

        public CombatData(List<List<Integer>> bandAttackerIds, List<int[]> bandDefenderRefs,
                          List<List<Integer>> bandBlockerIds, List<List<Integer>> bandPlannedBlockerIds) {
            this.bandAttackerIds = bandAttackerIds;
            this.bandDefenderRefs = bandDefenderRefs;
            this.bandBlockerIds = bandBlockerIds;
            this.bandPlannedBlockerIds = bandPlannedBlockerIds;
        }
    }

    /**
     * Create a new delta packet with optional checksum.
     * @param sequenceNumber monotonically increasing sequence number
     * @param objectDeltas map of composite delta key to property map (changed properties only)
     * @param newObjects map of composite delta key to full property map (for new objects)
     * @param checksum checksum of the full state for validation
     * @param checksumIncluded true if this packet includes a checksum for validation
     */
    public DeltaPacket(long sequenceNumber, Map<Integer, Map<TrackableProperty, Object>> objectDeltas,
                       Map<Integer, Map<TrackableProperty, Object>> newObjects,
                       int checksum, boolean checksumIncluded) {
        this.sequenceNumber = sequenceNumber;
        this.objectDeltas = objectDeltas != null ? new HashMap<>(objectDeltas) : new HashMap<>();
        this.newObjects = newObjects != null ? new HashMap<>(newObjects) : new HashMap<>();
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
     * Get the new objects map (full property maps for newly created objects).
     * Type and ID are encoded in the composite delta key.
     * @return unmodifiable map of composite delta key to property map
     */
    public Map<Integer, Map<TrackableProperty, Object>> getNewObjects() {
        return Collections.unmodifiableMap(newObjects);
    }

    public int getChecksum() {
        return checksum;
    }

    public boolean hasChecksum() {
        return checksumIncluded;
    }

    /**
     * Check if this packet is empty (no changes).
     * @return true if there are no deltas and no new objects
     */
    public boolean isEmpty() {
        return objectDeltas.isEmpty() && newObjects.isEmpty();
    }

    /**
     * Get the approximate size of this packet in bytes.
     */
    public int getApproximateSize() {
        int size = 8 + 4; // sequenceNumber + checksum
        for (Map<TrackableProperty, Object> delta : objectDeltas.values()) {
            size += 4 + delta.size() * 50; // key + ~50 bytes per property estimate
        }
        for (Map<TrackableProperty, Object> props : newObjects.values()) {
            size += 4 + props.size() * 50; // key + properties
        }
        return size;
    }

    @Override
    public void updateForClient(final RemoteClient client) {
        // No client-specific updates needed for delta packets
    }

    @Override
    public String toString() {
        return String.format("DeltaPacket[seq=%d, newObjects=%d, deltas=%d, size~%d bytes]",
                sequenceNumber, newObjects.size(), objectDeltas.size(), getApproximateSize());
    }
}
