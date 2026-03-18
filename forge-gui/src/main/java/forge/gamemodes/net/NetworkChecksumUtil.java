package forge.gamemodes.net;

import forge.card.mana.ManaAtom;
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
        if (gameView == null) {
            return computeStateChecksum(turn, phaseOrdinal, (Iterable<PlayerView>) null);
        }
        int hash = computeStateChecksum(turn, phaseOrdinal, gameView.getPlayers());

        for (PlayerView player : getSortedPlayers(gameView)) {
            hash = 31 * hash + player.getZoneSize(ZoneType.Hand);
            hash = 31 * hash + player.getZoneSize(ZoneType.Battlefield);
            hash = 31 * hash + player.getZoneSize(ZoneType.Graveyard);
            hash = 31 * hash + player.getZoneSize(ZoneType.Exile);
            hash = 31 * hash + player.getZoneSize(ZoneType.Command);

            Object manaObj = getEffectiveValue(player, TrackableProperty.Mana);
            int manaHash = 0;
            if (manaObj instanceof int[] manaPool) {
                for (int m : manaPool) manaHash += m;
            } else {
                for (byte manaType : ManaAtom.MANATYPES) {
                    manaHash += player.getMana(manaType);
                }
            }
            hash = 31 * hash + manaHash;
        }

        // Deep checksum: battlefield card properties (sorted by ID for determinism)
        List<CardView> allBattlefieldCards = new ArrayList<>();
        for (PlayerView player : getSortedPlayers(gameView)) {
            if (player.getBattlefield() != null) {
                for (CardView card : player.getBattlefield()) {
                    allBattlefieldCards.add(card);
                }
            }
        }
        allBattlefieldCards.sort(Comparator.comparingInt(CardView::getId));

        for (CardView card : allBattlefieldCards) {
            hash = 31 * hash + card.getId();
            hash = 31 * hash + (Boolean.TRUE.equals(getEffectiveValue(card, TrackableProperty.Tapped)) ? 1 : 0);
            CardView.CardStateView state = card.getCurrentState();
            if (state != null) {
                Object pow = getEffectiveValue(state, TrackableProperty.Power);
                Object tou = getEffectiveValue(state, TrackableProperty.Toughness);
                hash = 31 * hash + (pow instanceof Integer ? (int) pow : 0);
                hash = 31 * hash + (tou instanceof Integer ? (int) tou : 0);
            }
            Object zone = getEffectiveValue(card, TrackableProperty.Zone);
            hash = 31 * hash + (zone instanceof ZoneType ? ((ZoneType) zone).ordinal() : -1);
            Object ctrl = getEffectiveValue(card, TrackableProperty.Controller);
            hash = 31 * hash + (ctrl instanceof PlayerView ? ((PlayerView) ctrl).getId() : -1);

            Object countersObj = getEffectiveValue(card, TrackableProperty.Counters);
            int counterTotal = 0;
            if (countersObj instanceof Map) {
                for (Object count : ((Map<?, ?>) countersObj).values()) {
                    if (count instanceof Integer) counterTotal += (int) count;
                }
            }
            hash = 31 * hash + counterTotal;

            hash = 31 * hash + (Boolean.TRUE.equals(getEffectiveValue(card, TrackableProperty.Sickness)) ? 1 : 0);
            hash = 31 * hash + (Boolean.TRUE.equals(getEffectiveValue(card, TrackableProperty.Attacking)) ? 1 : 0);
            hash = 31 * hash + (Boolean.TRUE.equals(getEffectiveValue(card, TrackableProperty.Blocking)) ? 1 : 0);
            hash = 31 * hash + (Boolean.TRUE.equals(getEffectiveValue(card, TrackableProperty.PhasedOut)) ? 1 : 0);
            hash = 31 * hash + (Boolean.TRUE.equals(getEffectiveValue(card, TrackableProperty.Facedown)) ? 1 : 0);
            hash = 31 * hash + (Boolean.TRUE.equals(getEffectiveValue(card, TrackableProperty.Flipped)) ? 1 : 0);
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

    /**
     * Compute a diagnostic breakdown of the deep checksum, showing the hash
     * after each component. Used to identify exactly where server/client diverge.
     */
    public static String computeChecksumBreakdown(int turn, int phaseOrdinal, GameView gameView) {
        if (gameView == null || !stableChecksum) {
            return "deep checksum disabled or gameView null";
        }
        // This must mirror computeStateChecksum exactly (using getEffectiveValue)
        // so that breakdown final == actual checksum, enabling accurate mismatch diagnosis.
        StringBuilder sb = new StringBuilder();
        int hash = computeStateChecksum(turn, phaseOrdinal, gameView.getPlayers());
        sb.append("base=").append(hash);

        for (PlayerView player : getSortedPlayers(gameView)) {
            int zoneHash = hash;
            zoneHash = 31 * zoneHash + player.getZoneSize(ZoneType.Hand);
            zoneHash = 31 * zoneHash + player.getZoneSize(ZoneType.Battlefield);
            zoneHash = 31 * zoneHash + player.getZoneSize(ZoneType.Graveyard);
            zoneHash = 31 * zoneHash + player.getZoneSize(ZoneType.Exile);
            zoneHash = 31 * zoneHash + player.getZoneSize(ZoneType.Command);

            Object manaObj = getEffectiveValue(player, TrackableProperty.Mana);
            int manaHash = 0;
            if (manaObj instanceof int[] manaPool) {
                for (int m : manaPool) manaHash += m;
            } else {
                for (byte manaType : ManaAtom.MANATYPES) {
                    manaHash += player.getMana(manaType);
                }
            }
            zoneHash = 31 * zoneHash + manaHash;
            hash = zoneHash;
            sb.append(" | p").append(player.getId()).append("zones=").append(hash);
            sb.append("(H").append(player.getZoneSize(ZoneType.Hand));
            sb.append("B").append(player.getZoneSize(ZoneType.Battlefield));
            sb.append("G").append(player.getZoneSize(ZoneType.Graveyard));
            sb.append("E").append(player.getZoneSize(ZoneType.Exile));
            sb.append("C").append(player.getZoneSize(ZoneType.Command));
            sb.append("M").append(manaHash).append(")");
        }

        List<CardView> allBattlefieldCards = new ArrayList<>();
        for (PlayerView player : getSortedPlayers(gameView)) {
            if (player.getBattlefield() != null) {
                for (CardView card : player.getBattlefield()) {
                    allBattlefieldCards.add(card);
                }
            }
        }
        allBattlefieldCards.sort(Comparator.comparingInt(CardView::getId));
        sb.append(" | cards(").append(allBattlefieldCards.size()).append(")=[");
        for (CardView card : allBattlefieldCards) {
            boolean tapped = Boolean.TRUE.equals(getEffectiveValue(card, TrackableProperty.Tapped));
            CardView.CardStateView state = card.getCurrentState();
            int power = 0, toughness = 0;
            if (state != null) {
                Object pow = getEffectiveValue(state, TrackableProperty.Power);
                Object tou = getEffectiveValue(state, TrackableProperty.Toughness);
                power = pow instanceof Integer ? (int) pow : 0;
                toughness = tou instanceof Integer ? (int) tou : 0;
            }
            Object zoneObj = getEffectiveValue(card, TrackableProperty.Zone);
            int zoneOrd = zoneObj instanceof ZoneType ? ((ZoneType) zoneObj).ordinal() : -1;
            Object ctrlObj = getEffectiveValue(card, TrackableProperty.Controller);
            int ctrlId = ctrlObj instanceof PlayerView ? ((PlayerView) ctrlObj).getId() : -1;
            Object countersObj = getEffectiveValue(card, TrackableProperty.Counters);
            int ct = 0;
            if (countersObj instanceof Map) {
                for (Object v : ((Map<?, ?>) countersObj).values()) {
                    if (v instanceof Integer) ct += (int) v;
                }
            }
            boolean sick = Boolean.TRUE.equals(getEffectiveValue(card, TrackableProperty.Sickness));
            boolean attacking = Boolean.TRUE.equals(getEffectiveValue(card, TrackableProperty.Attacking));
            boolean blocking = Boolean.TRUE.equals(getEffectiveValue(card, TrackableProperty.Blocking));
            boolean phasedOut = Boolean.TRUE.equals(getEffectiveValue(card, TrackableProperty.PhasedOut));
            boolean faceDown = Boolean.TRUE.equals(getEffectiveValue(card, TrackableProperty.Facedown));
            boolean flipped = Boolean.TRUE.equals(getEffectiveValue(card, TrackableProperty.Flipped));

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

        // Hash cards using same getEffectiveValue path as computeStateChecksum
        for (CardView card : allBattlefieldCards) {
            hash = 31 * hash + card.getId();
            hash = 31 * hash + (Boolean.TRUE.equals(getEffectiveValue(card, TrackableProperty.Tapped)) ? 1 : 0);
            CardView.CardStateView state = card.getCurrentState();
            if (state != null) {
                Object pow = getEffectiveValue(state, TrackableProperty.Power);
                Object tou = getEffectiveValue(state, TrackableProperty.Toughness);
                hash = 31 * hash + (pow instanceof Integer ? (int) pow : 0);
                hash = 31 * hash + (tou instanceof Integer ? (int) tou : 0);
            }
            Object zone = getEffectiveValue(card, TrackableProperty.Zone);
            hash = 31 * hash + (zone instanceof ZoneType ? ((ZoneType) zone).ordinal() : -1);
            Object ctrl = getEffectiveValue(card, TrackableProperty.Controller);
            hash = 31 * hash + (ctrl instanceof PlayerView ? ((PlayerView) ctrl).getId() : -1);
            Object countersObj = getEffectiveValue(card, TrackableProperty.Counters);
            int counterTotal = 0;
            if (countersObj instanceof Map) {
                for (Object count : ((Map<?, ?>) countersObj).values()) {
                    if (count instanceof Integer) counterTotal += (int) count;
                }
            }
            hash = 31 * hash + counterTotal;
            hash = 31 * hash + (Boolean.TRUE.equals(getEffectiveValue(card, TrackableProperty.Sickness)) ? 1 : 0);
            hash = 31 * hash + (Boolean.TRUE.equals(getEffectiveValue(card, TrackableProperty.Attacking)) ? 1 : 0);
            hash = 31 * hash + (Boolean.TRUE.equals(getEffectiveValue(card, TrackableProperty.Blocking)) ? 1 : 0);
            hash = 31 * hash + (Boolean.TRUE.equals(getEffectiveValue(card, TrackableProperty.PhasedOut)) ? 1 : 0);
            hash = 31 * hash + (Boolean.TRUE.equals(getEffectiveValue(card, TrackableProperty.Facedown)) ? 1 : 0);
            hash = 31 * hash + (Boolean.TRUE.equals(getEffectiveValue(card, TrackableProperty.Flipped)) ? 1 : 0);
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
