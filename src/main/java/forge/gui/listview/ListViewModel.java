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
package forge.gui.listview;

import java.util.Map.Entry;

import com.google.common.base.Predicate;

import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.item.ItemPoolView;

/**
 * <p>
 * ListViewModel class.
 * </p>
 * 
 * @param <T>
 *            the generic type
 * @author Forge
 * @version $Id: ListViewModel.java 19857 2013-02-24 08:49:52Z Max mtg $
 */
public final class ListViewModel<T extends InventoryItem> {
    private final ItemPool<T> pool;
    private Predicate<T> filter = null;
    private final Class<T> genericType;
    private boolean infiniteSupply;

    /**
     * Instantiates a new list view model,
     * a column set, and a data set of generic type <T>.
     * 
     * @param genericType &emsp; Generic type <T>
     */
    public ListViewModel(final Class<T> genericType) {
        this.genericType = genericType;
        this.pool = new ItemPool<T>(genericType);
    }

    /**
     * Clears all data in the model.
     */
    public void clear() {
        this.pool.clear();
    }

    /**
     * Gets all items in the model.
     * 
     * @return the cards
     */
    public ItemPoolView<T> getItems() {
        return this.pool.getView();
    }

    /**
     * Removes an item from the model.
     * 
     * @param item
     *            a T
     * @param qty
     *            a int
     */
    public void removeItem(final T item, int qty) {
        if (this.infiniteSupply) { return; }

        if (this.pool.count(item) > 0) {
            this.pool.remove(item, qty);
        }
    }

    /**
     * Adds an item to the model.
     * 
     * @param item
     *            a T
     * @param qty
     *            a int
     */
    public void addItem(final T item, int qty) {
        this.pool.add(item, qty);
    }

    /**
     * Adds multiple copies of multiple items to the model.
     * 
     * @param items
     */
    public void addItems(final Iterable<Entry<T, Integer>> items) {
        this.pool.addAll(items);
    }

    /**
     * Sets whether this pool of items is in infinite supply.  If false, items in the
     * list view have a limited number of copies.
     */
    public void setInfinite(boolean infinite) {
        this.infiniteSupply = infinite;
    }

    public boolean isInfinite() {
        return infiniteSupply;
    }
} // ListViewModel
