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
package forge.gui.toolbox.itemmanager;

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

import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.itemmanager.table.ItemTable;
import forge.gui.toolbox.itemmanager.table.ItemTableModel;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.item.ItemPoolView;
import forge.util.Aggregates;


/**
 * ItemManager.
 * 
 * @param <T>
 *            the generic type
 */
public final class ItemManager<T extends InventoryItem> {
    private ItemPool<T> pool;
    private final ItemManagerModel<T> model;
    private final ItemTable<T> table;
    private Predicate<T> filter = null;
    private boolean wantUnique = false;
    private boolean alwaysNonUnique = false;
    private final Class<T> genericType;
    private Map<SItemManagerUtil.StatTypes, FLabel> statLabels;

    /**
     * 
     * getTable.
     * 
     * @return ItemTable<T>
     */
    public ItemTable<T> getTable() {
        return this.table;
    }
    
    /**
     * 
     * getTableModel.
     * 
     * @return ItemTableModel<T>
     */
    public ItemTableModel<T> getTableModel() {
        return this.table.getTableModel();
    }

    /**
     * ItemManager.
     * 
     * @param genericType0 - the class of item that this table will contain
     */
    public ItemManager(final Class<T> genericType0) {
        this(genericType0, false);
    }

    /**
     * ItemManager Constructor.
     * 
     * @param forceUnique whether this table should display only one item with the same name
     * @param type0 the class of item that this table will contain
     */
    public ItemManager(final Class<T> genericType0, final boolean wantUnique0) {
        this.genericType = genericType0;
        this.wantUnique = wantUnique0;
        this.model = new ItemManagerModel<T>(this, genericType0);
        this.table = new ItemTable<T>(this, this.model);
    }

    /**
     * 
     * Gets the item pool.
     * 
     * @return ItemPoolView
     */
    public ItemPoolView<T> getPool() {
        return this.pool;
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

    /**
     * 
     * addItems.
     * 
     * @param itemsToAdd
     */
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
    
    /**
     * 
     * addItems.
     * 
     * @param itemsToAdd
     */
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
     * @param qty
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

    /**
     * 
     * removeItems.
     * 
     * @param itemsToRemove
     */
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
    
    /**
     * 
     * getItemCount.
     * 
     * @param item
     */
    public int getItemCount(final T item) {
        return this.model.isInfinite() ? Integer.MAX_VALUE : this.pool.count(item);
    }
    
    /**
     * Gets all filtered items in the model.
     * 
     * @return ItemPoolView<T>
     */
    public ItemPoolView<T> getFilteredItems() {
        return this.model.getItems();
    }
    
    /**
     * 
     * getStatLabels.
     * 
     */
    public Map<SItemManagerUtil.StatTypes, FLabel> getStatLabels() {
        return this.statLabels;
    }
    
    /**
     * 
     * getStatLabel.
     * 
     * @param s
     */
    public void setStatLabels(Map<SItemManagerUtil.StatTypes, FLabel> statLabels0) {
        this.statLabels = statLabels0;
    }
    
    /**
     * 
     * getStatLabel.
     * 
     * @param s
     */
    public FLabel getStatLabel(SItemManagerUtil.StatTypes s) {
        if (this.statLabels != null) {
            return this.statLabels.get(s);
        }
        return null;
    }
    
    /**
     * 
     * getFilter.
     * 
     */
    public Predicate<T> getFilter() {
        return this.filter;
    }
    
    /**
     * 
     * isUnfiltered.
     * 
     */
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
        if (this.pool != null) {
            this.updateView(true);
        }
    }

    /**
     * 
     * updateView.
     * 
     * @param bForceFilter
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

    /**
     * 
     * setWantElasticColumns.
     * 
     * @param value
     */
    public void setWantElasticColumns(boolean value) {
        this.table.setAutoResizeMode(value ? JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS : JTable.AUTO_RESIZE_NEXT_COLUMN);
    }
    
    /**
     * 
     * selectAndScrollTo.
     * 
     * @param rowIdx
     */
    public void selectAndScrollTo(int rowIdx) {
        if (!(this.table.getParent() instanceof JViewport)) {
            return;
        }
        JViewport viewport = (JViewport)this.table.getParent();

        // compute where we're going and where we are
        Rectangle targetRect  = this.table.getCellRect(rowIdx, 0, true);
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
        
        this.table.scrollRectToVisible(targetRect);
        this.table.setRowSelectionInterval(rowIdx, rowIdx);
    }

    /**
     * 
     * focus.
     * 
     */
    public void focus() {
        this.table.requestFocusInWindow();
        
        if (this.table.getRowCount() > 0) {
            this.table.changeSelection(0, 0, false, false);
        }
    }
}
