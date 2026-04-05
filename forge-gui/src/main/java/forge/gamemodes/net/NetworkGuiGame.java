package forge.gamemodes.net;

import forge.card.CardStateName;
import forge.game.GameView;
import forge.game.event.GameEvent;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.AbstractGuiGame;
import forge.interfaces.IGameController;
import forge.trackable.Tracker;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;
import forge.trackable.TrackableTypes;
import forge.trackable.TrackableTypes.TrackableType;
import forge.util.IHasForgeLog;

import java.util.*;

/**
 * This class handles all network-specific deserialization and state management,
 * keeping the core AbstractGuiGame class free from network dependencies.
 */
public abstract class NetworkGuiGame extends AbstractGuiGame implements IHasForgeLog {

    @Override
    public void applyDelta(DeltaPacket packet) {
        if (packet == null || getGameView() == null) {
            return;
        }

        Tracker tracker = getGameView().getTracker();
        if (tracker == null) {
            netLog.error("[DeltaSync] Cannot apply delta: Tracker is null");
            return;
        }

        long startTime = System.currentTimeMillis();

        // Log with game context for easier correlation
        String activePlayerName = getGameView().getPlayerTurn() != null ? getGameView().getPlayerTurn().getName() : "?";
        String phaseName = getGameView().getPhase() != null ? getGameView().getPhase().name() : "?";
        netLog.info("[DeltaSync] === START applyDelta seq={} (Turn {}, {}, Active={}) ===",
                packet.getSequenceNumber(), getGameView().getTurn(), phaseName, activePlayerName);

        List<GameEvent> resolvedEvents = null;
        if (packet.hasEvents()) {
            List<GameEvent> events = new java.util.ArrayList<>(packet.getEvents().size());
            for (Object item : packet.getEvents()) {
                if (item instanceof GameEvent e) events.add(e);
            }
            resolvedEvents = events;
            netLog.info("[DeltaSync] {} events received with delta seq={}",
                    resolvedEvents.size(), packet.getSequenceNumber());
        }

        int newObjectCount = 0;
        int appliedCount = 0;
        int skippedCount = 0;

        List<Map.Entry<TrackableObject, Map<TrackableProperty, Object>>> pendingPropertyApplications = new ArrayList<>();
        Map<Integer, CardStateView> csvRegistry = new HashMap<>();
        Map<Integer, Map<TrackableProperty, Object>> newObjects = packet.getNewObjects();
        if (!newObjects.isEmpty()) {
            // Sort by delta key so CardViews (type 0) are created before CSVs (type 4)
            List<Map.Entry<Integer, Map<TrackableProperty, Object>>> sortedNew = new ArrayList<>(newObjects.entrySet());
            sortedNew.sort(Comparator.comparingInt(Map.Entry::getKey));

            // STEP 1: Create new objects first
            for (Map.Entry<Integer, Map<TrackableProperty, Object>> newEntry : sortedNew) {
                int deltaKey = newEntry.getKey();
                int objectType = DeltaPacket.getTypeFromDeltaKey(deltaKey);
                int objectId = DeltaPacket.getIdFromDeltaKey(deltaKey);
                try {
                    if (objectType == DeltaPacket.TYPE_CSV) {
                        int cardId = getCardIdFromCsvEncodedId(objectId);
                        CardStateName state = getStateFromCsvEncodedId(objectId);
                        CardView parent = tracker.getObj(TrackableTypes.CardViewType, cardId);
                        if (parent == null) {
                            netLog.error("[DeltaSync] Parent CardView ID={} not found for CSV state={}",
                                    cardId, state);
                            continue;
                        }
                        CardStateView csv = findCsvByState(parent, state);
                        if (csv != null) {
                            ((Map<TrackableProperty, Object>) csv.getProps()).clear();
                        } else {
                            csv = parent.createAlternateState(state);
                        }
                        csvRegistry.put(deltaKey, csv);
                        pendingPropertyApplications.add(new AbstractMap.SimpleEntry<>(csv, newEntry.getValue()));
                        newObjectCount++;
                    } else {
                        TrackableObject created = createObjectOnly(objectType, objectId, tracker);
                        if (created != null) {
                            pendingPropertyApplications.add(new AbstractMap.SimpleEntry<>(created, newEntry.getValue()));
                            newObjectCount++;
                        }
                    }
                } catch (Exception e) {
                    netLog.error(e, "[DeltaSync] Error creating new object type={} id={}", objectType, objectId);
                }
            }
            netLog.info("[DeltaSync] Created {} new objects (phase 1a)", newObjectCount);

            // STEP 2: Populate new objects now that all exist for cross-references
            int propsApplied = 0;
            for (Map.Entry<TrackableObject, Map<TrackableProperty, Object>> pending : pendingPropertyApplications) {
                try {
                    applyPropertyMap(pending.getKey(), pending.getValue(), tracker, csvRegistry);
                    propsApplied++;
                } catch (Exception e) {
                    netLog.error("[DeltaSync] Error applying properties to object {}: {}",
                            pending.getKey().getId(), e.getMessage());
                }
            }
            netLog.trace("[DeltaSync] Applied properties to {} objects (phase 1b)", propsApplied);
        }

        // STEP 3: Apply property deltas to existing objects
        for (Map.Entry<Integer, Map<TrackableProperty, Object>> entry : packet.getObjectDeltas().entrySet()) {
            int deltaKey = entry.getKey();
            Map<TrackableProperty, Object> deltaProps = entry.getValue();

            int objectType = DeltaPacket.getTypeFromDeltaKey(deltaKey);
            int actualObjectId = DeltaPacket.getIdFromDeltaKey(deltaKey);

            TrackableObject obj;
            if (objectType == DeltaPacket.TYPE_CSV) {
                obj = csvRegistry.get(deltaKey);
                if (obj == null) {
                    obj = findObjectByTypeAndId(tracker, objectType, actualObjectId);
                }
            } else {
                obj = findObjectByTypeAndId(tracker, objectType, actualObjectId);
            }

            if (obj != null) {
                try {
                    applyPropertyMap(obj, deltaProps, tracker, csvRegistry);
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

        // Dispatch pre-resolved events now that game state is current.
        if (resolvedEvents != null && !resolvedEvents.isEmpty()) {
            handleGameEvents(resolvedEvents);
        }

        // TODO shouldn't be needed if hands are ordered correctly
        afterDeltaApplied();

        if (packet.hasChecksum()) {
            int serverChecksum = packet.getChecksum();
            int clientChecksum = NetworkChecksumUtil.computeSampledChecksum(getGameView(), packet.getChecksumProperties(), null);

            if (serverChecksum != clientChecksum) {
                netLog.error("[DeltaSync] CHECKSUM MISMATCH! Server={}, Client={} at seq={}",
                        serverChecksum, clientChecksum, packet.getSequenceNumber());
                if (packet.getChecksumProperties() != null) {
                    List<String> clientDivLog = new ArrayList<>();
                    NetworkChecksumUtil.computeSampledChecksum(getGameView(),
                            packet.getChecksumProperties(), clientDivLog);
                    netLog.error("[DeltaSync] Client checksum detail: {}", clientDivLog);
                }
                logChecksumDetails(getGameView(), packet);
                requestFullStateResync();
            } else {
                netLog.info("[DeltaSync] Checksum OK (seq={}, checksum={})",
                        packet.getSequenceNumber(), serverChecksum);
            }
        }

    }

    /**
     * Apply a property map to a TrackableObject.
     * Resolves network values (IDs) back to object references.
     */
    private void applyPropertyMap(TrackableObject obj, Map<TrackableProperty, Object> delta,
                                   Tracker tracker, Map<Integer, CardStateView> csvRegistry) {
        // Snapshot existing CSVs before processing — slot assignments processed in ordinal
        // order can displace a CSV from its slot before a later slot assignment looks for it.
        Map<CardStateName, CardStateView> existingCsvs = null;
        if (obj instanceof CardView cardView) {
            for (TrackableProperty p : delta.keySet()) {
                if (isCsvSlotProperty(p)) {
                    existingCsvs = snapshotExistingCsvs(cardView);
                    break;
                }
            }
        }

        for (Map.Entry<TrackableProperty, Object> entry : delta.entrySet()) {
            TrackableProperty prop = entry.getKey();
            Object value = entry.getValue();

            try {
                // CSV slot-assignment properties carry a CardStateName ordinal
                if (isCsvSlotProperty(prop) && obj instanceof CardView cardView) {
                    if (value == null) {
                        cardView.set(prop, null);
                    } else {
                        int ordinal = (Integer) value;
                        CardStateName state = CardStateName.values()[ordinal];
                        int csvKey = DeltaPacket.makeDeltaKey(DeltaPacket.TYPE_CSV, cardView.getId() * 16 + ordinal);
                        CardStateView csv = csvRegistry.get(csvKey);
                        if (csv == null) csv = findCsvByState(cardView, state);
                        if (csv == null && existingCsvs != null) csv = existingCsvs.get(state);
                        if (csv == null) csv = cardView.createAlternateState(state);
                        cardView.set(prop, csv);
                    }
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
                } else {
                    netLog.warn("[DeltaSync] CombatView attacker CardView ID={} not found in tracker (band {})", id, i);
                }
            }

            int[] defRef = data.bandDefenderRefs.get(i);
            forge.game.GameEntityView defender = defRef[0] == 0
                    ? tracker.getObj(TrackableTypes.CardViewType, defRef[1])
                    : tracker.getObj(TrackableTypes.PlayerViewType, defRef[1]);
            if (defender == null) {
                if (defRef[1] == -1) {
                    // Rule 506.4c: when a planeswalker/battle leaves combat,
                    // the server replaces it with a fake Card(-1) placeholder
                    // that isn't in the object graph
                    defender = new CardView(defRef[1], tracker, "<Nothing>");
                } else {
                    netLog.warn("[DeltaSync] CombatView defender ID={} (type={}) not found in tracker (band {})",
                            defRef[1], defRef[0] == 0 ? "Card" : "Player", i);
                }
            }

            List<CardView> blockers = null;
            List<Integer> blockerIds = data.bandBlockerIds.get(i);
            if (blockerIds != null) {
                blockers = new ArrayList<>();
                for (int id : blockerIds) {
                    CardView cv = tracker.getObj(TrackableTypes.CardViewType, id);
                    if (cv != null) {
                        blockers.add(cv);
                    } else {
                        netLog.warn("[DeltaSync] CombatView blocker CardView ID={} not found in tracker (band {})", id, i);
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
                    } else {
                        netLog.warn("[DeltaSync] CombatView planned blocker CardView ID={} not found in tracker (band {})", id, i);
                    }
                }
            }

            combat.addAttackingBand(attackers, defender, blockers, plannedBlockers);
        }

        return combat;
    }

    private TrackableObject createObjectOnly(int objectType, int objectId, Tracker tracker) {
        String typeName = getObjectTypeName(objectType);

        // Check if object of the SAME TYPE already exists
        TrackableObject existing = findObjectByTypeAndId(tracker, objectType, objectId);
        if (existing != null) {
            if (objectType == DeltaPacket.TYPE_CARD_VIEW) {
                // Create a NEW instance rather than clearing the existing one in-place.
                // This mirrors the game engine: when a card changes zones, a new Card
                // object is created while the old one persists for existing references
                // (e.g., StackItemView.SourceCard still shows the card's state from
                // when the ability went on the stack).
                CardView replacement = new CardView(objectId, tracker);
                tracker.putObj(TrackableTypes.CardViewType, objectId, replacement);
                netLog.trace("[DeltaSync] CardView ID={} replaced with new instance", objectId);
                return replacement;
            }
            // Non-CardView types: clear and reuse (PlayerView identity must be preserved
            // for GUI player matching; other types are rarely replaced).
            ((Map<TrackableProperty, Object>) existing.getProps()).clear();
            netLog.trace("[DeltaSync] {} ID={} already exists (replaced), cleared stale props",
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
            case DeltaPacket.TYPE_CSV:
                int cardId = getCardIdFromCsvEncodedId(objectId);
                CardStateName state = getStateFromCsvEncodedId(objectId);
                CardView parent = tracker.getObj(TrackableTypes.CardViewType, cardId);
                return parent != null ? findCsvByState(parent, state) : null;
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
            case DeltaPacket.TYPE_CSV: return "CardStateView";
            default: return "Unknown(type=" + objectType + ")";
        }
    }

    private static Map<CardStateName, CardStateView> snapshotExistingCsvs(CardView cardView) {
        Map<CardStateName, CardStateView> map = new HashMap<>();
        CardStateView csv;
        csv = cardView.getCurrentState();
        if (csv != null) map.put(csv.getState(), csv);
        csv = cardView.getAlternateState();
        if (csv != null) map.put(csv.getState(), csv);
        csv = cardView.getLeftSplitState();
        if (csv != null) map.put(csv.getState(), csv);
        csv = cardView.getRightSplitState();
        if (csv != null) map.put(csv.getState(), csv);
        return map;
    }

    private static CardStateView findCsvByState(CardView parent, CardStateName state) {
        CardStateView csv;
        csv = parent.getCurrentState();
        if (csv != null && csv.getState() == state) return csv;
        csv = parent.getAlternateState();
        if (csv != null && csv.getState() == state) return csv;
        csv = parent.getLeftSplitState();
        if (csv != null && csv.getState() == state) return csv;
        csv = parent.getRightSplitState();
        if (csv != null && csv.getState() == state) return csv;
        return null;
    }

    private static int getCardIdFromCsvEncodedId(int encodedId) {
        return encodedId / 16;
    }
    private static CardStateName getStateFromCsvEncodedId(int encodedId) {
        return CardStateName.values()[encodedId % 16];
    }

    private static boolean isCsvSlotProperty(TrackableProperty prop) {
        return prop == TrackableProperty.CurrentState
                || prop == TrackableProperty.AlternateState
                || prop == TrackableProperty.LeftSplitState
                || prop == TrackableProperty.RightSplitState;
    }

    private static ZoneType getZoneTypeForProperty(TrackableProperty prop) {
        switch (prop) {
            case Hand: return ZoneType.Hand;
            case Library: return ZoneType.Library;
            case Graveyard: return ZoneType.Graveyard;
            case Battlefield: return ZoneType.Battlefield;
            case Exile: return ZoneType.Exile;
            case Command: return ZoneType.Command;
            // Commander is a list of commander cards — they may be in any zone, not just Command
            case Commander: return null;
            // Flashback is a virtual zone — cards in it keep their real zone (Graveyard, Command, etc.)
            case Flashback: return null;
            case Ante: return ZoneType.Ante;
            case Sideboard: return ZoneType.Sideboard;
            default: return null;
        }
    }

    private void requestFullStateResync() {
        IGameController controller = getGameController();
        if (controller != null) {
            netLog.warn("[DeltaSync] Requesting full state resync from server");
            controller.requestResync();
        } else {
            netLog.error("[DeltaSync] Cannot request resync: No game controller available");
        }
    }

    protected void afterDeltaApplied() {
        // Override in GUI subclasses to refresh views after delta application.
    }

    private void logChecksumDetails(GameView gameView, DeltaPacket packet) {
        netLog.error("[DeltaSync] Checksum details (client state):");
        netLog.error("[DeltaSync]   GameView ID: {}", gameView.getId());
        netLog.error("[DeltaSync]   Turn: {}", gameView.getTurn());
        netLog.error("[DeltaSync]   Phase: {}", gameView.getPhase() != null ? gameView.getPhase().name() : "null");
        netLog.error("[DeltaSync]   Sampled properties: {}", NetworkChecksumUtil.sampledPropertyNames(packet.getChecksumProperties()));
        for (PlayerView player : NetworkChecksumUtil.getSortedPlayers(gameView)) {
            int handSize = player.getHand() != null ? player.getHand().size() : 0;
            int graveyardSize = player.getGraveyard() != null ? player.getGraveyard().size() : 0;
            int battlefieldSize = player.getBattlefield() != null ? player.getBattlefield().size() : 0;
            netLog.error("[DeltaSync]   Player {} ({}): Life={}, Hand={}, GY={}, BF={}",
                    player.getId(), player.getName(), player.getLife(),
                    handSize, graveyardSize, battlefieldSize);
        }
        netLog.error("[DeltaSync] Compare with server state in host log at seq={}", packet.getSequenceNumber());
        int phaseOrdinal = gameView.getPhase() != null ? gameView.getPhase().ordinal() : -1;
        netLog.error("[DeltaSync] Client breakdown: {}",
                NetworkChecksumUtil.computeChecksumBreakdown(gameView.getTurn(), phaseOrdinal, gameView));
    }

}
