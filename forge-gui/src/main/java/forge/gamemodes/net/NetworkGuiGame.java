package forge.gamemodes.net;

import forge.game.GameView;
import forge.card.CardStateName;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.AbstractGuiGame;
import forge.gamemodes.net.DeltaPacket.CardStateData;
import forge.gamemodes.net.DeltaPacket.NewObjectData;
import forge.gamemodes.net.server.DeltaSyncManager;
import forge.interfaces.IGameController;
import forge.player.PlayerZoneUpdate;
import forge.trackable.Tracker;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;
import forge.trackable.TrackableTypes;
import forge.trackable.TrackableTypes.TrackableType;

import java.util.*;

/**
 * Extension of AbstractGuiGame with network delta synchronization support.
 * This class handles all network-specific deserialization and state management,
 * keeping the core AbstractGuiGame class free from network dependencies.
 *
 * All network-specific logic (delta packet application, tracker initialization,
 * reconnection handling) is contained in this subclass, allowing the base
 * AbstractGuiGame to remain focused on core local game functionality.
 */
public abstract class NetworkGuiGame extends AbstractGuiGame implements IHasNetLog {

    // Track zone changes during delta application for UI refresh
    private final Map<PlayerView, Set<ZoneType>> pendingZoneUpdates = new HashMap<>();

    /**
     * Returns whether this is a server-side GUI (NetGuiGame) or client-side.
     * Used for log prefixing to distinguish server vs client messages.
     * @return true for server-side (NetGuiGame), false for client-side
     */
    protected boolean isServerSide() {
        return false; // Default is client-side; NetGuiGame overrides to return true
    }

    /**
     * Get the log prefix for this GUI instance.
     * @return "[Server]" or "[Client]" based on isServerSide()
     */
    protected String getLogPrefix() {
        return isServerSide() ? "[Server]" : "[Client]";
    }

    /**
     * Override setGameView to add network-specific tracker initialization.
     * When receiving a deserialized GameView from the network, the tracker field
     * is null (it's transient). We need to create and populate it before use.
     */
    @Override
    public void setGameView(final GameView gameView0) {
        netLog.info("{} Called with gameView0={}, existing gameView={}",
                getLogPrefix(),
                gameView0 != null ? "non-null" : "null",
                getGameView() != null ? "non-null" : "null");

        // Skip if this exact instance was already set (e.g., beforeCall on Netty thread
        // followed by EDT dispatch). Running tracker init + copyChangedProps twice on the
        // same object is wasteful and not thread-safe.
        if (gameView0 != null && gameView0 == getGameView()) {
            return;
        }

        if (getGameView() == null || gameView0 == null) {
            if (gameView0 != null) {
                // When receiving a deserialized GameView from the network,
                // the tracker field is null (it's transient). We need to:
                // 1. Create a new Tracker
                // 2. Set it on the GameView and all contained objects
                // 3. Call updateObjLookup() to populate the index
                ensureTrackerInitialized(gameView0);
                gameView0.updateObjLookup();

                // Log PlayerView instances after initialization
                if (gameView0.getPlayers() != null) {
                    for (PlayerView pv : gameView0.getPlayers()) {
                        Tracker t = gameView0.getTracker();
                        PlayerView inTracker = t != null ? t.getObj(TrackableTypes.PlayerViewType, pv.getId()) : null;
                        netLog.trace("Initial setup: Player {} hash={}, inTracker={}, sameInstance={}",
                                pv.getId(), System.identityHashCode(pv),
                                inTracker != null, pv == inTracker);
                    }
                }
            }
            super.setGameView(gameView0);
            return;
        }

        // When updating an existing game view, the incoming gameView0 may not have
        // its tracker initialized (since tracker is transient and null after deserialization).
        // We need to ensure it has a tracker before copyChangedProps tries to use it.
        if (gameView0.getTracker() == null && getGameView() != null) {
            // Use the existing gameView's tracker for the incoming gameView0
            Tracker existingTracker = getGameView().getTracker();
            if (existingTracker != null) {
                java.util.Set<TrackableObject> visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
                setTrackerRecursively(gameView0, existingTracker, visited);
            }
        }

        // Delegate to parent for the actual property copy
        super.setGameView(gameView0);
    }

