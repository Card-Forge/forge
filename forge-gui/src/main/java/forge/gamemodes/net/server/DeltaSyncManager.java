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
import forge.trackable.Tracker;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;
import forge.trackable.TrackableTypes;
import forge.trackable.TrackableTypes.TrackableType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    private static final int SAMPLE_SIZE = 15;
    private static final int MIN_CHECKSUM_INTERVAL = 5;
    private static final int CLEAN_STREAK_TO_RESTORE = 10;

    // Sentinel for properties that should be skipped in network transport
    static final Object SKIP_MARKER = new Object();

    // Global consumer ID counter — each DeltaSyncManager gets a unique ID
    private static final AtomicInteger NEXT_CONSUMER_ID = new AtomicInteger(0);

    private final int consumerId = NEXT_CONSUMER_ID.getAndIncrement();
    private final AtomicLong sequenceNumber = new AtomicLong(0);
    // Objects that have been fully sent to the client (initial sync done)
    // Objects not in this set need full serialization when first encountered
    private final Set<Integer> sentObjectIds = ConcurrentHashMap.newKeySet();

    // All objects registered with this consumer (for cleanup on disconnect/reset)
    private final Set<TrackableObject> registeredObjects =
            Collections.newSetFromMap(new IdentityHashMap<>());

    // Not atomic: only accessed from game thread
    private long packetsSinceLastChecksum = 0;

    // Sampled checksum state
    private final EnumSet<TrackableProperty> recentDeltaProperties = EnumSet.noneOf(TrackableProperty.class);
    private int checksumInterval = CHECKSUM_INTERVAL;
    private int cleanChecksumStreak = 0;
    private final Random checksumRng = new Random();

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
        sentObjectIds.clear();
        packetsSinceLastChecksum = 0;
        recentDeltaProperties.clear();
        checksumInterval = CHECKSUM_INTERVAL;
        cleanChecksumStreak = 0;
    }

    /**
     * Collect all changes from the GameView hierarchy and build a delta packet.
     * New objects are registered with this consumer and sent in full.
     * Existing objects only send properties dirty for THIS consumer.
     */
    public DeltaPacket collectDeltas(GameView gameView) {
        return collectDeltas(gameView, true);
    }

    /**
     * @param allowChecksum if false, skip checksum computation and don't count
     *                      toward the checksum interval. Used by handleGameEvents()
     *                      which may run on the scheduler daemon thread where live
     *                      game state can race with game thread mutations.
     */
    public DeltaPacket collectDeltas(GameView gameView, boolean allowChecksum) {
        // Capture checksum-relevant values at the start to avoid race conditions
        final int snapshotTurn = gameView.getTurn();
        final int snapshotPhaseOrdinal = gameView.getPhase() != null ? gameView.getPhase().ordinal() : -1;

        Map<Integer, Map<TrackableProperty, Object>> objectDeltas = new HashMap<>();
        Map<Integer, Map<TrackableProperty, Object>> newObjects = new HashMap<>();
        Set<Integer> currentObjectIds = new HashSet<>();

        walkAndCollect(gameView, objectDeltas, newObjects, currentObjectIds);

        // Update tracked objects — prune IDs for removed objects
        sentObjectIds.retainAll(currentObjectIds);
        sentObjectIds.addAll(currentObjectIds);

        // Accumulate changed properties for delta-biased sampling
        for (Map<TrackableProperty, Object> delta : objectDeltas.values()) {
            recentDeltaProperties.addAll(delta.keySet());
        }

        if (!newObjects.isEmpty()) {
            netLog.info("[DeltaSync] New objects: {}, Deltas: {}", newObjects.size(), objectDeltas.size());
        }

        long seq = sequenceNumber.incrementAndGet();

        // Checksum computation — only when state is known to be stable
        int checksum = 0;
        int[] checksumPropertyOrdinals = null;
        if (allowChecksum) {
            packetsSinceLastChecksum++;
            if (packetsSinceLastChecksum >= checksumInterval) {
                checksumPropertyOrdinals = selectChecksumProperties();
                checksum = NetworkChecksumUtil.computeSampledChecksum(
                        snapshotTurn, snapshotPhaseOrdinal, gameView, checksumPropertyOrdinals);
                packetsSinceLastChecksum = 0;
                recentDeltaProperties.clear();
                cleanChecksumStreak++;

                // Restore default interval after sustained clean streak
                if (checksumInterval < CHECKSUM_INTERVAL && cleanChecksumStreak >= CLEAN_STREAK_TO_RESTORE) {
                    netLog.info("[DeltaSync] {} clean checksums, restoring interval to {}",
                            cleanChecksumStreak, CHECKSUM_INTERVAL);
                    checksumInterval = CHECKSUM_INTERVAL;
                }

                logSampledChecksumDetails(gameView, checksum, seq,
                        snapshotTurn, snapshotPhaseOrdinal, checksumPropertyOrdinals);
            }
        }

        return new DeltaPacket(seq, objectDeltas, newObjects, checksum, checksumPropertyOrdinals);
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
            if (obj instanceof CardView cv) {
                ensureCardStateConsumers(cv);
            }
        } else if (!registeredObjects.contains(obj)) {
            // Replacement instance — same ID but different object (e.g. zone change
            // creates a new Card+CardView via copyCard). Transfer consumer registration
            // to the new instance and send full state.
            // Sent via newObjects so the client can clear stale properties on the
            // existing object before applying the new state.

            Iterator<TrackableObject> it = registeredObjects.iterator();
            while (it.hasNext()) {
                TrackableObject old = it.next();
                if (DeltaPacket.typeTagFor(old) == objType && old.getId() == obj.getId()) {
                    old.unregisterConsumer(consumerId);
                    it.remove();
                    if (old instanceof CardView oldCard) {
                        unregisterOldCardStateViews(oldCard);
                    }
                    break;
                }
            }
            obj.registerConsumer(consumerId);
            registeredObjects.add(obj);
            Map<TrackableProperty, Object> allProps = buildFullPropertyMap(obj);
            mergeDelayedProps(obj, allProps);
            obj.getAndClearDirtyProps(consumerId);
            if (!allProps.isEmpty()) {
                newObjects.put(deltaKey, allProps);
                netLog.trace("[DeltaSync] Replaced instance: type={} id={}, {} props",
                        objType, obj.getId(), allProps.size());
            }
            if (obj instanceof CardView cv) {
                ensureCardStateConsumers(cv);
            }
        } else if (obj instanceof CardView cv) {
            // CardView: check own dirty props AND CardStateView dirty props
            boolean hasDirty = obj.hasConsumerChanges(consumerId);
            EnumSet<TrackableProperty> dirtyProps = hasDirty
                    ? obj.getAndClearDirtyProps(consumerId)
                    : EnumSet.noneOf(TrackableProperty.class);
            Map<TrackableProperty, Object> delta = dirtyProps.isEmpty()
                    ? new EnumMap<>(TrackableProperty.class)
                    : buildPropertyMap(obj, dirtyProps);
            mergeDelayedProps(obj, delta);
            mergeCardStateDirtyProps(cv, delta);
            if (!delta.isEmpty()) {
                objectDeltas.put(deltaKey, delta);
            }
        } else {
            // Existing non-CardView object — check dirty props and delayed (frozen) props
            EnumSet<TrackableProperty> dirtyProps = obj.hasConsumerChanges(consumerId)
                    ? obj.getAndClearDirtyProps(consumerId)
                    : EnumSet.noneOf(TrackableProperty.class);
            Map<TrackableProperty, Object> delta = dirtyProps.isEmpty()
                    ? new EnumMap<>(TrackableProperty.class)
                    : buildPropertyMap(obj, dirtyProps);
            mergeDelayedProps(obj, delta);
            if (!delta.isEmpty()) {
                objectDeltas.put(deltaKey, delta);
                netLog.trace("[DeltaSync] Delta: type={} id={}, {} dirty props",
                        objType, obj.getId(), delta.size());
            }
        }
    }

    // ==================== CardStateView dirty tracking ====================

    private void ensureCardStateConsumer(CardStateView csv) {
        if (csv != null && !registeredObjects.contains(csv)) {
            csv.registerConsumer(consumerId);
            registeredObjects.add(csv);
            csv.getAndClearDirtyProps(consumerId);
        }
    }

    private void ensureCardStateConsumers(CardView card) {
        ensureCardStateConsumer(card.getCurrentState());
        ensureCardStateConsumer(card.getAlternateState());
        ensureCardStateConsumer(card.getLeftSplitState());
        ensureCardStateConsumer(card.getRightSplitState());
    }

    /** Unregisters all CardStateViews tracked by ensureCardStateConsumers. */
    private void unregisterOldCardStateViews(CardView oldCard) {
        CardStateView cs = oldCard.getCurrentState();
        if (cs != null && registeredObjects.remove(cs)) {
            cs.unregisterConsumer(consumerId);
        }
        CardStateView as = oldCard.getAlternateState();
        if (as != null && registeredObjects.remove(as)) {
            as.unregisterConsumer(consumerId);
        }
        CardStateView ls = oldCard.getLeftSplitState();
        if (ls != null && registeredObjects.remove(ls)) {
            ls.unregisterConsumer(consumerId);
        }
        CardStateView rs = oldCard.getRightSplitState();
        if (rs != null && registeredObjects.remove(rs)) {
            rs.unregisterConsumer(consumerId);
        }
    }

    private void mergeCardStateDirtyProps(CardView card, Map<TrackableProperty, Object> delta) {
        mergeOneStateDirtyProps(card.getCurrentState(), TrackableProperty.CurrentState, delta);
        mergeOneStateDirtyProps(card.getAlternateState(), TrackableProperty.AlternateState, delta);
        mergeOneStateDirtyProps(card.getLeftSplitState(), TrackableProperty.LeftSplitState, delta);
        mergeOneStateDirtyProps(card.getRightSplitState(), TrackableProperty.RightSplitState, delta);
    }

    private void mergeOneStateDirtyProps(CardStateView csv, TrackableProperty prop,
                                          Map<TrackableProperty, Object> delta) {
        if (csv == null) return;
        if (delta.containsKey(prop)) return; // Full state already included from CardView dirty set

        Map<TrackableProperty, Object> csvMap = new EnumMap<>(TrackableProperty.class);

        // Collect consumer-dirty properties
        if (csv.hasConsumerChanges(consumerId)) {
            EnumSet<TrackableProperty> dirtyProps = csv.getAndClearDirtyProps(consumerId);
            Map<TrackableProperty, Object> csvProps = csv.getProps();
            for (TrackableProperty dirtyProp : dirtyProps) {
                Object netVal = toNetworkValue(dirtyProp, csvProps.get(dirtyProp));
                if (netVal != SKIP_MARKER) {
                    csvMap.put(dirtyProp, netVal);
                }
            }
        }

        // Include properties delayed by tracker freeze (e.g. Power/Toughness
        // set during state-based effects). Same logic as mergeDelayedProps for
        // top-level objects — peeks at delayed queue, no side effects.
        mergeDelayedProps(csv, csvMap);

        if (!csvMap.isEmpty()) {
            delta.put(prop, new CardStateData(csv.getId(), csv.getState(), csvMap));
        }
    }

    /**
     * Recursively walk the object graph starting from a TrackableObject, collecting deltas.
     * Discovers children by inspecting property values for TrackableObject/TrackableCollection
     * references. CombatView is serialized inline by toNetworkValue(). CardStateViews are not
     * top-level delta objects but get per-consumer dirty tracking registered here so their
     * property changes can be merged into the parent CardView's delta.
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
            if (obj instanceof CardStateView csv) {
                ensureCardStateConsumer(csv);
            }
            return;
        }
        int deltaKey = makeDeltaKey(type, obj.getId());
        if (currentObjectIds.contains(deltaKey)) {
            // Same deltaKey seen earlier this pass (e.g. stale CardView in Commander
            // property walked before current CardView in Battlefield zone collection).
            // If this is a different Java instance not yet tracked, let it through for
            // replacement detection in collectObjectDelta.
            if (registeredObjects.contains(obj)) {
                return;
            }
            collectObjectDelta(obj, objectDeltas, newObjects);
            return;
        }
        currentObjectIds.add(deltaKey);

        collectObjectDelta(obj, objectDeltas, newObjects);

        Map<TrackableProperty, Object> props = obj.getProps();
        if (props != null) {
            for (Object value : props.values()) {
                if (value instanceof TrackableObject to) {
                    walkAndCollect(to, objectDeltas, newObjects, currentObjectIds);
                } else if (value instanceof TrackableCollection) {
                    for (Object item : (TrackableCollection<?>) value) {
                        if (item instanceof TrackableObject to) {
                            walkAndCollect(to, objectDeltas, newObjects, currentObjectIds);
                        }
                    }
                }
            }
        }
    }

    /**
     * Merge properties delayed by a tracker freeze into a delta map.
     * Properties with FreezeMode.RespectsFreeze are not written
     * to the props map or marked dirty while frozen, but network
     * clients need them in the same delta as their accompanying events.
     *
     * This is safe because speculative freeze brackets (which call clearDelayed()) and real freeze brackets are disjoint
     * — speculative brackets always start from freezeCounter == 0 and complete before any sync point where delta collection occurs.
     */
    private void mergeDelayedProps(TrackableObject obj, Map<TrackableProperty, Object> delta) {
        Tracker tracker = obj.getTracker();
        if (tracker == null || !tracker.isFrozen()) return;
        for (Map.Entry<TrackableProperty, Object> entry : tracker.getDelayedPropsFor(obj).entrySet()) {
            Object netVal = toNetworkValue(entry.getKey(), entry.getValue());
            if (netVal != SKIP_MARKER) {
                delta.put(entry.getKey(), netVal);
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

        // Defensive copy mutable collections at the serialization boundary.
        // Views use in-place mutation + flagAsChanged(), so the live reference
        // could be mutated by the game thread while Netty serializes the packet.
        // (some like TrackableProperty.Mana are already safe but that's a minor optimization)
        if (value instanceof Map) {
            return new HashMap<>((Map<?, ?>) value);
        }
        if (value instanceof List) {
            return new ArrayList<>((List<?>) value);
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
            if (obj instanceof CardStateView csv) {
                ensureCardStateConsumer(csv);
            }
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

    // ==================== Sampled checksum selection ====================

    /**
     * Select properties for sampled checksum. Biases toward recently-changed
     * properties (up to half the sample), fills rest randomly from eligible pool.
     */
    private int[] selectChecksumProperties() {
        TrackableProperty[] eligible = NetworkChecksumUtil.getEligibleProperties();
        List<TrackableProperty> selected = new ArrayList<>(SAMPLE_SIZE);

        // Bias: pick up to SAMPLE_SIZE/2 from recently changed properties
        int biasTarget = SAMPLE_SIZE / 2;
        List<TrackableProperty> biasedCandidates = new ArrayList<>();
        for (TrackableProperty prop : recentDeltaProperties) {
            // Only include if in eligible pool
            if (prop.getType() != TrackableTypes.CardStateViewType
                    && prop.getType() != TrackableTypes.CombatViewType
                    && prop.getType() != TrackableTypes.IPaperCardType
                    && prop.getType() != TrackableTypes.StackItemViewListType) {
                biasedCandidates.add(prop);
            }
        }
        // Shuffle and pick up to biasTarget
        Collections.shuffle(biasedCandidates, checksumRng);
        int biasCount = Math.min(biasTarget, biasedCandidates.size());
        for (int i = 0; i < biasCount; i++) {
            selected.add(biasedCandidates.get(i));
        }

        // Fill remaining slots randomly from rest of eligible pool
        EnumSet<TrackableProperty> selectedSet = EnumSet.noneOf(TrackableProperty.class);
        selectedSet.addAll(selected);
        List<TrackableProperty> remaining = new ArrayList<>();
        for (TrackableProperty prop : eligible) {
            if (!selectedSet.contains(prop)) {
                remaining.add(prop);
            }
        }
        Collections.shuffle(remaining, checksumRng);
        int fillCount = Math.min(SAMPLE_SIZE - selected.size(), remaining.size());
        for (int i = 0; i < fillCount; i++) {
            selected.add(remaining.get(i));
        }

        // Convert to sorted ordinals for determinism
        int[] ordinals = new int[selected.size()];
        for (int i = 0; i < selected.size(); i++) {
            ordinals[i] = selected.get(i).ordinal();
        }
        Arrays.sort(ordinals);
        return ordinals;
    }

    /**
     * Called when a resync is requested due to checksum mismatch.
     * Halves the checksum interval (more frequent checks) and resets clean streak.
     */
    public void onResyncRequested() {
        cleanChecksumStreak = 0;
        int oldInterval = checksumInterval;
        checksumInterval = Math.max(MIN_CHECKSUM_INTERVAL, checksumInterval / 2);
        if (checksumInterval != oldInterval) {
            netLog.info("[DeltaSync] Resync detected, checksum interval reduced: {} -> {}",
                    oldInterval, checksumInterval);
        }
    }

    // ==================== Checksum and validation ====================

    private void logSampledChecksumDetails(GameView gameView, int checksum, long seq,
                                            int snapshotTurn, int snapshotPhaseOrdinal,
                                            int[] sampledOrdinals) {
        String phaseName = snapshotPhaseOrdinal >= 0 ?
                forge.game.phase.PhaseType.values()[snapshotPhaseOrdinal].name() : "null";
        netLog.info("[DeltaSync] Sampled checksum for seq={}: hash={}, props={}", seq, checksum,
                NetworkChecksumUtil.sampledPropertyNames(sampledOrdinals));
        netLog.info("[DeltaSync]   Turn: {} (snapshot), Phase: {} (snapshot, current={})",
                snapshotTurn, phaseName,
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

}
