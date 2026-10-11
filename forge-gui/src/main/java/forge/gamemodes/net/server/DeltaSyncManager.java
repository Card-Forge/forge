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

import forge.util.IHasForgeLog;
import forge.trackable.Tracker;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;
import forge.trackable.TrackableTypes;
import forge.trackable.TrackableTypes.TrackableType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages delta synchronization between server and clients.
 * Tracks changes to TrackableObjects via per-consumer dirty tracking and builds
 * minimal delta packets using property maps.
 */
public class DeltaSyncManager implements IHasForgeLog {

    // How often to include a checksum for validation (every N packets)
    public static final int CHECKSUM_INTERVAL = 20;
    private static final int MIN_CHECKSUM_INTERVAL = 5;
    private static final int CLEAN_STREAK_TO_RESTORE = 10;
    private static final int SAMPLE_SIZE = 15;
    private static final int MAX_WALK_ATTEMPTS = 3;

    // Sentinel for properties that should be skipped in network transport
    static final Object SKIP_MARKER = new Object();

    // Zone collection properties on PlayerView — the authoritative source for
    // CardView instances. Cross-reference properties (Commander, AttachedCards,
    // ExiledWith, etc.) may hold stale instances after zone changes via copyCard.
    // Built dynamically from ZoneType's trackable property mapping.
    // Excludes Flashback: virtual zone whose cards are references to cards in
    // other zones (Graveyard, Library, etc.), not unique canonical instances.
    private static final EnumSet<TrackableProperty> ZONE_COLLECTIONS = EnumSet.noneOf(TrackableProperty.class);
    static {
        for (ZoneType z : ZoneType.values()) {
            TrackableProperty prop = z.getTrackableProperty();
            if (prop != null && z != ZoneType.Flashback) {
                ZONE_COLLECTIONS.add(prop);
            }
        }
    }

    // A SHELL card's properties: what a card in a library carries that a client uses, whose it is and where.
    // Everything else names the card or belongs to another zone
    private static final EnumSet<TrackableProperty> SHELL_PROPS = EnumSet.of(
            TrackableProperty.Owner, TrackableProperty.Controller, TrackableProperty.Zone);

    /** How much of a card this consumer is sent. SHELL: the shell properties and no state views. FULL: everything. */
    private enum Level { SHELL, FULL }

    /** What this consumer holds at a delta key: the instance it was sent, and the level it was sent at. */
    private record Sent(TrackableObject obj, Level level) {}

    /**
     * One walk's classification inputs. Each card is classified once per walk; the cache is by instance, because two
     * instances at one key (a zone-change copy beside a stale reference) can sit in different zones.
     */
    private record Classifier(Collection<PlayerView> viewers, Map<CardView, Level> levels, Set<Integer> revealed) {
        Level levelFor(TrackableObject obj) {
            // Unfiltered, every card is FULL: nothing to classify or cache. A card in the reveal history is FULL
            // for every consumer: the history is one list on the GameView, sent to every proxy alike
            return viewers != null && obj instanceof CardView cv
                    ? levels.computeIfAbsent(cv, c -> revealed.contains(c.getId()) ? Level.FULL : levelOf(c, viewers))
                    : Level.FULL;
        }
    }

    // each DeltaSyncManager gets a unique ID
    private static final AtomicInteger NEXT_CONSUMER_ID = new AtomicInteger(0);
    private final int consumerId = NEXT_CONSUMER_ID.getAndIncrement();

    /** Per-client ID used to gate IdRef substitution on the outer codec. */
    public int getConsumerId() {
        return consumerId;
    }

    private long sequenceNumber = 0;

    // Objects registered with this consumer (for cleanup on disconnect/reset), with the level each was sent at
    private final Map<Integer, Sent> registeredByKey = new HashMap<>();
    // Used to block stale cross-reference replacements
    private final Map<Integer, CardView> authoritativeInstances = new HashMap<>();
    // Per-object detail of the current collectDeltas call, logged as one line
    private final StringBuilder walkTrace = new StringBuilder();

