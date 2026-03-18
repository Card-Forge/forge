package forge.gamemodes.net;

import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.combat.CombatView;
import forge.game.player.PlayerView;
import forge.game.spellability.StackItemView;
import forge.game.zone.ZoneType;
import forge.trackable.Tracker;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;
import forge.trackable.TrackableTypes;
import forge.trackable.TrackableTypes.TrackableType;

import forge.game.phase.PhaseType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Shared checksum computation for delta sync validation.
 * Used by both server ({@link forge.gamemodes.net.server.DeltaSyncManager}) and client ({@link forge.gamemodes.net.NetworkGuiGame})
 * to ensure identical checksums.
 */
public final class NetworkChecksumUtil {

    private NetworkChecksumUtil() {} // Utility class

    /**
     * When true, checksums include battlefield card properties, zone sizes,
     * mana pools, combat state, and stack size. Disabled by default for
     * production performance; enabled by the test harness to catch GUI desyncs.
     */
    private static boolean stableChecksum = false;

    public static void setStableChecksum(boolean enabled) {
        stableChecksum = enabled;
    }

    /**
     * Compute a state checksum from turn, phase, and player state.
     * Both server and client must call this method to ensure compatibility.
     *
     * @param turn the current turn number
     * @param phaseOrdinal the phase ordinal, or -1 if phase is null
     * @param players the players to include (will be sorted by ID internally)
     * @return checksum value
     */
    static int computeStateChecksum(int turn, int phaseOrdinal, Iterable<PlayerView> players) {
        int hash = 17;
        hash = 31 * hash + turn;
        if (phaseOrdinal >= 0) {
            hash = 31 * hash + phaseOrdinal;
        }
        if (players != null) {
            for (PlayerView player : getSortedPlayers(players)) {
                hash = 31 * hash + player.getId();
                Object life = getEffectiveValue(player, TrackableProperty.Life);
                hash = 31 * hash + (life instanceof Integer ? (int) life : 0);
            }
        }
        return hash;
    }

    /**
     * Compute a state checksum with fixed properties.
     *
     * @param turn the current turn number
     * @param phaseOrdinal the phase ordinal, or -1 if phase is null
     * @param gameView the game view for deep checksum data
     * @return checksum value
     */
    public static int computeStateChecksum(int turn, int phaseOrdinal, GameView gameView) {
        return computeStateChecksum(turn, phaseOrdinal, gameView, null);
    }

