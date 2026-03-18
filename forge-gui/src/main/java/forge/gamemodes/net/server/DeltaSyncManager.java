package forge.gamemodes.net.server;

import forge.game.GameEntityView;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gamemodes.net.DeltaPacket;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages delta synchronization between server and clients.
 * Tracks changes to TrackableObjects via per-consumer dirty tracking and builds
 * minimal delta packets using property maps.
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

    // All objects registered with this consumer, keyed by delta key (for cleanup on disconnect/reset)
    private final Map<Integer, TrackableObject> registeredByKey = new HashMap<>();

    // Not atomic: only accessed from game thread
    // Initialized to interval so the very first packet triggers a checksum
    private long packetsSinceLastChecksum = CHECKSUM_INTERVAL;

    // Sampled checksum state
    private final EnumSet<TrackableProperty> recentDeltaProperties = EnumSet.noneOf(TrackableProperty.class);
    private int checksumInterval = CHECKSUM_INTERVAL;
    private int cleanChecksumStreak = 0;
    // Stored at checksum time, logged on resync request
    private String lastChecksumBreakdown;
    private List<String> lastChecksumDetail;

    /**
     * Reset all tracking state for reconnection.
     * Unregisters this consumer from all tracked objects.
     * After reset, the next sync will be treated as a fresh initial sync.
     */
    public void reset() {
        // Unregister consumer from all tracked objects
        for (TrackableObject obj : registeredByKey.values()) {
            obj.unregisterConsumer(consumerId);
        }
        registeredByKey.clear();

        sequenceNumber.set(0);
        sentObjectIds.clear();
        packetsSinceLastChecksum = CHECKSUM_INTERVAL;
        recentDeltaProperties.clear();
        checksumInterval = CHECKSUM_INTERVAL;
        cleanChecksumStreak = 0;
        lastChecksumBreakdown = null;
        lastChecksumDetail = null;
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
     * @param onGameThread true when called from the game thread (updateGameView),
     *                     false from the event forwarder daemon thread. All calls
     *                     count toward the checksum interval; only game-thread
     *                     calls compute the checksum (the daemon thread can race
     *                     with game-thread mutations, causing false positives).
     */
    public DeltaPacket collectDeltas(GameView gameView, boolean onGameThread) {
        Map<Integer, Map<TrackableProperty, Object>> objectDeltas = new HashMap<>();
        Map<Integer, Map<TrackableProperty, Object>> newObjects = new HashMap<>();
        Set<Integer> currentObjectIds = new HashSet<>();

        walkAndCollect(gameView, objectDeltas, newObjects, currentObjectIds);

        // Update tracked objects — prune IDs for removed objects
        sentObjectIds.retainAll(currentObjectIds);
        sentObjectIds.addAll(currentObjectIds);

        // Prune registrations for objects no longer in the graph
        Iterator<Map.Entry<Integer, TrackableObject>> regIt =
                registeredByKey.entrySet().iterator();
        while (regIt.hasNext()) {
            Map.Entry<Integer, TrackableObject> entry = regIt.next();
            if (!currentObjectIds.contains(entry.getKey())) {
                entry.getValue().unregisterConsumer(consumerId);
                regIt.remove();
            }
        }

        // Accumulate changed properties for delta-biased sampling
        for (Map<TrackableProperty, Object> delta : objectDeltas.values()) {
            recentDeltaProperties.addAll(delta.keySet());
        }

        if (!newObjects.isEmpty()) {
            netLog.info("[DeltaSync] New objects: {}, Deltas: {}", newObjects.size(), objectDeltas.size());
        }

        long seq = sequenceNumber.incrementAndGet();

        // All paths count toward the interval. Only compute on the game thread —
        // the daemon thread can race with game-thread mutations (false positives).
        // Tracker freeze is handled by getEffectiveValue() in the checksum computation,
        // which reads delayed props to match what the delta carries.
        int checksum = 0;
        int[] checksumPropertyOrdinals = null;
        packetsSinceLastChecksum++;
        if (onGameThread && packetsSinceLastChecksum >= checksumInterval) {
            checksumPropertyOrdinals = selectChecksumProperties();
            checksum = NetworkChecksumUtil.computeSampledChecksum(gameView, checksumPropertyOrdinals);
            packetsSinceLastChecksum = 0;
            recentDeltaProperties.clear();
            cleanChecksumStreak++;

            // Restore default interval after sustained clean streak
            if (checksumInterval < CHECKSUM_INTERVAL && cleanChecksumStreak >= CLEAN_STREAK_TO_RESTORE) {
                netLog.info("[DeltaSync] {} clean checksums, restoring interval to {}",
                        cleanChecksumStreak, CHECKSUM_INTERVAL);
                checksumInterval = CHECKSUM_INTERVAL;
            }

            logSampledChecksumDetails(gameView, checksum, seq, checksumPropertyOrdinals);

            // Store breakdown for logging if the client reports a mismatch
            int turn = gameView.getTurn();
            int phaseOrdinal = gameView.getPhase() != null ? gameView.getPhase().ordinal() : -1;
            lastChecksumBreakdown = NetworkChecksumUtil.computeChecksumBreakdown(turn, phaseOrdinal, gameView);
            List<String> detail = new ArrayList<>();
            NetworkChecksumUtil.computeSampledChecksum(gameView, checksumPropertyOrdinals, detail);
            lastChecksumDetail = detail;
        }

        return new DeltaPacket(seq, objectDeltas, newObjects, checksum, checksumPropertyOrdinals);
    }

    // ==================== Delta collection ====================

    private void collectObjectDelta(TrackableObject obj,
                                    Map<Integer, Map<TrackableProperty, Object>> objectDeltas,
                                    Map<Integer, Map<TrackableProperty, Object>> newObjects) {
        int deltaKey = DeltaPacket.makeDeltaKey(obj);

        if (!sentObjectIds.contains(deltaKey)) {
            // New object — register consumer and build full property map.
            // Clear dirty props BEFORE building the map so that any concurrent
            // game-thread writes create NEW dirty bits that survive the clear.
            // If we cleared AFTER, a write between buildFullPropertyMap (reads
            // stale value) and getAndClearDirtyProps (clears the update's bit)
            // would permanently lose the update.
            obj.registerConsumer(consumerId);
            registeredByKey.put(deltaKey, obj);
            obj.getAndClearDirtyProps(consumerId);

            Map<TrackableProperty, Object> allProps = buildFullPropertyMap(obj);
            mergeDelayedProps(obj, allProps);
            if (!allProps.isEmpty()) {
                newObjects.put(deltaKey, allProps);
                netLog.trace("[DeltaSync] New object: key={} id={}, {} props",
                        String.format("0x%08X", deltaKey), obj.getId(), allProps.size());
            }
        } else if (registeredByKey.get(deltaKey) != obj) {
            // Replacement instance — different object at same key (e.g. zone change
            // creates a new Card+CardView via copyCard). Transfer consumer registration
            // to the new instance and send full state.
            // Sent via newObjects so the client can clear stale properties on the
            // existing object before applying the new state.
            TrackableObject old = registeredByKey.get(deltaKey);
            if (old != null) {
                old.unregisterConsumer(consumerId);
            }
            obj.registerConsumer(consumerId);
            registeredByKey.put(deltaKey, obj);
            obj.getAndClearDirtyProps(consumerId);
            Map<TrackableProperty, Object> allProps = buildFullPropertyMap(obj);
            mergeDelayedProps(obj, allProps);
            if (!allProps.isEmpty()) {
                objectDeltas.remove(deltaKey);
                newObjects.put(deltaKey, allProps);
                netLog.trace("[DeltaSync] Replaced instance: key={} id={}, {} props",
                        String.format("0x%08X", deltaKey), obj.getId(), allProps.size());
            }
        } else {
            // Existing object — check dirty props and delayed (frozen) props
            EnumSet<TrackableProperty> dirtyProps = obj.hasConsumerChanges(consumerId)
                    ? obj.getAndClearDirtyProps(consumerId)
                    : EnumSet.noneOf(TrackableProperty.class);
            Map<TrackableProperty, Object> delta = dirtyProps.isEmpty()
                    ? new EnumMap<>(TrackableProperty.class)
                    : buildPropertyMap(obj, dirtyProps);
            mergeDelayedProps(obj, delta);
            if (!delta.isEmpty()) {
                objectDeltas.put(deltaKey, delta);
                netLog.trace("[DeltaSync] Delta: key={} id={}, props={}",
                        String.format("0x%08X", deltaKey), obj.getId(), delta.keySet());
            }
        }
    }

    /**
     * Recursively walk the object graph starting from a TrackableObject, collecting deltas.
     * Discovers children by inspecting property values for TrackableObject/TrackableCollection
     * references. CombatView is serialized inline by toNetworkValue().
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
        int deltaKey = DeltaPacket.makeDeltaKey(obj);
        if (currentObjectIds.contains(deltaKey)) {
            // Same deltaKey seen earlier this pass (e.g. stale CardView in Commander
            // property walked before current CardView in Battlefield zone collection).
            // If this is the exact same Java instance, it was already fully processed.
            if (registeredByKey.get(deltaKey) == obj) {
                return;
            }
            // Different instance at same key — let replacement detection handle the
            // delta, then fall through to walk children so that child replacements
            // (e.g. CardStateViews with updated P/T) are also picked up.
            collectObjectDelta(obj, objectDeltas, newObjects);
        } else {
            currentObjectIds.add(deltaKey);
            collectObjectDelta(obj, objectDeltas, newObjects);
        }

        obj.getVersion(); // volatile read — memory barrier for child traversal
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
        // Volatile read of version establishes happens-before from the game thread's
        // props.put (which precedes the volatile version++ in markDirtyForConsumers).
        // Without this, the daemon thread's props.get may see stale values when the
        // game thread modifies properties concurrently after the dirty bit was consumed.
        obj.getVersion();
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
        obj.getVersion(); // volatile read — memory barrier (see buildPropertyMap)
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

        // CardStateView slot reference → ordinal of CardStateName
        if (type == TrackableTypes.CardStateViewType) {
            CardStateView csv = (CardStateView) value;
            return csv.getState().ordinal();
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

        // No defensive copy needed here — flagAsChanged() now replaces mutable
        // values with immutable copies at mutation time, and set() callers create
        // new instances by convention. Values in props are safe to pass through.
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
                consumerId, registeredByKey.size());
    }

    private void registerAndMark(TrackableObject obj) {
        int deltaKey = DeltaPacket.makeDeltaKey(obj);
        sentObjectIds.add(deltaKey);
        registeredByKey.put(deltaKey, obj);
        obj.registerConsumer(consumerId);
        // Clear any dirty props accumulated before registration
        obj.getAndClearDirtyProps(consumerId);
    }

    /**
     * Register consumers on objects not yet tracked, without clearing dirty bits.
     * Used when the view graph has been populated after the initial sendFullState
     * (which sees an empty view). Objects already registered by collectDeltas'
     * new-object path are skipped — their consumers and dirty bits are preserved.
     */
    public void registerNewObjects(GameView gameView) {
        if (gameView == null) {
            return;
        }
        int before = registeredByKey.size();
        walkAndRegisterNew(gameView, new HashSet<>());
        int added = registeredByKey.size() - before;
        if (added > 0) {
            netLog.info("[DeltaSync] Registered {} new objects (total {})", added, registeredByKey.size());
        }
    }

    private void walkAndRegisterNew(TrackableObject obj, Set<Integer> visited) {
        if (obj == null) return;
        int type = DeltaPacket.typeTagFor(obj);
        if (type < 0) return;
        int deltaKey = DeltaPacket.makeDeltaKey(obj);
        if (!visited.add(deltaKey)) return;

        // Only register consumer if not already tracked — don't add to
        // sentObjectIds so collectDeltas' new-object path still fires and
        // sends the full property map to the client.
        if (!registeredByKey.containsKey(deltaKey)) {
            obj.registerConsumer(consumerId);
        }

        Map<TrackableProperty, Object> props = obj.getProps();
        if (props != null) {
            for (Object value : props.values()) {
                if (value instanceof TrackableObject) {
                    walkAndRegisterNew((TrackableObject) value, visited);
                } else if (value instanceof TrackableCollection) {
                    for (Object item : (TrackableCollection<?>) value) {
                        if (item instanceof TrackableObject) {
                            walkAndRegisterNew((TrackableObject) item, visited);
                        }
                    }
                }
            }
        }
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
        int deltaKey = DeltaPacket.makeDeltaKey(obj);
        if (!visited.add(deltaKey)) {
            return;
        }

        registerAndMark(obj);

        Map<TrackableProperty, Object> props = obj.getProps();
        if (props != null) {
            for (Object value : props.values()) {
                if (value instanceof TrackableObject to) {
                    walkAndMarkAsSent(to, visited);
                } else if (value instanceof TrackableCollection) {
                    for (Object item : (TrackableCollection<?>) value) {
                        if (item instanceof TrackableObject to) {
                            walkAndMarkAsSent(to, visited);
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
        Set<TrackableProperty> eligible = NetworkChecksumUtil.getEligibleProperties();
        List<TrackableProperty> selected = new ArrayList<>(SAMPLE_SIZE);

        int biasTarget = SAMPLE_SIZE / 2;
        List<TrackableProperty> biasedCandidates = new ArrayList<>();
        for (TrackableProperty prop : recentDeltaProperties) {
            if (eligible.contains(prop)) {
                biasedCandidates.add(prop);
            }
        }
        Collections.shuffle(biasedCandidates);
        int biasCount = Math.min(biasTarget, biasedCandidates.size());
        for (int i = 0; i < biasCount; i++) {
            selected.add(biasedCandidates.get(i));
        }

        // Fill remaining slots randomly from rest of eligible pool
        Set<TrackableProperty> selectedSet = EnumSet.noneOf(TrackableProperty.class);
        selectedSet.addAll(selected);
        List<TrackableProperty> remaining = new ArrayList<>();
        for (TrackableProperty prop : eligible) {
            if (!selectedSet.contains(prop)) {
                remaining.add(prop);
            }
        }
        Collections.shuffle(remaining);
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
        if (lastChecksumBreakdown != null) {
            netLog.error("[DeltaSync] Server breakdown: {}", lastChecksumBreakdown);
        }
        if (lastChecksumDetail != null) {
            netLog.error("[DeltaSync] Server checksum detail: {}", lastChecksumDetail);
        }
    }

    // ==================== Checksum and validation ====================

    private void logSampledChecksumDetails(GameView gameView, int checksum, long seq, int[] sampledOrdinals) {
        int turn = gameView.getTurn();
        int phaseOrdinal = gameView.getPhase() != null ? gameView.getPhase().ordinal() : -1;
        String phaseName = phaseOrdinal >= 0 ?
                forge.game.phase.PhaseType.values()[phaseOrdinal].name() : "null";
        netLog.info("[DeltaSync] Sampled checksum for seq={}: hash={}, props={}", seq, checksum,
                NetworkChecksumUtil.sampledPropertyNames(sampledOrdinals));
        netLog.info("[DeltaSync]   Turn: {} (snapshot), Phase: {} (snapshot, current={})",
                turn, phaseName,
                gameView.getPhase() != null ? gameView.getPhase().name() : "null");
        for (PlayerView player : NetworkChecksumUtil.getSortedPlayers(gameView)) {
            netLog.info("[DeltaSync]   Player {} ({}): Life={}, Hand={}, GY={}, BF={}",
                    player.getId(), player.getName(), player.getLife(),
                    player.getZoneSize(ZoneType.Hand), player.getZoneSize(ZoneType.Graveyard), player.getZoneSize(ZoneType.Battlefield));
        }
    }

    public long getCurrentSequence() {
        return sequenceNumber.get();
    }

}
