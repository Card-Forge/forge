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
package forge.gui.listview;

import java.awt.Rectangle;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JTable;
import javax.swing.JViewport;
import com.google.common.base.Predicate;


import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.item.ItemPoolView;
import forge.util.Aggregates;


/**
 * ListView.
 * 
 * @param <T>
 *            the generic type
 */
public final class ListView<T extends InventoryItem> {
    private ItemPool<T> pool;
    private ListViewModel<T> model;
    private final ListViewTable<T> table;
    private Predicate<T> filter = null;
    private boolean wantUnique = false;
    private boolean alwaysNonUnique = false;
    private final Class<T> genericType;

    /**
     * 
     * getTable.
     * 
     * @return ListViewTable<T>
     */
    public ListViewTable<T> getTable() {
        return this.table;
    }
    
    /**
     * 
     * getTableModel.
     * 
     * @return ListViewTableModel<T>
     */
    public ListViewTableModel<T> getTableModel() {
        return this.table.getTableModel();
    }

    /**
     * ListView.
     * 
     * @param genericType0 - the class of item that this table will contain
     */
    public ListView(final Class<T> genericType0) {
        this(genericType0, false);
    }

    /**
     * ListView Constructor.
     * 
     * @param forceUnique whether this table should display only one item with the same name
     * @param type0 the class of item that this table will contain
     */
    public ListView(final Class<T> genericType0, final boolean wantUnique0) {
        this.genericType = genericType0;
        this.wantUnique = wantUnique0;
        this.model = new ListViewModel<T>(this, genericType0);
        this.table = new ListViewTable<T>(this.model);
    }

    /**
     * 
     * Sets the item pool.
     * 
     * @param items
     */
    public void setPool(final Iterable<InventoryItem> items) {
        this.setPool(ItemPool.createFrom(items, this.genericType), false);
    }
    
    /**
     * 
     * Sets the item pool.
     * 
     * @param poolView
     */
    public void setPool(final ItemPoolView<T> poolView) {
        this.setPool(poolView, false);
    }
    
    /**
     * 
     * Sets the item pool.
     * 
     * @param poolView
     * @param infinite
     */
    public void setPool(final ItemPoolView<T> poolView, boolean infinite) {
        this.setPoolImpl(ItemPool.createFrom(poolView, this.genericType), infinite);

    }

    /**
     * 
     * Sets the item pool.
     * 
     * @param pool0
     */
    public void setPool(final ItemPool<T> pool0) {
        this.setPoolImpl(pool0, false);
    }

    /**
     * 
     * Sets the item pool.
     * 
     * @param pool0
     * @param infinite
     */
    protected void setPoolImpl(final ItemPool<T> pool0, boolean infinite) {
        this.model.clear();
        this.pool = pool0;
        this.model.addItems(this.pool);
        this.model.setInfinite(infinite);
        this.updateView(true);
    }

    /**
     * 
     * getSelectedItem.
     * 
     * @return InventoryItem
     */
    public InventoryItem getSelectedItem() {
        return this.table.getSelectedItem();
    }
    
    /**
     * 
     * getSelectedItems.
     * 
     * @return List<InventoryItem>
     */
    public List<InventoryItem> getSelectedItems() {
        return this.table.getSelectedItems();
    }

    private boolean isUnfiltered() {
        return this.filter == null;
    }

    /**
     * 
     * setFilter.
     * 
     * @param filterToSet
     */
    public void setFilter(final Predicate<T> filterToSet) {
        this.filter = filterToSet;
        if (null != pool) {
            this.updateView(true);
        }
    }

    /**
     * 
     * addItem.
     * 
     * @param item
     * @param qty
     */
    public void addItem(final T item, int qty) {
        final int n = this.table.getSelectedRow();
        this.pool.add(item, qty);
        if (this.isUnfiltered()) {
            this.model.addItem(item, qty);
       }
        this.updateView(false);
        this.table.fixSelection(n);
    }

    public void addItems(Iterable<Map.Entry<T, Integer>> itemsToAdd) {
        final int n = this.table.getSelectedRow();
        for (Map.Entry<T, Integer> item : itemsToAdd) {
            this.pool.add(item.getKey(), item.getValue());
            if (this.isUnfiltered()) {
                this.model.addItem(item.getKey(), item.getValue());
            }
        }
        this.updateView(false);
        this.table.fixSelection(n);
    }
    
    public void addItems(Collection<T> itemsToAdd) {
        final int n = this.table.getSelectedRow();
        for (T item : itemsToAdd) {
            this.pool.add(item, 1);
            if (this.isUnfiltered()) {
                this.model.addItem(item, 1);
            }
        }
        this.updateView(false);
        this.table.fixSelection(n);
    }

