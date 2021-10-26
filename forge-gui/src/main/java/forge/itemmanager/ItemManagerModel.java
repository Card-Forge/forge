/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011
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
package forge.itemmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import forge.item.InventoryItem;
import forge.itemmanager.ItemColumnConfig.SortState;
import forge.util.ItemPool;
import forge.util.ItemPoolSorter;


public final class ItemManagerModel<T extends InventoryItem> {
    private static final int maxSortDepth = 3;

    private final ItemPool<T> data;
    private boolean infiniteSupply;
    private final CascadeManager cascadeManager = new CascadeManager();

    public ItemManagerModel(final Class<T> genericType0) {
        data = new ItemPool<>(genericType0);
    }

    public void clear() {
        data.clear();
    }

    // same thing as above, it was copied to provide sorting (needed by table
    // views in deck editors)
    private final transient List<Entry<T, Integer>> itemsOrdered = Collections.synchronizedList(new ArrayList<>());

    protected transient boolean isListInSync = false;

    public synchronized List<Entry<T, Integer>> getOrderedList() {
        if (!isListInSync) {
            rebuildOrderedList();
        }
        return itemsOrdered;
    }

    private void rebuildOrderedList() {
        itemsOrdered.clear();
        if (data != null) {
            for (final Entry<T, Integer> e : data) {
                itemsOrdered.add(e);
            }
        }
        isListInSync = true;
    }

    public int countDistinct() {
        return data.countDistinct();
    }

    public ItemPool<T> getItems() {
        return data.getView();
    }

    public void removeItem(final T item0, final int qty) {
        if (data.count(item0) > 0) {
            if (isInfinite()) {
                data.removeAll(item0);
            } else {
                data.remove(item0, qty);
            }
            isListInSync = false;
        }
    }

    public void replaceAll(final T item0, final T replacement0) {
        final int count = data.count(item0);
        if (count > 0) {
            data.removeAll(item0);
            data.add(replacement0, count);
            isListInSync = false;
        }
    }

    public void addItem(final T item0, final int qty) {
        data.add(item0, qty);
        isListInSync = false;
    }

    public void addItems(final Iterable<Entry<T, Integer>> items0) {
        data.addAll(items0);
        isListInSync = false;
    }

    /**
     * Sets whether this table's pool of items is in infinite supply. If false,
     * items in the table have a limited number of copies.
     */
    public void setInfinite(final boolean infinite) {
        infiniteSupply = infinite;
    }

    public boolean isInfinite() {
        return infiniteSupply;
    }

    public CascadeManager getCascadeManager() {
        return cascadeManager;
    }

    public void refreshSort() {
        final List<Entry<T, Integer>> list = getOrderedList();
        if (list.isEmpty()) { return; }
        try { Collections.sort(list, new MyComparator()); }
        //fix NewDeck editor not loading on Android if a user deleted unwanted sets on edition folder
        catch (IllegalArgumentException ex) {}
    }

    //Manages sorting orders for multiple depths of sorting
    public final class CascadeManager {
        private final List<ItemColumn> colsToSort = Collections.synchronizedList(new ArrayList<>(3));
        private Sorter sorter = null;

        // Adds a column to sort cascade list.
        // If column is first in the cascade, inverts direction of sort.
        // Otherwise, sorts in ascending direction.
        public void add(final ItemColumn col0, final boolean forSetup) {
            sorter = null;

            if (forSetup) { //just add column unmodified if setting up sort columns
                colsToSort.add(0, col0);
            }
            else {
                if (colsToSort.size() > 0 && colsToSort.get(0).equals(col0)) { //if column already at top level, just invert
                    col0.getConfig().setSortPriority(1);
                    col0.getConfig().setSortState(col0.getConfig().getSortState() == SortState.ASC ? SortState.DESC : SortState.ASC);
                }
                else { //otherwise move column to top level and move others down
                    colsToSort.remove(col0);
                    col0.getConfig().setSortPriority(1);
                    col0.getConfig().setSortState(col0.getConfig().getDefaultSortState());
                    colsToSort.add(0, col0);
                }

                //decrement sort priority on remaining columns
                for (int i = 1; i < maxSortDepth; i++) {
                    if (colsToSort.size() == i) { break; }

                    if (colsToSort.get(i).getConfig().getSortPriority() != 0) {
                        colsToSort.get(i).getConfig().setSortPriority(i + 1);
                    }
                }
            }

            //unset and remove boundary columns.
            if (colsToSort.size() > maxSortDepth) {
                colsToSort.get(maxSortDepth).getConfig().setSortPriority(0);
                colsToSort.remove(maxSortDepth);
            }
        }

        public Sorter getSorter() {
            if (sorter == null) {
                sorter = createSorter();
            }
            return sorter;
        }

        public void reset() {
            colsToSort.clear();
            sorter = null;
        }

        private Sorter createSorter() {
            final List<ItemPoolSorter<InventoryItem>> oneColSorters = new ArrayList<>(maxSortDepth);

            synchronized (colsToSort) {
                final Iterator<ItemColumn> it = colsToSort.iterator();
                while (it.hasNext()) {
                    final ItemColumn col = it.next();
                    oneColSorters.add(new ItemPoolSorter<>(
                            col.getFnSort(),
                            col.getConfig().getSortState().equals(SortState.ASC)));
                }
            }
            return new Sorter(oneColSorters);
        }

        public class Sorter implements Comparator<Entry<InventoryItem, Integer>> {
            private final List<ItemPoolSorter<InventoryItem>> sorters;
            private final int cntFields;

            public Sorter(final List<ItemPoolSorter<InventoryItem>> sorters0) {
                sorters = sorters0;
                cntFields = sorters0.size();
            }

            @Override
            public final int compare(final Entry<InventoryItem, Integer> arg0, final Entry<InventoryItem, Integer> arg1) {
                int lastCompare = 0;
                int iField = -1;
                while ((++iField < cntFields) && (lastCompare == 0)) { // reverse
                                                                            // iteration
                    final ItemPoolSorter<InventoryItem> sorter = sorters.get(iField);
                    if (sorter == null) {
                        break;
                    }
                    lastCompare = sorter.compare(arg0, arg1);
                }
                return lastCompare;
            }
        }
    }

    private final class MyComparator implements Comparator<Entry<T, Integer>> {
        @SuppressWarnings("unchecked")
        @Override
        public int compare(final Entry<T, Integer> o1, final Entry<T, Integer> o2) {
            return cascadeManager.getSorter().compare((Entry<InventoryItem, Integer>)o1, (Entry<InventoryItem, Integer>)o2);
        }
    }
}
