package forge.gamemodes.net;

import forge.card.CardStateName;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
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
    private static final long serialVersionUID = 8L;

    private final long sequenceNumber;
    private final Map<Integer, Map<TrackableProperty, Object>> objectDeltas;
    private final Map<Integer, Map<TrackableProperty, Object>> newObjects;
    private final int checksum;
    /** Ordinals of TrackableProperty values included in the sampled checksum, or null. */
    private final int[] checksumProperties;
    private List<Object> proxiedEvents;

    public static final int TYPE_CARD_VIEW = 0;
    public static final int TYPE_PLAYER_VIEW = 1;
    public static final int TYPE_STACK_ITEM_VIEW = 2;
    public static final int TYPE_GAME_VIEW = 3;
    public static final int TYPE_CSV = 4;

    static {
        if (CardStateName.values().length > 16) {
            throw new AssertionError("CardStateName has " + CardStateName.values().length
                    + " values; CSV delta key encoding supports at most 16");
        }
    }

    public static int typeTagFor(TrackableObject obj) {
        if (obj instanceof CardStateView) return TYPE_CSV;
        if (obj instanceof CardView) return TYPE_CARD_VIEW;
        if (obj instanceof PlayerView) return TYPE_PLAYER_VIEW;
        if (obj instanceof StackItemView) return TYPE_STACK_ITEM_VIEW;
        if (obj instanceof GameView) return TYPE_GAME_VIEW;
        return -1;
    }

    /**
     * Create a composite delta key encoding both object type and ID.
     * Upper 4 bits = type (0-15), lower 28 bits = ID.
     */
    public static int makeDeltaKey(TrackableObject obj) {
        int type = typeTagFor(obj);
        int id = obj.getId();
        if (obj instanceof CardStateView csv) {
            id *= 16 + csv.getState().ordinal();
        }
        return (type << 28) | (id & 0x0FFFFFFF);
    }

    public static int getTypeFromDeltaKey(int deltaKey) {
        return (deltaKey >>> 28) & 0xF;
    }

    public static int getIdFromDeltaKey(int deltaKey) {
        int id = deltaKey & 0x0FFFFFFF;
        if ((id & 0x08000000) != 0) {
            id |= 0xF0000000;
        }
        return id;
    }

    public static TrackableType<?> trackableTypeFor(int typeTag) {
        switch (typeTag) {
            case TYPE_CARD_VIEW: return TrackableTypes.CardViewType;
            case TYPE_PLAYER_VIEW: return TrackableTypes.PlayerViewType;
            case TYPE_STACK_ITEM_VIEW: return TrackableTypes.StackItemViewType;
            default: return null;
        }
    }

    /** Each entry represents one attacking band with its defender, blockers, and planned blockers. */
    public static class CombatData implements Serializable {
        private static final long serialVersionUID = 1L;

        public final List<List<Integer>> bandAttackerIds;
        /** {typeMarker, id} per band — 0=CardView, 1=PlayerView. */
        public final List<int[]> bandDefenderRefs;
        public final List<List<Integer>> bandBlockerIds;
        public final List<List<Integer>> bandPlannedBlockerIds;

        public CombatData(List<List<Integer>> bandAttackerIds, List<int[]> bandDefenderRefs,
                          List<List<Integer>> bandBlockerIds, List<List<Integer>> bandPlannedBlockerIds) {
            this.bandAttackerIds = bandAttackerIds;
            this.bandDefenderRefs = bandDefenderRefs;
            this.bandBlockerIds = bandBlockerIds;
            this.bandPlannedBlockerIds = bandPlannedBlockerIds;
        }
    }

    /** Create an events-only DeltaPacket with no state deltas (seq=-1 means no ack needed). */
    public static DeltaPacket eventsOnly(List<Object> proxiedEvents) {
        DeltaPacket packet = new DeltaPacket(-1L, null, null, 0, null);
        packet.setProxiedEvents(proxiedEvents);
        return packet;
    }

    public DeltaPacket(long sequenceNumber, Map<Integer, Map<TrackableProperty, Object>> objectDeltas,
                       Map<Integer, Map<TrackableProperty, Object>> newObjects,
                       int checksum, int[] checksumProperties) {
        this.sequenceNumber = sequenceNumber;
        this.objectDeltas = objectDeltas != null ? objectDeltas : Collections.emptyMap();
        this.newObjects = newObjects != null ? newObjects : Collections.emptyMap();
        this.checksum = checksum;
        this.checksumProperties = checksumProperties;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public Map<Integer, Map<TrackableProperty, Object>> getObjectDeltas() {
        return Collections.unmodifiableMap(objectDeltas);
    }

    public Map<Integer, Map<TrackableProperty, Object>> getNewObjects() {
        return Collections.unmodifiableMap(newObjects);
    }

    public int getChecksum() {
        return checksum;
    }

    public boolean hasChecksum() {
        return checksumProperties != null;
    }

    /** Property ordinals included in the sampled checksum, or null for legacy checksum. */
    public int[] getChecksumProperties() {
        return checksumProperties;
    }

    public void setProxiedEvents(List<Object> events) {
        this.proxiedEvents = events;
    }

    public List<Object> getProxiedEvents() {
        return proxiedEvents;
    }

    public boolean hasEvents() {
        return proxiedEvents != null && !proxiedEvents.isEmpty();
    }

    /** Return a shallow copy without proxied events, for state-only size measurement. */
    public DeltaPacket withoutEvents() {
        return new DeltaPacket(sequenceNumber, objectDeltas, newObjects, checksum, checksumProperties);
    }

    public boolean isEmpty() {
        return objectDeltas.isEmpty() && newObjects.isEmpty() && !hasEvents() && !hasChecksum();
    }

    public int getApproximateSize() {
        int size = 8 + 4; // sequenceNumber + checksum
        if (checksumProperties != null) {
            size += 4 + checksumProperties.length * 4; // array overhead + int per ordinal
        }
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
    }

    @Override
    public String toString() {
        return String.format("DeltaPacket[seq=%d, newObjects=%d, deltas=%d, size~%d bytes]",
                sequenceNumber, newObjects.size(), objectDeltas.size(), getApproximateSize());
    }
}