    /**
     * Ensure the Tracker is initialized on a GameView and all its contained objects.
     * This is necessary after network deserialization because the tracker field is transient.
     */
    private void ensureTrackerInitialized(GameView gameView0) {
        if (gameView0 == null) return;

        // Check if tracker needs to be created
        Tracker tracker = gameView0.getTracker();
        if (tracker == null) {
            tracker = new Tracker();
            gameView0.setTracker(tracker);
        }

        // Recursively set tracker on all TrackableObjects in the GameView's properties
        java.util.Set<TrackableObject> visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        setTrackerRecursively(gameView0, tracker, visited);
        netLog.info("Set tracker on {} unique objects", visited.size());

        // Verify trackers are set on players and their cards
        if (gameView0.getPlayers() != null) {
            for (PlayerView player : gameView0.getPlayers()) {
                boolean playerHasTracker = player.getTracker() != null;
                int cardsWithTracker = 0;
                int cardsWithoutTracker = 0;
                if (player.getHand() != null) {
                    for (CardView card : player.getHand()) {
                        if (card.getTracker() != null) cardsWithTracker++;
                        else cardsWithoutTracker++;
                    }
                }
                if (player.getLibrary() != null) {
                    for (CardView card : player.getLibrary()) {
                        if (card.getTracker() != null) cardsWithTracker++;
                        else cardsWithoutTracker++;
                    }
                }
                netLog.trace("Player {}: hasTracker={}, cards with tracker={}, cards without={}",
                    player.getId(), playerHasTracker, cardsWithTracker, cardsWithoutTracker);
            }
        }
    }

    /**
     * Recursively set the tracker on a TrackableObject and all objects it references.
     * Uses a visited set to avoid infinite loops from circular references.
     */
    @SuppressWarnings("unchecked")
    private void setTrackerRecursively(TrackableObject obj, Tracker tracker, java.util.Set<TrackableObject> visited) {
        if (obj == null) return;

        // Avoid infinite loops (uses identity comparison via IdentityHashMap-backed set)
        if (!visited.add(obj)) return;

        // Set tracker on this object
        if (obj.getTracker() == null) {
            obj.setTracker(tracker);
        }

        // Get all properties and recursively process any TrackableObjects
        Map<TrackableProperty, Object> props = obj.getProps();
        if (props == null) return;

        for (Map.Entry<TrackableProperty, Object> entry : props.entrySet()) {
            Object value = entry.getValue();
            if (value == null) continue;

            // Handle single TrackableObject
            if (value instanceof TrackableObject trackable) {
                setTrackerRecursively(trackable, tracker, visited);
            }
            // Handle collections of TrackableObjects
            else if (value instanceof Iterable<?> iterable) {
                for (Object item : iterable) {
                    if (item instanceof TrackableObject trackable) {
                        setTrackerRecursively(trackable, tracker, visited);
                    }
                }
            }
            // Handle maps that might contain TrackableObjects
            else if (value instanceof Map<?, ?> map) {
                for (Object mapValue : map.values()) {
                    if (mapValue instanceof TrackableObject trackable) {
                        setTrackerRecursively(trackable, tracker, visited);
                    }
                }
            }
        }
    }

