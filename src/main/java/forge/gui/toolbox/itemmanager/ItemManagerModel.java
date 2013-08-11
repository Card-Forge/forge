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
package forge.gui.toolbox.itemmanager;

import java.util.List;
import java.util.Map.Entry;

import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.item.ItemPoolView;

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
    private final ItemManager<T> ItemManager;
    private final ItemPool<T> data;
    private boolean infiniteSupply;

    /**
     * Instantiates a new list view model
     * 
     * @param ItemManager0
     * @param genericType0
     */
    public ItemManagerModel(final ItemManager<T> ItemManager0, final Class<T> genericType0) {
        this.ItemManager = ItemManager0;
        this.data = new ItemPool<T>(genericType0);
    }

    /**
     * Clears all data in the model.
     */
    public void clear() {
        this.data.clear();
    }
    
    /**
     * 
     * getOrderedList.
     * 
     * @return List<Entry<T, Integer>>
     */
    public final List<Entry<T, Integer>> getOrderedList() {
        return this.data.getOrderedList();
    }
    
    /**
     * 
     * countDistinct.
     * 
     * @return int
     */
    public final int countDistinct() {
        return this.data.countDistinct();
    }

    /**
     * Gets all items in the model.
     * 
     * @return ItemPoolView<T>
     */
    public ItemPoolView<T> getItems() {
        return this.data.getView();
    }

    /**
     * Removes a item from the model.
     * 
     * @param item0 &emsp; {@link forge.Item} object
     */
    public void removeItem(final T item0, int qty) {
        if (isInfinite()) 
            return;

        final boolean wasThere = this.data.count(item0) > 0;
        if (wasThere) {
            this.data.remove(item0, qty);
            this.ItemManager.getTableModel().fireTableDataChanged();
        }
    }

    /**
     * Adds a item to the model.
     * 
     * @param item0 &emsp; {@link forge.Item} object.
     */
    public void addItem(final T item0, int qty) {
        this.data.add(item0, qty);
        this.ItemManager.getTableModel().fireTableDataChanged();
    }

    /**
     * Adds multiple copies of multiple items to the model.
     * 
     * @param items0 &emsp; {@link java.lang.Iterable}<Entry<T, Integer>>
     */
    public void addItems(final Iterable<Entry<T, Integer>> items0) {
        this.data.addAll(items0);
        this.ItemManager.getTableModel().fireTableDataChanged();
    }
    /**
     * Sets whether this table's pool of items is in infinite supply.  If false, items in the
     * table have a limited number of copies.
     */
    public void setInfinite(boolean infinite) {
        this.infiniteSupply = infinite;
    }

    public boolean isInfinite() {
        return infiniteSupply;
    }
} // ItemManagerModel
