package forge.deck;

import java.util.Map.Entry;

import forge.card.CardRules;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.item.PaperCard;

/**
 * Immutable-ish snapshot of the numbers shown on the mobile deck statistics screen.
 * Colour arrays are indexed in WUBRG + colourless order.
 */
public class DeckStatistics {
    public static final int CURVE_BUCKETS = 9; //0, 1, 2, ... 7, 8+
    public static final int COLOR_COUNT = 6;   //W U B R G C
    public static final byte[] COLOR_ORDER = {
            MagicColor.WHITE, MagicColor.BLUE, MagicColor.BLACK, MagicColor.RED, MagicColor.GREEN, MagicColor.COLORLESS
    };
    public static final String[] COLOR_SYMBOLS = {"W", "U", "B", "R", "G", "C"};

    public final int[] permanentCurve = new int[CURVE_BUCKETS];
    public final int[] spellCurve = new int[CURVE_BUCKETS];
    public final int[] pips = new int[COLOR_COUNT];
    public final int[] landSources = new int[COLOR_COUNT];

    public int totalCards;
    public int landCount;
    public int nonLandCount;
    public double averageCmc; //over non-land cards

    //each card is counted once, using the primary type below
    public int creatures, instants, sorceries, artifacts, enchantments, planeswalkers, battles, otherTypes;

    public int totalPips;
    public int totalLandSources;

    /** Builds statistics from the main deck (plus commander section, if any). */
    public static DeckStatistics fromDeck(Deck deck) {
        DeckStatistics stats = new DeckStatistics();
        if (deck == null) {
            return stats;
        }
        stats.add(deck.getMain());
        if (deck.has(DeckSection.Commander)) {
            stats.add(deck.get(DeckSection.Commander));
        }
        stats.finish();
        return stats;
    }

    public static DeckStatistics fromPool(Iterable<Entry<PaperCard, Integer>> pool) {
        DeckStatistics stats = new DeckStatistics();
        stats.add(pool);
        stats.finish();
        return stats;
    }

    private long cmcSum;

    private void add(Iterable<Entry<PaperCard, Integer>> pool) {
        if (pool == null) { return; }
        for (Entry<PaperCard, Integer> entry : pool) {
            PaperCard card = entry.getKey();
            int qty = entry.getValue();
            CardRules rules = card.getRules();
            if (rules == null || qty <= 0) { continue; }
            addCard(rules, qty);
        }
    }

    private void addCard(CardRules rules, int qty) {
        CardType type = rules.getType();
        totalCards += qty;

        if (type.isLand()) {
            landCount += qty;
            addLandSources(rules, qty);
            return;
        }

        nonLandCount += qty;
        ManaCost cost = rules.getManaCost();
        int cmc = cost.getCMC();
        cmcSum += (long) cmc * qty;

        int bucket = Math.min(cmc, CURVE_BUCKETS - 1);
        if (type.isInstant() || type.isSorcery()) {
            spellCurve[bucket] += qty;
        } else {
            permanentCurve[bucket] += qty;
        }

        int[] shards = cost.getColorShardCounts();
        for (int i = 0; i < COLOR_COUNT; i++) {
            pips[i] += shards[i] * qty;
        }

        if (type.isCreature()) { creatures += qty; }
        else if (type.isPlaneswalker()) { planeswalkers += qty; }
        else if (type.isBattle()) { battles += qty; }
        else if (type.isInstant()) { instants += qty; }
        else if (type.isSorcery()) { sorceries += qty; }
        else if (type.isArtifact()) { artifacts += qty; }
        else if (type.isEnchantment()) { enchantments += qty; }
        else { otherTypes += qty; }
    }

    private void addLandSources(CardRules rules, int qty) {
        ColorSet identity = rules.getColorIdentity();
        boolean any = false;
        for (int i = 0; i < 5; i++) {
            if (identity.hasAnyColor(COLOR_ORDER[i])) {
                landSources[i] += qty;
                any = true;
            }
        }
        if (!any) {
            landSources[5] += qty; //colourless land
        }
    }

    private void finish() {
        averageCmc = nonLandCount == 0 ? 0 : (double) cmcSum / nonLandCount;
        totalPips = 0;
        totalLandSources = 0;
        for (int i = 0; i < COLOR_COUNT; i++) {
            totalPips += pips[i];
            totalLandSources += landSources[i];
        }
    }

    public int maxCurveValue() {
        int max = 1;
        for (int i = 0; i < CURVE_BUCKETS; i++) {
            max = Math.max(max, permanentCurve[i] + spellCurve[i]);
        }
        return max;
    }

    public static int percent(int value, int total) {
        return total <= 0 ? 0 : Math.round(value * 100f / total);
    }
}
