package forge.gui.deckeditor;

import java.util.Comparator;
import java.util.Map.Entry;

import net.slightlymagic.braids.util.lambda.Lambda1;
import forge.item.CardPrinted;

/**
 * <p>
 * TableSorter class.
 * </p>
 * 
 * @param <T>
 *            the generic type
 * @author Forge
 * @version $Id$
 */
@SuppressWarnings("unchecked")
// Comparable needs <type>
public class TableSorter<T> implements Comparator<Entry<T, Integer>> {
    private final boolean ascending;
    @SuppressWarnings("rawtypes")
    private final Lambda1<Comparable, Entry<T, Integer>> field;

    /**
     * <p>
     * Constructor for TableSorter.
     * </p>
     * 
     * @param field
     *            the field
     * @param inAscending
     *            a boolean.
     */
    @SuppressWarnings("rawtypes")
    public TableSorter(final Lambda1<Comparable, Entry<T, Integer>> field, final boolean inAscending) {
        this.field = field;
        this.ascending = inAscending;
    }

    /** The Constant byNameThenSet. */
    @SuppressWarnings("rawtypes")
    public static final TableSorter<CardPrinted> BY_NAME_THEN_SET = new TableSorter<CardPrinted>(
            new Lambda1<Comparable, Entry<CardPrinted, Integer>>() {
                @Override
                public Comparable apply(final Entry<CardPrinted, Integer> from) {
                    return from.getKey();
                }
            }, true);

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public final int compare(final Entry<T, Integer> arg0, final Entry<T, Integer> arg1) {
        final Comparable obj1 = this.field.apply(arg0);
        final Comparable obj2 = this.field.apply(arg1);
        // System.out.println(String.format("%s vs %s _______ %s vs %s", arg0,
        // arg1, obj1, obj2));
        return this.ascending ? obj1.compareTo(obj2) : obj2.compareTo(obj1);
    }
}
