package forge.gamemodes.net;

import forge.game.GameView;
import forge.game.event.GameEvent;
import forge.card.CardStateName;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.AbstractGuiGame;
import forge.gamemodes.net.DeltaPacket.CardStateData;
import forge.gamemodes.net.server.DeltaSyncManager;
import forge.interfaces.IGameController;
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

        // STEP 1: Create new objects first (so deltas can reference them)
        Map<Integer, Map<TrackableProperty, Object>> newObjects = packet.getNewObjects();
        List<Map.Entry<TrackableObject, Map<TrackableProperty, Object>>> pendingPropertyApplications = new ArrayList<>();

        if (!newObjects.isEmpty()) {
            // Phase 1a: Create all objects first (without applying properties)
            for (Map.Entry<Integer, Map<TrackableProperty, Object>> newEntry : newObjects.entrySet()) {
                int deltaKey = newEntry.getKey();
                int objectType = DeltaSyncManager.getTypeFromDeltaKey(deltaKey);
                int objectId = DeltaSyncManager.getIdFromDeltaKey(deltaKey);
                try {
                    TrackableObject created = createObjectOnly(objectType, objectId, tracker);
                    if (created != null) {
                        pendingPropertyApplications.add(new AbstractMap.SimpleEntry<>(created, newEntry.getValue()));
                        newObjectCount++;
                    }
                } catch (Exception e) {
                    netLog.error("[DeltaSync] Error creating new object type={} id={}", objectType, objectId, e);
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
                    netLog.error(e, "[DeltaSync] Exception details:");
                    skippedCount++;
                }
            } else {
                String typeName = getObjectTypeName(objectType);
                netLog.warn("[DeltaSync] {} ID={} (deltaKey={}) NOT FOUND for delta application",
                        typeName, actualObjectId, String.format("0x%08X", deltaKey));
                skippedCount++;
            }
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

        // Forward bundled events AFTER delta is applied — guarantees
        // getGameView() state is current when event handlers read it.
        if (packet.hasEvents()) {
            Tracker tracker2 = getGameView().getTracker();
            List<GameEvent> unwrapped = GameEventProxy.unwrapAll(packet.getProxiedEvents(), tracker2);
            for (GameEvent event : unwrapped) {
                handleGameEvent(event);
            }
        }

        if (packet.hasChecksum()) {
            int serverChecksum = packet.getChecksum();
            int clientChecksum = computeStateChecksum(getGameView());

            if (serverChecksum != clientChecksum) {
                netLog.error("[DeltaSync] CHECKSUM MISMATCH! Server={}, Client={} at seq={}",
                        serverChecksum, clientChecksum, packet.getSequenceNumber());
                logChecksumDetails(getGameView(), packet);
                requestFullStateResync();
                // Don't send ack for corrupted state
                return;
            } else {
                netLog.info("[DeltaSync] Checksum OK (seq={}, checksum={})",
                        packet.getSequenceNumber(), serverChecksum);
            }
        }

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

                if (prop == TrackableProperty.Zone && obj instanceof CardView) {
                    netLog.debug("[DeltaSync] CardView id={} Zone: {} -> {}",
                            obj.getId(), ((CardView) obj).getZone(), resolved);
                }

                // Sync per-card Zone when zone collections change, otherwise
                // canBeShownTo() fails and cards render as hidden/empty.
                if (obj instanceof PlayerView) {
                    ZoneType changedZone = getZoneTypeForProperty(prop);
                    if (changedZone != null && resolved instanceof TrackableCollection<?> coll) {
                        for (Object item : coll) {
                            if (item instanceof CardView cv && cv.getZone() != changedZone) {
                                cv.set(TrackableProperty.Zone, changedZone);
                            }
                        }
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

        if (type == TrackableTypes.CombatViewType) {
            if (value instanceof DeltaPacket.CombatData combatData) {
                return combatDataToCombatView(combatData, tracker);
            }
            netLog.warn("[DeltaSync] Expected CombatData for prop {}, got {}", prop, value.getClass().getSimpleName());
            return null;
        }

        if (type == TrackableTypes.StackItemViewType) {
            return tracker.getObj(TrackableTypes.StackItemViewType, (Integer) value);
        }

        if (type == TrackableTypes.StackItemViewListType) {
            List<Integer> ids = (List<Integer>) value;
            TrackableCollection<forge.game.spellability.StackItemView> coll = new TrackableCollection<>();
            for (int id : ids) {
                if (id != -1) {
                    forge.game.spellability.StackItemView siv = tracker.getObj(TrackableTypes.StackItemViewType, id);
                    if (siv != null) {
                        coll.add(siv);
                    }
                }
            }
            return coll;
        }

        return value;
    }

    /**
     * Convert a CombatData back into a CombatView by resolving IDs and calling addAttackingBand.
     */
    private static forge.game.combat.CombatView combatDataToCombatView(DeltaPacket.CombatData data, Tracker tracker) {
        forge.game.combat.CombatView combat = new forge.game.combat.CombatView(tracker);

        for (int i = 0; i < data.bandAttackerIds.size(); i++) {
            List<CardView> attackers = new ArrayList<>();
            for (int id : data.bandAttackerIds.get(i)) {
                CardView cv = tracker.getObj(TrackableTypes.CardViewType, id);
                if (cv != null) {
                    attackers.add(cv);
                }
            }

            int[] defRef = data.bandDefenderRefs.get(i);
            forge.game.GameEntityView defender = defRef[0] == 0
                    ? tracker.getObj(TrackableTypes.CardViewType, defRef[1])
                    : tracker.getObj(TrackableTypes.PlayerViewType, defRef[1]);

            List<CardView> blockers = null;
            List<Integer> blockerIds = data.bandBlockerIds.get(i);
            if (blockerIds != null) {
                blockers = new ArrayList<>();
                for (int id : blockerIds) {
                    CardView cv = tracker.getObj(TrackableTypes.CardViewType, id);
                    if (cv != null) {
                        blockers.add(cv);
                    }
                }
            }

            List<CardView> plannedBlockers = null;
            List<Integer> plannedIds = data.bandPlannedBlockerIds.get(i);
            if (plannedIds != null) {
                plannedBlockers = new ArrayList<>();
                for (int id : plannedIds) {
                    CardView cv = tracker.getObj(TrackableTypes.CardViewType, id);
                    if (cv != null) {
                        plannedBlockers.add(cv);
                    }
                }
            }

            combat.addAttackingBand(attackers, defender, blockers, plannedBlockers);
        }

        return combat;
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

    private TrackableObject createObjectOnly(int objectType, int objectId, Tracker tracker) {
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
            case DeltaPacket.TYPE_GAME_VIEW: return "GameView";
            default: return "Unknown(type=" + objectType + ")";
        }
    }

    private static ZoneType getZoneTypeForProperty(TrackableProperty prop) {
        switch (prop) {
            case Hand: return ZoneType.Hand;
            case Library: return ZoneType.Library;
            case Graveyard: return ZoneType.Graveyard;
            case Battlefield: return ZoneType.Battlefield;
            case Exile: return ZoneType.Exile;
            case Command: return ZoneType.Command;
            case Commander: return ZoneType.Command;
            case Flashback: return ZoneType.Flashback;
            case Ante: return ZoneType.Ante;
            case Sideboard: return ZoneType.Sideboard;
            default: return null;
        }
    }

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

    // ==================== Full state sync via setGameView ====================

    @Override
    public void setGameView(GameView gameView, long sequenceNumber) {
        setGameView(gameView);
        if (sequenceNumber >= 0) {
            IGameController controller = getGameController();
            if (controller != null) {
                controller.ackSync(sequenceNumber);
            }
        }
    }
}