    /**
     * Snapshot-aware stable checksum. When snapshot is non-null, reads property
     * values from the snapshot (captured during walkAndCollect) instead of live
     * state, ensuring the server checksum matches exactly what the delta carries.
     * When snapshot is null (client-side), reads live state as before.
     */
    public static int computeStateChecksum(int turn, int phaseOrdinal, GameView gameView,
                                            Map<TrackableObject, Map<TrackableProperty, Object>> snapshot) {
        if (gameView == null) {
            return computeStateChecksum(turn, phaseOrdinal, (Iterable<PlayerView>) null);
        }
        // Inline base hash to use snapValue for Life — the 3-arg overload reads
        // live state via getEffectiveValue, which races with the game thread.
        int hash = 17;
        hash = 31 * hash + turn;
        if (phaseOrdinal >= 0) {
            hash = 31 * hash + phaseOrdinal;
        }
        List<PlayerView> sortedPlayers = getSortedPlayers(gameView);
        for (PlayerView player : sortedPlayers) {
            hash = 31 * hash + player.getId();
            Object life = snapValue(player, TrackableProperty.Life, snapshot);
            hash = 31 * hash + (life instanceof Integer ? (int) life : 0);
        }

        for (PlayerView player : sortedPlayers) {
            hash = 31 * hash + snapZoneSize(player, TrackableProperty.Hand, snapshot);
            hash = 31 * hash + snapZoneSize(player, TrackableProperty.Battlefield, snapshot);
            hash = 31 * hash + snapZoneSize(player, TrackableProperty.Graveyard, snapshot);
            hash = 31 * hash + snapZoneSize(player, TrackableProperty.Exile, snapshot);
            hash = 31 * hash + snapZoneSize(player, TrackableProperty.Command, snapshot);

            int manaHash = 0;
            Object manaObj = snapValue(player, TrackableProperty.Mana, snapshot);
            if (manaObj instanceof Map) {
                for (Object v : ((Map<?, ?>) manaObj).values()) {
                    if (v instanceof Integer) manaHash += (int) v;
                }
            }
            hash = 31 * hash + manaHash;
        }

        // Deep checksum: battlefield card properties (sorted by ID for determinism)
        List<CardView> allBattlefieldCards = new ArrayList<>();
        for (PlayerView player : getSortedPlayers(gameView)) {
            Object bf = snapValue(player, TrackableProperty.Battlefield, snapshot);
            if (bf instanceof Iterable<?>) {
                for (Object item : (Iterable<?>) bf) {
                    if (item instanceof CardView) allBattlefieldCards.add((CardView) item);
                }
            }
        }
        allBattlefieldCards.sort(Comparator.comparingInt(CardView::getId));

        for (CardView card : allBattlefieldCards) {
            hash = 31 * hash + card.getId();
            hash = 31 * hash + (Boolean.TRUE.equals(snapValue(card, TrackableProperty.Tapped, snapshot)) ? 1 : 0);
            CardView.CardStateView state = card.getCurrentState();
            if (state != null) {
                Object pow = snapValue(state, TrackableProperty.Power, snapshot);
                Object tou = snapValue(state, TrackableProperty.Toughness, snapshot);
                hash = 31 * hash + (pow instanceof Integer ? (int) pow : 0);
                hash = 31 * hash + (tou instanceof Integer ? (int) tou : 0);
            }
            Object zone = snapValue(card, TrackableProperty.Zone, snapshot);
            hash = 31 * hash + (zone instanceof ZoneType ? ((ZoneType) zone).ordinal() : -1);
            Object ctrl = snapValue(card, TrackableProperty.Controller, snapshot);
            hash = 31 * hash + (ctrl instanceof PlayerView ? ((PlayerView) ctrl).getId() : -1);

            Object countersObj = snapValue(card, TrackableProperty.Counters, snapshot);
            int counterTotal = 0;
            if (countersObj instanceof Map) {
                for (Object count : ((Map<?, ?>) countersObj).values()) {
                    if (count instanceof Integer) counterTotal += (int) count;
                }
            }
            hash = 31 * hash + counterTotal;

            hash = 31 * hash + (Boolean.TRUE.equals(snapValue(card, TrackableProperty.Sickness, snapshot)) ? 1 : 0);
            hash = 31 * hash + (Boolean.TRUE.equals(snapValue(card, TrackableProperty.Attacking, snapshot)) ? 1 : 0);
            hash = 31 * hash + (Boolean.TRUE.equals(snapValue(card, TrackableProperty.Blocking, snapshot)) ? 1 : 0);
            hash = 31 * hash + (Boolean.TRUE.equals(snapValue(card, TrackableProperty.PhasedOut, snapshot)) ? 1 : 0);
            hash = 31 * hash + (Boolean.TRUE.equals(snapValue(card, TrackableProperty.Facedown, snapshot)) ? 1 : 0);
            hash = 31 * hash + (Boolean.TRUE.equals(snapValue(card, TrackableProperty.Flipped, snapshot)) ? 1 : 0);
        }

        CombatView combat = gameView.getCombat();
        if (combat != null) {
            hash = 31 * hash + combat.getNumAttackers();
            List<Integer> attackerIds = new ArrayList<>();
            for (CardView attacker : combat.getAttackers()) {
                attackerIds.add(attacker.getId());
            }
            attackerIds.sort(null);
            for (int id : attackerIds) {
                hash = 31 * hash + id;
            }
        }

        if (gameView.getStack() != null) {
            hash = 31 * hash + gameView.getStack().size();
        }

        return hash;
    }

