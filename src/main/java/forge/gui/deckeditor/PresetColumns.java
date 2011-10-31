package forge.gui.deckeditor;

import java.util.Map.Entry;

import net.slightlymagic.braids.util.lambda.Lambda1;
import forge.SetUtils;
import forge.card.CardColor;
import forge.card.CardManaCost;
import forge.card.CardRarity;
import forge.card.CardSet;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.InventoryItemFromSet;

/**
 * TODO: Write javadoc for this type.
 * 
 */
public abstract class PresetColumns {

    private static CardManaCost toManaCost(final InventoryItem i) {
        return i instanceof CardPrinted ? ((CardPrinted) i).getCard().getManaCost() : CardManaCost.EMPTY;
    }

    private static CardColor toColor(final InventoryItem i) {
        return i instanceof CardPrinted ? ((CardPrinted) i).getCard().getColor() : CardColor.getNullColor();
    }

    private static String toPTL(final InventoryItem i) {
        return i instanceof CardPrinted ? ((CardPrinted) i).getCard().getPTorLoyalty() : "";
    }

    private static CardRarity toRarity(final InventoryItem i) {
        return i instanceof CardPrinted ? ((CardPrinted) i).getRarity() : CardRarity.Unknown;
    }

    private static CardSet toSetCmp(final InventoryItem i) {
        return i instanceof InventoryItemFromSet ? SetUtils.getSetByCode(((InventoryItemFromSet) i).getSet())
                : CardSet.UNKNOWN;
    }

    private static String toSetStr(final InventoryItem i) {
        return i instanceof InventoryItemFromSet ? ((InventoryItemFromSet) i).getSet() : "n/a";
    }

    private static Integer toAiCmp(final InventoryItem i) {
        return i instanceof CardPrinted ? ((CardPrinted) i).getCard().getAiStatusComparable() : Integer.valueOf(-1);
    }

    private static String toAiStr(final InventoryItem i) {
        return i instanceof CardPrinted ? ((CardPrinted) i).getCard().getAiStatus() : "n/a";
    }

    /** The Constant fnQtyCompare. */
    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<InventoryItem, Integer>> FN_QTY_COMPARE = new Lambda1<Comparable, Entry<InventoryItem, Integer>>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return from.getValue();
        }
    };

    /** The Constant fnQtyGet. */
    public static final Lambda1<Object, Entry<InventoryItem, Integer>> FN_QTY_GET = new Lambda1<Object, Entry<InventoryItem, Integer>>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return from.getValue();
        }
    };

    /** The Constant fnNameCompare. */
    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<InventoryItem, Integer>> FN_NAME_COMPARE = new Lambda1<Comparable, Entry<InventoryItem, Integer>>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return from.getKey().getName();
        }
    };

    /** The Constant fnNameGet. */
    public static final Lambda1<Object, Entry<InventoryItem, Integer>> FN_NAME_GET = new Lambda1<Object, Entry<InventoryItem, Integer>>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return from.getKey().getName();
        }
    };

    /** The Constant fnCostCompare. */
    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<InventoryItem, Integer>> FN_COST_COMPARE = new Lambda1<Comparable, Entry<InventoryItem, Integer>>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return PresetColumns.toManaCost(from.getKey());
        }
    };

    /** The Constant fnCostGet. */
    public static final Lambda1<Object, Entry<InventoryItem, Integer>> FN_COST_GET = new Lambda1<Object, Entry<InventoryItem, Integer>>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return PresetColumns.toManaCost(from.getKey());
        }
    };

    /** The Constant fnColorCompare. */
    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<InventoryItem, Integer>> FN_COLOR_COMPARE = new Lambda1<Comparable, Entry<InventoryItem, Integer>>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return PresetColumns.toColor(from.getKey());
        }
    };

    /** The Constant fnColorGet. */
    public static final Lambda1<Object, Entry<InventoryItem, Integer>> FN_COLOR_GET = new Lambda1<Object, Entry<InventoryItem, Integer>>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return PresetColumns.toColor(from.getKey());
        }
    };

    /** The Constant fnTypeCompare. */
    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<InventoryItem, Integer>> FN_TYPE_COMPARE = new Lambda1<Comparable, Entry<InventoryItem, Integer>>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return from.getKey().getType();
        }
    };

    /** The Constant fnTypeGet. */
    public static final Lambda1<Object, Entry<InventoryItem, Integer>> FN_TYPE_GET = new Lambda1<Object, Entry<InventoryItem, Integer>>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return from.getKey().getType();
        }
    };

    /** The Constant fnStatsCompare. */
    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<InventoryItem, Integer>> FN_STATS_COMPARE = new Lambda1<Comparable, Entry<InventoryItem, Integer>>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return PresetColumns.toPTL(from.getKey());
        }
    };

    /** The Constant fnStatsGet. */
    public static final Lambda1<Object, Entry<InventoryItem, Integer>> FN_STATS_GET = new Lambda1<Object, Entry<InventoryItem, Integer>>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return PresetColumns.toPTL(from.getKey());
        }
    };

    /** The Constant fnRarityCompare. */
    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<InventoryItem, Integer>> FN_RARITY_COMPARE = new Lambda1<Comparable, Entry<InventoryItem, Integer>>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return PresetColumns.toRarity(from.getKey());
        }
    };

    /** The Constant fnRarityGet. */
    public static final Lambda1<Object, Entry<InventoryItem, Integer>> FN_RARITY_GET = new Lambda1<Object, Entry<InventoryItem, Integer>>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return PresetColumns.toRarity(from.getKey());
        }
    };

    /** The Constant fnSetCompare. */
    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<InventoryItem, Integer>> FN_SET_COMPARE = new Lambda1<Comparable, Entry<InventoryItem, Integer>>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return PresetColumns.toSetCmp(from.getKey());
        }
    };

    /** The Constant fnSetGet. */
    public static final Lambda1<Object, Entry<InventoryItem, Integer>> FN_SET_GET = new Lambda1<Object, Entry<InventoryItem, Integer>>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return PresetColumns.toSetStr(from.getKey());
        }
    };

    /** The Constant fnAiStatusCompare. */
    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<InventoryItem, Integer>> FN_AI_STATUS_COMPARE = new Lambda1<Comparable, Entry<InventoryItem, Integer>>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return PresetColumns.toAiCmp(from.getKey());
        }
    };

    /** The Constant fnAiStatusGet. */
    public static final Lambda1<Object, Entry<InventoryItem, Integer>> FN_AI_STATUS_GET = new Lambda1<Object, Entry<InventoryItem, Integer>>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return PresetColumns.toAiStr(from.getKey());
        }
    };
}
