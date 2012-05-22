/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.gui.deckeditor.tables;

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
