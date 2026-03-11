package forge.gamemodes.net.server;

import forge.game.GameEntityView;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.player.PlayerView;
import forge.game.spellability.StackItemView;
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.DeltaPacket.CardStateData;
import forge.gamemodes.net.NetworkChecksumUtil;
import forge.gamemodes.net.DeltaPacket.NewObjectData;
import forge.gamemodes.net.IHasNetLog;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;
import forge.trackable.TrackableTypes;
import forge.trackable.TrackableTypes.TrackableType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages delta synchronization between server and clients.
 * Tracks changes to TrackableObjects via per-consumer dirty tracking and builds
 * minimal delta packets using property maps (standard Java serialization).
 */
public class DeltaSyncManager implements IHasNetLog {

    // How often to include a checksum for validation (every N packets)
    private static final int CHECKSUM_INTERVAL = 20;

    // Sentinel for properties that should be skipped in network transport
    static final Object SKIP_MARKER = new Object();

    // Global consumer ID counter — each DeltaSyncManager gets a unique ID
    private static final AtomicInteger NEXT_CONSUMER_ID = new AtomicInteger(0);

    private final int consumerId = NEXT_CONSUMER_ID.getAndIncrement();
    private final AtomicLong sequenceNumber = new AtomicLong(0);
    private final Map<Integer, Long> clientAcknowledgedSeq = new ConcurrentHashMap<>();

    // Objects that have been fully sent to the client (initial sync done)
    // Objects not in this set need full serialization when first encountered
    private final Set<Integer> sentObjectIds = ConcurrentHashMap.newKeySet();

    // All objects registered with this consumer (for cleanup on disconnect/reset)
    private final Set<TrackableObject> registeredObjects =
            Collections.newSetFromMap(new IdentityHashMap<>());

    // Not atomic: only accessed from game thread
    private long packetsSinceLastChecksum = 0;

    /**
     * Reset all tracking state for reconnection.
     * Unregisters this consumer from all tracked objects.
     * After reset, the next sync will be treated as a fresh initial sync.
     */
    public void reset() {
        // Unregister consumer from all tracked objects
        for (TrackableObject obj : registeredObjects) {
            obj.unregisterConsumer(consumerId);
        }
        registeredObjects.clear();

        sequenceNumber.set(0);
        clientAcknowledgedSeq.clear();
        sentObjectIds.clear();
        packetsSinceLastChecksum = 0;
    }

    /**
     * Process an acknowledgment from a client.
     */
    public void processAcknowledgment(int clientIndex, long acknowledgedSeq) {
        clientAcknowledgedSeq.compute(clientIndex, (k, v) -> {
            if (v == null) return acknowledgedSeq;
            return Math.max(v, acknowledgedSeq);
        });
    }

    /**
     * Get the minimum acknowledged sequence across all clients.
     */
    public long getMinAcknowledgedSequence() {
        return clientAcknowledgedSeq.values().stream()
                .mapToLong(Long::longValue)
                .min()
                .orElse(0L);
    }