    /**
     * 
     * removeItem.
     * 
     * @param item
     *            an InventoryItem
     */
    public void removeItem(final T item, int qty) {
        final int n = this.table.getSelectedRow();
        this.pool.remove(item, qty);
        if (this.isUnfiltered()) {
            this.model.removeItem(item, qty);
        }
        this.updateView(false);
        this.table.fixSelection(n);
    }

    public void removeItems(List<Map.Entry<T, Integer>> itemsToRemove) {
        final int n = this.table.getSelectedRow();
        for (Map.Entry<T, Integer> item : itemsToRemove) {
            this.pool.remove(item.getKey(), item.getValue());
            if (this.isUnfiltered()) {
                this.model.removeItem(item.getKey(), item.getValue());
            }
        }
        this.updateView(false);
        this.table.fixSelection(n);
    }
    
    public int getItemCount(final T item) {
        return model.isInfinite() ? Integer.MAX_VALUE : this.pool.count(item);
    }
    
    public Predicate<T> getFilter() {
        return filter;
    }

    /**
     * 
     * updateView.
     * 
     * @param bForceFilter
     *            a boolean
     */
    public void updateView(final boolean bForceFilter) {
        final boolean useFilter = (bForceFilter && (this.filter != null)) || !isUnfiltered();

        if (useFilter || this.wantUnique || bForceFilter) {
            this.model.clear();
        }

        if (useFilter && this.wantUnique) {
            Predicate<Entry<T, Integer>> filterForPool = Predicates.compose(this.filter, this.pool.FN_GET_KEY);
            Iterable<Entry<T, Integer>> items = Aggregates.uniqueByLast(Iterables.filter(this.pool, filterForPool), this.pool.FN_GET_NAME);
            this.model.addItems(items);
        } else if (useFilter) {
            Predicate<Entry<T, Integer>> pred = Predicates.compose(this.filter, this.pool.FN_GET_KEY);
            this.model.addItems(Iterables.filter(this.pool, pred));
        } else if (this.wantUnique) {
            Iterable<Entry<T, Integer>> items = Aggregates.uniqueByLast(this.pool, this.pool.FN_GET_NAME);
            this.model.addItems(items);
        } else if (!useFilter && bForceFilter) {
            this.model.addItems(this.pool);
        }

        this.table.getTableModel().refreshSort();
    }

    /**
     * 
     * getItems.
     * 
     * @return ItemPoolView
     */
    public ItemPoolView<T> getItems() {
        return this.pool;
    }

    /**
     * 
     * getWantUnique.
     * 
     * @return true if the editor is in "unique item names only" mode.
     */
    public boolean getWantUnique() {
        return this.wantUnique;
    }

    /**
     * 
     * setWantUnique.
     * 
     * @param unique if true, the editor will be set to the "unique item names only" mode.
     */
    public void setWantUnique(boolean unique) {
        this.wantUnique = this.alwaysNonUnique ? false : unique;
    }

    /**
     * 
     * getAlwaysNonUnique.
     * 
     * @return if ture, this editor must always show non-unique items (e.g. quest editor).
     */
    public boolean getAlwaysNonUnique() {
        return this.alwaysNonUnique;
    }

    /**
     * 
     * setAlwaysNonUnique.
     * 
     * @param nonUniqueOnly if true, this editor must always show non-unique items (e.g. quest editor).
     */
    public void setAlwaysNonUnique(boolean nonUniqueOnly) {
        this.alwaysNonUnique = nonUniqueOnly;
    }

    public void setWantElasticColumns(boolean value) {
        table.setAutoResizeMode(value ? JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS : JTable.AUTO_RESIZE_NEXT_COLUMN);
    }
    
    public void selectAndScrollTo(int rowIdx) {
        if (!(table.getParent() instanceof JViewport)) {
            return;
        }
        JViewport viewport = (JViewport)table.getParent();

        // compute where we're going and where we are
        Rectangle targetRect  = table.getCellRect(rowIdx, 0, true);
        Rectangle curViewRect = viewport.getViewRect();

        // if the target cell is not visible, attempt to jump to a location where it is
        // visible but not on the edge of the viewport
        if (targetRect.y + targetRect.height > curViewRect.y + curViewRect.height) {
            // target is below us, move to position 3 rows below target
            targetRect.setLocation(targetRect.x, targetRect.y + (targetRect.height * 3));
        } else if  (targetRect.y < curViewRect.y) {
            // target is above is, move to position 3 rows above target
            targetRect.setLocation(targetRect.x, targetRect.y - (targetRect.height * 3));
        }
        
        table.scrollRectToVisible(targetRect);
        table.setRowSelectionInterval(rowIdx, rowIdx);
    }
}