    /** Read a property value from snapshot if available, otherwise from live state. */
    private static Object snapValue(TrackableObject obj, TrackableProperty prop,
                                     Map<TrackableObject, Map<TrackableProperty, Object>> snapshot) {
        if (snapshot != null) {
            Map<TrackableProperty, Object> snap = snapshot.get(obj);
            if (snap != null && snap.containsKey(prop)) {
                return snap.get(prop);
            }
        }
        return getEffectiveValue(obj, prop);
    }

    /** Read a zone collection size from snapshot if available. */
    private static int snapZoneSize(PlayerView player, TrackableProperty zoneProp,
                                     Map<TrackableObject, Map<TrackableProperty, Object>> snapshot) {
        Object val = snapValue(player, zoneProp, snapshot);
        if (val instanceof TrackableCollection<?>) return ((TrackableCollection<?>) val).size();
        return player.getZoneSize(zoneProp == TrackableProperty.Hand ? ZoneType.Hand
                : zoneProp == TrackableProperty.Battlefield ? ZoneType.Battlefield
                : zoneProp == TrackableProperty.Graveyard ? ZoneType.Graveyard
                : zoneProp == TrackableProperty.Exile ? ZoneType.Exile
                : ZoneType.Command);
    }

    /**
     * Compute a diagnostic breakdown of the deep checksum, showing the hash
     * after each component. Used to identify exactly where server/client diverge.
     */
    public static String computeChecksumBreakdown(int turn, int phaseOrdinal, GameView gameView) {
        return computeChecksumBreakdown(turn, phaseOrdinal, gameView, null);
    }