    // Not atomic: only accessed from game thread
    // Defer the first checksum until the game state stabilizes — seq=1 races
    // with game initialization (hand drawing), so an immediate checksum would
    // compare a mid-init snapshot against the client's post-delta state.
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
     * <p>A library card none of {@code viewers} may see goes out as a shell (whose it is and where, no state views)
     * and is filled in on the instance the client holds once one of them may see it; an instance sent in full is
     * never sent as a shell again. A card in the game's reveal history goes out in full. {@code null} viewers means
     * unfiltered; an empty collection sees nothing.
     *
     * <p>Another thread can change the graph while this runs, so a walk that throws is tried
     * again. If none of the attempts get through, the packet ships with whatever was collected
     * rather than letting the exception reach the caller — on the game thread that would end
     * the game loop and leave the match unable to continue.
     */
    public DeltaPacket collectDeltas(GameView gameView, Collection<PlayerView> viewers) {
        Map<Integer, Map<TrackableProperty, Object>> objectDeltas = new HashMap<>();
        // need parent-before-child insertion order
        Map<Integer, Map<TrackableProperty, Object>> newObjects = new LinkedHashMap<>();
        Set<Integer> currentObjectIds = null;
        walkTrace.setLength(0);

        // A fresh visited set per attempt: reusing it would make walkAndCollect return at the
        // root and collect nothing. Deltas already gathered are kept, because the dirty flags
        // behind them have been cleared and no later walk would find them again.
        for (int attempt = 1; attempt <= MAX_WALK_ATTEMPTS; attempt++) {
            Set<Integer> visited = new HashSet<>();
            try {
                // Per attempt as well: a retry classifies afresh; an unfiltered walk classifies nothing
                Classifier classifier = new Classifier(viewers, viewers == null ? null : new IdentityHashMap<>(),
                        viewers == null ? null : revealedCards(gameView));
                authoritativeInstances.clear();
                preScanZoneCollections(gameView);
                walkAndCollect(gameView, classifier, objectDeltas, newObjects, visited);
                currentObjectIds = visited;
                break;
            } catch (ConcurrentModificationException e) {
                netLog.warn(e, "[DeltaSync] Walk attempt {} of {} failed", attempt, MAX_WALK_ATTEMPTS);
            }
        }
        if (currentObjectIds == null) {
            netLog.error("[DeltaSync] Walk failed {} times, shipping what was collected", MAX_WALK_ATTEMPTS);
        }

        // Only after a complete walk: an unfinished one never reached every object, so its
        // visited set would unregister objects that are still in the graph
        if (currentObjectIds != null) {
            // Prune registrations for objects no longer in the graph
            Iterator<Map.Entry<Integer, Sent>> regIt = registeredByKey.entrySet().iterator();
            while (regIt.hasNext()) {
                Map.Entry<Integer, Sent> entry = regIt.next();
                if (!currentObjectIds.contains(entry.getKey())) {
                    entry.getValue().obj().unregisterConsumer(consumerId);
                    regIt.remove();
                }
            }
        }

        // Accumulate changed properties for delta-biased sampling
        for (Map<TrackableProperty, Object> delta : objectDeltas.values()) {
            recentDeltaProperties.addAll(delta.keySet());
        }

        if (!newObjects.isEmpty()) {
            netLog.info("[DeltaSync] New objects: {}, Deltas: {}", newObjects.size(), objectDeltas.size());
        }
        if (walkTrace.length() > 0) {
            // e.g. 0x30000001[Phase] +0x20000007(8) ~0x00000046(14): changed props, new object (+), replaced instance (~)
            netLog.trace("[DeltaSync] Collected:{}", walkTrace);
        }

        sequenceNumber++;

        int checksum = 0;
        int[] checksumPropertyOrdinals = null;
        packetsSinceLastChecksum++;
        // Skipped after an unfinished walk: the client cannot match a checksum over state the packet did not carry
        if (currentObjectIds != null && packetsSinceLastChecksum >= checksumInterval) {
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

            logSampledChecksumDetails(gameView, checksum, sequenceNumber, checksumPropertyOrdinals);

            // Store breakdown for logging if the client reports a mismatch
            int turn = gameView.getTurn();
            int phaseOrdinal = gameView.getPhase() != null ? gameView.getPhase().ordinal() : -1;
            lastChecksumBreakdown = NetworkChecksumUtil.computeChecksumBreakdown(turn, phaseOrdinal, gameView);
            lastChecksumDetail = detail;
        }

        return new DeltaPacket(sequenceNumber, objectDeltas, newObjects, checksum, checksumPropertyOrdinals);
    }

