package forge.gamemodes.net.server;

import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.combat.CombatView;
import forge.game.player.PlayerView;
import forge.game.spellability.StackItemView;
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.DeltaPacket.NewObjectData;
import forge.gamemodes.net.FullStatePacket;
import forge.gamemodes.net.NetworkPropertySerializer;
import forge.gamemodes.net.NetworkTrackableSerializer;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages delta synchronization between server and clients.
 * Tracks changes to TrackableObjects and builds minimal delta packets.
 * New objects (not yet sent to client) are serialized in full.
 * Existing objects only send changed properties.
 */
public class DeltaSyncManager {
    // How often to include a checksum for validation (every N packets)
    private static final int CHECKSUM_INTERVAL = 10;

    private final AtomicLong sequenceNumber = new AtomicLong(0);
    private final Map<Integer, Long> clientAcknowledgedSeq = new ConcurrentHashMap<>();
    private final Set<Integer> trackedObjectIds = ConcurrentHashMap.newKeySet();
    private final Set<Integer> removedObjectIds = new HashSet<>();

    // Objects that have been fully sent to the client (initial sync done)
    // Objects not in this set need full serialization when first encountered
    private final Set<Integer> sentObjectIds = ConcurrentHashMap.newKeySet();

    private long packetsSinceLastChecksum = 0;

    /**
     * Register a client for tracking.
     * @param clientIndex the client's player index
     */
    public void registerClient(int clientIndex) {
        clientAcknowledgedSeq.put(clientIndex, 0L);
    }

    /**
     * Unregister a client.
     * @param clientIndex the client's player index
     */
    public void unregisterClient(int clientIndex) {
        clientAcknowledgedSeq.remove(clientIndex);
    }

    /**
     * Process an acknowledgment from a client.
     * @param clientIndex the client's player index
     * @param acknowledgedSeq the sequence number being acknowledged
     */
    public void processAcknowledgment(int clientIndex, long acknowledgedSeq) {
        clientAcknowledgedSeq.compute(clientIndex, (k, v) -> {
            if (v == null) return acknowledgedSeq;
            return Math.max(v, acknowledgedSeq);
        });

        // Check if all clients have acknowledged - if so, we can clear changes
        long minAcked = getMinAcknowledgedSequence();
        if (minAcked > 0) {
            // All clients have caught up, changes for older sequences can be cleared
        }
    }

    /**
     * Get the minimum acknowledged sequence across all clients.
     * @return minimum acknowledged sequence number
     */
    public long getMinAcknowledgedSequence() {
        return clientAcknowledgedSeq.values().stream()
                .mapToLong(Long::longValue)
                .min()
                .orElse(0L);
    }

    /**
     * Collect all changes from the GameView hierarchy and build a delta packet.
     * New objects (not yet sent to client) are serialized in full.
     * Existing objects only send changed properties.
     * @param gameView the game view to collect changes from
     * @return a DeltaPacket containing all changes, or null if no changes
     */
    public DeltaPacket collectDeltas(GameView gameView) {
        if (gameView == null) {
            return null;
        }

        Map<Integer, byte[]> objectDeltas = new HashMap<>();
        Map<Integer, NewObjectData> newObjects = new HashMap<>();
        Set<Integer> currentObjectIds = new HashSet<>();

        // Collect changes from GameView itself
        collectObjectDelta(gameView, objectDeltas, newObjects, currentObjectIds);

        // Collect changes from all players
        if (gameView.getPlayers() != null) {
            for (PlayerView player : gameView.getPlayers()) {
                collectPlayerDeltas(player, objectDeltas, newObjects, currentObjectIds);
            }
        }

        // Collect changes from the stack
        if (gameView.getStack() != null) {
            for (StackItemView stackItem : gameView.getStack()) {
                collectObjectDelta(stackItem, objectDeltas, newObjects, currentObjectIds);
            }
        }

        // Collect changes from combat
        if (gameView.getCombat() != null) {
            collectObjectDelta(gameView.getCombat(), objectDeltas, newObjects, currentObjectIds);
        }

        // Detect removed objects
        Set<Integer> nowRemoved = new HashSet<>(trackedObjectIds);
        nowRemoved.removeAll(currentObjectIds);
        removedObjectIds.addAll(nowRemoved);

        // Also remove from sentObjectIds when objects are removed
        sentObjectIds.removeAll(nowRemoved);

        // Update tracked objects
        trackedObjectIds.clear();
        trackedObjectIds.addAll(currentObjectIds);

        // After collecting, mark all current objects as sent
        sentObjectIds.addAll(currentObjectIds);

        // Build the packet
        long seq = sequenceNumber.incrementAndGet();
        packetsSinceLastChecksum++;

        // Include checksum periodically
        int checksum = 0;
        if (packetsSinceLastChecksum >= CHECKSUM_INTERVAL) {
            checksum = computeStateChecksum(gameView);
            packetsSinceLastChecksum = 0;
        }

        DeltaPacket packet = new DeltaPacket(seq, objectDeltas, newObjects, new HashSet<>(removedObjectIds), checksum);

        // Log new objects for debugging
        if (!newObjects.isEmpty()) {
            System.out.println(String.format("[DeltaSync] New objects: %d, Deltas: %d, Removed: %d",
                    newObjects.size(), objectDeltas.size(), removedObjectIds.size()));
        }

        // Clear removedObjectIds after including in packet
        removedObjectIds.clear();

        return packet;
    }