    /**
     * Snapshot-aware breakdown. When snapshot is non-null, reads from snapshot
     * to match exactly what the checksum was computed from.
     */
    public static String computeChecksumBreakdown(int turn, int phaseOrdinal, GameView gameView,
                                                   Map<TrackableObject, Map<TrackableProperty, Object>> snapshot) {
        if (gameView == null || !stableChecksum) {
            return "deep checksum disabled or gameView null";
        }
        // This must mirror computeStateChecksum exactly so that
        // breakdown final == actual checksum, enabling accurate mismatch diagnosis.
        StringBuilder sb = new StringBuilder();
        // Inline base hash with snapValue for Life (mirrors computeStateChecksum)
        int hash = 17;
        hash = 31 * hash + turn;
        if (phaseOrdinal >= 0) {
            hash = 31 * hash + phaseOrdinal;
        }
        List<PlayerView> sortedPlayers = getSortedPlayers(gameView);
        for (PlayerView player : sortedPlayers) {
            hash = 31 * hash + player.getId();
            Object life = snapValue(player, TrackableProperty.Life, snapshot);
            hash = 31 * hash + (life instanceof Integer ? (int) life : 0);
        }
        sb.append("base=").append(hash);

        for (PlayerView player : sortedPlayers) {
            int zoneHash = hash;
            zoneHash = 31 * zoneHash + snapZoneSize(player, TrackableProperty.Hand, snapshot);
            zoneHash = 31 * zoneHash + snapZoneSize(player, TrackableProperty.Battlefield, snapshot);
            zoneHash = 31 * zoneHash + snapZoneSize(player, TrackableProperty.Graveyard, snapshot);
            zoneHash = 31 * zoneHash + snapZoneSize(player, TrackableProperty.Exile, snapshot);
            zoneHash = 31 * zoneHash + snapZoneSize(player, TrackableProperty.Command, snapshot);

            int manaHash = 0;
            Object manaObj = snapValue(player, TrackableProperty.Mana, snapshot);
            if (manaObj instanceof Map) {
                for (Object v : ((Map<?, ?>) manaObj).values()) {
                    if (v instanceof Integer) manaHash += (int) v;
                }
            }
            zoneHash = 31 * zoneHash + manaHash;
            hash = zoneHash;
            sb.append(" | p").append(player.getId()).append("zones=").append(hash);
            sb.append("(H").append(snapZoneSize(player, TrackableProperty.Hand, snapshot));
            sb.append("B").append(snapZoneSize(player, TrackableProperty.Battlefield, snapshot));
            sb.append("G").append(snapZoneSize(player, TrackableProperty.Graveyard, snapshot));
            sb.append("E").append(snapZoneSize(player, TrackableProperty.Exile, snapshot));
            sb.append("C").append(snapZoneSize(player, TrackableProperty.Command, snapshot));
            sb.append("M").append(manaHash).append(")");
        }

        List<CardView> allBattlefieldCards = new ArrayList<>();
        for (PlayerView player : getSortedPlayers(gameView)) {
            Object bf = snapValue(player, TrackableProperty.Battlefield, snapshot);
            if (bf instanceof Iterable<?>) {
                for (Object item : (Iterable<?>) bf) {
                    if (item instanceof CardView) allBattlefieldCards.add((CardView) item);
                }
            }
        }
        allBattlefieldCards.sort(Comparator.comparingInt(CardView::getId));
        sb.append(" | cards(").append(allBattlefieldCards.size()).append(")=[");
        for (CardView card : allBattlefieldCards) {
            boolean tapped = Boolean.TRUE.equals(snapValue(card, TrackableProperty.Tapped, snapshot));
            CardView.CardStateView state = card.getCurrentState();
            int power = 0, toughness = 0;
            if (state != null) {
                Object pow = snapValue(state, TrackableProperty.Power, snapshot);
                Object tou = snapValue(state, TrackableProperty.Toughness, snapshot);
                power = pow instanceof Integer ? (int) pow : 0;
                toughness = tou instanceof Integer ? (int) tou : 0;
            }
            Object zoneObj = snapValue(card, TrackableProperty.Zone, snapshot);
            int zoneOrd = zoneObj instanceof ZoneType ? ((ZoneType) zoneObj).ordinal() : -1;
            Object ctrlObj = snapValue(card, TrackableProperty.Controller, snapshot);
            int ctrlId = ctrlObj instanceof PlayerView ? ((PlayerView) ctrlObj).getId() : -1;
            Object countersObj = snapValue(card, TrackableProperty.Counters, snapshot);
            int ct = 0;
            if (countersObj instanceof Map) {
                for (Object v : ((Map<?, ?>) countersObj).values()) {
                    if (v instanceof Integer) ct += (int) v;
                }
            }
            boolean sick = Boolean.TRUE.equals(snapValue(card, TrackableProperty.Sickness, snapshot));
            boolean attacking = Boolean.TRUE.equals(snapValue(card, TrackableProperty.Attacking, snapshot));
            boolean blocking = Boolean.TRUE.equals(snapValue(card, TrackableProperty.Blocking, snapshot));
            boolean phasedOut = Boolean.TRUE.equals(snapValue(card, TrackableProperty.PhasedOut, snapshot));
            boolean faceDown = Boolean.TRUE.equals(snapValue(card, TrackableProperty.Facedown, snapshot));
            boolean flipped = Boolean.TRUE.equals(snapValue(card, TrackableProperty.Flipped, snapshot));

            sb.append(card.getId());
            sb.append(tapped ? "T" : "U");
            if (state != null) sb.append(power).append("/").append(toughness);
            sb.append("z").append(zoneOrd);
            sb.append("c").append(ctrlId);
            if (ct > 0) sb.append("k").append(ct);
            if (sick) sb.append("S");
            if (attacking) sb.append("A");
            if (blocking) sb.append("B");
            if (phasedOut) sb.append("P");
            if (faceDown) sb.append("D");
            if (flipped) sb.append("F");
            sb.append(" ");
        }
        sb.append("]");

        // Hash cards using same path as computeStateChecksum
        for (CardView card : allBattlefieldCards) {
            hash = 31 * hash + card.getId();
            hash = 31 * hash + (Boolean.TRUE.equals(snapValue(card, TrackableProperty.Tapped, snapshot)) ? 1 : 0);
            CardView.CardStateView state = card.getCurrentState();
            if (state != null) {
                Object pow = snapValue(state, TrackableProperty.Power, snapshot);
                Object tou = snapValue(state, TrackableProperty.Toughness, snapshot);
                hash = 31 * hash + (pow instanceof Integer ? (int) pow : 0);
                hash = 31 * hash + (tou instanceof Integer ? (int) tou : 0);
            }
            Object zone = snapValue(card, TrackableProperty.Zone, snapshot);
            hash = 31 * hash + (zone instanceof ZoneType ? ((ZoneType) zone).ordinal() : -1);
            Object ctrl = snapValue(card, TrackableProperty.Controller, snapshot);
            hash = 31 * hash + (ctrl instanceof PlayerView ? ((PlayerView) ctrl).getId() : -1);
            Object countersObj = snapValue(card, TrackableProperty.Counters, snapshot);
            int counterTotal = 0;
            if (countersObj instanceof Map) {
                for (Object count : ((Map<?, ?>) countersObj).values()) {
                    if (count instanceof Integer) counterTotal += (int) count;
                }
            }
            hash = 31 * hash + counterTotal;
            hash = 31 * hash + (Boolean.TRUE.equals(snapValue(card, TrackableProperty.Sickness, snapshot)) ? 1 : 0);
            hash = 31 * hash + (Boolean.TRUE.equals(snapValue(card, TrackableProperty.Attacking, snapshot)) ? 1 : 0);
            hash = 31 * hash + (Boolean.TRUE.equals(snapValue(card, TrackableProperty.Blocking, snapshot)) ? 1 : 0);
            hash = 31 * hash + (Boolean.TRUE.equals(snapValue(card, TrackableProperty.PhasedOut, snapshot)) ? 1 : 0);
            hash = 31 * hash + (Boolean.TRUE.equals(snapValue(card, TrackableProperty.Facedown, snapshot)) ? 1 : 0);
            hash = 31 * hash + (Boolean.TRUE.equals(snapValue(card, TrackableProperty.Flipped, snapshot)) ? 1 : 0);
        }
        sb.append(" afterCards=").append(hash);

        CombatView combat = gameView.getCombat();
        sb.append(" | combat=").append(combat != null ? combat.getNumAttackers() : "null");
        if (combat != null) {
            hash = 31 * hash + combat.getNumAttackers();
            List<Integer> attackerIds = new ArrayList<>();
            for (CardView attacker : combat.getAttackers()) {
                attackerIds.add(attacker.getId());
            }
            attackerIds.sort(null);
            for (int id : attackerIds) {
                hash = 31 * hash + id;
            }
        }
        sb.append(" afterCombat=").append(hash);

        int stackSize = gameView.getStack() != null ? gameView.getStack().size() : 0;
        if (gameView.getStack() != null) {
            hash = 31 * hash + stackSize;
        }
        sb.append(" | stack=").append(stackSize);
        sb.append(" final=").append(hash);

        return sb.toString();
    }

