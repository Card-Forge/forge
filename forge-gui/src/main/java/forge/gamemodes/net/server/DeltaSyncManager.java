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

    // Track the last sent property values for each object (per-client change tracking)
    // Key: composite delta key (type + id), Value: map of property ordinal to serialized checksum
    // This allows each DeltaSyncManager to independently track what has been sent to its client,
    // rather than relying on the global hasChanges() flags which get cleared by other clients.
    private final Map<Integer, Map<Integer, Integer>> lastSentPropertyChecksums = new ConcurrentHashMap<>();

    private long packetsSinceLastChecksum = 0;

    /**
     * Reset all tracking state for reconnection.
     * After reset, the next sync will be treated as a fresh initial sync.
     */
    public void reset() {
        sequenceNumber.set(0);
        clientAcknowledgedSeq.clear();
        trackedObjectIds.clear();
        removedObjectIds.clear();
        sentObjectIds.clear();
        lastSentPropertyChecksums.clear();
        packetsSinceLastChecksum = 0;
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

        // CRITICAL FIX for Bug #16 (checksum race condition):
        // Capture checksum-relevant values NOW, at the start of delta collection.
        // The game loop may advance the phase while we're collecting deltas, causing
        // a mismatch between what we collect and what we compute the checksum from.
        // By capturing these values first, we ensure consistency.
        final int snapshotTurn = gameView.getTurn();
        final int snapshotPhaseOrdinal = gameView.getPhase() != null ? gameView.getPhase().ordinal() : -1;

        Map<Integer, byte[]> objectDeltas = new HashMap<>();
        Map<Integer, NewObjectData> newObjects = new HashMap<>();
        Set<Integer> currentObjectIds = new HashSet<>();

        // Collect changes from GameView itself - always log for debugging
        boolean gvHasChanges = gameView.hasChanges();
        boolean gvIsSent = sentObjectIds.contains(makeDeltaKey(DeltaPacket.TYPE_GAME_VIEW, gameView.getId()));
        NetworkDebugLogger.trace("[DeltaSync] collectDeltas: GameView ID=%d, hasChanges=%b, isSent=%b, phase=%s",
                gameView.getId(), gvHasChanges, gvIsSent, gameView.getPhase());
        if (gvHasChanges) {
            NetworkDebugLogger.trace("[DeltaSync] GameView ID=%d changedProps: %s",
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

        // Now do bookkeeping operations
        // Also remove from sentObjectIds when objects are removed
        sentObjectIds.removeAll(nowRemoved);

        // Update tracked objects
        trackedObjectIds.clear();
        trackedObjectIds.addAll(currentObjectIds);

        // After collecting, mark all current objects as sent
        sentObjectIds.addAll(currentObjectIds);

        // CRITICAL FIX for Bug #16 (checksum race condition):
        // Compute checksum using the SNAPSHOT values captured at the start of delta collection,
        // NOT the live gameView values. The game loop may have advanced the phase while we
        // were collecting deltas, which would cause a mismatch.
        //
        // The checksum represents the state after applying THIS delta. Since we captured
        // Turn/Phase at the start, and the delta includes any changes to these properties
        // that happened BEFORE we started collecting, the client will have these same values
        // after applying the delta.
        packetsSinceLastChecksum++;
        int checksum = 0;
        if (packetsSinceLastChecksum >= CHECKSUM_INTERVAL) {
            // Use snapshot values for Turn and Phase to avoid race condition
            checksum = computeStateChecksumWithSnapshot(gameView, snapshotTurn, snapshotPhaseOrdinal);
            packetsSinceLastChecksum = 0;
            // Log checksum details for debugging mismatches
            logChecksumDetailsWithSnapshot(gameView, checksum, sequenceNumber.get() + 1,
                    snapshotTurn, snapshotPhaseOrdinal);
        }

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
     * If already sent, compare current state to per-client tracking and serialize changed properties.
     *
     * IMPORTANT: Uses per-client change tracking (lastSentPropertyChecksums) instead of relying
     * solely on obj.hasChanges(). This fixes a bug where multiple remote clients share the same
     * GameView, and the first client's clearAllChanges() would prevent subsequent clients from
     * seeing any changes (causing 3-4 player games to desync).
     */
    private void collectObjectDelta(TrackableObject obj, Map<Integer, byte[]> objectDeltas,
                                    Map<Integer, NewObjectData> newObjects, Set<Integer> currentObjectIds) {
        if (obj == null) {
            return;
        }

        int objId = obj.getId();

        // Create composite delta key that includes object type to prevent ID collisions
        // E.g., CardView id=5 and StackItemView id=5 won't collide anymore
        int objType = getObjectType(obj);
        int deltaKey = makeDeltaKey(objType, objId);

        // Track current objects using composite key to prevent type collisions
        currentObjectIds.add(deltaKey);

        // Check if this is a new object (not yet sent to client)
        // Use composite key for sentObjectIds to prevent ID collisions between types
        if (!sentObjectIds.contains(deltaKey)) {
            // New object - serialize ALL properties
            NewObjectData newObjData = serializeNewObject(obj);
            if (newObjData != null) {
                newObjects.put(deltaKey, newObjData);
                // Record the property checksums for future delta comparison
                recordPropertyChecksums(deltaKey, obj);
            }
        } else {
            // Existing object - use per-client tracking to detect changes
            // This is more reliable than obj.hasChanges() because it doesn't get cleared
            // by other clients' DeltaSyncManagers
            byte[] serialized = serializeChangesPerClient(deltaKey, obj);
            if (serialized != null && serialized.length > 0) {
                objectDeltas.put(deltaKey, serialized);
            }
        }
    }

    /**
     * Record checksums of all current property values for an object.
     * Used for per-client change tracking.
     */
    private void recordPropertyChecksums(int deltaKey, TrackableObject obj) {
        Map<Integer, Integer> checksums = new HashMap<>();
        Map<TrackableProperty, Object> props = obj.getProps();
        if (props != null) {
            for (Map.Entry<TrackableProperty, Object> entry : props.entrySet()) {
                int propOrdinal = entry.getKey().ordinal();
                int checksum = computePropertyChecksum(entry.getValue());
                checksums.put(propOrdinal, checksum);
            }
        }
        lastSentPropertyChecksums.put(deltaKey, checksums);
    }

    /**
     * Compute a checksum for a property value.
     * Uses Objects.hashCode for simplicity - collisions are acceptable since
     * we're only using this to detect changes, not for cryptographic purposes.
     */
    private int computePropertyChecksum(Object value) {
        if (value == null) {
            return 0;
        }
        // For collections, hash all elements
        if (value instanceof Iterable) {
            int hash = 17;
            int count = 0;
            for (Object item : (Iterable<?>) value) {
                hash = 31 * hash + (item != null ? item.hashCode() : 0);
                count++;
            }
            hash = 31 * hash + count;
            return hash;
        }
        return value.hashCode();
    }

    /**
     * Serialize properties that have changed for THIS client, using per-client tracking.
     * Compares current property values to the last sent values (stored in lastSentPropertyChecksums).
     * This is more reliable than using hasChanges() which can be cleared by other clients.
     */
    private byte[] serializeChangesPerClient(int deltaKey, TrackableObject obj) {
        Map<TrackableProperty, Object> props = obj.getProps();
        if (props == null || props.isEmpty()) {
            return null;
        }

        Map<Integer, Integer> lastChecksums = lastSentPropertyChecksums.get(deltaKey);
        boolean noLastChecksums = (lastChecksums == null);
        if (lastChecksums == null) {
            // No previous record - shouldn't happen for sent objects, but handle gracefully
            // Serialize all properties as if it's a new object
            lastChecksums = new HashMap<>();
            NetworkDebugLogger.debug("[DeltaSync] serializeChangesPerClient: No lastChecksums for deltaKey=0x%08X, treating as new object", deltaKey);
        }

        // Find properties that have changed since last send to THIS client
        Set<TrackableProperty> changedProps = new HashSet<>();
        Map<Integer, Integer> newChecksums = new HashMap<>();

        for (Map.Entry<TrackableProperty, Object> entry : props.entrySet()) {
            TrackableProperty prop = entry.getKey();
            Object value = entry.getValue();
            int propOrdinal = prop.ordinal();
            int currentChecksum = computePropertyChecksum(value);
            newChecksums.put(propOrdinal, currentChecksum);

            Integer lastChecksum = lastChecksums.get(propOrdinal);
            if (lastChecksum == null || lastChecksum != currentChecksum) {
                changedProps.add(prop);
            }
        }

        // Check for properties that were removed (in lastChecksums but not in current props)
        for (Integer propOrdinal : lastChecksums.keySet()) {
            if (!newChecksums.containsKey(propOrdinal)) {
                // Property was removed - need to send null value
                TrackableProperty prop = TrackableProperty.deserialize(propOrdinal);
                if (prop != null) {
                    changedProps.add(prop);
                }
            }
        }

        if (changedProps.isEmpty()) {
            return null;
        }

        // Log that we detected changes via per-client tracking
        int objType = getTypeFromDeltaKey(deltaKey);
        int objId = getIdFromDeltaKey(deltaKey);
        NetworkDebugLogger.trace("[DeltaSync] Per-client tracking detected %d changed props for type=%d id=%d (deltaKey=0x%08X): %s",
                changedProps.size(), objType, objId, deltaKey, changedProps);

        // Serialize the changed properties
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            NetworkTrackableSerializer nts = new NetworkTrackableSerializer(dos);

            dos.writeInt(changedProps.size());
            for (TrackableProperty prop : changedProps) {
                dos.writeInt(prop.ordinal());
                Object value = props.get(prop);
                NetworkPropertySerializer.serialize(nts, prop, value);
            }

            // Update the recorded checksums for this client
            lastSentPropertyChecksums.put(deltaKey, newChecksums);

            return baos.toByteArray();
        } catch (Exception e) {
            NetworkDebugLogger.error("[DeltaSync] Error serializing changes per-client: %s", e.getMessage());
            return null;
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
        boolean isSent = sentObjectIds.contains(makeDeltaKey(DeltaPacket.TYPE_PLAYER_VIEW, player.getId()));
        boolean hasChanges = player.hasChanges();
        int handSize = player.getHand() != null ? player.getHand().size() : 0;
        Set<TrackableProperty> changedProps = player.getChangedProps();
        NetworkDebugLogger.trace("[DeltaSync] collectPlayerDeltas: player=%d, isSent=%b, hasChanges=%b, handSize=%d, changedProps=%s",
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
        collectCardsFromZone(player.getSideboard(), objectDeltas, newObjects, currentObjectIds);
        collectCardsFromZone(player.getCommand(), objectDeltas, newObjects, currentObjectIds);

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

            NetworkDebugLogger.trace("[DeltaSync] Created NewObjectData: ID=%d, Type=%d (%s), Size=%d bytes, Props=%d",
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
     *
     * IMPORTANT: Also records property checksums for per-client change tracking.
     * This is essential for 3-4 player games where multiple clients share the same GameView.
     */
    public void markObjectsAsSent(GameView gameView) {
        if (gameView == null) {
            return;
        }

        int gameViewKey = makeDeltaKey(DeltaPacket.TYPE_GAME_VIEW, gameView.getId());
        sentObjectIds.add(gameViewKey);
        recordPropertyChecksums(gameViewKey, gameView);

        if (gameView.getPlayers() != null) {
            for (PlayerView player : gameView.getPlayers()) {
                markPlayerObjectsAsSent(player);
            }
        }

        if (gameView.getStack() != null) {
            for (StackItemView stackItem : gameView.getStack()) {
                int key = makeDeltaKey(DeltaPacket.TYPE_STACK_ITEM_VIEW, stackItem.getId());
                sentObjectIds.add(key);
                recordPropertyChecksums(key, stackItem);
            }
        }

        if (gameView.getCombat() != null) {
            int key = makeDeltaKey(DeltaPacket.TYPE_COMBAT_VIEW, gameView.getCombat().getId());
            sentObjectIds.add(key);
            recordPropertyChecksums(key, gameView.getCombat());
        }

        NetworkDebugLogger.log("[DeltaSync] Marked %d objects as sent after full state sync, recorded %d checksum entries",
                sentObjectIds.size(), lastSentPropertyChecksums.size());
    }

    private void markPlayerObjectsAsSent(PlayerView player) {
        if (player == null) {
            return;
        }

        int key = makeDeltaKey(DeltaPacket.TYPE_PLAYER_VIEW, player.getId());
        sentObjectIds.add(key);
        recordPropertyChecksums(key, player);

        markCardsAsSent(player.getHand());
        markCardsAsSent(player.getGraveyard());
        markCardsAsSent(player.getLibrary());
        markCardsAsSent(player.getExile());
        markCardsAsSent(player.getFlashback());
        markCardsAsSent(player.getCommanders());
        markCardsAsSent(player.getAnte());
        markCardsAsSent(player.getSideboard());
        markCardsAsSent(player.getCommand());

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
        int key = makeDeltaKey(DeltaPacket.TYPE_CARD_VIEW, card.getId());
        sentObjectIds.add(key);
        recordPropertyChecksums(key, card);
        if (card.getAttachedCards() != null) {
            for (CardView attached : card.getAttachedCards()) {
                markCardAsSent(attached);
            }
        }
    }

    /**
     * Compute a checksum using snapshot values for Turn and Phase.
     * This avoids race conditions where the game loop advances these values
     * while we're collecting deltas.
     *
     * @param gameView the game view (used for player data)
     * @param snapshotTurn the turn number captured at start of delta collection
     * @param snapshotPhaseOrdinal the phase ordinal captured at start (-1 if null)
     * @return checksum based on snapshot values
     */
    private int computeStateChecksumWithSnapshot(GameView gameView, int snapshotTurn, int snapshotPhaseOrdinal) {
        int hash = 17;
        // Use snapshot values for Turn and Phase to avoid race condition
        hash = 31 * hash + snapshotTurn;
        if (snapshotPhaseOrdinal >= 0) {
            hash = 31 * hash + snapshotPhaseOrdinal;
        }
        if (gameView.getPlayers() != null) {
            // Sort players by ID for consistent iteration order
            java.util.List<PlayerView> sortedPlayers = new java.util.ArrayList<>();
            for (PlayerView p : gameView.getPlayers()) {
                sortedPlayers.add(p);
            }
            sortedPlayers.sort(java.util.Comparator.comparingInt(PlayerView::getId));
            for (PlayerView player : sortedPlayers) {
                hash = 31 * hash + player.getId();
                hash = 31 * hash + player.getLife();
            }
        }
        return hash;
    }

    /**
     * Log detailed checksum information using snapshot values.
     */
    private void logChecksumDetailsWithSnapshot(GameView gameView, int checksum, long seq,
                                                 int snapshotTurn, int snapshotPhaseOrdinal) {
        String phaseName = snapshotPhaseOrdinal >= 0 ?
                forge.game.phase.PhaseType.values()[snapshotPhaseOrdinal].name() : "null";
        NetworkDebugLogger.log("[DeltaSync] Checksum for seq=%d: %d", seq, checksum);
        NetworkDebugLogger.log("[DeltaSync] Checksum details (server snapshot state):");
        NetworkDebugLogger.log("[DeltaSync]   GameView ID: %d", gameView.getId());
        NetworkDebugLogger.log("[DeltaSync]   Turn: %d (snapshot)", snapshotTurn);
        NetworkDebugLogger.log("[DeltaSync]   Phase: %s (snapshot, current=%s)", phaseName,
                gameView.getPhase() != null ? gameView.getPhase().name() : "null");
        if (gameView.getPlayers() != null) {
            java.util.List<PlayerView> sortedPlayers = new java.util.ArrayList<>();
            for (PlayerView p : gameView.getPlayers()) {
                sortedPlayers.add(p);
            }
            sortedPlayers.sort(java.util.Comparator.comparingInt(PlayerView::getId));
            for (PlayerView player : sortedPlayers) {
                int handSize = player.getHand() != null ? player.getHand().size() : 0;
                int graveyardSize = player.getGraveyard() != null ? player.getGraveyard().size() : 0;
                int battlefieldSize = player.getBattlefield() != null ? player.getBattlefield().size() : 0;
                NetworkDebugLogger.log("[DeltaSync]   Player %d (%s): Life=%d, Hand=%d, GY=%d, BF=%d",
                        player.getId(), player.getName(), player.getLife(),
                        handSize, graveyardSize, battlefieldSize);
            }
        }
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
        clearCardsChanges(player.getSideboard());
        clearCardsChanges(player.getCommand());

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
