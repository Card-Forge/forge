package forge.gui.deckeditor;

import java.util.Map.Entry;

import net.slightlymagic.braids.util.lambda.Lambda1;
import forge.SetUtils;
import forge.card.CardPrinted;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public abstract class PresetColumns {

    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<CardPrinted, Integer>> fnQtyCompare =
        new Lambda1<Comparable, Entry<CardPrinted, Integer>>() { @Override
            public Comparable apply(final Entry<CardPrinted, Integer> from) { return from.getValue(); } };
    public static final Lambda1<Object, Entry<CardPrinted, Integer>> fnQtyGet =
        new Lambda1<Object, Entry<CardPrinted, Integer>>() { @Override
            public Object apply(final Entry<CardPrinted, Integer> from) { return from.getValue(); } };

    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<CardPrinted, Integer>> fnNameCompare =
        new Lambda1<Comparable, Entry<CardPrinted, Integer>>() { @Override
            public Comparable apply(final Entry<CardPrinted, Integer> from) { return from.getKey().getName(); } };
    public static final Lambda1<Object, Entry<CardPrinted, Integer>> fnNameGet =
        new Lambda1<Object, Entry<CardPrinted, Integer>>() { @Override
            public Object apply(final Entry<CardPrinted, Integer> from) { return from.getKey().getName(); } };

    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<CardPrinted, Integer>> fnCostCompare =
        new Lambda1<Comparable, Entry<CardPrinted, Integer>>() { @Override
            public Comparable apply(final Entry<CardPrinted, Integer> from) { return from.getKey().getCard().getManaCost(); } };
    public static final Lambda1<Object, Entry<CardPrinted, Integer>> fnCostGet =
        new Lambda1<Object, Entry<CardPrinted, Integer>>() { @Override
            public Object apply(final Entry<CardPrinted, Integer> from) { return from.getKey().getCard().getManaCost(); } };

    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<CardPrinted, Integer>> fnColorCompare =
        new Lambda1<Comparable, Entry<CardPrinted, Integer>>() { @Override
            public Comparable apply(final Entry<CardPrinted, Integer> from) { return from.getKey().getCard().getColor(); } };
    public static final Lambda1<Object, Entry<CardPrinted, Integer>> fnColorGet =
        new Lambda1<Object, Entry<CardPrinted, Integer>>() { @Override
            public Object apply(final Entry<CardPrinted, Integer> from) { return from.getKey().getCard().getColor().toString(); } };

    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<CardPrinted, Integer>> fnTypeCompare =
       new Lambda1<Comparable, Entry<CardPrinted, Integer>>() { @Override
            public Comparable apply(final Entry<CardPrinted, Integer> from) { return from.getKey().getCard().getType(); } };
   public static final Lambda1<Object, Entry<CardPrinted, Integer>> fnTypeGet =
       new Lambda1<Object, Entry<CardPrinted, Integer>>() { @Override
            public Object apply(final Entry<CardPrinted, Integer> from) { return from.getKey().getCard().getType().toString(); } };

    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<CardPrinted, Integer>> fnStatsCompare =
        new Lambda1<Comparable, Entry<CardPrinted, Integer>>() { @Override
            public Comparable apply(final Entry<CardPrinted, Integer> from) { return from.getKey().getCard().getPTorLoyalty(); } };
    public static final Lambda1<Object, Entry<CardPrinted, Integer>> fnStatsGet =
        new Lambda1<Object, Entry<CardPrinted, Integer>>() { @Override
            public Object apply(final Entry<CardPrinted, Integer> from) { return from.getKey().getCard().getPTorLoyalty(); } };

    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<CardPrinted, Integer>> fnRarityCompare =
        new Lambda1<Comparable, Entry<CardPrinted, Integer>>() { @Override
            public Comparable apply(final Entry<CardPrinted, Integer> from) { return from.getKey().getRarity(); } };
    public static final Lambda1<Object, Entry<CardPrinted, Integer>> fnRarityGet =
        new Lambda1<Object, Entry<CardPrinted, Integer>>() { @Override
            public Object apply(final Entry<CardPrinted, Integer> from) { return from.getKey().getRarity(); } };

    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<CardPrinted, Integer>> fnSetCompare =
        new Lambda1<Comparable, Entry<CardPrinted, Integer>>() { @Override
            public Comparable apply(final Entry<CardPrinted, Integer> from) { return SetUtils.getSetByCode(from.getKey().getSet()); } };
    public static final Lambda1<Object, Entry<CardPrinted, Integer>> fnSetGet =
        new Lambda1<Object, Entry<CardPrinted, Integer>>() { @Override
            public Object apply(final Entry<CardPrinted, Integer> from) { return from.getKey().getSet(); } };

    @SuppressWarnings("rawtypes")
    public static final Lambda1<Comparable, Entry<CardPrinted, Integer>> fnAiStatusCompare =
        new Lambda1<Comparable, Entry<CardPrinted, Integer>>() { @Override
            public Comparable apply(final Entry<CardPrinted, Integer> from) { return from.getKey().getCard().getAiStatusComparable(); } };
    public static final Lambda1<Object, Entry<CardPrinted, Integer>> fnAiStatusGet =
        new Lambda1<Object, Entry<CardPrinted, Integer>>() { @Override
            public Object apply(final Entry<CardPrinted, Integer> from) { return from.getKey().getCard().getAiStatus(); } };
}