    /**
     * Collect delta for a single TrackableObject.
     * If the object hasn't been sent before, serialize ALL properties.
     * If already sent, only serialize changed properties.
     */
    private void collectObjectDelta(TrackableObject obj, Map<Integer, byte[]> objectDeltas,
                                    Map<Integer, NewObjectData> newObjects, Set<Integer> currentObjectIds) {
        if (obj == null) {
            return;
        }

        int objId = obj.getId();
        currentObjectIds.add(objId);

        // Check if this is a new object (not yet sent to client)
        if (!sentObjectIds.contains(objId)) {
            // New object - serialize ALL properties
            NewObjectData newObjData = serializeNewObject(obj);
            if (newObjData != null) {
                newObjects.put(objId, newObjData);
            }
        } else if (obj.hasChanges()) {
            // Existing object with changes - serialize only changed properties
            byte[] serialized = serializeChanges(obj);
            if (serialized != null && serialized.length > 0) {
                objectDeltas.put(objId, serialized);
            }
        }
    }

    /**
     * Collect deltas from a PlayerView and all its associated objects.
     */
    private void collectPlayerDeltas(PlayerView player, Map<Integer, byte[]> objectDeltas,
                                     Map<Integer, NewObjectData> newObjects, Set<Integer> currentObjectIds) {
        if (player == null) {
            return;
        }

        collectObjectDelta(player, objectDeltas, newObjects, currentObjectIds);

        // Collect from player's cards in various zones
        collectCardsFromZone(player.getHand(), objectDeltas, newObjects, currentObjectIds);
        collectCardsFromZone(player.getGraveyard(), objectDeltas, newObjects, currentObjectIds);
        collectCardsFromZone(player.getLibrary(), objectDeltas, newObjects, currentObjectIds);
        collectCardsFromZone(player.getExile(), objectDeltas, newObjects, currentObjectIds);
        collectCardsFromZone(player.getFlashback(), objectDeltas, newObjects, currentObjectIds);
        collectCardsFromZone(player.getCommanders(), objectDeltas, newObjects, currentObjectIds);
        collectCardsFromZone(player.getAnte(), objectDeltas, newObjects, currentObjectIds);

        // Collect battlefield cards
        if (player.getBattlefield() != null) {
            for (CardView card : player.getBattlefield()) {
                collectCardDelta(card, objectDeltas, newObjects, currentObjectIds);
            }
        }
    }

    /**
     * Collect deltas from a zone's cards.
     */
    private void collectCardsFromZone(Iterable<CardView> cards, Map<Integer, byte[]> objectDeltas,
                                      Map<Integer, NewObjectData> newObjects, Set<Integer> currentObjectIds) {
        if (cards == null) {
            return;
        }
        for (CardView card : cards) {
            collectCardDelta(card, objectDeltas, newObjects, currentObjectIds);
        }
    }

    /**
     * Collect delta from a card and its associated objects.
     */
    private void collectCardDelta(CardView card, Map<Integer, byte[]> objectDeltas,
                                  Map<Integer, NewObjectData> newObjects, Set<Integer> currentObjectIds) {
        if (card == null) {
            return;
        }

        collectObjectDelta(card, objectDeltas, newObjects, currentObjectIds);

        // Collect from attached cards
        if (card.getAttachedCards() != null) {
            for (CardView attached : card.getAttachedCards()) {
                collectCardDelta(attached, objectDeltas, newObjects, currentObjectIds);
            }
        }
    }