    /**
     * Recursively walk the object graph starting from a TrackableObject, collecting deltas.
     * Discovers children by inspecting property values for TrackableObject/TrackableCollection
     * references. CombatView is serialized inline by toNetworkValue().
     */
    private void walkAndCollect(TrackableObject obj, Classifier classifier,
                                Map<Integer, Map<TrackableProperty, Object>> objectDeltas,
                                Map<Integer, Map<TrackableProperty, Object>> newObjects,
                                Set<Integer> currentObjectIds) {
        int type = DeltaPacket.typeTagFor(obj);
        if (type < 0) return;
        int deltaKey = DeltaPacket.makeDeltaKey(obj);

        // Block stale cross-references before touching currentObjectIds.
        // Zone instances (seeded by preScanZoneCollections) are authoritative —
        // any other instance at the same key is stale and must not be processed
        // or have its children walked (stale CardStateViews would bypass the
        // CardView-only auth check and overwrite correct deltas).
        TrackableObject auth = authoritativeInstances.get(deltaKey);
        if (auth != null && auth != obj) return;

        Sent held = registeredByKey.get(deltaKey);
        // Dedup: skip if same instance already processed this pass
        if (!currentObjectIds.add(deltaKey)) {
            if (held != null && held.obj() == obj) return;
            // Different instance at same key — replacement (zone change)
        }

        // An instance sent in full stays FULL; another instance at this key, a zone-change copy, is classified afresh
        Level level = held != null && held.obj() == obj && held.level() == Level.FULL ? Level.FULL : classifier.levelFor(obj);
        collectObjectDelta(obj, level, objectDeltas, newObjects);

        boolean parentIsGameEntityView = obj instanceof GameEntityView;
        for (Map.Entry<TrackableProperty, Object> entry : ((Map<TrackableProperty, Object>) obj.getProps()).entrySet()) {
            Object value = entry.getValue();
            if (value instanceof TrackableObject to) {
                // A shell carries no state views, so they are neither sent nor registered
                if (level == Level.SHELL) {
                    continue;
                }
                // Skip GameEntityView→GameEntityView scalar cross-references
                // (CardView→CardView, PlayerView→CardView are stale after zone
                // changes). Non-GameEntityView parents (StackItemView.SourceCard)
                // hold primary containment refs — walked and auth-checked above.
                if (parentIsGameEntityView && to instanceof GameEntityView) {
                    continue;
                }
                walkAndCollect(to, classifier, objectDeltas, newObjects, currentObjectIds);
            } else if (value instanceof TrackableCollection<?> tc) {
                // Walked for a shell too: each card in it is classified on its own, and walkAndRegister has
                // registered it, so a card registered but never sent would reach prompts as an id the client lacks
                for (TrackableObject to : tc) {
                    walkAndCollect(to, classifier, objectDeltas, newObjects, currentObjectIds);
                }
            }
        }
    }

