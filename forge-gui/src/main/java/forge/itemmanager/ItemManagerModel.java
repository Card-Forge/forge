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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import forge.item.InventoryItem;
import forge.itemmanager.ItemColumnConfig.SortState;
import forge.util.ItemPool;
import forge.util.ItemPoolSorter;

/**
 * <p>
 * ItemManagerModel class.
 * </p>
 *
 * @param <T>
 *            the generic type
 * @author Forge
 * @version $Id: ItemManagerModel.java 19857 2013-02-24 08:49:52Z Max mtg $
 */
public final class ItemManagerModel<T extends InventoryItem> {
    private static final int maxSortDepth = 3;

    private final ItemPool<T> data;
    private boolean infiniteSupply;
    private final CascadeManager cascadeManager = new CascadeManager();

    /**
     * Instantiates a new list view model
     *
     * @param ItemManager0
     * @param genericType0
     */
    public ItemManagerModel(final Class<T> genericType0) {
        this.data = new ItemPool<T>(genericType0);
    }

    /**
     * Clears all data in the model.
     */
    public void clear() {
        this.data.clear();
    }

    // same thing as above, it was copied to provide sorting (needed by table
    // views in deck editors)
    /** The items ordered. */
    private final transient List<Entry<T, Integer>> itemsOrdered = new ArrayList<Map.Entry<T, Integer>>();

    /** Whether list is in sync. */
    protected transient boolean isListInSync = false;

    public List<Entry<T, Integer>> getOrderedList() {
        if (!this.isListInSync) {
            this.rebuildOrderedList();
        }
        return this.itemsOrdered;
    }

    private void rebuildOrderedList() {
        this.itemsOrdered.clear();
        if (this.data != null) {
            for (final Entry<T, Integer> e : this.data) {
                this.itemsOrdered.add(e);
            }
        }
        this.isListInSync = true;
    }

    public int countDistinct() {
        return this.data.countDistinct();
    }

    /**
     * Gets all items in the model.
     *
     * @return ItemPoolView<T>
     */
    public ItemPool<T> getItems() {
        return this.data.getView();
    }

    /**
     * Removes a item from the model.
     *
     * @param item0 &emsp; {@link forge.Item} object
     */
    public void removeItem(final T item0, final int qty) {
        if (isInfinite()) { return; }

        final boolean wasThere = this.data.count(item0) > 0;
        if (wasThere) {
            this.data.remove(item0, qty);
            isListInSync = false;
        }
    }

    public void replaceAll(final T item0, final T replacement0) {
        final int count = this.data.count(item0);
        if (count > 0) {
            this.data.removeAll(item0);
            this.data.add(replacement0, count);
            isListInSync = false;
        }
    }

    /**
     * Adds a item to the model.
     *
     * @param item0 &emsp; {@link forge.Item} object.
     */
    public void addItem(final T item0, final int qty) {
        this.data.add(item0, qty);
        isListInSync = false;
    }

    /**
     * Adds multiple copies of multiple items to the model.
     *
     * @param items0 &emsp; {@link java.lang.Iterable}<Entry<T, Integer>>
     */
    public void addItems(final Iterable<Entry<T, Integer>> items0) {
        this.data.addAll(items0);
        isListInSync = false;
    }

    /**
     * Sets whether this table's pool of items is in infinite supply. If false,
     * items in the table have a limited number of copies.
     */
    public void setInfinite(final boolean infinite) {
        this.infiniteSupply = infinite;
    }

    public boolean isInfinite() {
        return infiniteSupply;
    }

    public CascadeManager getCascadeManager() {
        return cascadeManager;
    }

    /**
     * Resort.
     */
    public void refreshSort() {
        if (this.getOrderedList().isEmpty()) { return; }

        Collections.sort(this.getOrderedList(), new MyComparator());
    }

    /**
     * Manages sorting orders for multiple depths of sorting.
     */
    public final class CascadeManager {
        private final List<ItemColumn> colsToSort = new ArrayList<ItemColumn>(3);
        private Sorter sorter = null;

        // Adds a column to sort cascade list.
        // If column is first in the cascade, inverts direction of sort.
        // Otherwise, sorts in ascending direction.
        public void add(final ItemColumn col0, final boolean forSetup) {
            this.sorter = null;

            if (forSetup) { //just add column unmodified if setting up sort columns
                this.colsToSort.add(0, col0);
            } else {
                if (colsToSort.size() > 0 && colsToSort.get(0).equals(col0)) { //if column already at top level, just invert
                    col0.getConfig().setSortPriority(1);
                    col0.getConfig().setSortState(col0.getConfig().getSortState() == SortState.ASC ? SortState.DESC : SortState.ASC);
                }
                else { //otherwise move column to top level and move others down
                    this.colsToSort.remove(col0);
                    col0.getConfig().setSortPriority(1);
                    col0.getConfig().setSortState(col0.getConfig().getDefaultSortState());
                    this.colsToSort.add(0, col0);
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
            if (this.colsToSort.size() > maxSortDepth) {
                this.colsToSort.get(maxSortDepth).getConfig().setSortPriority(0);
                this.colsToSort.remove(maxSortDepth);
            }
        }

        public Sorter getSorter() {
            if (this.sorter == null) {
                this.sorter = createSorter();
            }
            return this.sorter;
        }

        public void reset() {
            this.colsToSort.clear();
            this.sorter = null;
        }

        private Sorter createSorter() {
            final List<ItemPoolSorter<InventoryItem>> oneColSorters = new ArrayList<ItemPoolSorter<InventoryItem>>(maxSortDepth);

            for (final ItemColumn col : this.colsToSort) {
                oneColSorters.add(new ItemPoolSorter<InventoryItem>(
                        col.getFnSort(),
                        col.getConfig().getSortState().equals(SortState.ASC) ? true : false));
            }

            return new Sorter(oneColSorters);
        }

        public class Sorter implements Comparator<Entry<InventoryItem, Integer>> {
            private final List<ItemPoolSorter<InventoryItem>> sorters;
            private final int cntFields;

            /**
             *
             * Sorter Constructor.
             *
             * @param sorters0
             *            a List<TableSorter<InventoryItem>>
             */
            public Sorter(final List<ItemPoolSorter<InventoryItem>> sorters0) {
                this.sorters = sorters0;
                this.cntFields = sorters0.size();
            }

            /*
             * (non-Javadoc)
             *
             * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
             */
            @Override
            public final int compare(final Entry<InventoryItem, Integer> arg0, final Entry<InventoryItem, Integer> arg1) {
                int lastCompare = 0;
                int iField = -1;
                while ((++iField < this.cntFields) && (lastCompare == 0)) { // reverse
                                                                            // iteration
                    final ItemPoolSorter<InventoryItem> sorter = this.sorters.get(iField);
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
        /* (non-Javadoc)
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        @SuppressWarnings("unchecked")
        @Override
        public int compare(final Entry<T, Integer> o1, final Entry<T, Integer> o2) {
            return cascadeManager.getSorter().compare((Entry<InventoryItem, Integer>)o1, (Entry<InventoryItem, Integer>)o2);
        }
    }
} // ItemManagerModel
