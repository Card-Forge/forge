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
import java.util.ConcurrentModificationException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private static final int MIN_CHECKSUM_INTERVAL = 5;
    private static final int CLEAN_STREAK_TO_RESTORE = 10;
    private static final int SAMPLE_SIZE = 15;

    // Sentinel for properties that should be skipped in network transport
    static final Object SKIP_MARKER = new Object();

    // Zone collection properties on PlayerView — the authoritative source for
    // CardView instances. Cross-reference properties (Commander, AttachedCards,
    // ExiledWith, etc.) may hold stale instances after zone changes via copyCard.
    private static final EnumSet<TrackableProperty> ZONE_COLLECTIONS = EnumSet.of(
            TrackableProperty.Ante, TrackableProperty.Battlefield,
            TrackableProperty.Command, TrackableProperty.Exile,
            TrackableProperty.Flashback, TrackableProperty.Graveyard,
            TrackableProperty.Hand, TrackableProperty.Library,
            TrackableProperty.Sideboard);

    // each DeltaSyncManager gets a unique ID
    private static final AtomicInteger NEXT_CONSUMER_ID = new AtomicInteger(0);
    private final int consumerId = NEXT_CONSUMER_ID.getAndIncrement();

    private final AtomicLong sequenceNumber = new AtomicLong(0);

    // Objects that have been fully sent to the client (initial sync done)
    private final Set<Integer> sentObjectIds = ConcurrentHashMap.newKeySet();
    // Objects registered with this consumer (for cleanup on disconnect/reset)
    private final Map<Integer, TrackableObject> registeredByKey = new HashMap<>();
    // Used to block stale cross-reference replacements
    private final Map<Integer, CardView> authoritativeInstances = new HashMap<>();

    // Not atomic: only accessed from game thread
    // Start at 0 so the first checksum is deferred until the game state stabilizes.
    // The first delta (seq=1) races with game initialization (hand drawing), so
    // an immediate checksum would compare a mid-initialization snapshot against
    // the client's post-delta state, causing false mismatches (~7% of games).
    private long packetsSinceLastChecksum = 0;

    // Sampled checksum state
    private final EnumSet<TrackableProperty> recentDeltaProperties = EnumSet.noneOf(TrackableProperty.class);
    private int checksumInterval = CHECKSUM_INTERVAL;
    private int cleanChecksumStreak = 0;
    // Stored at checksum time, logged on resync request
    private String lastChecksumBreakdown;
    private List<String> lastChecksumDetail;

    /**
     * Collect all changes from the GameView hierarchy and build a delta packet.
     * New objects are registered with this consumer and sent in full.
     * Existing objects only send properties dirty for THIS consumer.
     *
     * <p>Must be called on the game thread. All delta collection and checksum
     * computation runs single-threaded — no locks, snapshots, or volatile
     * barriers needed.
     */
    public DeltaPacket collectDeltas(GameView gameView) {
        Map<Integer, Map<TrackableProperty, Object>> objectDeltas = new HashMap<>();
        // need parent-before-child insertion order
        Map<Integer, Map<TrackableProperty, Object>> newObjects = new LinkedHashMap<>();
        Set<Integer> currentObjectIds = new HashSet<>();

        authoritativeInstances.clear();
        // Pre-scan zone collections across all players for cross-player coverage.
        preScanZoneCollections(gameView);
        walkAndCollect(gameView, objectDeltas, newObjects, currentObjectIds);

        // Update tracked objects — prune IDs for removed objects
        sentObjectIds.retainAll(currentObjectIds);
        sentObjectIds.addAll(currentObjectIds);

        // Prune registrations for objects no longer in the graph
        Iterator<Map.Entry<Integer, TrackableObject>> regIt = registeredByKey.entrySet().iterator();
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

        int checksum = 0;
        int[] checksumPropertyOrdinals = null;
        packetsSinceLastChecksum++;
        if (packetsSinceLastChecksum >= checksumInterval) {
            checksumPropertyOrdinals = selectChecksumProperties();
            List<String> detail = new ArrayList<>();
            checksum = NetworkChecksumUtil.computeSampledChecksum(gameView, checksumPropertyOrdinals, detail);
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
            lastChecksumDetail = detail;
        }

        return new DeltaPacket(seq, objectDeltas, newObjects, checksum, checksumPropertyOrdinals);
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
        } else {
            currentObjectIds.add(deltaKey);
        }

        collectObjectDelta(obj, objectDeltas, newObjects);

        Map<TrackableProperty, Object> props = obj.getProps();
        if (props != null) {
            // Snapshot props into entries, sorted with zone collections first.
            // Zone-first ordering ensures zone CardViews are registered as
            // authoritative before cross-reference properties are walked.
            boolean parentIsGameEntityView = obj instanceof GameEntityView;
            @SuppressWarnings("unchecked")
            Map.Entry<TrackableProperty, Object>[] entries =
                    props.entrySet().toArray(new Map.Entry[0]);
            Arrays.sort(entries, (a, b) -> Boolean.compare(
                    ZONE_COLLECTIONS.contains(b.getKey()), ZONE_COLLECTIONS.contains(a.getKey())));

            for (Map.Entry<TrackableProperty, Object> entry : entries) {
                Object value = entry.getValue();
                boolean isZone = ZONE_COLLECTIONS.contains(entry.getKey());
                if (value instanceof TrackableObject to) {
                    // Skip stale GameEntityView cross-references from
                    // GameEntityView parents (CardView→CardView, PlayerView→CardView).
                    // Non-GameEntityView parents (StackItemView.SourceCard) hold
                    // primary containment refs that must be walked.
                    if (parentIsGameEntityView && to instanceof GameEntityView) {
                        continue;
                    }
                    walkAndCollect(to, objectDeltas, newObjects, currentObjectIds);
                } else if (value instanceof TrackableCollection<?> tc) {
                    for (Object item : tc) {
                        if (item instanceof TrackableObject to) {
                            // Register zone CardViews as authoritative
                            if (isZone && to instanceof CardView cv) {
                                authoritativeInstances.put(DeltaPacket.makeDeltaKey(cv), cv);
                            }
                            // Skip already-discovered CardViews in non-zone collections.
                            // Zone items are always walked (sorted first, authoritative).
                            // Non-zone collections on GameEntityView parents may hold
                            // stale cross-refs (Commander, AttachedCards, etc.).
                            if (!isZone && parentIsGameEntityView && to instanceof CardView
                                    && currentObjectIds.contains(DeltaPacket.makeDeltaKey(to))) {
                                continue;
                            }
                            walkAndCollect(to, objectDeltas, newObjects, currentObjectIds);
                        }
                    }
                }
            }
        }
    }

    private void collectObjectDelta(TrackableObject obj,
                                    Map<Integer, Map<TrackableProperty, Object>> objectDeltas,
                                    Map<Integer, Map<TrackableProperty, Object>> newObjects) {
        int deltaKey = DeltaPacket.makeDeltaKey(obj);

        if (!sentObjectIds.contains(deltaKey)) {
            // New object — register consumer and build full property map.
            // Clear dirty props BEFORE building the map.
            //
            // If the authoritative instance (from a zone collection this cycle) is
            // already registered for this key, block this stale cross-reference.
            TrackableObject auth = authoritativeInstances.get(deltaKey);
            if (auth != null && registeredByKey.get(deltaKey) == auth && auth != obj) {
                return;
            }
            obj.registerConsumer(consumerId);
            registeredByKey.put(deltaKey, obj);
            obj.getAndClearDirtyProps(consumerId);
            Map<TrackableProperty, Object> allProps = buildPropertyMap(obj, null);
            if (allProps != null && !allProps.isEmpty()) {
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
            //
            // Block stale cross-reference replacements when the existing registered
            // instance IS the authoritative one (discovered through a zone collection
            // this cycle). Legitimate replacements (card moved zones) are allowed
            // because the old instance won't match the new authoritative instance.
            TrackableObject auth = authoritativeInstances.get(deltaKey);
            if (auth != null && registeredByKey.get(deltaKey) == auth) {
                return;
            }
            TrackableObject old = registeredByKey.get(deltaKey);
            if (old != null) {
                old.unregisterConsumer(consumerId);
            }
            obj.registerConsumer(consumerId);
            registeredByKey.put(deltaKey, obj);
            obj.getAndClearDirtyProps(consumerId);
            Map<TrackableProperty, Object> allProps = buildPropertyMap(obj, null);
            if (allProps != null && !allProps.isEmpty()) {
                objectDeltas.remove(deltaKey);
                newObjects.put(deltaKey, allProps);
                netLog.trace("[DeltaSync] Replaced instance: key={} id={}, {} props",
                        String.format("0x%08X", deltaKey), obj.getId(), allProps.size());
            }
        } else {
            // Existing object
            EnumSet<TrackableProperty> dirtyProps = obj.getAndClearDirtyProps(consumerId);
            Map<TrackableProperty, Object> delta = buildPropertyMap(obj, dirtyProps);
            if (delta != null && !delta.isEmpty()) {
                objectDeltas.put(deltaKey, delta);
                netLog.trace("[DeltaSync] Delta: key={} id={}, props={}",
                        String.format("0x%08X", deltaKey), obj.getId(), delta.keySet());
            }
        }
    }

    /**
     * Pre-scan zone collections across all players to seed authoritativeInstances.
     * Provides cross-player coverage for stale Commander references.
     */
    private void preScanZoneCollections(GameView gameView) {
        if (gameView == null || gameView.getPlayers() == null) return;
        for (PlayerView player : gameView.getPlayers()) {
            Map<TrackableProperty, Object> props = player.getProps();
            if (props == null) continue;
            for (TrackableProperty zoneProp : ZONE_COLLECTIONS) {
                Object val = props.get(zoneProp);
                if (val instanceof TrackableCollection<?> tc) {
                    try {
                        for (Object item : tc) {
                            if (item instanceof CardView cv) {
                                authoritativeInstances.putIfAbsent(DeltaPacket.makeDeltaKey(cv), cv);
                            }
                        }
                    } catch (ConcurrentModificationException e) {
                        // Rare race — best effort
                    }
                }
            }
        }
    }

    /**
     * Build a property map for a subset of dirty properties.
     */
    private Map<TrackableProperty, Object> buildPropertyMap(TrackableObject obj,
                                                             Set<TrackableProperty> dirtyProps) {
        Map<TrackableProperty, Object> props = obj.getProps();
        // Copy props — mergeDelayedProps may add entries, and we iterate later
        Map<TrackableProperty, Object> snapshot = new EnumMap<>(props);
        mergeDelayedProps(obj, snapshot, dirtyProps);
        if (dirtyProps == null) {
            // additional delayed props will be included from fresh object
            dirtyProps = snapshot.keySet();
        }
        Map<TrackableProperty, Object> delta = new EnumMap<>(TrackableProperty.class);
        for (TrackableProperty prop : dirtyProps) {
            Object netValue = toNetworkValue(prop, snapshot.get(prop));
            if (netValue != SKIP_MARKER) {
                delta.put(prop, netValue);
            }
        }
        return delta;
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
    private void mergeDelayedProps(TrackableObject obj, Map<TrackableProperty, Object> delta, Set<TrackableProperty> dirtyProps) {
        Tracker tracker = obj.getTracker();
        if (tracker == null || !tracker.isFrozen()) return;
        for (Map.Entry<TrackableProperty, Object> entry : tracker.getDelayedPropsFor(obj).entrySet()) {
            delta.put(entry.getKey(), entry.getValue());
            if (dirtyProps != null) {
                dirtyProps.add(entry.getKey());
            }
        }
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
        walkAndRegister(gameView, new HashSet<>());
        int added = registeredByKey.size() - before;
        if (added > 0) {
            netLog.info("[DeltaSync] Registered {} new objects (total {})", added, registeredByKey.size());
        }
    }

    private void walkAndRegister(TrackableObject obj, Set<Integer> visited) {
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

        // Same skip guards as walkAndCollect's non-PlayerView branch.
        // No two-pass needed here: walkAndRegister only registers consumers
        // (no data sent), and the first collectDeltas corrects registrations
        Map<TrackableProperty, Object> props = obj.getProps();
        if (props != null) {
            boolean parentIsGameEntityView = obj instanceof GameEntityView;
            boolean parentIsCardView = obj instanceof CardView;
            for (Object value : props.values()) {
                if (value instanceof TrackableObject to) {
                    if (!(parentIsGameEntityView && to instanceof GameEntityView)) {
                        walkAndRegister(to, visited);
                    }
                } else if (value instanceof TrackableCollection) {
                    for (Object item : (TrackableCollection<?>) value) {
                        if (item instanceof TrackableObject to) {
                            if (parentIsCardView && to instanceof CardView
                                    && visited.contains(DeltaPacket.makeDeltaKey(to))) {
                                continue;
                            }
                            walkAndRegister(to, visited);
                        }
                    }
                }
            }
        }
    }

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
        sentObjectIds.clear();
        sequenceNumber.set(0);
        packetsSinceLastChecksum = 0;
        recentDeltaProperties.clear();
        checksumInterval = CHECKSUM_INTERVAL;
        cleanChecksumStreak = 0;
        lastChecksumBreakdown = null;
        lastChecksumDetail = null;
    }

    public long getCurrentSequence() {
        return sequenceNumber.get();
    }

}
