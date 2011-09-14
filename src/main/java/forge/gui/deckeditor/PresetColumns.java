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

    private static CardManaCost toManaCost(InventoryItem i) { return i instanceof CardPrinted ? ((CardPrinted) i).getCard().getManaCost() : CardManaCost.empty; }
    private static CardColor toColor(InventoryItem i) { return i instanceof CardPrinted ? ((CardPrinted) i).getCard().getColor() : CardColor.nullColor; }
    private static String toType(InventoryItem i) { return i instanceof CardPrinted ? ((CardPrinted) i).getCard().getType().toString() : i.getClass().toString(); }
    private static String toPTL(InventoryItem i) { return i instanceof CardPrinted ? ((CardPrinted) i).getCard().getPTorLoyalty() : ""; }
    private static CardRarity toRarity(InventoryItem i) { return i instanceof CardPrinted ? ((CardPrinted) i).getRarity() : CardRarity.Unknown; }
    private static CardSet toSetCmp(InventoryItem i) { return i instanceof InventoryItemFromSet ? SetUtils.getSetByCode(((InventoryItemFromSet) i).getSet()) : CardSet.unknown; }
    private static String toSetStr(InventoryItem i) { return i instanceof InventoryItemFromSet ? ((InventoryItemFromSet) i).getSet() : "n/a"; }
    private static Integer toAiCmp(InventoryItem i) { return i instanceof CardPrinted ? ((CardPrinted) i).getCard().getAiStatusComparable() : Integer.valueOf(-1); }
    private static String toAiStr(InventoryItem i) { return i instanceof CardPrinted ? ((CardPrinted) i).getCard().getAiStatus() : "n/a"; }
    
    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<InventoryItem, Integer>> fnQtyCompare =
        new Lambda1<Comparable, Entry<InventoryItem, Integer>>() { @Override
            public Comparable apply(final Entry<InventoryItem, Integer> from) { return from.getValue(); } };
    public static final Lambda1<Object, Entry<InventoryItem, Integer>> fnQtyGet =
        new Lambda1<Object, Entry<InventoryItem, Integer>>() { @Override
            public Object apply(final Entry<InventoryItem, Integer> from) { return from.getValue(); } };

    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<InventoryItem, Integer>> fnNameCompare =
        new Lambda1<Comparable, Entry<InventoryItem, Integer>>() { @Override
            public Comparable apply(final Entry<InventoryItem, Integer> from) { return from.getKey().getName(); } };
    public static final Lambda1<Object, Entry<InventoryItem, Integer>> fnNameGet =
        new Lambda1<Object, Entry<InventoryItem, Integer>>() { @Override
            public Object apply(final Entry<InventoryItem, Integer> from) { return from.getKey().getName(); } };

    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<InventoryItem, Integer>> fnCostCompare =
        new Lambda1<Comparable, Entry<InventoryItem, Integer>>() { @Override
            public Comparable apply(final Entry<InventoryItem, Integer> from) { return toManaCost(from.getKey()); } };
    public static final Lambda1<Object, Entry<InventoryItem, Integer>> fnCostGet =
        new Lambda1<Object, Entry<InventoryItem, Integer>>() { @Override
            public Object apply(final Entry<InventoryItem, Integer> from) { return toManaCost(from.getKey()); } };

    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<InventoryItem, Integer>> fnColorCompare =
        new Lambda1<Comparable, Entry<InventoryItem, Integer>>() { @Override
            public Comparable apply(final Entry<InventoryItem, Integer> from) { return toColor(from.getKey()); } };
    public static final Lambda1<Object, Entry<InventoryItem, Integer>> fnColorGet =
        new Lambda1<Object, Entry<InventoryItem, Integer>>() { @Override
            public Object apply(final Entry<InventoryItem, Integer> from) { return toColor(from.getKey()); } };

    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<InventoryItem, Integer>> fnTypeCompare =
       new Lambda1<Comparable, Entry<InventoryItem, Integer>>() { @Override
            public Comparable apply(final Entry<InventoryItem, Integer> from) { return toType(from.getKey()); } };
   public static final Lambda1<Object, Entry<InventoryItem, Integer>> fnTypeGet =
       new Lambda1<Object, Entry<InventoryItem, Integer>>() { @Override
            public Object apply(final Entry<InventoryItem, Integer> from) { return toType(from.getKey()); } };

    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<InventoryItem, Integer>> fnStatsCompare =
        new Lambda1<Comparable, Entry<InventoryItem, Integer>>() { @Override
            public Comparable apply(final Entry<InventoryItem, Integer> from) { return toPTL(from.getKey()); } };
    public static final Lambda1<Object, Entry<InventoryItem, Integer>> fnStatsGet =
        new Lambda1<Object, Entry<InventoryItem, Integer>>() { @Override
            public Object apply(final Entry<InventoryItem, Integer> from) { return toPTL(from.getKey()); } };

    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<InventoryItem, Integer>> fnRarityCompare =
        new Lambda1<Comparable, Entry<InventoryItem, Integer>>() { @Override
            public Comparable apply(final Entry<InventoryItem, Integer> from) { return toRarity(from.getKey()); } };
    public static final Lambda1<Object, Entry<InventoryItem, Integer>> fnRarityGet =
        new Lambda1<Object, Entry<InventoryItem, Integer>>() { @Override
            public Object apply(final Entry<InventoryItem, Integer> from) { return toRarity(from.getKey()); } };

    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<InventoryItem, Integer>> fnSetCompare =
        new Lambda1<Comparable, Entry<InventoryItem, Integer>>() { @Override
            public Comparable apply(final Entry<InventoryItem, Integer> from) { return toSetCmp(from.getKey()); } };
    public static final Lambda1<Object, Entry<InventoryItem, Integer>> fnSetGet =
        new Lambda1<Object, Entry<InventoryItem, Integer>>() { @Override
            public Object apply(final Entry<InventoryItem, Integer> from) { return toSetStr(from.getKey()); } };

    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<InventoryItem, Integer>> fnAiStatusCompare =
        new Lambda1<Comparable, Entry<InventoryItem, Integer>>() { @Override
            public Comparable apply(final Entry<InventoryItem, Integer> from) { return toAiCmp(from.getKey()); } };
    public static final Lambda1<Object, Entry<InventoryItem, Integer>> fnAiStatusGet =
        new Lambda1<Object, Entry<InventoryItem, Integer>>() { @Override
            public Object apply(final Entry<InventoryItem, Integer> from) { return toAiStr(from.getKey()); } };
}
