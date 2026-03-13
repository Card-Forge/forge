package forge.gamemodes.net.server;

import forge.game.GameEntityView;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.player.PlayerView;
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.DeltaPacket.CardStateData;
import forge.gamemodes.net.DeltaPacket.CombatData;
import forge.gamemodes.net.NetworkChecksumUtil;
import forge.game.combat.CombatView;
import forge.util.collect.FCollection;

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
        // Capture checksum-relevant values at the start to avoid race conditions
        final int snapshotTurn = gameView.getTurn();
        final int snapshotPhaseOrdinal = gameView.getPhase() != null ? gameView.getPhase().ordinal() : -1;

        Map<Integer, Map<TrackableProperty, Object>> objectDeltas = new HashMap<>();
        Map<Integer, Map<TrackableProperty, Object>> newObjects = new HashMap<>();
        Set<Integer> currentObjectIds = new HashSet<>();

        walkAndCollect(gameView, objectDeltas, newObjects, currentObjectIds);

        // Update tracked objects
        sentObjectIds.retainAll(currentObjectIds);
        sentObjectIds.addAll(currentObjectIds);

        if (!newObjects.isEmpty()) {
            netLog.info("[DeltaSync] New objects: {}, Deltas: {}", newObjects.size(), objectDeltas.size());
        }

        long seq = sequenceNumber.incrementAndGet();

        // Checksum computation
        packetsSinceLastChecksum++;
        int checksum = 0;
        boolean includeChecksum = packetsSinceLastChecksum >= CHECKSUM_INTERVAL;
        if (includeChecksum) {
            checksum = NetworkChecksumUtil.computeStateChecksum(snapshotTurn, snapshotPhaseOrdinal, gameView.getPlayers());
            packetsSinceLastChecksum = 0;
            logChecksumDetailsWithSnapshot(gameView, checksum, seq,
                    snapshotTurn, snapshotPhaseOrdinal);
        }

        return new DeltaPacket(seq, objectDeltas, newObjects, checksum, includeChecksum);
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
                                    Map<Integer, Map<TrackableProperty, Object>> newObjects) {
        int objType = DeltaPacket.typeTagFor(obj);
        int deltaKey = makeDeltaKey(objType, obj.getId());

        if (!sentObjectIds.contains(deltaKey)) {
            // New object — register consumer and build full property map
            obj.registerConsumer(consumerId);
            registeredObjects.add(obj);

            Map<TrackableProperty, Object> allProps = buildFullPropertyMap(obj);
            if (!allProps.isEmpty()) {
                newObjects.put(deltaKey, allProps);
                netLog.trace("[DeltaSync] New object: type={} id={}, {} props",
                        objType, obj.getId(), allProps.size());
            }

            // Clear dirty props since we just sent everything
            obj.getAndClearDirtyProps(consumerId);
        } else if (obj.hasConsumerChanges(consumerId)) {
            // Existing object — check per-consumer dirty set
            EnumSet<TrackableProperty> dirtyProps = obj.getAndClearDirtyProps(consumerId);
            Map<TrackableProperty, Object> delta = buildPropertyMap(obj, dirtyProps);
            if (!delta.isEmpty()) {
                objectDeltas.put(deltaKey, delta);
                netLog.trace("[DeltaSync] Delta: type={} id={}, {} dirty props",
                        objType, obj.getId(), delta.size());
            }
        }
    }

    /**
     * Recursively walk the object graph starting from a TrackableObject, collecting deltas.
     * Discovers children by inspecting property values for TrackableObject/TrackableCollection
     * references. CombatView and CardStateView are skipped — they are serialized inline as
     * property values by toNetworkValue(), not as top-level delta objects.
     */
    private void walkAndCollect(TrackableObject obj,
                                Map<Integer, Map<TrackableProperty, Object>> objectDeltas,
                                Map<Integer, Map<TrackableProperty, Object>> newObjects,
                                Set<Integer> currentObjectIds) {
        if (obj == null) {
            return;
        }
        int type = DeltaPacket.typeTagFor(obj);
        if (type < 0) {
            return;
        }
        int deltaKey = makeDeltaKey(type, obj.getId());
        if (currentObjectIds.contains(deltaKey)) {
            return;
        }
        currentObjectIds.add(deltaKey);

        collectObjectDelta(obj, objectDeltas, newObjects);

        Map<TrackableProperty, Object> props = obj.getProps();
        if (props != null) {
            for (Object value : props.values()) {
                if (value instanceof TrackableObject) {
                    walkAndCollect((TrackableObject) value, objectDeltas, newObjects, currentObjectIds);
                } else if (value instanceof TrackableCollection) {
                    for (Object item : (TrackableCollection<?>) value) {
                        if (item instanceof TrackableObject) {
                            walkAndCollect((TrackableObject) item, objectDeltas, newObjects, currentObjectIds);
                        }
                    }
                }
            }
        }
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
        if (type == TrackableTypes.CardViewType || type == TrackableTypes.PlayerViewType)
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

        if (type == TrackableTypes.CombatViewType) {
            return combatViewToCombatData((CombatView) value);
        }

        if (type == TrackableTypes.StackItemViewType)
            return ((TrackableObject) value).getId();

        if (type == TrackableTypes.StackItemViewListType) {
            TrackableCollection<?> coll = (TrackableCollection<?>) value;
            List<Integer> ids = new ArrayList<>(coll.size());
            for (TrackableObject obj : coll) ids.add(obj == null ? -1 : obj.getId());
            return ids;
        }

        return value;
    }

    /**
     * Convert a CombatView into a serializable CombatData by iterating its band entries.
     */
    @SuppressWarnings("unchecked")
    private static CombatData combatViewToCombatData(CombatView combat) {
        Map<TrackableProperty, Object> props = combat.getProps();
        Map<FCollection<CardView>, GameEntityView> bandsWithDefenders =
                (Map<FCollection<CardView>, GameEntityView>) props.get(TrackableProperty.BandsWithDefenders);
        Map<FCollection<CardView>, FCollection<CardView>> bandsWithBlockers =
                (Map<FCollection<CardView>, FCollection<CardView>>) props.get(TrackableProperty.BandsWithBlockers);
        Map<FCollection<CardView>, FCollection<CardView>> bandsWithPlannedBlockers =
                (Map<FCollection<CardView>, FCollection<CardView>>) props.get(TrackableProperty.BandsWithPlannedBlockers);

        if (bandsWithDefenders == null || bandsWithDefenders.isEmpty()) {
            return new CombatData(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        List<List<Integer>> allAttackerIds = new ArrayList<>();
        List<int[]> allDefenderRefs = new ArrayList<>();
        List<List<Integer>> allBlockerIds = new ArrayList<>();
        List<List<Integer>> allPlannedBlockerIds = new ArrayList<>();

        for (Map.Entry<FCollection<CardView>, GameEntityView> entry : bandsWithDefenders.entrySet()) {
            FCollection<CardView> band = entry.getKey();
            GameEntityView defender = entry.getValue();

            // Attacker IDs for this band
            List<Integer> attackerIds = new ArrayList<>();
            for (CardView attacker : band) {
                attackerIds.add(attacker.getId());
            }
            allAttackerIds.add(attackerIds);

            // Defender reference: {typeMarker, id}
            allDefenderRefs.add(new int[]{ defender instanceof CardView ? 0 : 1, defender.getId() });

            // Blockers for this band
            FCollection<CardView> blockers = bandsWithBlockers != null ? bandsWithBlockers.get(band) : null;
            if (blockers != null && !blockers.isEmpty()) {
                List<Integer> blockerIds = new ArrayList<>();
                for (CardView blocker : blockers) {
                    blockerIds.add(blocker.getId());
                }
                allBlockerIds.add(blockerIds);
            } else {
                allBlockerIds.add(null);
            }

            // Planned blockers for this band
            FCollection<CardView> plannedBlockers = bandsWithPlannedBlockers != null ? bandsWithPlannedBlockers.get(band) : null;
            if (plannedBlockers != null && !plannedBlockers.isEmpty()) {
                List<Integer> plannedIds = new ArrayList<>();
                for (CardView pb : plannedBlockers) {
                    plannedIds.add(pb.getId());
                }
                allPlannedBlockerIds.add(plannedIds);
            } else {
                allPlannedBlockerIds.add(null);
            }
        }

        return new CombatData(allAttackerIds, allDefenderRefs, allBlockerIds, allPlannedBlockerIds);
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

        walkAndMarkAsSent(gameView, new HashSet<>());

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

    /**
     * Recursively walk the object graph starting from a TrackableObject, registering
     * each for dirty tracking. Same traversal as walkAndCollect but for initial sync.
     */
    private void walkAndMarkAsSent(TrackableObject obj, Set<Integer> visited) {
        if (obj == null) {
            return;
        }
        int type = DeltaPacket.typeTagFor(obj);
        if (type < 0) {
            return;
        }
        int deltaKey = makeDeltaKey(type, obj.getId());
        if (!visited.add(deltaKey)) {
            return;
        }

        registerAndMark(obj, type);

        Map<TrackableProperty, Object> props = obj.getProps();
        if (props != null) {
            for (Object value : props.values()) {
                if (value instanceof TrackableObject) {
                    walkAndMarkAsSent((TrackableObject) value, visited);
                } else if (value instanceof TrackableCollection) {
                    for (Object item : (TrackableCollection<?>) value) {
                        if (item instanceof TrackableObject) {
                            walkAndMarkAsSent((TrackableObject) item, visited);
                        }
                    }
                }
            }
        }
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
