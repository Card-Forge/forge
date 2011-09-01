package forge.gui.deckeditor;

import forge.card.CardPrinted;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

/**
 * <p>TableSorter class.</p>
 *
 * @author Forge
 * @version $Id: TableSorter.java 10146 2011-09-01 18:11:00Z Max mtg $
 */
public class TableSorterCascade implements Comparator<Entry<CardPrinted, Integer>> {
    private TableSorter[] sorters;
    private final int cntFields;
    private static final TableSorter[] EMPTY_SORTER_ARRAY = new TableSorter[0];

    public TableSorterCascade(final List<TableSorter> sortersCascade) {
        this(sortersCascade.toArray(EMPTY_SORTER_ARRAY));
    }

    public TableSorterCascade(final TableSorter[] sortersCascade) {
        this.sorters = sortersCascade;
        cntFields = sortersCascade.length;
    }

    @Override
    public final int compare(final Entry<CardPrinted, Integer> arg0, final Entry<CardPrinted, Integer> arg1) {
        int lastCompare = 0;
        int iField = -1;
        while (++iField < cntFields && lastCompare == 0) {
            TableSorter sorter = sorters[iField];
            if (sorter == null) { break; }
            lastCompare = sorter.compare(arg0, arg1);
        }
        return lastCompare;
    }
}
