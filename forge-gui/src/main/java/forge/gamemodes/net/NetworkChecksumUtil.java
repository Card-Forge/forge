package forge.gamemodes.net;

import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.combat.CombatView;
import forge.game.phase.PhaseType;
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
import java.util.Collections;
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
     * When true, checksums include battlefield fixed card properties, zone sizes,
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
     * Stable checksum including zone sizes, mana, battlefield cards,
     * combat state, and stack. Used when stableChecksum is enabled.
     */
    public static int computeStateChecksum(int turn, int phaseOrdinal, GameView gameView) {
        if (gameView == null) {
            return computeStateChecksum(turn, phaseOrdinal, (Iterable<PlayerView>) null);
        }
        int hash = 17;
        hash = 31 * hash + turn;
        if (phaseOrdinal >= 0) {
            hash = 31 * hash + phaseOrdinal;
        }
        List<PlayerView> sortedPlayers = getSortedPlayers(gameView);
        for (PlayerView player : sortedPlayers) {
            hash = 31 * hash + player.getId();
            Object life = getEffectiveValue(player, TrackableProperty.Life);
            hash = 31 * hash + (life instanceof Integer ? (int) life : 0);
        }

        for (PlayerView player : sortedPlayers) {
            hash = 31 * hash + getZoneSize(player, TrackableProperty.Hand);
            hash = 31 * hash + getZoneSize(player, TrackableProperty.Battlefield);
            hash = 31 * hash + getZoneSize(player, TrackableProperty.Graveyard);
            hash = 31 * hash + getZoneSize(player, TrackableProperty.Exile);
            hash = 31 * hash + getZoneSize(player, TrackableProperty.Command);

            int manaHash = 0;
            Object manaObj = getEffectiveValue(player, TrackableProperty.Mana);
            if (manaObj instanceof Map) {
                for (Object v : ((Map<?, ?>) manaObj).values()) {
                    if (v instanceof Integer) manaHash += (int) v;
                }
            }
            hash = 31 * hash + manaHash;
        }

        // Battlefield card properties (sorted by ID for determinism)
        List<CardView> allBattlefieldCards = new ArrayList<>();
        for (PlayerView player : getSortedPlayers(gameView)) {
            Object bf = getEffectiveValue(player, TrackableProperty.Battlefield);
            if (bf instanceof Iterable<?>) {
                for (Object item : (Iterable<?>) bf) {
                    if (item instanceof CardView cv) allBattlefieldCards.add(cv);
                }
            }
        }
        allBattlefieldCards.sort(Comparator.comparingInt(CardView::getId));

        for (CardView card : allBattlefieldCards) {
            hash = 31 * hash + card.getId();
            hash = 31 * hash + (Boolean.TRUE.equals(getEffectiveValue(card, TrackableProperty.Tapped)) ? 1 : 0);
            Object stateObj = getEffectiveValue(card, TrackableProperty.CurrentState);
            CardView.CardStateView state = stateObj instanceof CardView.CardStateView ? (CardView.CardStateView) stateObj : null;
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

        Object combatObj = getEffectiveValue(gameView, TrackableProperty.CombatView);
        if (combatObj instanceof CombatView combat) {
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

        Object stackObj = getEffectiveValue(gameView, TrackableProperty.Stack);
        if (stackObj instanceof TrackableCollection<?>) {
            hash = 31 * hash + ((TrackableCollection<?>) stackObj).size();
        }

        return hash;
    }

    private static int getZoneSize(PlayerView player, TrackableProperty zoneProp) {
        Object val = getEffectiveValue(player, zoneProp);
        return val instanceof TrackableCollection<?> tc ? tc.size() : 0;
    }

    /**
     * Compute a diagnostic breakdown of the stable checksum, showing the hash
     * after each component. Used to identify exactly where server/client diverge.
     */
    public static String computeChecksumBreakdown(int turn, int phaseOrdinal, GameView gameView) {
        if (gameView == null || !stableChecksum) {
            return "stable checksum disabled or gameView null";
        }
        // This must mirror computeStateChecksum exactly so that
        // breakdown final == actual checksum, enabling accurate mismatch diagnosis
        StringBuilder sb = new StringBuilder();
        int hash = 17;
        hash = 31 * hash + turn;
        if (phaseOrdinal >= 0) {
            hash = 31 * hash + phaseOrdinal;
        }
        List<PlayerView> sortedPlayers = getSortedPlayers(gameView);
        for (PlayerView player : sortedPlayers) {
            hash = 31 * hash + player.getId();
            Object life = getEffectiveValue(player, TrackableProperty.Life);
            hash = 31 * hash + (life instanceof Integer ? (int) life : 0);
        }
        sb.append("base=").append(hash);

        for (PlayerView player : sortedPlayers) {
            int zoneHash = hash;
            zoneHash = 31 * zoneHash + getZoneSize(player, TrackableProperty.Hand);
            zoneHash = 31 * zoneHash + getZoneSize(player, TrackableProperty.Battlefield);
            zoneHash = 31 * zoneHash + getZoneSize(player, TrackableProperty.Graveyard);
            zoneHash = 31 * zoneHash + getZoneSize(player, TrackableProperty.Exile);
            zoneHash = 31 * zoneHash + getZoneSize(player, TrackableProperty.Command);

            int manaHash = 0;
            Object manaObj = getEffectiveValue(player, TrackableProperty.Mana);
            if (manaObj instanceof Map) {
                for (Object v : ((Map<?, ?>) manaObj).values()) {
                    if (v instanceof Integer) manaHash += (int) v;
                }
            }
            zoneHash = 31 * zoneHash + manaHash;
            hash = zoneHash;
            sb.append(" | p").append(player.getId()).append("zones=").append(hash);
            sb.append("(H").append(getZoneSize(player, TrackableProperty.Hand));
            sb.append("B").append(getZoneSize(player, TrackableProperty.Battlefield));
            sb.append("G").append(getZoneSize(player, TrackableProperty.Graveyard));
            sb.append("E").append(getZoneSize(player, TrackableProperty.Exile));
            sb.append("C").append(getZoneSize(player, TrackableProperty.Command));
            sb.append("M").append(manaHash).append(")");
        }

        List<CardView> allBattlefieldCards = new ArrayList<>();
        for (PlayerView player : getSortedPlayers(gameView)) {
            Object bf = getEffectiveValue(player, TrackableProperty.Battlefield);
            if (bf instanceof Iterable<?>) {
                for (Object item : (Iterable<?>) bf) {
                    if (item instanceof CardView cv) allBattlefieldCards.add(cv);
                }
            }
        }
        allBattlefieldCards.sort(Comparator.comparingInt(CardView::getId));
        sb.append(" | cards(").append(allBattlefieldCards.size()).append(")=[");
        for (CardView card : allBattlefieldCards) {
            boolean tapped = Boolean.TRUE.equals(getEffectiveValue(card, TrackableProperty.Tapped));
            Object stateObj = getEffectiveValue(card, TrackableProperty.CurrentState);
            CardView.CardStateView state = stateObj instanceof CardView.CardStateView ? (CardView.CardStateView) stateObj : null;
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

        // Hash cards using same path as computeStateChecksum
        for (CardView card : allBattlefieldCards) {
            hash = 31 * hash + card.getId();
            hash = 31 * hash + (Boolean.TRUE.equals(getEffectiveValue(card, TrackableProperty.Tapped)) ? 1 : 0);
            Object stObj = getEffectiveValue(card, TrackableProperty.CurrentState);
            CardView.CardStateView state = stObj instanceof CardView.CardStateView ? (CardView.CardStateView) stObj : null;
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

        Object combatObj = getEffectiveValue(gameView, TrackableProperty.CombatView);
        CombatView combat = combatObj instanceof CombatView ? (CombatView) combatObj : null;
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

        Object stackObj = getEffectiveValue(gameView, TrackableProperty.Stack);
        int stackSize = stackObj instanceof TrackableCollection<?> ? ((TrackableCollection<?>) stackObj).size() : 0;
        if (stackObj instanceof TrackableCollection<?>) {
            hash = 31 * hash + stackSize;
        }
        sb.append(" | stack=").append(stackSize);
        sb.append(" final=").append(hash);

        return sb.toString();
    }

    /** Cached arrays to avoid repeated allocation from values(). */
    private static Set<TrackableProperty> eligibleProperties = null;
    private static TrackableProperty[] allProperties = null;

    /**
     * Get all TrackableProperty values eligible for sampled checksum hashing.
     * Excludes properties whose types are either:
     * <ul>
     *   <li>{@code CardStateViewType} — nested child objects of CardView,
     *       already traversed individually in {@link #collectChecksumObjects}</li>
     *   <li>{@code StackItemViewListType} — stack items are already traversed
     *       individually in {@link #collectChecksumObjects}</li>
     *   <li>{@code CombatViewType} — complex structure (attacker bands with
     *       blockers); handled specially in the stable checksum path</li>
     *   <li>{@code IPaperCardType} — server-side card definition reference,
     *       not sent to or used by the client</li>
     * </ul>
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
            addCards(cards, getEffectiveValue(player, TrackableProperty.Battlefield));
            addCards(cards, getEffectiveValue(player, TrackableProperty.Hand));
            addCards(cards, getEffectiveValue(player, TrackableProperty.Graveyard));
            addCards(cards, getEffectiveValue(player, TrackableProperty.Exile));
            addCards(cards, getEffectiveValue(player, TrackableProperty.Command));
            cards.sort(Comparator.comparingInt(CardView::getId));

            for (CardView card : cards) {
                objects.add(card);
                Object state = getEffectiveValue(card, TrackableProperty.CurrentState);
                if (state instanceof TrackableObject to) objects.add(to);
                Object alt = getEffectiveValue(card, TrackableProperty.AlternateState);
                if (alt instanceof TrackableObject to) objects.add(to);
                Object left = getEffectiveValue(card, TrackableProperty.LeftSplitState);
                if (left instanceof TrackableObject to) objects.add(to);
                Object right = getEffectiveValue(card, TrackableProperty.RightSplitState);
                if (right instanceof TrackableObject to) objects.add(to);
            }
        }

        Object stackObj = getEffectiveValue(gameView, TrackableProperty.Stack);
        if (stackObj instanceof Iterable<?>) {
            List<StackItemView> stackItems = new ArrayList<>();
            for (Object item : (Iterable<?>) stackObj) {
                if (item instanceof StackItemView siv) stackItems.add(siv);
            }
            stackItems.sort(Comparator.comparingInt(StackItemView::getId));
            objects.addAll(stackItems);
        }

        return objects;
    }

    private static void addCards(List<CardView> cards, Object zoneValue) {
        if (zoneValue instanceof Iterable<?>) {
            for (Object item : (Iterable<?>) zoneValue) {
                if (item instanceof CardView cv) cards.add(cv);
            }
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
        Object value = ((Map<TrackableProperty, Object>) obj.getProps()).get(prop);
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
     * Must produce deterministic hashes for the same logical value across
     * different Java object instances (server vs client deserialized).
     * Types without a content-based hashCode() need explicit handling here.
     */
    static int hashPropertyValue(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof TrackableObject to) {
            return to.getId();
        }
        if (value instanceof TrackableCollection tc) {
            List<Integer> ids = new ArrayList<>(tc.size());
            for (Object item : tc) {
                ids.add(item instanceof TrackableObject to ? to.getId() : Objects.hashCode(item));
            }
            ids.sort(null);
            return ids.hashCode();
        }
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            List<Integer> entryHashes = new ArrayList<>(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                entryHashes.add(31 * hashPropertyValue(entry.getKey()) + Objects.hashCode(entry.getValue()));
            }
            entryHashes.sort(null);
            return entryHashes.hashCode();
        }
        if (value instanceof Enum<?> e) {
            return e.ordinal();
        }
        // KeywordCollectionView has no hashCode or content-based toString — hash by keyword strings
        if (value instanceof forge.game.keyword.KeywordCollection.KeywordCollectionView kcv) {
            List<String> kws = kcv.asStringList();
            Collections.sort(kws);
            return kws.hashCode();
        }
        // Fallback: use toString() for content-based hashing. Covers types like CardType
        // and ManaCost that have content-based toString() but no hashCode() override.
        return value.toString().hashCode();
    }

    /**
     * Compute a sampled checksum over a dynamic set of TrackableProperties.
     *
     * @param gameView the game view to checksum
     * @param sampledPropertyOrdinals ordinals of TrackableProperty values to sample
     * @param divergenceLog if non-null, logs per-property hash contributions for mismatch diagnosis
     * @return checksum value
     */
    public static int computeSampledChecksum(GameView gameView, int[] sampledPropertyOrdinals,
                                              List<String> divergenceLog) {
        Object turnObj = getEffectiveValue(gameView, TrackableProperty.Turn);
        int turn = turnObj instanceof Integer ? (int) turnObj : 0;
        Object phaseObj = getEffectiveValue(gameView, TrackableProperty.Phase);
        int phaseOrdinal = phaseObj instanceof PhaseType ? ((PhaseType) phaseObj).ordinal() : -1;

        if (stableChecksum) {
            return computeStateChecksum(turn, phaseOrdinal, gameView);
        }

        // Base hash: turn + phase + player life
        int hash = 17;
        hash = 31 * hash + turn;
        if (phaseOrdinal >= 0) {
            hash = 31 * hash + phaseOrdinal;
        }
        if (gameView.getPlayers() != null) {
            for (PlayerView player : getSortedPlayers(gameView.getPlayers())) {
                hash = 31 * hash + player.getId();
                Object life = getEffectiveValue(player, TrackableProperty.Life);
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