    @Override
    public void applyDelta(DeltaPacket packet) {
        if (packet == null || getGameView() == null) {
            return;
        }

        long startTime = System.currentTimeMillis();

        Tracker tracker = getGameView().getTracker();
        if (tracker == null) {
            netLog.error("[DeltaSync] Cannot apply delta: Tracker is null");
            return;
        }

        // Log with game context for easier correlation
        String activePlayerName = getGameView().getPlayerTurn() != null ? getGameView().getPlayerTurn().getName() : "?";
        String phaseName = getGameView().getPhase() != null ? getGameView().getPhase().name() : "?";
        netLog.info("[DeltaSync] === START applyDelta seq={} (Turn {}, {}, Active={}) ===",
                packet.getSequenceNumber(), getGameView().getTurn(), phaseName, activePlayerName);

        int newObjectCount = 0;
        int appliedCount = 0;
        int skippedCount = 0;

        // Clear pending zone updates before processing
        pendingZoneUpdates.clear();

        // STEP 1: Create new objects first (so deltas can reference them)
        // Two-phase approach for cross-references (Bug #12 fix):
        // Phase 1a: Create all objects and register in tracker (without properties)
        // Phase 1b: Apply properties to all objects (now all objects exist for cross-references)
        Map<Integer, NewObjectData> newObjects = packet.getNewObjects();
        List<Map.Entry<TrackableObject, Map<TrackableProperty, Object>>> pendingPropertyApplications = new ArrayList<>();

        if (!newObjects.isEmpty()) {
            // Phase 1a: Create all objects first (without applying properties)
            for (NewObjectData newObj : newObjects.values()) {
                try {
                    TrackableObject created = createObjectOnly(newObj, tracker);
                    if (created != null) {
                        pendingPropertyApplications.add(new AbstractMap.SimpleEntry<>(created, newObj.getProperties()));
                        newObjectCount++;
                    }
                } catch (Exception e) {
                    netLog.error("[DeltaSync] Error creating new object {}", newObj.getObjectId(), e);
                }
            }
            netLog.info("[DeltaSync] Created {} new objects (phase 1a)", newObjectCount);

            // Phase 1b: Apply properties to all objects (all objects exist for cross-references)
            int propsApplied = 0;
            for (Map.Entry<TrackableObject, Map<TrackableProperty, Object>> pending : pendingPropertyApplications) {
                try {
                    applyPropertyMap(pending.getKey(), pending.getValue(), tracker);
                    propsApplied++;
                } catch (Exception e) {
                    netLog.error("[DeltaSync] Error applying properties to object {}: {}",
                            pending.getKey().getId(), e.getMessage());
                }
            }
            netLog.trace("[DeltaSync] Applied properties to {} objects (phase 1b)", propsApplied);
        }

        // STEP 2: Apply property deltas to existing objects
        for (Map.Entry<Integer, Map<TrackableProperty, Object>> entry : packet.getObjectDeltas().entrySet()) {
            int deltaKey = entry.getKey();
            Map<TrackableProperty, Object> deltaProps = entry.getValue();

            int objectType = DeltaSyncManager.getTypeFromDeltaKey(deltaKey);
            int actualObjectId = DeltaSyncManager.getIdFromDeltaKey(deltaKey);

            TrackableObject obj = findObjectByTypeAndId(tracker, objectType, actualObjectId);
            if (obj != null) {
                try {
                    applyPropertyMap(obj, deltaProps, tracker);
                    appliedCount++;
                } catch (Exception e) {
                    netLog.error("[DeltaSync] Error applying delta to object ID={} type={} (deltaKey={})",
                            actualObjectId, objectType, String.format("0x%08X", deltaKey));
                    netLog.error("[DeltaSync] Exception details:", e);
                    skippedCount++;
                }
            } else {
                String typeName = getObjectTypeName(objectType);
                netLog.warn("[DeltaSync] {} ID={} (deltaKey={}) NOT FOUND for delta application",
                        typeName, actualObjectId, String.format("0x%08X", deltaKey));
                skippedCount++;
            }
        }

        // STEP 3: Handle removed objects
        if (!packet.getRemovedObjectIds().isEmpty()) {
            netLog.trace("[DeltaSync] Packet contains {} removed objects", packet.getRemovedObjectIds().size());
        }

        // STEP 4: Refresh UI for any changed zones
        if (!pendingZoneUpdates.isEmpty()) {
            List<PlayerZoneUpdate> zoneUpdates = new ArrayList<>();
            for (Map.Entry<PlayerView, Set<ZoneType>> entry : pendingZoneUpdates.entrySet()) {
                PlayerView player = entry.getKey();
                Set<ZoneType> zones = entry.getValue();
                if (!zones.isEmpty()) {
                    PlayerZoneUpdate update = new PlayerZoneUpdate(player, null);
                    for (ZoneType zone : zones) {
                        update.addZone(zone);
                    }
                    zoneUpdates.add(update);
                    netLog.trace("[DeltaSync] UI refresh: player={}, zones={}, hash={}",
                            player.getId(), zones, System.identityHashCode(player));
                }
            }
            if (!zoneUpdates.isEmpty()) {
                updateZones(zoneUpdates);
            }
            pendingZoneUpdates.clear();
        }

        // Log summary with timing
        long elapsed = System.currentTimeMillis() - startTime;
        if (newObjectCount > 0 || appliedCount > 0 || skippedCount > 0) {
            netLog.info("[DeltaSync] === END seq={} ({}ms, {} new, {} deltas, {} skipped) ===",
                    packet.getSequenceNumber(), elapsed, newObjectCount, appliedCount, skippedCount);
        } else {
            netLog.info("[DeltaSync] === END seq={} ({}ms, no changes) ===",
                    packet.getSequenceNumber(), elapsed);
        }

        // Validate checksum if present (every 20 packets)
        if (packet.hasChecksum()) {
            int serverChecksum = packet.getChecksum();
            int clientChecksum = computeStateChecksum(getGameView());

            if (serverChecksum != clientChecksum) {
                netLog.error("[DeltaSync] CHECKSUM MISMATCH! Server={}, Client={} at seq={}",
                        serverChecksum, clientChecksum, packet.getSequenceNumber());
                logChecksumDetails(getGameView(), packet);
                requestFullStateResync();
                return; // Don't send ack for corrupted state
            } else {
                netLog.info("[DeltaSync] Checksum OK (seq={}, checksum={})",
                        packet.getSequenceNumber(), serverChecksum);
            }
        }

        // Send acknowledgment
        IGameController controller = getGameController();
        if (controller != null) {
            controller.ackSync(packet.getSequenceNumber());
        }
    }

