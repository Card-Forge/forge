package forge.gui.deckeditor;

import forge.item.InventoryItem;

import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

/**
 * <p>TableSorter class.</p>
 *
 * @author Forge
 * @version $Id: TableSorter.java 10146 2011-09-01 18:11:00Z Max mtg $
 */
public class TableSorterCascade<T extends InventoryItem> implements Comparator<Entry<T, Integer>> {
    private List<TableSorter<T>> sorters;
    private final int cntFields;

    public TableSorterCascade(final List<TableSorter<T>> sortersCascade) {
        this.sorters = sortersCascade;
        cntFields = sortersCascade.size();
    }

    @Override
    public final int compare(final Entry<T, Integer> arg0, final Entry<T, Integer> arg1) {
        int lastCompare = 0;
        int iField = -1;
        while (++iField < cntFields && lastCompare == 0) { // reverse iteration
            TableSorter<T> sorter = sorters.get(iField);
            if (sorter == null) { break; }
            lastCompare = sorter.compare(arg0, arg1);
        }
        return lastCompare;
    }
}