    // ==================== Sampled checksum ====================

    /** Cached arrays to avoid repeated allocation from values(). */
    private static Set<TrackableProperty> eligibleProperties = null;
    private static TrackableProperty[] allProperties = null;

    /**
     * Get all TrackableProperty values whose types can be generically hashed.
     * Excludes container/nested types that require special traversal.
     */
    public static Set<TrackableProperty> getEligibleProperties() {
        if (eligibleProperties != null) {
            return eligibleProperties;
        }
        eligibleProperties = EnumSet.noneOf(TrackableProperty.class);
        for (TrackableProperty prop : getAllProperties()) {
            TrackableType<?> type = prop.getType();
            if (type == TrackableTypes.CardStateViewType
                    || type == TrackableTypes.CombatViewType
                    || type == TrackableTypes.IPaperCardType
                    || type == TrackableTypes.StackItemViewListType) {
                continue;
            }
            eligibleProperties.add(prop);
        }
        return eligibleProperties;
    }

    private static TrackableProperty[] getAllProperties() {
        if (allProperties == null) {
            allProperties = TrackableProperty.values();
        }
        return allProperties;
    }

    /**
     * Walk the GameView object graph in deterministic order and collect
     * all TrackableObjects whose properties should be sampled.
     */
    public static List<TrackableObject> collectChecksumObjects(GameView gameView) {
        List<TrackableObject> objects = new ArrayList<>();
        if (gameView == null) {
            return objects;
        }

        objects.add(gameView);

        List<PlayerView> players = getSortedPlayers(gameView);
        objects.addAll(players);

        for (PlayerView player : players) {
            List<CardView> cards = new ArrayList<>();
            addCardsFromZone(cards, player.getBattlefield());
            addCardsFromZone(cards, player.getHand());
            addCardsFromZone(cards, player.getGraveyard());
            addCardsFromZone(cards, player.getExile());
            addCardsFromZone(cards, player.getCommand());
            cards.sort(Comparator.comparingInt(CardView::getId));

            for (CardView card : cards) {
                objects.add(card);
                addIfNotNull(objects, card.getCurrentState());
                addIfNotNull(objects, card.getAlternateState());
                addIfNotNull(objects, card.getLeftSplitState());
                addIfNotNull(objects, card.getRightSplitState());
            }
        }

        if (gameView.getStack() != null) {
            List<StackItemView> stackItems = new ArrayList<>();
            for (StackItemView siv : gameView.getStack()) {
                stackItems.add(siv);
            }
            stackItems.sort(Comparator.comparingInt(StackItemView::getId));
            objects.addAll(stackItems);
        }

        return objects;
    }