    /**
     * Apply a property map to a TrackableObject.
     * Resolves network values (IDs) back to object references.
     */
    private void applyPropertyMap(TrackableObject obj, Map<TrackableProperty, Object> delta, Tracker tracker) {
        for (Map.Entry<TrackableProperty, Object> entry : delta.entrySet()) {
            TrackableProperty prop = entry.getKey();
            Object value = entry.getValue();

            try {
                // Handle CardStateData specially — apply to existing CardStateView
                if (value instanceof CardStateData csvData && obj instanceof CardView cardView) {
                    applyCardStateData(cardView, prop, csvData, tracker);
                    continue;
                }

                Object resolved = resolveFromNetwork(prop, value, tracker);

                // Track zone changes for UI refresh
                if (obj instanceof PlayerView playerView) {
                    ZoneType changedZone = getZoneTypeForProperty(prop);
                    if (changedZone != null) {
                        trackZoneChange(playerView, changedZone);
                    }
                }

                obj.set(prop, resolved);
            } catch (Exception e) {
                netLog.error("[DeltaSync] Error setting property {} on object {}: {}",
                        prop, obj.getId(), e.getMessage());
            }
        }
    }

    /**
     * Resolve a network value back to its local form.
     * Integer IDs become object references via tracker lookup.
     */
    @SuppressWarnings("unchecked")
    static Object resolveFromNetwork(TrackableProperty prop, Object value, Tracker tracker) {
        if (value == null) return null;
        TrackableType<?> type = prop.getType();

        // Integer ID → CardView
        if (type == TrackableTypes.CardViewType) {
            return tracker.getObj(TrackableTypes.CardViewType, (Integer) value);
        }
        // Integer ID → PlayerView
        if (type == TrackableTypes.PlayerViewType) {
            return tracker.getObj(TrackableTypes.PlayerViewType, (Integer) value);
        }

        // int[]{typeMarker, id} → GameEntityView
        if (type == TrackableTypes.GameEntityViewType) {
            int[] arr = (int[]) value;
            return arr[0] == 0
                ? tracker.getObj(TrackableTypes.CardViewType, arr[1])
                : tracker.getObj(TrackableTypes.PlayerViewType, arr[1]);
        }

        // List<Integer> → TrackableCollection<CardView>
        if (type == TrackableTypes.CardViewCollectionType) {
            List<Integer> ids = (List<Integer>) value;
            TrackableCollection<CardView> coll = new TrackableCollection<>();
            for (int id : ids) {
                if (id != -1) {
                    CardView cv = tracker.getObj(TrackableTypes.CardViewType, id);
                    if (cv != null) {
                        coll.add(cv);
                    } else {
                        netLog.warn("[DeltaSync] CardView ID={} not found in tracker during collection resolve", id);
                    }
                }
            }
            return coll;
        }

        // List<Integer> → TrackableCollection<PlayerView>
        if (type == TrackableTypes.PlayerViewCollectionType) {
            List<Integer> ids = (List<Integer>) value;
            TrackableCollection<PlayerView> coll = new TrackableCollection<>();
            for (int id : ids) {
                if (id != -1) {
                    PlayerView pv = tracker.getObj(TrackableTypes.PlayerViewType, id);
                    if (pv != null) {
                        coll.add(pv);
                    } else {
                        netLog.warn("[DeltaSync] PlayerView ID={} not found in tracker during collection resolve", id);
                    }
                }
            }
            return coll;
        }

        // CardStateData is handled by the caller before reaching here

        // Everything else passes through unchanged
        return value;
    }

