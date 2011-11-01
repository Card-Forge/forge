package forge.gui.deckeditor;

import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import forge.item.InventoryItem;

/**
 * <p>
 * TableSorter class.
 * </p>
 * 
 * @param <T>
 *            extends InventoryItem
 * @author Forge
 * @version $Id: TableSorter.java 10146 2011-09-01 18:11:00Z Max mtg $
 */
public class TableSorterCascade<T extends InventoryItem> implements Comparator<Entry<T, Integer>> {
    private final List<TableSorter<T>> sorters;
    private final int cntFields;

    /**
     * 
     * TableSorterCascade Constructor.
     * 
     * @param sortersCascade
     *            a List<TableSorter<T>>
     */
    public TableSorterCascade(final List<TableSorter<T>> sortersCascade) {
        this.sorters = sortersCascade;
        this.cntFields = sortersCascade.size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public final int compare(final Entry<T, Integer> arg0, final Entry<T, Integer> arg1) {
        int lastCompare = 0;
        int iField = -1;
        while ((++iField < this.cntFields) && (lastCompare == 0)) { // reverse
                                                                    // iteration
            final TableSorter<T> sorter = this.sorters.get(iField);
            if (sorter == null) {
                break;
            }
            lastCompare = sorter.compare(arg0, arg1);
        }
        return lastCompare;
    }
}
