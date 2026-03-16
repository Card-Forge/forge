package forge.gamemodes.net;

import forge.card.mana.ManaAtom;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.combat.CombatView;
import forge.game.player.PlayerView;
import forge.game.spellability.StackItemView;
import forge.game.zone.ZoneType;
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
 * Used by both server (DeltaSyncManager) and client (NetworkGuiGame)
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
    public static int computeStateChecksum(int turn, int phaseOrdinal, Iterable<PlayerView> players) {
        int hash = 17;
        hash = 31 * hash + turn;
        if (phaseOrdinal >= 0) {
            hash = 31 * hash + phaseOrdinal;
        }
        if (players != null) {
            for (PlayerView player : getSortedPlayers(players)) {
                hash = 31 * hash + player.getId();
                hash = 31 * hash + player.getLife();
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

        // Deep checksum: per-player zone sizes and mana
        for (PlayerView player : getSortedPlayers(gameView)) {
            hash = 31 * hash + player.getZoneSize(ZoneType.Hand);
            hash = 31 * hash + player.getZoneSize(ZoneType.Battlefield);
            hash = 31 * hash + player.getZoneSize(ZoneType.Graveyard);
            hash = 31 * hash + player.getZoneSize(ZoneType.Exile);
            hash = 31 * hash + player.getZoneSize(ZoneType.Command);

            // Mana pool hash
            int manaHash = 0;
            for (byte manaType : ManaAtom.MANATYPES) {
                manaHash += player.getMana(manaType);
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
            hash = 31 * hash + (card.isTapped() ? 1 : 0);
            if (card.getCurrentState() != null) {
                hash = 31 * hash + card.getCurrentState().getPower();
                hash = 31 * hash + card.getCurrentState().getToughness();
            }
            hash = 31 * hash + (card.getZone() != null ? card.getZone().ordinal() : -1);
            hash = 31 * hash + (card.getController() != null ? card.getController().getId() : -1);

            // Total counter count
            Map<?, Integer> counters = card.getCounters();
            int counterTotal = 0;
            if (counters != null) {
                for (Integer count : counters.values()) {
                    counterTotal += count;
                }
            }
            hash = 31 * hash + counterTotal;

            hash = 31 * hash + (card.isSick() ? 1 : 0);
            hash = 31 * hash + (card.isAttacking() ? 1 : 0);
            hash = 31 * hash + (card.isBlocking() ? 1 : 0);
            hash = 31 * hash + (card.isPhasedOut() ? 1 : 0);
            hash = 31 * hash + (card.isFaceDown() ? 1 : 0);
            hash = 31 * hash + (card.isFlipped() ? 1 : 0);
        }

        // Deep checksum: combat state
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

        // Deep checksum: stack size
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
            int manaHash = 0;
            for (byte manaType : ManaAtom.MANATYPES) {
                manaHash += player.getMana(manaType);
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
            sb.append(card.getId());
            sb.append(card.isTapped() ? "T" : "U");
            if (card.getCurrentState() != null) {
                sb.append(card.getCurrentState().getPower()).append("/").append(card.getCurrentState().getToughness());
            }
            // Show all hash-relevant properties
            sb.append("z").append(card.getZone() != null ? card.getZone().ordinal() : -1);
            sb.append("c").append(card.getController() != null ? card.getController().getId() : -1);
            Map<?, Integer> cntrs = card.getCounters();
            int ct = 0;
            if (cntrs != null) { for (Integer v : cntrs.values()) ct += v; }
            if (ct > 0) sb.append("k").append(ct);
            if (card.isSick()) sb.append("S");
            if (card.isAttacking()) sb.append("A");
            if (card.isBlocking()) sb.append("B");
            if (card.isPhasedOut()) sb.append("P");
            if (card.isFaceDown()) sb.append("D");
            if (card.isFlipped()) sb.append("F");
            sb.append(" ");
        }
        sb.append("]");

        // Recompute hash for cards
        for (CardView card : allBattlefieldCards) {
            hash = 31 * hash + card.getId();
            hash = 31 * hash + (card.isTapped() ? 1 : 0);
            if (card.getCurrentState() != null) {
                hash = 31 * hash + card.getCurrentState().getPower();
                hash = 31 * hash + card.getCurrentState().getToughness();
            }
            hash = 31 * hash + (card.getZone() != null ? card.getZone().ordinal() : -1);
            hash = 31 * hash + (card.getController() != null ? card.getController().getId() : -1);
            Map<?, Integer> counters = card.getCounters();
            int counterTotal = 0;
            if (counters != null) {
                for (Integer count : counters.values()) {
                    counterTotal += count;
                }
            }
            hash = 31 * hash + counterTotal;
            hash = 31 * hash + (card.isSick() ? 1 : 0);
            hash = 31 * hash + (card.isAttacking() ? 1 : 0);
            hash = 31 * hash + (card.isBlocking() ? 1 : 0);
            hash = 31 * hash + (card.isPhasedOut() ? 1 : 0);
            hash = 31 * hash + (card.isFaceDown() ? 1 : 0);
            hash = 31 * hash + (card.isFlipped() ? 1 : 0);
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

    /** Cached array of properties eligible for sampled checksum. */
    private static Set<TrackableProperty> eligibleProperties = null;

    /**
     * Get all TrackableProperty values whose types can be generically hashed.
     * Excludes container/nested types that require special traversal.
     */
    public static Set<TrackableProperty> getEligibleProperties() {
        if (eligibleProperties != null) {
            return eligibleProperties;
        }
        eligibleProperties = EnumSet.noneOf(TrackableProperty.class);
        for (TrackableProperty prop : TrackableProperty.values()) {
            TrackableType<?> type = prop.getType();
            // Exclude types that are nested containers or not meaningful for desync detection
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

    /**
     * Walk the GameView object graph in deterministic order and collect
     * all TrackableObjects whose properties should be sampled.
     * Order: GameView, Players (sorted by ID), per-player visible zone cards
     * (Battlefield, Hand, Graveyard, Exile, Command — sorted by ID),
     * per-card state views (Current, Alternate, Left, Right), StackItemViews.
     */
    public static List<TrackableObject> collectChecksumObjects(GameView gameView) {
        List<TrackableObject> objects = new ArrayList<>();
        if (gameView == null) {
            return objects;
        }

        // 1. GameView itself
        objects.add(gameView);

        // 2. Players sorted by ID
        List<PlayerView> players = getSortedPlayers(gameView);
        objects.addAll(players);

        // 3. Per-player visible zone cards, sorted by ID
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
                // 4. Card state views (skip nulls)
                addIfNotNull(objects, card.getCurrentState());
                addIfNotNull(objects, card.getAlternateState());
                addIfNotNull(objects, card.getLeftSplitState());
                addIfNotNull(objects, card.getRightSplitState());
            }
        }

        // 5. StackItemViews sorted by ID
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
     * Hash a property value generically for checksum purposes.
     * Handles TrackableObject references (by ID), TrackableCollections (sorted IDs),
     * Maps (sorted entries), and everything else via Objects.hashCode.
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
        return Objects.hashCode(value);
    }

    /**
     * Compute a sampled checksum over a dynamic set of TrackableProperties.
     * Starts with the core state checksum (turn + phase + player life), then
     * reads the specified properties from all objects in the game view graph.
     *
     * @param gameView the game view to checksum
     * @param sampledPropertyOrdinals ordinals of TrackableProperty values to sample
     * @return checksum value
     */
    public static int computeSampledChecksum(GameView gameView, int[] sampledPropertyOrdinals) {
        final int turn = gameView.getTurn();
        int phaseOrdinal = gameView.getPhase() != null ? gameView.getPhase().ordinal() : -1;
        if (stableChecksum) {
            return computeStateChecksum(turn, phaseOrdinal, gameView);
        }

        // Start with core hash
        int hash = computeStateChecksum(turn, phaseOrdinal, gameView.getPlayers());

        // Convert ordinals to properties
        TrackableProperty[] allProps = TrackableProperty.values();
        TrackableProperty[] sampled = new TrackableProperty[sampledPropertyOrdinals.length];
        for (int i = 0; i < sampledPropertyOrdinals.length; i++) {
            sampled[i] = allProps[sampledPropertyOrdinals[i]];
        }

        // Walk object graph and hash sampled properties
        List<TrackableObject> objects = collectChecksumObjects(gameView);
        for (TrackableObject obj : objects) {
            Map<TrackableProperty, Object> objProps = obj.getProps();
            if (objProps == null) continue;
            for (TrackableProperty prop : sampled) {
                Object value = objProps.get(prop);
                if (value != null && !value.equals(prop.getDefaultValue())) {
                    hash = 31 * hash + prop.ordinal();
                    hash = 31 * hash + hashPropertyValue(value);
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
        TrackableProperty[] allProps = TrackableProperty.values();
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