    /**
     * Apply CardStateData to the appropriate CardStateView on a CardView.
     */
    private void applyCardStateData(CardView cardView, TrackableProperty prop,
                                     CardStateData csvData, Tracker tracker) {
        CardStateView csv = null;

        if (prop == TrackableProperty.CurrentState) {
            csv = cardView.getCurrentState();
            if (csv == null) {
                netLog.warn("[DeltaSync] CurrentState is null for CardView {}, creating with state={}",
                        cardView.getId(), csvData.state);
                csv = createCardStateView(cardView, csvData.state);
                if (csv != null) {
                    cardView.set(TrackableProperty.CurrentState, csv);
                }
            }
        } else if (prop == TrackableProperty.AlternateState) {
            csv = cardView.getAlternateState();
            if (csv == null) {
                netLog.debug("[DeltaSync] Creating AlternateState for CardView {} with state={}",
                        cardView.getId(), csvData.state);
                csv = createCardStateView(cardView, csvData.state);
                if (csv != null) {
                    cardView.set(TrackableProperty.AlternateState, csv);
                }
            }
        } else if (prop == TrackableProperty.LeftSplitState) {
            csv = cardView.getLeftSplitState();
            if (csv == null) {
                csv = createCardStateView(cardView, csvData.state);
                if (csv != null) {
                    cardView.set(TrackableProperty.LeftSplitState, csv);
                }
            }
        } else if (prop == TrackableProperty.RightSplitState) {
            csv = cardView.getRightSplitState();
            if (csv == null) {
                csv = createCardStateView(cardView, csvData.state);
                if (csv != null) {
                    cardView.set(TrackableProperty.RightSplitState, csv);
                }
            }
        }

        if (csv != null) {
            // Resolve each property in the CardStateData and apply to the CardStateView
            int appliedCount = 0;
            for (Map.Entry<TrackableProperty, Object> entry : csvData.properties.entrySet()) {
                TrackableProperty csvProp = entry.getKey();
                Object csvValue = entry.getValue();

                if (csvValue instanceof CardStateData) {
                    netLog.error("[DeltaSync] Nested CardStateData not supported for property {}", csvProp);
                    continue;
                }

                Object resolved = resolveFromNetwork(csvProp, csvValue, tracker);
                csv.set(csvProp, resolved);
                appliedCount++;
            }
            netLog.trace("[DeltaSync] Applied {}/{} properties to CardStateView (state={}) of CardView {}",
                    appliedCount, csvData.properties.size(), csvData.state, cardView.getId());
        } else {
            netLog.error("[DeltaSync] Failed to get/create CardStateView for property {} on CardView {}",
                    prop, cardView.getId());
        }
    }

    // ==================== Checksum validation ====================

    private int computeStateChecksum(GameView gameView) {
        int phaseOrdinal = gameView.getPhase() != null ? gameView.getPhase().ordinal() : -1;
        return NetworkChecksumUtil.computeStateChecksum(gameView.getTurn(), phaseOrdinal, gameView.getPlayers());
    }

    private void logChecksumDetails(GameView gameView, DeltaPacket packet) {
        netLog.error("[DeltaSync] Checksum details (client state):");
        netLog.error("[DeltaSync]   GameView ID: {}", gameView.getId());
        netLog.error("[DeltaSync]   Turn: {}", gameView.getTurn());
        netLog.error("[DeltaSync]   Phase: {}", gameView.getPhase() != null ? gameView.getPhase().name() : "null");
        for (PlayerView player : NetworkChecksumUtil.getSortedPlayers(gameView)) {
            int handSize = player.getHand() != null ? player.getHand().size() : 0;
            int graveyardSize = player.getGraveyard() != null ? player.getGraveyard().size() : 0;
            int battlefieldSize = player.getBattlefield() != null ? player.getBattlefield().size() : 0;
            netLog.error("[DeltaSync]   Player {} ({}): Life={}, Hand={}, GY={}, BF={}",
                    player.getId(), player.getName(), player.getLife(),
                    handSize, graveyardSize, battlefieldSize);
        }
        netLog.error("[DeltaSync] Compare with server state in host log at seq={}", packet.getSequenceNumber());
    }

