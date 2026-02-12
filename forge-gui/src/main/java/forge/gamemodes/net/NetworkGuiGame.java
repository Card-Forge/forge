package forge.gamemodes.net;

import forge.game.GameView;
import forge.card.CardStateName;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.AbstractGuiGame;
import forge.gamemodes.net.DeltaPacket.NewObjectData;
import forge.gamemodes.net.NetworkPropertySerializer.CardStateViewData;
import forge.gamemodes.net.server.DeltaSyncManager;
import forge.interfaces.IGameController;
import forge.player.PlayerZoneUpdate;
import forge.trackable.Tracker;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;
import forge.trackable.TrackableTypes;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.*;
import java.util.AbstractMap;

/**
 * Extension of AbstractGuiGame with network delta synchronization support.
 * This class handles all network-specific deserialization and state management,
 * keeping the core AbstractGuiGame class free from network dependencies.
 *
 * All network-specific logic (delta packet application, tracker initialization,
 * reconnection handling) is contained in this subclass, allowing the base
 * AbstractGuiGame to remain focused on core local game functionality.
 */
public abstract class NetworkGuiGame extends AbstractGuiGame {

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
        NetworkDebugLogger.log("%s [setGameView] Called with gameView0=%s, existing gameView=%s",
                getLogPrefix(),
                gameView0 != null ? "non-null" : "null",
                getGameView() != null ? "non-null" : "null");

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
                        NetworkDebugLogger.trace("[setGameView] Initial setup: Player %d hash=%d, inTracker=%b, sameInstance=%b",
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
                java.util.Set<Integer> visited = new java.util.HashSet<>();
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
        java.util.Set<Integer> visited = new java.util.HashSet<>();
        setTrackerRecursively(gameView0, tracker, visited);
        NetworkDebugLogger.log("[EnsureTracker] Set tracker on %d unique objects", visited.size());

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
                NetworkDebugLogger.trace("[EnsureTracker] Player %d: hasTracker=%b, cards with tracker=%d, cards without=%d",
                    player.getId(), playerHasTracker, cardsWithTracker, cardsWithoutTracker);
            }
        }
    }

    /**
     * Recursively set the tracker on a TrackableObject and all objects it references.
     * Uses a visited set to avoid infinite loops from circular references.
     */
    @SuppressWarnings("unchecked")
    private void setTrackerRecursively(TrackableObject obj, Tracker tracker, java.util.Set<Integer> visited) {
        if (obj == null) return;

        // Avoid infinite loops
        int objId = System.identityHashCode(obj);
        if (visited.contains(objId)) return;
        visited.add(objId);

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
            if (value instanceof TrackableObject) {
                setTrackerRecursively((TrackableObject) value, tracker, visited);
            }
            // Handle collections of TrackableObjects
            else if (value instanceof Iterable) {
                for (Object item : (Iterable<?>) value) {
                    if (item instanceof TrackableObject) {
                        setTrackerRecursively((TrackableObject) item, tracker, visited);
                    }
                }
            }
            // Handle maps that might contain TrackableObjects
            else if (value instanceof Map) {
                for (Object mapValue : ((Map<?, ?>) value).values()) {
                    if (mapValue instanceof TrackableObject) {
                        setTrackerRecursively((TrackableObject) mapValue, tracker, visited);
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
            NetworkDebugLogger.error("[DeltaSync] Cannot apply delta: Tracker is null");
            return;
        }

        // Log with game context for easier correlation
        String activePlayerName = getGameView().getPlayerTurn() != null ? getGameView().getPlayerTurn().getName() : "?";
        String phaseName = getGameView().getPhase() != null ? getGameView().getPhase().name() : "?";
        NetworkDebugLogger.log("[DeltaSync] === START applyDelta seq=%d (Turn %d, %s, Active=%s) ===",
                packet.getSequenceNumber(), getGameView().getTurn(), phaseName, activePlayerName);
        if (getGameView().getPlayers() != null) {
            for (PlayerView pv : getGameView().getPlayers()) {
                PlayerView inTracker = tracker.getObj(TrackableTypes.PlayerViewType, pv.getId());
                boolean sameInstance = (pv == inTracker);
                NetworkDebugLogger.trace("[DeltaSync] GameView PlayerView ID=%d hash=%d, inTracker=%s trackerHash=%d, sameInstance=%b",
                        pv.getId(), System.identityHashCode(pv),
                        inTracker != null ? "FOUND" : "NULL",
                        inTracker != null ? System.identityHashCode(inTracker) : 0,
                        sameInstance);
            }
        }

        int newObjectCount = 0;
        int appliedCount = 0;
        int skippedCount = 0;

        // Clear pending zone updates before processing
        pendingZoneUpdates.clear();

        // STEP 1: Create new objects first (so deltas can reference them)
        // BUG FIX for Bug #12: Split into two phases to handle object cross-references
        // Phase 1a: Create all objects and register in tracker (without properties)
        // Phase 1b: Apply properties to all objects (now all objects exist for cross-references)
        Map<Integer, NewObjectData> newObjects = packet.getNewObjects();
        List<Map.Entry<TrackableObject, byte[]>> pendingPropertyApplications = new ArrayList<>();

        if (!newObjects.isEmpty()) {
            // Phase 1a: Create all objects first (without applying properties)
            for (NewObjectData newObj : newObjects.values()) {
                try {
                    TrackableObject created = createObjectOnly(newObj, tracker);
                    if (created != null) {
                        pendingPropertyApplications.add(new AbstractMap.SimpleEntry<>(created, newObj.getFullProperties()));
                        newObjectCount++;
                    }
                } catch (Exception e) {
                    NetworkDebugLogger.error("[DeltaSync] Error creating new object " + newObj.getObjectId(), e);
                }
            }
            NetworkDebugLogger.log("[DeltaSync] Created %d new objects (phase 1a)", newObjectCount);

            // Verify CardViews are in tracker before property application
            int verifyCount = 0;
            for (NewObjectData newObj : newObjects.values()) {
                if (newObj.getObjectType() == DeltaPacket.TYPE_CARD_VIEW) {
                    CardView cv = tracker.getObj(TrackableTypes.CardViewType, newObj.getObjectId());
                    if (cv != null) {
                        verifyCount++;
                    } else {
                        NetworkDebugLogger.warn("[DeltaSync] VERIFY FAILED: CardView %d not in tracker after creation!", newObj.getObjectId());
                    }
                }
            }
            NetworkDebugLogger.trace("[DeltaSync] Verified %d CardViews in tracker", verifyCount);

            // Phase 1b: Now apply properties to all objects (all objects exist for cross-references)
            int propsApplied = 0;
            for (Map.Entry<TrackableObject, byte[]> pending : pendingPropertyApplications) {
                try {
                    applyDeltaToObject(pending.getKey(), pending.getValue(), tracker);
                    propsApplied++;
                } catch (Exception e) {
                    NetworkDebugLogger.error("[DeltaSync] Error applying properties to object %d: %s",
                            pending.getKey().getId(), e.getMessage());
                }
            }
            NetworkDebugLogger.trace("[DeltaSync] Applied properties to %d objects (phase 1b)", propsApplied);
        }

        // STEP 2: Apply property deltas to existing objects
        for (Map.Entry<Integer, byte[]> entry : packet.getObjectDeltas().entrySet()) {
            int deltaKey = entry.getKey();
            byte[] deltaBytes = entry.getValue();

            // Decode composite delta key to get object type and ID
            // Key format: upper 4 bits = type, lower 28 bits = ID
            int objectType = DeltaSyncManager.getTypeFromDeltaKey(deltaKey);
            int actualObjectId = DeltaSyncManager.getIdFromDeltaKey(deltaKey);

            // Look up object by type and ID
            TrackableObject obj = findObjectByTypeAndId(tracker, objectType, actualObjectId);
            if (obj != null) {
                try {
                    // Log if this is the GameView or PlayerView
                    if (obj == getGameView()) {
                        NetworkDebugLogger.trace("[DeltaSync] Applying delta to GameView ID=%d (type=%d, deltaKey=0x%08X), bytes=%d",
                                actualObjectId, objectType, deltaKey, deltaBytes.length);
                    } else if (obj instanceof PlayerView) {
                        NetworkDebugLogger.trace("[DeltaSync] Applying delta to PlayerView ID=%d (type=%d, deltaKey=0x%08X), bytes=%d",
                                actualObjectId, objectType, deltaKey, deltaBytes.length);
                    }
                    applyDeltaToObject(obj, deltaBytes, tracker);
                    appliedCount++;
                } catch (Exception e) {
                    NetworkDebugLogger.error("[DeltaSync] Error applying delta to object ID=%d type=%d (deltaKey=0x%08X)",
                            actualObjectId, objectType, deltaKey);
                    NetworkDebugLogger.error("[DeltaSync] Exception details:", e);
                    skippedCount++;
                }
            } else {
                // Object not found - this shouldn't happen if new objects were created first
                String typeName = getObjectTypeName(objectType);
                NetworkDebugLogger.warn("[DeltaSync] %s ID=%d (deltaKey=0x%08X) NOT FOUND for delta application",
                        typeName, actualObjectId, deltaKey);
                skippedCount++;
            }
        }

        // STEP 3: Handle removed objects
        if (!packet.getRemovedObjectIds().isEmpty()) {
            NetworkDebugLogger.trace("[DeltaSync] Packet contains %d removed objects", packet.getRemovedObjectIds().size());
            // Note: Objects are not removed from Tracker - they'll just not be updated anymore
            // and will be garbage collected when no longer referenced by the game state
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
                    NetworkDebugLogger.trace("[DeltaSync] UI refresh: player=%d, zones=%s, hash=%d",
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
            NetworkDebugLogger.log("[DeltaSync] === END seq=%d (%dms, %d new, %d deltas, %d skipped) ===",
                    packet.getSequenceNumber(), elapsed, newObjectCount, appliedCount, skippedCount);
        } else {
            NetworkDebugLogger.log("[DeltaSync] === END seq=%d (%dms, no changes) ===",
                    packet.getSequenceNumber(), elapsed);
        }

        // Validate checksum if present (every 20 packets)
        if (packet.hasChecksum()) {
            int serverChecksum = packet.getChecksum();
            int clientChecksum = computeStateChecksum(getGameView());

            if (serverChecksum != clientChecksum) {
                NetworkDebugLogger.error("[DeltaSync] CHECKSUM MISMATCH! Server=%d, Client=%d at seq=%d",
                        serverChecksum, clientChecksum, packet.getSequenceNumber());
                // Log detailed comparison to identify what differs
                logChecksumDetails(getGameView(), packet);

                // Automatically request full state resync to recover
                requestFullStateResync();
                return; // Don't send ack for corrupted state
            } else {
                NetworkDebugLogger.log("[DeltaSync] Checksum OK (seq=%d, checksum=%d)",
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
     * Compute a checksum of the current game state for validation.
     * Uses the same algorithm as the server for consistency.
     * IMPORTANT: Only use value-based fields here, not identity-based hashCode().
     * Server and client are separate JVMs, so Object.hashCode() will differ.
     *
     * BUG FIX (Bug #13): Sort players by ID before computing checksum.
     * The player collection order may differ between server and client,
     * causing checksum mismatches even when all values are identical.
     */
    private int computeStateChecksum(GameView gameView) {
        int hash = 17;
        // NOTE: Do NOT include gameView.getId() in the checksum.
        // The GameView ID is a local JVM identifier that differs between server and client
        // because each side creates its own Game/GameView instances. Only include actual
        // game state properties that should be synchronized.
        hash = 31 * hash + gameView.getTurn();
        if (gameView.getPhase() != null) {
            // Use ordinal() not hashCode() - ordinal is consistent across JVMs
            hash = 31 * hash + gameView.getPhase().ordinal();
        }
        if (gameView.getPlayers() != null) {
            // Sort players by ID for consistent iteration order across server and client
            List<PlayerView> sortedPlayers = new ArrayList<>();
            for (PlayerView p : gameView.getPlayers()) {
                sortedPlayers.add(p);
            }
            sortedPlayers.sort(Comparator.comparingInt(PlayerView::getId));
            for (PlayerView player : sortedPlayers) {
                hash = 31 * hash + player.getId();
                hash = 31 * hash + player.getLife();
            }
        }
        return hash;
    }

    /**
     * Log detailed checksum comparison to help identify what differs.
     * Called when checksum mismatch is detected.
     * Players are sorted by ID to match the checksum computation order.
     */
    private void logChecksumDetails(GameView gameView, DeltaPacket packet) {
        NetworkDebugLogger.error("[DeltaSync] Checksum details (client state):");
        NetworkDebugLogger.error("[DeltaSync]   GameView ID: %d", gameView.getId());
        NetworkDebugLogger.error("[DeltaSync]   Turn: %d", gameView.getTurn());
        NetworkDebugLogger.error("[DeltaSync]   Phase: %s", gameView.getPhase() != null ? gameView.getPhase().name() : "null");
        if (gameView.getPlayers() != null) {
            // Sort players by ID to match checksum computation order
            List<PlayerView> sortedPlayers = new ArrayList<>();
            for (PlayerView p : gameView.getPlayers()) {
                sortedPlayers.add(p);
            }
            sortedPlayers.sort(Comparator.comparingInt(PlayerView::getId));
            for (PlayerView player : sortedPlayers) {
                int handSize = player.getHand() != null ? player.getHand().size() : 0;
                int graveyardSize = player.getGraveyard() != null ? player.getGraveyard().size() : 0;
                int battlefieldSize = player.getBattlefield() != null ? player.getBattlefield().size() : 0;
                NetworkDebugLogger.error("[DeltaSync]   Player %d (%s): Life=%d, Hand=%d, GY=%d, BF=%d",
                        player.getId(), player.getName(), player.getLife(),
                        handSize, graveyardSize, battlefieldSize);
            }
        }
        NetworkDebugLogger.error("[DeltaSync] Compare with server state in host log at seq=%d", packet.getSequenceNumber());
    }

    /**
     * Request a full state resync from the server to recover from desynchronization.
     * This is called automatically when checksum validation fails.
     */
    private void requestFullStateResync() {
        NetworkDebugLogger.warn("[DeltaSync] Requesting full state resync from server");

        IGameController controller = getGameController();
        if (controller != null) {
            controller.requestResync();
        } else {
            NetworkDebugLogger.error("[DeltaSync] Cannot request resync: No game controller available");
        }
    }

    /**
     * Create a new TrackableObject and register it in the Tracker, but do NOT apply properties yet.
     * This is Phase 1a of the two-phase object creation process (Bug #12 fix).
     * Properties are applied in Phase 1b after ALL objects have been created, so cross-references work.
     *
     * @param data the new object data from the delta packet
     * @param tracker the tracker to register the object in
     * @return the created object, or null if it already existed or creation failed
     */
    private TrackableObject createObjectOnly(NewObjectData data, Tracker tracker) {
        int objectId = data.getObjectId();
        int objectType = data.getObjectType();

        // Log what type of object we're trying to create
        String typeName = getObjectTypeName(objectType);

        // Check if object of the SAME TYPE already exists
        TrackableObject existing = findObjectByTypeAndId(tracker, objectType, objectId);
        if (existing != null) {
            NetworkDebugLogger.trace("[DeltaSync] %s ID=%d already exists (hash=%d), will apply properties in phase 1b",
                    typeName, objectId, System.identityHashCode(existing));
            return existing; // Return existing so properties get applied in phase 1b
        }

        // Log when creating new object (especially important for PlayerView)
        if (objectType == DeltaPacket.TYPE_PLAYER_VIEW) {
            NetworkDebugLogger.warn("[DeltaSync] Creating NEW PlayerView ID=%d - this may cause identity mismatch!", objectId);
        }

        // Create the appropriate object type and register in tracker
        TrackableObject obj = null;
        switch (objectType) {
            case DeltaPacket.TYPE_CARD_VIEW:
                obj = new CardView(objectId, tracker);
                tracker.putObj(TrackableTypes.CardViewType, objectId, (CardView) obj);
                NetworkDebugLogger.trace("[DeltaSync] Created CardView ID=%d, registered in tracker", objectId);
                break;
            case DeltaPacket.TYPE_PLAYER_VIEW:
                obj = new PlayerView(objectId, tracker);
                tracker.putObj(TrackableTypes.PlayerViewType, objectId, (PlayerView) obj);
                NetworkDebugLogger.debug("[DeltaSync] Created PlayerView ID=%d hash=%d, registered in tracker", objectId, System.identityHashCode(obj));
                break;
            case DeltaPacket.TYPE_STACK_ITEM_VIEW:
                obj = new forge.game.spellability.StackItemView(objectId, tracker);
                tracker.putObj(TrackableTypes.StackItemViewType, objectId, (forge.game.spellability.StackItemView) obj);
                break;
            case DeltaPacket.TYPE_COMBAT_VIEW:
                // CombatView uses ID -1 (singleton pattern)
                obj = new forge.game.combat.CombatView(tracker);
                tracker.putObj(TrackableTypes.CombatViewType, obj.getId(), (forge.game.combat.CombatView) obj);
                break;
            case DeltaPacket.TYPE_GAME_VIEW:
                // GameView is special - return the existing one for property application
                if (getGameView() != null) {
                    return getGameView();
                }
                break;
            default:
                NetworkDebugLogger.error("[DeltaSync] Unknown object type: %d", objectType);
                return null;
        }

        return obj;
    }

    /**
     * Find a TrackableObject by type and ID using the composite delta key.
     * This prevents ID collisions between different object types.
     *
     * @param tracker the tracker to search in
     * @param objectType the object type from DeltaPacket.TYPE_* constants
     * @param objectId the object's ID
     * @return the TrackableObject, or null if not found
     */
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
                return getGameView(); // GameView is special - return the singleton
            default:
                NetworkDebugLogger.warn("[DeltaSync] Unknown object type %d for object %d", objectType, objectId);
                return null;
        }
    }

    /**
     * Get a human-readable name for an object type.
     */
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

    /**
     * Apply deserialized delta bytes to a TrackableObject.
     * Reads the compact binary format and updates properties.
     */
    @SuppressWarnings("unchecked")
    private void applyDeltaToObject(TrackableObject obj, byte[] deltaBytes, Tracker tracker) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(deltaBytes);
        DataInputStream dis = new DataInputStream(bais);
        NetworkTrackableDeserializer ntd = new NetworkTrackableDeserializer(dis, tracker);

        int propCount = dis.readInt();
        ntd.resetBytesRead(); // Reset counter after the initial propCount read
        Map<TrackableProperty, Object> props = obj.getProps();

        NetworkDebugLogger.trace("[DeltaSync] applyDeltaToObject: objId=%d, objType=%s, deltaBytes=%d, propCount=%d",
                obj.getId(), obj.getClass().getSimpleName(), deltaBytes.length, propCount);

        for (int i = 0; i < propCount; i++) {
            int bytePosBefore = ntd.getBytesRead() + 4; // +4 for the initial propCount
            int ordinal = dis.readInt();

            // Validate ordinal before deserialization
            if (ordinal < 0 || ordinal >= TrackableProperty.values().length) {
                NetworkDebugLogger.error("[DeltaSync] ERROR: Invalid ordinal %d (0x%08X) at byte %d for prop %d/%d in object %d (%s)",
                        ordinal, ordinal, bytePosBefore, i + 1, propCount, obj.getId(), obj.getClass().getSimpleName());
                NetworkDebugLogger.error("[DeltaSync] Valid ordinal range: 0-%d",
                        TrackableProperty.values().length - 1);
                NetworkDebugLogger.hexDump("[DeltaSync] Hex dump of delta bytes:", deltaBytes, bytePosBefore);
                throw new RuntimeException("Invalid TrackableProperty ordinal: " + ordinal);
            }

            TrackableProperty prop = TrackableProperty.deserialize(ordinal);
            Object oldValue = props != null ? props.get(prop) : null;

            try {
                Object value = NetworkPropertySerializer.deserialize(ntd, prop, oldValue);
                // Log what's being set for PlayerView
                if (obj instanceof PlayerView) {
                    String valueDesc = value == null ? "null" :
                        (value instanceof TrackableCollection ? "Collection[" + ((TrackableCollection<?>)value).size() + "]" : value.getClass().getSimpleName());
                    NetworkDebugLogger.trace("[DeltaSync] PlayerView %d: setting %s = %s", obj.getId(), prop, valueDesc);

                    // Track zone changes for UI refresh
                    ZoneType changedZone = getZoneTypeForProperty(prop);
                    if (changedZone != null) {
                        trackZoneChange((PlayerView) obj, changedZone);
                    }
                }
                // Use reflection to call the protected set method
                setPropertyValue(obj, prop, value);
            } catch (Exception e) {
                int bytePosAfter = ntd.getBytesRead() + 4; // +4 for the initial propCount
                NetworkDebugLogger.error("[DeltaSync] Error deserializing property %s (ordinal=%d) at bytes %d-%d: %s",
                        prop, ordinal, bytePosBefore, bytePosAfter, e.getMessage());
                NetworkDebugLogger.hexDump("[DeltaSync] Hex dump of delta bytes:", deltaBytes, bytePosBefore);
                NetworkDebugLogger.error("[DeltaSync] Exception details:", e);
            }
        }
    }

    /**
     * Map a TrackableProperty to its corresponding ZoneType, if any.
     * @param prop the property to check
     * @return the ZoneType if this is a zone property, null otherwise
     */
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

    /**
     * Track a zone change for UI refresh after delta application.
     * @param player the player whose zone changed
     * @param zone the zone that changed
     */
    private void trackZoneChange(PlayerView player, ZoneType zone) {
        pendingZoneUpdates.computeIfAbsent(player, k -> EnumSet.noneOf(ZoneType.class)).add(zone);
    }

    /**
     * Set a property value on a TrackableObject.
     * Uses reflection to access the protected set method.
     * Handles CardStateViewData specially by applying properties to existing CardStateView.
     */
    private void setPropertyValue(TrackableObject obj, TrackableProperty prop, Object value) {
        try {
            // Handle CardStateViewData specially - apply to existing CardStateView
            if (value instanceof CardStateViewData && obj instanceof CardView) {
                CardView cardView = (CardView) obj;
                CardStateViewData csvData = (CardStateViewData) value;
                CardStateView csv = null;

                // Get the appropriate CardStateView based on the property
                if (prop == TrackableProperty.CurrentState) {
                    csv = cardView.getCurrentState();
                    if (csv == null) {
                        // CurrentState should have been created by CardView constructor
                        // If it's null, something is wrong - try to create it
                        NetworkDebugLogger.warn("[DeltaSync] CurrentState is null for CardView %d, attempting to create with state=%s",
                                cardView.getId(), csvData.state);
                        csv = createCardStateView(cardView, csvData.state);
                        if (csv != null) {
                            // Set the newly created CardStateView as CurrentState
                            cardView.set(TrackableProperty.CurrentState, csv);
                        }
                    } else if (csv.getState() != csvData.state) {
                        // State mismatch - log warning but continue with property application
                        NetworkDebugLogger.debug("[DeltaSync] CurrentState state mismatch for CardView %d: existing=%s, data=%s (will apply properties anyway)",
                                cardView.getId(), csv.getState(), csvData.state);
                    }
                } else if (prop == TrackableProperty.AlternateState) {
                    csv = cardView.getAlternateState();
                    if (csv == null) {
                        // AlternateState doesn't exist - try to create it
                        NetworkDebugLogger.debug("[DeltaSync] Creating AlternateState for CardView %d with state=%s",
                                cardView.getId(), csvData.state);
                        csv = createCardStateView(cardView, csvData.state);
                        if (csv != null) {
                            // Set the newly created CardStateView as AlternateState
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
                    // Apply all deserialized properties to the CardStateView
                    int appliedCount = 0;
                    for (Map.Entry<TrackableProperty, Object> entry : csvData.properties.entrySet()) {
                        TrackableProperty csvProp = entry.getKey();
                        Object csvValue = entry.getValue();

                        // Recursively handle nested CardStateViewData (shouldn't happen, but be safe)
                        if (csvValue instanceof CardStateViewData) {
                            NetworkDebugLogger.error("[DeltaSync] Nested CardStateViewData not supported for property %s", csvProp);
                            continue;
                        }

                        csv.set(csvProp, csvValue);
                        appliedCount++;
                    }
                    NetworkDebugLogger.trace("[DeltaSync] Applied %d/%d properties to CardStateView (state=%s) of CardView %d",
                            appliedCount, csvData.properties.size(), csvData.state, cardView.getId());
                } else {
                    NetworkDebugLogger.error("[DeltaSync] Failed to get/create CardStateView for property %s on CardView %d",
                            prop, cardView.getId());
                }
                return;
            }

            // Normal property setting (direct call - much faster than reflection)
            obj.set(prop, value);
        } catch (Exception e) {
            NetworkDebugLogger.error("[DeltaSync] Error setting property %s on object %d: %s", prop, obj.getId(), e.getMessage());
            NetworkDebugLogger.error("[DeltaSync] Stack trace:", e);
        }
    }

    /**
     * Create a new CardStateView for a CardView using reflection to access the inner class constructor.
     */
    private CardStateView createCardStateView(CardView cardView, CardStateName state) {
        try {
            // Try to use the createAlternateState method if available
            java.lang.reflect.Method createMethod = CardView.class.getDeclaredMethod(
                    "createAlternateState", CardStateName.class);
            createMethod.setAccessible(true);
            return (CardStateView) createMethod.invoke(cardView, state);
        } catch (NoSuchMethodException e) {
            // Method doesn't exist, try direct constructor approach
            try {
                // CardStateView is an inner class, so we need the outer CardView instance
                Class<?> csvClass = Class.forName("forge.game.card.CardView$CardStateView");
                java.lang.reflect.Constructor<?> constructor = csvClass.getDeclaredConstructor(
                        CardView.class, int.class, CardStateName.class, Tracker.class);
                constructor.setAccessible(true);
                Tracker tracker = cardView.getTracker();
                return (CardStateView) constructor.newInstance(cardView, cardView.getId(), state, tracker);
            } catch (Exception e2) {
                NetworkDebugLogger.error("[DeltaSync] Failed to create CardStateView via constructor: %s", e2.getMessage());
                return null;
            }
        } catch (Exception e) {
            NetworkDebugLogger.error("[DeltaSync] Failed to create CardStateView: %s", e.getMessage());
            return null;
        }
    }

    @Override
    public void fullStateSync(FullStatePacket packet) {
        // Default implementation - apply the full state
        if (packet != null && packet.getGameView() != null) {
            GameView newGameView = packet.getGameView();

            // CRITICAL FIX: If gameView already exists with a tracker, we must NOT replace it
            // because the UI (sortedPlayers, CHand, etc.) holds references to the existing PlayerViews.
            // Replacing gameView would orphan those references and break the UI.
            if (getGameView() != null && getGameView().getTracker() != null) {
                NetworkDebugLogger.log("[FullStateSync] gameView already exists - using copyChangedProps to preserve object identity");

                // Initialize tracker on the incoming gameView so copyChangedProps can work
                if (newGameView.getTracker() == null) {
                    // Use the existing tracker for the new GameView's objects
                    Tracker existingTracker = getGameView().getTracker();
                    java.util.Set<Integer> visited = new java.util.HashSet<>();
                    setTrackerRecursively(newGameView, existingTracker, visited);
                }

                // Copy changed properties to existing objects (preserves object identity)
                getGameView().copyChangedProps(newGameView);

                NetworkDebugLogger.log("[FullStateSync] Used copyChangedProps - existing PlayerView instances preserved");
                if (getGameView().getPlayers() != null) {
                    for (PlayerView player : getGameView().getPlayers()) {
                        NetworkDebugLogger.trace("[FullStateSync] Preserved Player %d: hash=%d",
                                player.getId(), System.identityHashCode(player));
                    }
                }
            } else {
                // No existing gameView - initialize fresh (this is the first sync or reconnection)
                NetworkDebugLogger.log("[FullStateSync] No existing gameView - performing fresh initialization");

                ensureTrackerInitialized(newGameView);
                newGameView.updateObjLookup();

                // Debug: Log what's in the tracker after updateObjLookup
                Tracker tracker = newGameView.getTracker();
                if (tracker != null) {
                    int cardCount = 0;
                    int playerCount = 0;
                    // Count objects by iterating through known collections
                    if (newGameView.getPlayers() != null) {
                        for (PlayerView player : newGameView.getPlayers()) {
                            playerCount++;
                            // Count cards in player zones
                            cardCount += countCards(player.getHand());
                            cardCount += countCards(player.getGraveyard());
                            cardCount += countCards(player.getLibrary());
                            cardCount += countCards(player.getExile());
                            cardCount += countCards(player.getBattlefield());
                        }
                    }
                    NetworkDebugLogger.log("[FullStateSync] After updateObjLookup: %d players, ~%d cards found in zones",
                            playerCount, cardCount);

                    // Verify a few objects are actually in the tracker
                    if (newGameView.getPlayers() != null) {
                        for (PlayerView player : newGameView.getPlayers()) {
                            TrackableObject foundPlayer = tracker.getObj(TrackableTypes.PlayerViewType, player.getId());
                            boolean sameInstance = (player == foundPlayer);
                            NetworkDebugLogger.trace("[FullStateSync] Player %d: hash=%d, trackerLookup=%s, trackerHash=%d, sameInstance=%b",
                                    player.getId(),
                                    System.identityHashCode(player),
                                    foundPlayer != null ? "FOUND" : "NOT FOUND",
                                    foundPlayer != null ? System.identityHashCode(foundPlayer) : 0,
                                    sameInstance);

                            // Check a card from hand
                            if (player.getHand() != null) {
                                for (CardView card : player.getHand()) {
                                    TrackableObject foundCard = tracker.getObj(TrackableTypes.CardViewType, card.getId());
                                    NetworkDebugLogger.trace("[FullStateSync] Card %d (from hand): tracker lookup = %s",
                                            card.getId(), foundCard != null ? "FOUND" : "NOT FOUND");
                                    break; // Just check first card
                                }
                            }
                        }
                    }
                }

                // Set the new game view
                setGameView(newGameView);
            }

            // Send acknowledgment
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
