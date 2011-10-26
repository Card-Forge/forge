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
    private boolean ascending;
    @SuppressWarnings("rawtypes")
    private Lambda1<Comparable, Entry<T, Integer>> field;

    /**
     * <p>
     * Constructor for TableSorter.
     * </p>
     * 
     * @param field
     *            the field
     * @param in_ascending
     *            a boolean.
     */
    @SuppressWarnings("rawtypes")
    public TableSorter(final Lambda1<Comparable, Entry<T, Integer>> field, final boolean in_ascending) {
        this.field = field;
        ascending = in_ascending;
    }

    /** The Constant byNameThenSet. */
    @SuppressWarnings("rawtypes")
    public static final TableSorter<CardPrinted> byNameThenSet = new TableSorter<CardPrinted>(
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
        Comparable obj1 = field.apply(arg0);
        Comparable obj2 = field.apply(arg1);
        // System.out.println(String.format("%s vs %s _______ %s vs %s", arg0,
        // arg1, obj1, obj2));
        return ascending ? obj1.compareTo(obj2) : obj2.compareTo(obj1);
    }
}