    /**
     * Serialize the changed properties of an object using compact binary format.
     * Uses NetworkTrackableSerializer to create a byte array containing:
     * - The number of changed properties
     * - For each changed property: the property ordinal and its value (IDs for object refs)
     *
     * Key optimization: Object references (CardView, PlayerView, etc.) are written as
     * 4-byte IDs only, not full object graphs. This reduces CardView serialization from
     * ~96KB to ~200 bytes.
     */
    private byte[] serializeChanges(TrackableObject obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            NetworkTrackableSerializer nts = new NetworkTrackableSerializer(dos);

            Set<TrackableProperty> changedProps = obj.getChangedProps();
            dos.writeInt(changedProps.size());

            // Get the props map to read values
            @SuppressWarnings("unchecked")
            Map<TrackableProperty, Object> props = obj.getProps();

            for (TrackableProperty prop : changedProps) {
                dos.writeInt(prop.ordinal());
                Object value = props != null ? props.get(prop) : null;
                // Use NetworkPropertySerializer for type-aware compact serialization
                NetworkPropertySerializer.serialize(nts, prop, value);
            }

            dos.flush();
            dos.close();

            byte[] result = baos.toByteArray();
            // Debug: log large serializations (threshold lowered since we expect much smaller sizes)
            if (result.length > 5000) {
                System.out.println(String.format("[DeltaSync DEBUG] Object %d (%s) serialized to %d bytes with %d changed props",
                    obj.getId(), obj.getClass().getSimpleName(), result.length, changedProps.size()));
            }
            return result;
        } catch (Exception e) {
            System.err.println("Error serializing delta for object " + obj.getId() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Serialize ALL properties of a new object for initial sync to client.
     * Unlike serializeChanges which only sends changed properties, this sends
     * all properties that have non-default values.
     *
     * @param obj the TrackableObject to serialize
     * @return NewObjectData containing the full serialization
     */
    private NewObjectData serializeNewObject(TrackableObject obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            NetworkTrackableSerializer nts = new NetworkTrackableSerializer(dos);

            // Get the props map to read values
            @SuppressWarnings("unchecked")
            Map<TrackableProperty, Object> props = obj.getProps();

            if (props == null || props.isEmpty()) {
                // No properties to serialize - write empty
                dos.writeInt(0);
            } else {
                // Write all properties that have non-null values
                dos.writeInt(props.size());
                for (Map.Entry<TrackableProperty, Object> entry : props.entrySet()) {
                    TrackableProperty prop = entry.getKey();
                    Object value = entry.getValue();
                    dos.writeInt(prop.ordinal());
                    NetworkPropertySerializer.serialize(nts, prop, value);
                }
            }

            dos.flush();
            dos.close();

            byte[] fullProps = baos.toByteArray();
            int objectType = getObjectType(obj);

            System.out.println(String.format("[DeltaSync] Created NewObjectData: ID=%d, Type=%d (%s), Size=%d bytes, Props=%d",
                    obj.getId(), objectType, obj.getClass().getSimpleName(), fullProps.length,
                    props != null ? props.size() : 0));

            return new NewObjectData(obj.getId(), objectType, fullProps);
        } catch (Exception e) {
            System.err.println("Error serializing new object " + obj.getId() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get the object type code for a TrackableObject.
     */
    private int getObjectType(TrackableObject obj) {
        if (obj instanceof CardView) {
            return NewObjectData.TYPE_CARD_VIEW;
        } else if (obj instanceof PlayerView) {
            return NewObjectData.TYPE_PLAYER_VIEW;
        } else if (obj instanceof StackItemView) {
            return NewObjectData.TYPE_STACK_ITEM_VIEW;
        } else if (obj instanceof CombatView) {
            return NewObjectData.TYPE_COMBAT_VIEW;
        } else if (obj instanceof GameView) {
            return NewObjectData.TYPE_GAME_VIEW;
        }
        // Unknown type - use GameView as fallback
        return NewObjectData.TYPE_GAME_VIEW;
    }

    /**
     * Mark objects as sent to client. Called after initial fullStateSync.
     * This ensures that subsequent delta packets only send changed properties,
     * not full object data.
     */
    public void markObjectsAsSent(GameView gameView) {
        if (gameView == null) {
            return;
        }

        sentObjectIds.add(gameView.getId());

        if (gameView.getPlayers() != null) {
            for (PlayerView player : gameView.getPlayers()) {
                markPlayerObjectsAsSent(player);
            }
        }

        if (gameView.getStack() != null) {
            for (StackItemView stackItem : gameView.getStack()) {
                sentObjectIds.add(stackItem.getId());
            }
        }

        if (gameView.getCombat() != null) {
            sentObjectIds.add(gameView.getCombat().getId());
        }

        System.out.println("[DeltaSync] Marked " + sentObjectIds.size() + " objects as sent after full state sync");
    }

    private void markPlayerObjectsAsSent(PlayerView player) {
        if (player == null) {
            return;
        }

        sentObjectIds.add(player.getId());

        markCardsAsSent(player.getHand());
        markCardsAsSent(player.getGraveyard());
        markCardsAsSent(player.getLibrary());
        markCardsAsSent(player.getExile());
        markCardsAsSent(player.getFlashback());
        markCardsAsSent(player.getCommanders());
        markCardsAsSent(player.getAnte());

        if (player.getBattlefield() != null) {
            for (CardView card : player.getBattlefield()) {
                markCardAsSent(card);
            }
        }
    }

    private void markCardsAsSent(Iterable<CardView> cards) {
        if (cards == null) {
            return;
        }
        for (CardView card : cards) {
            markCardAsSent(card);
        }
    }

    private void markCardAsSent(CardView card) {
        if (card == null) {
            return;
        }
        sentObjectIds.add(card.getId());
        if (card.getAttachedCards() != null) {
            for (CardView attached : card.getAttachedCards()) {
                markCardAsSent(attached);
            }
        }
    }

    /**
     * Compute a checksum for the current game state.
     */
    private int computeStateChecksum(GameView gameView) {
        int hash = 17;
        hash = 31 * hash + gameView.getId();
        hash = 31 * hash + gameView.getTurn();
        if (gameView.getPhase() != null) {
            hash = 31 * hash + gameView.getPhase().hashCode();
        }
        if (gameView.getPlayers() != null) {
            for (PlayerView player : gameView.getPlayers()) {
                hash = 31 * hash + player.getId();
                hash = 31 * hash + player.getLife();
            }
        }
        return hash;
    }

    /**
     * Create a full state packet for initial sync or reconnection.
     * @param gameView the complete game view
     * @return a FullStatePacket
     */
    public FullStatePacket createFullStatePacket(GameView gameView) {
        long seq = sequenceNumber.get();
        return new FullStatePacket(seq, gameView);
    }

    /**
     * Create a full state packet for reconnection with session info.
     * @param gameView the complete game view
     * @param sessionId the session identifier
     * @param sessionToken the session token
     * @return a FullStatePacket
     */
    public FullStatePacket createFullStatePacketForReconnect(GameView gameView, String sessionId, String sessionToken) {
        long seq = sequenceNumber.get();
        return new FullStatePacket(seq, gameView, sessionId, sessionToken);
    }

    /**
     * Clear all tracked changes for all objects.
     * Should be called after all clients have acknowledged.
     * @param gameView the game view to clear changes from
     */
    public void clearAllChanges(GameView gameView) {
        if (gameView == null) {
            return;
        }

        gameView.clearChanges();

        if (gameView.getPlayers() != null) {
            for (PlayerView player : gameView.getPlayers()) {
                clearPlayerChanges(player);
            }
        }

        if (gameView.getStack() != null) {
            for (StackItemView stackItem : gameView.getStack()) {
                stackItem.clearChanges();
            }
        }

        if (gameView.getCombat() != null) {
            gameView.getCombat().clearChanges();
        }
    }

    /**
     * Clear changes for a player and all associated objects.
     */
    private void clearPlayerChanges(PlayerView player) {
        if (player == null) {
            return;
        }

        player.clearChanges();

        clearCardsChanges(player.getHand());
        clearCardsChanges(player.getGraveyard());
        clearCardsChanges(player.getLibrary());
        clearCardsChanges(player.getExile());
        clearCardsChanges(player.getFlashback());
        clearCardsChanges(player.getCommanders());
        clearCardsChanges(player.getAnte());

        if (player.getBattlefield() != null) {
            for (CardView card : player.getBattlefield()) {
                clearCardChanges(card);
            }
        }
    }

    /**
     * Clear changes for cards in a zone.
     */
    private void clearCardsChanges(Iterable<CardView> cards) {
        if (cards == null) {
            return;
        }
        for (CardView card : cards) {
            clearCardChanges(card);
        }
    }

    /**
     * Clear changes for a card and its attached cards.
     */
    private void clearCardChanges(CardView card) {
        if (card == null) {
            return;
        }

        card.clearChanges();

        if (card.getAttachedCards() != null) {
            for (CardView attached : card.getAttachedCards()) {
                clearCardChanges(attached);
            }
        }
    }

    /**
     * Get the current sequence number.
     * @return current sequence number
     */
    public long getCurrentSequence() {
        return sequenceNumber.get();
    }

    /**
     * Check if a client needs a full resync.
     * @param clientIndex the client's player index
     * @return true if the client has fallen too far behind
     */
    public boolean needsFullResync(int clientIndex) {
        Long acked = clientAcknowledgedSeq.get(clientIndex);
        if (acked == null) {
            return true;
        }
        // If client is more than 100 packets behind, force full resync
        return (sequenceNumber.get() - acked) > 100;
    }
}