    private void requestFullStateResync() {
        netLog.warn("[DeltaSync] Requesting full state resync from server");
        IGameController controller = getGameController();
        if (controller != null) {
            controller.requestResync();
        } else {
            netLog.error("[DeltaSync] Cannot request resync: No game controller available");
        }
    }

    // ==================== Object creation ====================

    /**
     * Create a new TrackableObject and register it in the Tracker, but do NOT apply properties yet.
     * Phase 1a of the two-phase object creation process (Bug #12 fix).
     */
    private TrackableObject createObjectOnly(NewObjectData data, Tracker tracker) {
        int objectId = data.getObjectId();
        int objectType = data.getObjectType();
        String typeName = getObjectTypeName(objectType);

        // Check if object of the SAME TYPE already exists
        TrackableObject existing = findObjectByTypeAndId(tracker, objectType, objectId);
        if (existing != null) {
            netLog.trace("[DeltaSync] {} ID={} already exists, will apply properties in phase 1b",
                    typeName, objectId);
            return existing;
        }

        if (objectType == DeltaPacket.TYPE_PLAYER_VIEW) {
            netLog.warn("[DeltaSync] Creating NEW PlayerView ID={} - this may cause identity mismatch!", objectId);
        }

        TrackableObject obj = null;
        switch (objectType) {
            case DeltaPacket.TYPE_CARD_VIEW:
                obj = new CardView(objectId, tracker);
                tracker.putObj(TrackableTypes.CardViewType, objectId, (CardView) obj);
                netLog.trace("[DeltaSync] Created CardView ID={}, registered in tracker", objectId);
                break;
            case DeltaPacket.TYPE_PLAYER_VIEW:
                obj = new PlayerView(objectId, tracker);
                tracker.putObj(TrackableTypes.PlayerViewType, objectId, (PlayerView) obj);
                netLog.debug("[DeltaSync] Created PlayerView ID={} hash={}", objectId, System.identityHashCode(obj));
                break;
            case DeltaPacket.TYPE_STACK_ITEM_VIEW:
                obj = new forge.game.spellability.StackItemView(objectId, tracker);
                tracker.putObj(TrackableTypes.StackItemViewType, objectId, (forge.game.spellability.StackItemView) obj);
                break;
            case DeltaPacket.TYPE_COMBAT_VIEW:
                obj = new forge.game.combat.CombatView(tracker);
                tracker.putObj(TrackableTypes.CombatViewType, obj.getId(), (forge.game.combat.CombatView) obj);
                break;
            case DeltaPacket.TYPE_GAME_VIEW:
                if (getGameView() != null) {
                    return getGameView();
                }
                break;
            default:
                netLog.error("[DeltaSync] Unknown object type: {}", objectType);
                return null;
        }

        return obj;
    }

    private TrackableObject findObjectByTypeAndId(Tracker tracker, int objectType, int objectId) {
        switch (objectType) {
            case DeltaPacket.TYPE_CARD_VIEW:
                return tracker.getObj(TrackableTypes.CardViewType, objectId);
            case DeltaPacket.TYPE_PLAYER_VIEW:
                return tracker.getObj(TrackableTypes.PlayerViewType, objectId);
            case DeltaPacket.TYPE_STACK_ITEM_VIEW:
                return tracker.getObj(TrackableTypes.StackItemViewType, objectId);
            case DeltaPacket.TYPE_COMBAT_VIEW:
                return tracker.getObj(TrackableTypes.CombatViewType, objectId);
            case DeltaPacket.TYPE_GAME_VIEW:
                return getGameView();
            default:
                netLog.warn("[DeltaSync] Unknown object type {} for object {}", objectType, objectId);
                return null;
        }
    }