    /**
     * Collect all changes from the GameView hierarchy and build a delta packet.
     * New objects are registered with this consumer and sent in full.
     * Existing objects only send properties dirty for THIS consumer.
     */
    public DeltaPacket collectDeltas(GameView gameView) {
        if (gameView == null) {
            return null;
        }

        // Capture checksum-relevant values at the start to avoid race conditions
        final int snapshotTurn = gameView.getTurn();
        final int snapshotPhaseOrdinal = gameView.getPhase() != null ? gameView.getPhase().ordinal() : -1;

        Map<Integer, Map<TrackableProperty, Object>> objectDeltas = new HashMap<>();
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
                CardView sourceCard = stackItem.getSourceCard();
                if (sourceCard != null) {
                    collectCardDelta(sourceCard, objectDeltas, newObjects, currentObjectIds);
                }
                if (stackItem.getTargetCards() != null) {
                    for (CardView target : stackItem.getTargetCards()) {
                        collectCardDelta(target, objectDeltas, newObjects, currentObjectIds);
                    }
                }
            }
        }

        // Collect changes from combat
        if (gameView.getCombat() != null) {
            collectObjectDelta(gameView.getCombat(), objectDeltas, newObjects, currentObjectIds);
        }

        // Detect removed objects — unregister consumer from them
        Set<Integer> trackedKeysBefore = new HashSet<>(sentObjectIds);
        trackedKeysBefore.removeAll(currentObjectIds);
        Set<Integer> removedObjectIds = trackedKeysBefore;

        // Update tracked objects
        sentObjectIds.removeAll(removedObjectIds);
        sentObjectIds.addAll(currentObjectIds);

        // Checksum computation
        packetsSinceLastChecksum++;
        int checksum = 0;
        boolean includeChecksum = packetsSinceLastChecksum >= CHECKSUM_INTERVAL;
        if (includeChecksum) {
            checksum = NetworkChecksumUtil.computeStateChecksum(snapshotTurn, snapshotPhaseOrdinal, gameView.getPlayers());
            packetsSinceLastChecksum = 0;
            logChecksumDetailsWithSnapshot(gameView, checksum, sequenceNumber.get() + 1,
                    snapshotTurn, snapshotPhaseOrdinal);
        }

        long seq = sequenceNumber.incrementAndGet();

        DeltaPacket packet = new DeltaPacket(seq, objectDeltas, newObjects,
                removedObjectIds, checksum, includeChecksum);

        if (!newObjects.isEmpty()) {
            netLog.info("[DeltaSync] New objects: {}, Deltas: {}, Removed: {}",
                    newObjects.size(), objectDeltas.size(), removedObjectIds.size());
        }

        return packet;
    }

    // ==================== Object type and key management ====================

    /**
     * Create a composite delta key encoding both object type and ID.
     * Prevents ID collisions between different object types.
     * Upper 4 bits = type (0-15), lower 28 bits = ID.
     */
    private static int makeDeltaKey(int type, int id) {
        if (type < 0 || type > 15) {
            netLog.error("[DeltaSync] Invalid object type {}, using 0", type);
            type = 0;
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

    // ==================== Delta collection ====================

    private void collectObjectDelta(TrackableObject obj,
                                    Map<Integer, Map<TrackableProperty, Object>> objectDeltas,
                                    Map<Integer, NewObjectData> newObjects,
                                    Set<Integer> currentObjectIds) {
        if (obj == null) {
            return;
        }

        int objType = DeltaPacket.typeTagFor(obj);
        int deltaKey = makeDeltaKey(objType, obj.getId());
        currentObjectIds.add(deltaKey);

        if (!sentObjectIds.contains(deltaKey)) {
            // New object — register consumer and build full property map
            obj.registerConsumer(consumerId);
            registeredObjects.add(obj);

            Map<TrackableProperty, Object> allProps = buildFullPropertyMap(obj);
            if (!allProps.isEmpty()) {
                newObjects.put(deltaKey, new NewObjectData(obj.getId(), objType, allProps));
                netLog.trace("[DeltaSync] New object: type={} id={}, {} props",
                        objType, obj.getId(), allProps.size());
            }

            // Clear dirty props since we just sent everything
            obj.getAndClearDirtyProps(consumerId);
        } else {
            // Existing object — check per-consumer dirty set
            if (obj.hasConsumerChanges(consumerId)) {
                EnumSet<TrackableProperty> dirtyProps = obj.getAndClearDirtyProps(consumerId);
                Map<TrackableProperty, Object> delta = buildPropertyMap(obj, dirtyProps);
                if (!delta.isEmpty()) {
                    objectDeltas.put(deltaKey, delta);
                    netLog.trace("[DeltaSync] Delta: type={} id={}, {} dirty props",
                            objType, obj.getId(), delta.size());
                }
            }
        }
    }

    private void collectPlayerDeltas(PlayerView player,
                                     Map<Integer, Map<TrackableProperty, Object>> objectDeltas,
                                     Map<Integer, NewObjectData> newObjects,
                                     Set<Integer> currentObjectIds) {
        if (player == null) {
            return;
        }

        collectObjectDelta(player, objectDeltas, newObjects, currentObjectIds);

        forEachZone(player, zone ->
                collectCardsFromZone(zone, objectDeltas, newObjects, currentObjectIds));
    }

    private void collectCardsFromZone(Iterable<CardView> cards,
                                      Map<Integer, Map<TrackableProperty, Object>> objectDeltas,
                                      Map<Integer, NewObjectData> newObjects,
                                      Set<Integer> currentObjectIds) {
        if (cards == null) {
            return;
        }
        for (CardView card : cards) {
            collectCardDelta(card, objectDeltas, newObjects, currentObjectIds);
        }
    }

    private void collectCardDelta(CardView card,
                                  Map<Integer, Map<TrackableProperty, Object>> objectDeltas,
                                  Map<Integer, NewObjectData> newObjects,
                                  Set<Integer> currentObjectIds) {
        if (card == null) {
            return;
        }

        // Skip cards already visited to prevent duplicate processing
        int deltaKey = makeDeltaKey(DeltaPacket.TYPE_CARD_VIEW, card.getId());
        if (currentObjectIds.contains(deltaKey)) {
            return;
        }

        collectObjectDelta(card, objectDeltas, newObjects, currentObjectIds);
    }

    private static void forEachZone(PlayerView player,
                                    java.util.function.Consumer<Iterable<CardView>> action) {
        action.accept(player.getHand());
        action.accept(player.getGraveyard());
        action.accept(player.getLibrary());
        action.accept(player.getExile());
        action.accept(player.getFlashback());
        action.accept(player.getCommanders());
        action.accept(player.getAnte());
        action.accept(player.getSideboard());
        action.accept(player.getCommand());
        action.accept(player.getBattlefield());
    }

    // ==================== Property map building ====================

    /**
     * Build a property map for a subset of dirty properties.
     * Values are converted to network-safe form via toNetworkValue.
     */
    private Map<TrackableProperty, Object> buildPropertyMap(TrackableObject obj,
                                                             EnumSet<TrackableProperty> dirtyProps) {
        Map<TrackableProperty, Object> props = obj.getProps();
        Map<TrackableProperty, Object> delta = new EnumMap<>(TrackableProperty.class);
        for (TrackableProperty prop : dirtyProps) {
            Object netValue = toNetworkValue(prop, props.get(prop));
            if (netValue != SKIP_MARKER) {
                delta.put(prop, netValue);
            }
        }
        return delta;
    }

    /**
     * Build a full property map for a new object (all properties).
     */
    private Map<TrackableProperty, Object> buildFullPropertyMap(TrackableObject obj) {
        Map<TrackableProperty, Object> props = obj.getProps();
        if (props == null || props.isEmpty()) {
            return new EnumMap<>(TrackableProperty.class);
        }
        Map<TrackableProperty, Object> result = new EnumMap<>(TrackableProperty.class);
        for (Map.Entry<TrackableProperty, Object> entry : props.entrySet()) {
            Object netValue = toNetworkValue(entry.getKey(), entry.getValue());
            if (netValue != SKIP_MARKER) {
                result.put(entry.getKey(), netValue);
            }
        }
        return result;
    }

    /**
     * Convert a property value to a network-safe form.
     * Object references become Integer IDs. Everything else passes through
     * as-is — Java serialization handles it natively.
     */
    @SuppressWarnings("unchecked")
    static Object toNetworkValue(TrackableProperty prop, Object value) {
        if (value == null) return null;
        TrackableType<?> type = prop.getType();

        // Object references → Integer ID
        if (type == TrackableTypes.CardViewType)
            return ((TrackableObject) value).getId();
        if (type == TrackableTypes.PlayerViewType)
            return ((TrackableObject) value).getId();

        // Polymorphic reference → int[]{typeMarker, id}
        if (type == TrackableTypes.GameEntityViewType) {
            GameEntityView entity = (GameEntityView) value;
            return new int[]{ entity instanceof CardView ? 0 : 1, entity.getId() };
        }

        // Collections of objects → List<Integer> of IDs
        if (type == TrackableTypes.CardViewCollectionType || type == TrackableTypes.PlayerViewCollectionType) {
            TrackableCollection<?> coll = (TrackableCollection<?>) value;
            List<Integer> ids = new ArrayList<>(coll.size());
            for (TrackableObject obj : coll) ids.add(obj == null ? -1 : obj.getId());
            return ids;
        }

        // CardStateView → nested property map (recursive substitution)
        if (type == TrackableTypes.CardStateViewType) {
            CardStateView csv = (CardStateView) value;
            Map<TrackableProperty, Object> csvMap = new EnumMap<>(TrackableProperty.class);
            Map<TrackableProperty, Object> csvProps = csv.getProps();
            if (csvProps != null) {
                for (Map.Entry<TrackableProperty, Object> e : csvProps.entrySet()) {
                    Object netVal = toNetworkValue(e.getKey(), e.getValue());
                    if (netVal != SKIP_MARKER) {
                        csvMap.put(e.getKey(), netVal);
                    }
                }
            }
            return new CardStateData(csv.getId(), csv.getState(), csvMap);
        }

        // Skip unsupported types (same as current MARKER_SKIP behavior)
        if (type == TrackableTypes.StackItemViewType || type == TrackableTypes.StackItemViewListType ||
            type == TrackableTypes.CombatViewType || type == TrackableTypes.KeywordCollectionViewType ||
            type == TrackableTypes.CardTypeViewType || type == TrackableTypes.IPaperCardType ||
            type == TrackableTypes.GenericMapType)
            return SKIP_MARKER;

        // Everything else passes through — Java serialization handles it natively:
        // Boolean, Integer, Float, String, ColorSet, ManaCost, CounterType,
        // List<String>, Set<String>, Map<String,String>, Set<Integer>,
        // Map<Integer,Integer>, Map<Byte,Integer>, Map<CounterType,Integer>, Enums
        return value;
    }

    // ==================== Object registration ====================

    /**
     * Mark objects as sent and register consumer after initial full state sync.
     * This starts per-consumer dirty tracking for all existing objects.
     */
    public void markObjectsAsSent(GameView gameView) {
        if (gameView == null) {
            return;
        }

        registerAndMark(gameView, DeltaPacket.TYPE_GAME_VIEW);

        if (gameView.getPlayers() != null) {
            for (PlayerView player : gameView.getPlayers()) {
                markPlayerObjectsAsSent(player);
            }
        }

        if (gameView.getStack() != null) {
            for (StackItemView stackItem : gameView.getStack()) {
                registerAndMark(stackItem, DeltaPacket.TYPE_STACK_ITEM_VIEW);
                CardView sourceCard = stackItem.getSourceCard();
                if (sourceCard != null) {
                    markCardAsSent(sourceCard);
                }
                if (stackItem.getTargetCards() != null) {
                    for (CardView target : stackItem.getTargetCards()) {
                        markCardAsSent(target);
                    }
                }
            }
        }

        if (gameView.getCombat() != null) {
            registerAndMark(gameView.getCombat(), DeltaPacket.TYPE_COMBAT_VIEW);
        }

        netLog.info("[DeltaSync] Registered consumer {} on {} objects after full state sync",
                consumerId, registeredObjects.size());
    }

    private void registerAndMark(TrackableObject obj, int type) {
        int key = makeDeltaKey(type, obj.getId());
        sentObjectIds.add(key);
        obj.registerConsumer(consumerId);
        registeredObjects.add(obj);
        // Clear any dirty props accumulated before registration
        obj.getAndClearDirtyProps(consumerId);
    }

    private void markPlayerObjectsAsSent(PlayerView player) {
        if (player == null) {
            return;
        }
        registerAndMark(player, DeltaPacket.TYPE_PLAYER_VIEW);

        forEachZone(player, this::markCardsAsSent);
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
        registerAndMark(card, DeltaPacket.TYPE_CARD_VIEW);
    }

    // ==================== Checksum and validation ====================

    private void logChecksumDetailsWithSnapshot(GameView gameView, int checksum, long seq,
                                                 int snapshotTurn, int snapshotPhaseOrdinal) {
        String phaseName = snapshotPhaseOrdinal >= 0 ?
                forge.game.phase.PhaseType.values()[snapshotPhaseOrdinal].name() : "null";
        netLog.info("[DeltaSync] Checksum for seq={}: {}", seq, checksum);
        netLog.info("[DeltaSync] Checksum details (server snapshot state):");
        netLog.info("[DeltaSync]   GameView ID: {}", gameView.getId());
        netLog.info("[DeltaSync]   Turn: {} (snapshot)", snapshotTurn);
        netLog.info("[DeltaSync]   Phase: {} (snapshot, current={})", phaseName,
                gameView.getPhase() != null ? gameView.getPhase().name() : "null");
        for (PlayerView player : NetworkChecksumUtil.getSortedPlayers(gameView)) {
            int handSize = player.getHand() != null ? player.getHand().size() : 0;
            int graveyardSize = player.getGraveyard() != null ? player.getGraveyard().size() : 0;
            int battlefieldSize = player.getBattlefield() != null ? player.getBattlefield().size() : 0;
            netLog.info("[DeltaSync]   Player {} ({}): Life={}, Hand={}, GY={}, BF={}",
                    player.getId(), player.getName(), player.getLife(),
                    handSize, graveyardSize, battlefieldSize);
        }
    }

    public long getCurrentSequence() {
        return sequenceNumber.get();
    }

    /**
     * Check if a client needs a full resync.
     */
    public boolean needsFullResync(int clientIndex) {
        Long acked = clientAcknowledgedSeq.get(clientIndex);
        if (acked == null) {
            return true;
        }
        return (sequenceNumber.get() - acked) > 100;
    }
}
