package forge.gamemodes.net.server;

import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.combat.CombatView;
import forge.game.player.PlayerView;
import forge.game.spellability.StackItemView;
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.DeltaPacket.NewObjectData;
import forge.gamemodes.net.FullStatePacket;
import forge.gamemodes.net.NetworkDebugLogger;
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
    // Increased from 10 to 20 to reduce frequency of race-condition-induced mismatches
    private static final int CHECKSUM_INTERVAL = 20;

    // Recursion safety limits to prevent stack overflow
    private static final int MAX_ATTACHMENT_DEPTH = 20;
    private static final int MAX_COLLECTION_SIZE = 1000;

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

        // Collect changes from GameView itself - always log for debugging
        boolean gvHasChanges = gameView.hasChanges();
        boolean gvIsSent = sentObjectIds.contains(gameView.getId());
        NetworkDebugLogger.debug("[DeltaSync] collectDeltas: GameView ID=%d, hasChanges=%b, isSent=%b, phase=%s",
                gameView.getId(), gvHasChanges, gvIsSent, gameView.getPhase());
        if (gvHasChanges) {
            NetworkDebugLogger.debug("[DeltaSync] GameView ID=%d changedProps: %s",
                    gameView.getId(), gameView.getChangedProps());
        }
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

        // IMPORTANT: Compute checksum IMMEDIATELY after collecting all deltas,
        // before any other operations that could allow game state to change.
        // This fixes a race condition where the game loop could modify gameView
        // between delta collection and checksum computation, causing mismatches.
        packetsSinceLastChecksum++;
        int checksum = 0;
        if (packetsSinceLastChecksum >= CHECKSUM_INTERVAL) {
            checksum = computeStateChecksum(gameView);
            packetsSinceLastChecksum = 0;
        }

        // Now do bookkeeping operations
        // Also remove from sentObjectIds when objects are removed
        sentObjectIds.removeAll(nowRemoved);

        // Update tracked objects
        trackedObjectIds.clear();
        trackedObjectIds.addAll(currentObjectIds);

        // After collecting, mark all current objects as sent
        sentObjectIds.addAll(currentObjectIds);

        // Build the packet
        long seq = sequenceNumber.incrementAndGet();

        DeltaPacket packet = new DeltaPacket(seq, objectDeltas, newObjects, new HashSet<>(removedObjectIds), checksum);

        // Log new objects for debugging
        if (!newObjects.isEmpty()) {
            NetworkDebugLogger.log("[DeltaSync] New objects: %d, Deltas: %d, Removed: %d",
                    newObjects.size(), objectDeltas.size(), removedObjectIds.size());
        }

        // Clear removedObjectIds after including in packet
        removedObjectIds.clear();

        return packet;
    }

    /**
     * Create a composite delta key that encodes both object type and ID.
     * This prevents ID collisions between different object types in delta packet maps.
     *
     * Problem: Different object types can have the same ID (e.g., CardView id=5 and StackItemView id=5)
     * causing collisions in Map<Integer, byte[]> objectDeltas when using raw IDs as keys.
     *
     * Solution: Encode type in upper 4 bits, ID in lower 28 bits.
     * - Bits 31-28: Object type (0-15, currently 0-4 used from DeltaPacket.TYPE_*)
     * - Bits 27-0: Object ID (supports IDs up to 268 million, handles negatives like CombatView id=-1)
     *
     * Examples:
     * - CardView (type=0) id=5 → key=0x00000005 (5)
     * - PlayerView (type=1) id=5 → key=0x10000005 (268435461)
     * - StackItemView (type=2) id=5 → key=0x20000005 (536870917)
     * - CombatView (type=3) id=-1 → key=0x3FFFFFFF (1073741823)
     *
     * @param type the object type constant from DeltaPacket (TYPE_CARD_VIEW, TYPE_PLAYER_VIEW, etc.)
     * @param id the object's ID
     * @return composite key that uniquely identifies the (type, id) pair
     */
    private static int makeDeltaKey(int type, int id) {
        // Ensure type fits in 4 bits (0-15)
        if (type < 0 || type > 15) {
            NetworkDebugLogger.error("[DeltaSync] Invalid object type %d, using 0", type);
            type = 0;
        }
        // For negative IDs (like CombatView id=-1), mask to 28 bits
        return (type << 28) | (id & 0x0FFFFFFF);
    }

    /**
     * Extract the object type from a composite delta key.
     * @param deltaKey the composite key
     * @return object type (0-15)
     */
    public static int getTypeFromDeltaKey(int deltaKey) {
        return (deltaKey >>> 28) & 0xF;
    }

    /**
     * Extract the object ID from a composite delta key.
     * Handles sign extension for negative IDs.
     * @param deltaKey the composite key
     * @return object ID (with proper sign extension)
     */
    public static int getIdFromDeltaKey(int deltaKey) {
        int id = deltaKey & 0x0FFFFFFF;
        // Check if original ID was negative (bit 27 set in masked value)
        if ((id & 0x08000000) != 0) {
            // Sign-extend from 28 bits to 32 bits
            id |= 0xF0000000;
        }
        return id;
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

        // Create composite delta key that includes object type to prevent ID collisions
        // E.g., CardView id=5 and StackItemView id=5 won't collide anymore
        int objType = getObjectType(obj);
        int deltaKey = makeDeltaKey(objType, objId);

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
                objectDeltas.put(deltaKey, serialized);
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

        // Debug: log player state before collecting
        boolean isSent = sentObjectIds.contains(player.getId());
        boolean hasChanges = player.hasChanges();
        int handSize = player.getHand() != null ? player.getHand().size() : 0;
        Set<TrackableProperty> changedProps = player.getChangedProps();
        NetworkDebugLogger.debug("[DeltaSync] collectPlayerDeltas: player=%d, isSent=%b, hasChanges=%b, handSize=%d, changedProps=%s",
                player.getId(), isSent, hasChanges, handSize, changedProps);

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
            int battlefieldCount = 0;
            for (CardView card : player.getBattlefield()) {
                if (battlefieldCount++ >= MAX_COLLECTION_SIZE) {
                    NetworkDebugLogger.warn("[DeltaSync] Max collection size (%d) exceeded on battlefield for player %d",
                        MAX_COLLECTION_SIZE, player.getId());
                    break;
                }
                collectCardDelta(card, objectDeltas, newObjects, currentObjectIds, 0);
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
        int cardCount = 0;
        for (CardView card : cards) {
            if (cardCount++ >= MAX_COLLECTION_SIZE) {
                NetworkDebugLogger.warn("[DeltaSync] Max collection size (%d) exceeded in zone, skipping remaining cards",
                    MAX_COLLECTION_SIZE);
                break;
            }
            collectCardDelta(card, objectDeltas, newObjects, currentObjectIds, 0);
        }
    }

    /**
     * Collect delta from a card and its associated objects.
     */
    private void collectCardDelta(CardView card, Map<Integer, byte[]> objectDeltas,
                                  Map<Integer, NewObjectData> newObjects, Set<Integer> currentObjectIds, int depth) {
        if (card == null) {
            return;
        }

        // Safety check: prevent stack overflow from deep attachment chains
        if (depth > MAX_ATTACHMENT_DEPTH) {
            NetworkDebugLogger.warn("[DeltaSync] Max attachment depth (%d) reached for card %d, skipping deeper attachments",
                MAX_ATTACHMENT_DEPTH, card.getId());
            return;
        }

        collectObjectDelta(card, objectDeltas, newObjects, currentObjectIds);

        // Collect from attached cards
        if (card.getAttachedCards() != null) {
            int attachmentCount = 0;
            for (CardView attached : card.getAttachedCards()) {
                if (attachmentCount++ >= MAX_COLLECTION_SIZE) {
                    NetworkDebugLogger.warn("[DeltaSync] Max collection size (%d) exceeded for attached cards on card %d",
                        MAX_COLLECTION_SIZE, card.getId());
                    break;
                }
                collectCardDelta(attached, objectDeltas, newObjects, currentObjectIds, depth + 1);
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

            // Critical: if props is null but changedProps is not empty, we have inconsistent state
            if (props == null && !changedProps.isEmpty()) {
                NetworkDebugLogger.error("[DeltaSync] CRITICAL: Object %d has %d changed properties but null props map!",
                    obj.getId(), changedProps.size());
                return null;
            }

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
                NetworkDebugLogger.debug("[DeltaSync] Object %d (%s) serialized to %d bytes with %d changed props",
                    obj.getId(), obj.getClass().getSimpleName(), result.length, changedProps.size());
            }
            return result;
        } catch (Exception e) {
            NetworkDebugLogger.error("[DeltaSync] Error serializing delta for object %d: %s", obj.getId(), e.getMessage());
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

            NetworkDebugLogger.debug("[DeltaSync] Created NewObjectData: ID=%d, Type=%d (%s), Size=%d bytes, Props=%d",
                    obj.getId(), objectType, obj.getClass().getSimpleName(), fullProps.length,
                    props != null ? props.size() : 0);

            return new NewObjectData(obj.getId(), objectType, fullProps);
        } catch (Exception e) {
            NetworkDebugLogger.error("[DeltaSync] Error serializing new object %d: %s", obj.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * Get the object type code for a TrackableObject.
     */
    private int getObjectType(TrackableObject obj) {
        if (obj instanceof CardView) {
            return DeltaPacket.TYPE_CARD_VIEW;
        } else if (obj instanceof PlayerView) {
            return DeltaPacket.TYPE_PLAYER_VIEW;
        } else if (obj instanceof StackItemView) {
            return DeltaPacket.TYPE_STACK_ITEM_VIEW;
        } else if (obj instanceof CombatView) {
            return DeltaPacket.TYPE_COMBAT_VIEW;
        } else if (obj instanceof GameView) {
            return DeltaPacket.TYPE_GAME_VIEW;
        }
        // Unknown type - use GameView as fallback
        return DeltaPacket.TYPE_GAME_VIEW;
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

        NetworkDebugLogger.log("[DeltaSync] Marked %d objects as sent after full state sync", sentObjectIds.size());
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