    /**
     * Process a single object's delta. Stale cross-references are already
     * filtered by the authoritative check in walkAndCollect.
     *
     * @param level the level this walk classified the object at, FULL for anything but a card
     */
    private void collectObjectDelta(TrackableObject obj, Level level,
                                    Map<Integer, Map<TrackableProperty, Object>> objectDeltas,
                                    Map<Integer, Map<TrackableProperty, Object>> newObjects) {
        int deltaKey = DeltaPacket.makeDeltaKey(obj);
        Sent old = registeredByKey.get(deltaKey);
        Set<TrackableProperty> allowed = level == Level.SHELL ? SHELL_PROPS : null;

        if (old != null && old.obj() == obj) {
            // Existing object — dirty props only
            EnumSet<TrackableProperty> dirtyProps = obj.getAndClearDirtyProps(consumerId);
            // identical to mergeDelayedProps' own early-out: only a frozen tracker can add to an empty
            // dirty set, so with no dirty props and no freeze the delta below would always come out empty.
            // A level change sends a clean card too: the viewers and the controller's MindSlaveMaster dirty nothing on it
            Tracker tracker = obj.getTracker();
            if (dirtyProps.isEmpty() && (tracker == null || !tracker.isFrozen()) && level == old.level()) {
                return;
            }
            Map<TrackableProperty, Object> delta;
            if (level == old.level()) {
                delta = buildPropertyMap(obj, dirtyProps, allowed);
            } else {
                // SHELL to FULL, the only change a held instance makes: everything the card holds, applied to the
                // instance the client has, plus any dirty key it no longer holds. The walk then reaches its state
                // views, which go out as new objects
                dirtyProps.addAll(((Map<TrackableProperty, Object>) obj.getProps()).keySet());
                delta = buildPropertyMap(obj, dirtyProps);
                registeredByKey.put(deltaKey, new Sent(obj, level));
                netLog.trace("[DeltaSync] Level {} -> {}: id={}", old.level(), level, obj.getId());
            }
            if (!delta.isEmpty()) {
                // Merged, not replaced: a retried walk must not drop props an earlier attempt took
                Map<TrackableProperty, Object> collected = objectDeltas.get(deltaKey);
                if (collected != null) {
                    collected.putAll(delta);
                } else {
                    objectDeltas.put(deltaKey, delta);
                }
                walkTrace.append(String.format(" 0x%08X", deltaKey)).append(delta.keySet());
            }
            return;
        }

        // New or replacement — send full state via newObjects so the client
        // clears stale properties before applying
        // Built before registering: if this throws, the object stays unregistered and a later
        // walk retries it, instead of being on the books as sent when it never was
        Map<TrackableProperty, Object> allProps = buildPropertyMap(obj, null, allowed);
        if (old != null) {
            old.obj().unregisterConsumer(consumerId);
            objectDeltas.remove(deltaKey);
        }
        obj.registerConsumer(consumerId);
        obj.getAndClearDirtyProps(consumerId);
        registeredByKey.put(deltaKey, new Sent(obj, level));
        if (!allProps.isEmpty()) {
            newObjects.put(deltaKey, allProps);
            walkTrace.append(String.format(" %s0x%08X(%d%s)", old != null ? "~" : "+", deltaKey, allProps.size(),
                    level == Level.SHELL ? " shell" : ""));
        }
    }

    /**
     * The ids of the cards in the game's reveal history; empty when it has none. By id, not instance: a revealed card
     * that changes instance on its way back into a library stays listed.
     */
    private static Set<Integer> revealedCards(GameView gameView) {
        Set<Integer> revealed = new HashSet<>();
        TrackableCollection<CardView> collection = gameView == null ? null : gameView.getRevealedCollection();
        if (collection != null) {
            for (CardView cv : collection) {
                revealed.add(cv.getId());
            }
        }
        return revealed;
    }

    /**
     * The level {@code cv} goes out at for {@code viewers}: FULL outside the library; in it, FULL when a viewer is in
     * PlayerMayLook or is the controller's MindSlaveMaster while the controller is, otherwise SHELL. Zone, Controller
     * and MindSlaveMaster are read as the packet carries them, delayed under a freeze; PlayerMayLook ignores freezes.
     */
    private static Level levelOf(CardView cv, Collection<PlayerView> viewers) {
        Map<TrackableProperty, Object> delayed = delayedPropsFor(cv);
        if (effectiveValue(cv, delayed, TrackableProperty.Zone) != ZoneType.Library) {
            return Level.FULL;
        }
        // CardView.canBeShownTo restricted to the Library zone
        @SuppressWarnings("unchecked")
        TrackableCollection<PlayerView> mayLook = (TrackableCollection<PlayerView>) ((Map<TrackableProperty, Object>) cv.getProps()).get(TrackableProperty.PlayerMayLook);
        if (mayLook == null) {
            return Level.SHELL;
        }
        PlayerView controller = (PlayerView) effectiveValue(cv, delayed, TrackableProperty.Controller);
        PlayerView master = controller == null ? null
                : (PlayerView) effectiveValue(controller, delayedPropsFor(controller), TrackableProperty.MindSlaveMaster);
        for (PlayerView viewer : viewers) {
            if (mayLook.indexOf(viewer) != -1
                    || master != null && master != controller && master == viewer && mayLook.indexOf(controller) != -1) {
                return Level.FULL;
            }
        }
        return Level.SHELL;
    }

    /** The props a freeze has delayed for {@code obj}, empty when the tracker is not frozen. */
    private static Map<TrackableProperty, Object> delayedPropsFor(TrackableObject obj) {
        Tracker tracker = obj.getTracker();
        if (tracker == null || !tracker.isFrozen()) {
            return Collections.emptyMap();
        }
        return tracker.getDelayedPropsFor(obj);
    }