    private static String getObjectTypeName(int objectType) {
        switch (objectType) {
            case DeltaPacket.TYPE_CARD_VIEW: return "CardView";
            case DeltaPacket.TYPE_PLAYER_VIEW: return "PlayerView";
            case DeltaPacket.TYPE_STACK_ITEM_VIEW: return "StackItemView";
            case DeltaPacket.TYPE_COMBAT_VIEW: return "CombatView";
            case DeltaPacket.TYPE_GAME_VIEW: return "GameView";
            default: return "Unknown(type=" + objectType + ")";
        }
    }

    // ==================== Zone tracking ====================

    private static ZoneType getZoneTypeForProperty(TrackableProperty prop) {
        switch (prop) {
            case Hand: return ZoneType.Hand;
            case Library: return ZoneType.Library;
            case Graveyard: return ZoneType.Graveyard;
            case Battlefield: return ZoneType.Battlefield;
            case Exile: return ZoneType.Exile;
            case Command: return ZoneType.Command;
            case Flashback: return ZoneType.Flashback;
            case Ante: return ZoneType.Ante;
            case Sideboard: return ZoneType.Sideboard;
            default: return null;
        }
    }

    private void trackZoneChange(PlayerView player, ZoneType zone) {
        pendingZoneUpdates.computeIfAbsent(player, k -> EnumSet.noneOf(ZoneType.class)).add(zone);
    }

    // ==================== CardStateView creation ====================

    private CardStateView createCardStateView(CardView cardView, CardStateName state) {
        try {
            java.lang.reflect.Method createMethod = CardView.class.getDeclaredMethod(
                    "createAlternateState", CardStateName.class);
            createMethod.setAccessible(true);
            return (CardStateView) createMethod.invoke(cardView, state);
        } catch (NoSuchMethodException e) {
            try {
                Class<?> csvClass = Class.forName("forge.game.card.CardView$CardStateView");
                java.lang.reflect.Constructor<?> constructor = csvClass.getDeclaredConstructor(
                        CardView.class, int.class, CardStateName.class, Tracker.class);
                constructor.setAccessible(true);
                Tracker tracker = cardView.getTracker();
                return (CardStateView) constructor.newInstance(cardView, cardView.getId(), state, tracker);
            } catch (Exception e2) {
                netLog.error("[DeltaSync] Failed to create CardStateView via constructor: {}", e2.getMessage());
                return null;
            }
        } catch (Exception e) {
            netLog.error("[DeltaSync] Failed to create CardStateView: {}", e.getMessage());
            return null;
        }
    }

    // ==================== Full state sync ====================

    @Override
    public void fullStateSync(FullStatePacket packet) {
        if (packet != null && packet.getGameView() != null) {
            GameView newGameView = packet.getGameView();

            if (getGameView() != null && getGameView().getTracker() != null) {
                netLog.info("gameView already exists - using copyChangedProps to preserve object identity");

                if (newGameView.getTracker() == null) {
                    Tracker existingTracker = getGameView().getTracker();
                    java.util.Set<TrackableObject> visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
                    setTrackerRecursively(newGameView, existingTracker, visited);
                }

                getGameView().copyChangedProps(newGameView);

                netLog.info("Used copyChangedProps - existing PlayerView instances preserved");
            } else {
                netLog.info("No existing gameView - performing fresh initialization");

                ensureTrackerInitialized(newGameView);
                newGameView.updateObjLookup();

                Tracker tracker = newGameView.getTracker();
                if (tracker != null && newGameView.getPlayers() != null) {
                    int playerCount = 0;
                    int cardCount = 0;
                    for (PlayerView player : newGameView.getPlayers()) {
                        playerCount++;
                        cardCount += countCards(player.getHand());
                        cardCount += countCards(player.getGraveyard());
                        cardCount += countCards(player.getLibrary());
                        cardCount += countCards(player.getExile());
                        cardCount += countCards(player.getBattlefield());
                    }
                    netLog.info("After updateObjLookup: {} players, ~{} cards found in zones",
                            playerCount, cardCount);
                }

                setGameView(newGameView);
            }

            IGameController controller = getGameController();
            if (controller != null) {
                controller.ackSync(packet.getSequenceNumber());
            }
        }
    }

    private int countCards(Iterable<CardView> cards) {
        if (cards == null) return 0;
        int count = 0;
        for (CardView c : cards) {
            if (c != null) count++;
        }
        return count;
    }

}