    private static void addCardsFromZone(List<CardView> cards, Iterable<CardView> zone) {
        if (zone == null) return;
        for (CardView card : zone) {
            cards.add(card);
        }
    }

    private static void addIfNotNull(List<TrackableObject> objects, TrackableObject obj) {
        if (obj != null) {
            objects.add(obj);
        }
    }

    /**
     * Read the effective value of a property, checking the tracker's delayed
     * queue when frozen. During a tracker freeze, mergeDelayedProps adds pending
     * values to the delta that aren't yet in the live props. The checksum must
     * read these same values to match what the client will have after applying
     * the delta.
     */
    static Object getEffectiveValue(TrackableObject obj, TrackableProperty prop) {
        Map<TrackableProperty, Object> props = obj.getProps();
        Object value = props != null ? props.get(prop) : null;
        Tracker tracker = obj.getTracker();
        if (tracker != null && tracker.isFrozen()) {
            Map<TrackableProperty, Object> delayed = tracker.getDelayedPropsFor(obj);
            if (delayed.containsKey(prop)) {
                value = delayed.get(prop);
            }
        }
        return value;
    }

    /**
     * Hash a property value specifically for checksum purposes.
     */
    static int hashPropertyValue(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof TrackableObject to) {
            return to.getId();
        }
        if (value instanceof TrackableCollection tc) {
            // Sort IDs for determinism
            List<Integer> ids = new ArrayList<>(tc.size());
            for (Object item : tc) {
                ids.add(item instanceof TrackableObject ? ((TrackableObject) item).getId() : Objects.hashCode(item));
            }
            ids.sort(null);
            return ids.hashCode();
        }
        if (value instanceof Map) {
            // Sort entries by key hashCode for determinism
            Map<?, ?> map = (Map<?, ?>) value;
            List<Integer> entryHashes = new ArrayList<>(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                entryHashes.add(31 * Objects.hashCode(entry.getKey()) + Objects.hashCode(entry.getValue()));
            }
            entryHashes.sort(null);
            return entryHashes.hashCode();
        }
        // Use ordinal() for enums — identity hashCode() differs across JVMs
        if (value instanceof Enum<?> e) {
            return e.ordinal();
        }
        return Objects.hashCode(value);
    }

    /**
     * Compute a sampled checksum over a dynamic set of TrackableProperties.
     * Starts with the core state checksum, then reads the specified
     * properties from all objects in the game view graph.
     *
     * @param gameView the game view to checksum
     * @param sampledPropertyOrdinals ordinals of TrackableProperty values to sample
     * @return checksum value
     */
    public static int computeSampledChecksum(GameView gameView, int[] sampledPropertyOrdinals) {
        return computeSampledChecksum(gameView, sampledPropertyOrdinals, null);
    }

    /**
     * @param divergenceLog if non-null, logs per-property hash contributions for mismatch diagnosis
     */
    public static int computeSampledChecksum(GameView gameView, int[] sampledPropertyOrdinals,
                                              List<String> divergenceLog) {
        final int turn = gameView.getTurn();
        int phaseOrdinal = gameView.getPhase() != null ? gameView.getPhase().ordinal() : -1;
        if (stableChecksum) {
            return computeStateChecksum(turn, phaseOrdinal, gameView);
        }

        // Start with core hash
        int hash = computeStateChecksum(turn, phaseOrdinal, gameView.getPlayers());
        if (divergenceLog != null) {
            divergenceLog.add("base=" + hash);
        }

        // Convert ordinals to properties
        TrackableProperty[] allProps = getAllProperties();
        TrackableProperty[] sampled = new TrackableProperty[sampledPropertyOrdinals.length];
        for (int i = 0; i < sampledPropertyOrdinals.length; i++) {
            sampled[i] = allProps[sampledPropertyOrdinals[i]];
        }

        List<TrackableObject> objects = collectChecksumObjects(gameView);
        for (TrackableObject obj : objects) {
            if (obj.getProps() == null) continue;
            for (TrackableProperty prop : sampled) {
                Object value = getEffectiveValue(obj, prop);
                if (value != null && !value.equals(prop.getDefaultValue())) {
                    int propHash = hashPropertyValue(value);
                    hash = 31 * hash + prop.ordinal();
                    hash = 31 * hash + propHash;
                    if (divergenceLog != null) {
                        divergenceLog.add(obj.getClass().getSimpleName() + "#" + obj.getId()
                                + "." + prop.name() + "=" + propHash + " hash=" + hash);
                    }
                }
            }
        }

        return hash;
    }

    /**
     * Compute a sampled checksum from pre-captured property snapshots.
     * The snapshot is captured during walkAndCollect under the same volatile
     * barrier as the delta reads, so the checksum reflects exactly the values
     * the client will have after applying the delta — eliminating false-positive
     * mismatches from the game thread modifying properties between the delta
     * build and checksum read.
     *
     * @param gameView the game view (used for object graph ordering via collectChecksumObjects)
     * @param sampledPropertyOrdinals ordinals of TrackableProperty values to sample
     * @param snapshot property snapshots keyed by object identity
     * @param divergenceLog if non-null, logs per-property hash contributions for mismatch diagnosis
     * @return checksum value
     */
    public static int computeSampledChecksumFromSnapshot(
            GameView gameView, int[] sampledPropertyOrdinals,
            Map<TrackableObject, Map<TrackableProperty, Object>> snapshot,
            List<String> divergenceLog) {

        // stableChecksum mode (test harness) — use the snapshot-aware stable
        // checksum so the server reads from the same values the delta carries.
        // The client calls computeStateChecksum(turn, phase, gameView) without
        // snapshot, reading from its local state which matches after delta apply.
        if (stableChecksum) {
            Map<TrackableProperty, Object> gvSnap = snapshot != null ? snapshot.get(gameView) : null;
            int turn = gvSnap != null && gvSnap.get(TrackableProperty.Turn) instanceof Integer
                    ? (int) gvSnap.get(TrackableProperty.Turn) : gameView.getTurn();
            int phaseOrd = gvSnap != null && gvSnap.get(TrackableProperty.Phase) instanceof PhaseType
                    ? ((PhaseType) gvSnap.get(TrackableProperty.Phase)).ordinal()
                    : (gameView.getPhase() != null ? gameView.getPhase().ordinal() : -1);
            return computeStateChecksum(turn, phaseOrd, gameView, snapshot);
        }

        // Read turn and phase from the snapshot of the GameView
        Map<TrackableProperty, Object> gvSnap = snapshot != null ? snapshot.get(gameView) : null;
        int turn;
        int phaseOrdinal;
        if (gvSnap != null) {
            Object turnObj = gvSnap.get(TrackableProperty.Turn);
            turn = turnObj instanceof Integer ? (int) turnObj : 0;
            Object phaseObj = gvSnap.get(TrackableProperty.Phase);
            phaseOrdinal = phaseObj instanceof PhaseType ? ((PhaseType) phaseObj).ordinal() : -1;
        } else {
            turn = gameView.getTurn();
            phaseOrdinal = gameView.getPhase() != null ? gameView.getPhase().ordinal() : -1;
        }

        // Build base hash from turn + phase + player life (from snapshots)
        int hash = 17;
        hash = 31 * hash + turn;
        if (phaseOrdinal >= 0) {
            hash = 31 * hash + phaseOrdinal;
        }
        if (gameView.getPlayers() != null) {
            for (PlayerView player : getSortedPlayers(gameView.getPlayers())) {
                hash = 31 * hash + player.getId();
                Map<TrackableProperty, Object> playerSnap =
                        snapshot != null ? snapshot.get(player) : null;
                Object life;
                if (playerSnap != null) {
                    life = playerSnap.get(TrackableProperty.Life);
                } else {
                    life = getEffectiveValue(player, TrackableProperty.Life);
                }
                hash = 31 * hash + (life instanceof Integer ? (int) life : 0);
            }
        }
        if (divergenceLog != null) {
            divergenceLog.add("base=" + hash);
        }

        // Convert ordinals to properties
        TrackableProperty[] allProps = getAllProperties();
        TrackableProperty[] sampled = new TrackableProperty[sampledPropertyOrdinals.length];
        for (int i = 0; i < sampledPropertyOrdinals.length; i++) {
            sampled[i] = allProps[sampledPropertyOrdinals[i]];
        }

        List<TrackableObject> objects = collectChecksumObjects(gameView);
        for (TrackableObject obj : objects) {
            if (obj.getProps() == null) continue;
            Map<TrackableProperty, Object> objSnap =
                    snapshot != null ? snapshot.get(obj) : null;
            for (TrackableProperty prop : sampled) {
                Object value;
                if (objSnap != null) {
                    value = objSnap.get(prop);
                } else {
                    value = getEffectiveValue(obj, prop);
                }
                if (value != null && !value.equals(prop.getDefaultValue())) {
                    int propHash = hashPropertyValue(value);
                    hash = 31 * hash + prop.ordinal();
                    hash = 31 * hash + propHash;
                    if (divergenceLog != null) {
                        divergenceLog.add(obj.getClass().getSimpleName() + "#" + obj.getId()
                                + "." + prop.name() + "=" + propHash + " hash=" + hash);
                    }
                }
            }
        }

        return hash;
    }

    /**
     * Convert sampled property ordinals to a human-readable string of property names.
     */
    public static String sampledPropertyNames(int[] ordinals) {
        if (ordinals == null || ordinals.length == 0) {
            return "[]";
        }
        TrackableProperty[] allProps = getAllProperties();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ordinals.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(allProps[ordinals[i]].name());
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Get players sorted by ID for consistent iteration order.
     * Required for deterministic checksum computation across server and client.
     *
     * @param gameView the game view
     * @return sorted list, or empty list if no players
     */
    public static List<PlayerView> getSortedPlayers(GameView gameView) {
        if (gameView == null || gameView.getPlayers() == null) {
            return new ArrayList<>();
        }
        return getSortedPlayers(gameView.getPlayers());
    }

    /**
     * Get players sorted by ID for consistent iteration order.
     *
     * @param players the players iterable
     * @return sorted list
     */
    public static List<PlayerView> getSortedPlayers(Iterable<PlayerView> players) {
        List<PlayerView> sorted = new ArrayList<>();
        for (PlayerView p : players) {
            sorted.add(p);
        }
        sorted.sort(Comparator.comparingInt(PlayerView::getId));
        return sorted;
    }
}