    /**
     * {@code prop} as a packet built now carries it: the lookup of NetworkChecksumUtil.getEffectiveValue over a
     * delayed map read once per object, with the property's default where that finds null.
     */
    private static Object effectiveValue(TrackableObject obj, Map<TrackableProperty, Object> delayed, TrackableProperty prop) {
        Object value = delayed.containsKey(prop) ? delayed.get(prop) : ((Map<TrackableProperty, Object>) obj.getProps()).get(prop);
        return value != null ? value : prop.getDefaultValue();
    }

    /**
     * Pre-scan zone collections across all players to seed authoritativeInstances.
     * Provides cross-player coverage for stale Commander references.
     */
    private void preScanZoneCollections(GameView gameView) {
        if (gameView == null || gameView.getPlayers() == null) return;
        for (PlayerView player : gameView.getPlayers()) {
            for (TrackableProperty zoneProp : ZONE_COLLECTIONS) {
                if (((Map<TrackableProperty, Object>) player.getProps()).get(zoneProp) instanceof TrackableCollection<?> tc) {
                    for (Object item : tc) {
                        if (item instanceof CardView cv) {
                            authoritativeInstances.putIfAbsent(DeltaPacket.makeDeltaKey(cv), cv);
                        }
                    }
                }
            }
        }
    }

    /**
     * Build a property map for a subset of dirty properties.
     */
    private Map<TrackableProperty, Object> buildPropertyMap(TrackableObject obj, Set<TrackableProperty> dirtyProps) {
        return buildPropertyMap(obj, dirtyProps, null);
    }

    /**
     * Build a property map for a subset of dirty properties. {@code allowed}, when not null, bounds the keys that can
     * reach the map, including the delayed ones a freeze adds to {@code dirtyProps}.
     */
    private Map<TrackableProperty, Object> buildPropertyMap(TrackableObject obj, Set<TrackableProperty> dirtyProps,
                                                            Set<TrackableProperty> allowed) {
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
            if (allowed != null && !allowed.contains(prop)) {
                continue;
            }
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

        if (type == TrackableTypes.StackItemViewType) {
            return ((TrackableObject) value).getId();
        }

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
        int type = DeltaPacket.typeTagFor(obj);
        if (type < 0) return;
        int deltaKey = DeltaPacket.makeDeltaKey(obj);
        if (!visited.add(deltaKey)) return;
        // A state view is registered by the collectDeltas walk that sends it: a shell's is never sent, and a
        // registration made here would never be pruned or reset. It holds no TrackableObject: nothing below is skipped
        if (obj instanceof CardStateView) return;

        // Only register consumer if not already tracked — don't add to
        // registeredByKey so collectDeltas' new-object path still fires and
        // sends the full property map to the client.
        if (!registeredByKey.containsKey(deltaKey)) {
            obj.registerConsumer(consumerId);
        }

        // Same skip guards as walkAndCollect. No auth check needed here:
        // walkAndRegister only registers consumers (no data sent), and the
        // first collectDeltas corrects any stale registrations.
        boolean parentIsGameEntityView = obj instanceof GameEntityView;
        for (Object value : ((Map<TrackableProperty, Object>) obj.getProps()).values()) {
            if (value instanceof TrackableObject to) {
                if (parentIsGameEntityView && to instanceof GameEntityView) {
                    continue;
                }
                walkAndRegister(to, visited);
            } else if (value instanceof TrackableCollection<?> tc) {
                for (TrackableObject to : tc) {
                    walkAndRegister(to, visited);
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
        // Clear so a later resync only logs if a fresh checksum has been
        // computed since — otherwise we'd log a breakdown that postdates the
        // mismatch the client is reporting.
        lastChecksumBreakdown = null;
        lastChecksumDetail = null;
    }

    /**
     * Reset all tracking state for reconnection.
     * Unregisters this consumer from all tracked objects.
     * After reset, the next sync will be treated as a fresh initial sync.
     */
    public void reset() {
        // Unregister consumer from all tracked objects
        for (Sent sent : registeredByKey.values()) {
            sent.obj().unregisterConsumer(consumerId);
        }
        registeredByKey.clear();
        sequenceNumber = 0;
        packetsSinceLastChecksum = 0;
        recentDeltaProperties.clear();
        checksumInterval = CHECKSUM_INTERVAL;
        cleanChecksumStreak = 0;
        lastChecksumBreakdown = null;
        lastChecksumDetail = null;
    }

    public long getCurrentSequence() {
        return sequenceNumber;
    }

}
