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
package forge.util;

import java.util.Collections;
import java.util.Map.Entry;

import forge.item.InventoryItem;

/**
 * <p>
 * ItemPool class.
 * </p>
 * Represents a list of items with amount of each
 * 
 * @param <T>
 *            an Object
 */
public class ItemPool<T extends InventoryItem> extends ItemPoolView<T> {

    
    
    // Constructors here
    /**
     * 
     * ItemPool Constructor.
     * 
     * @param cls
     *            a T
     */
    public ItemPool(final Class<T> cls) {
        super(cls);
    }

   
    @SuppressWarnings("unchecked")
    public static <Tin extends InventoryItem, Tout extends InventoryItem> ItemPool<Tout> createFrom(final ItemPoolView<Tin> from, final Class<Tout> clsHint) {
        final ItemPool<Tout> result = new ItemPool<Tout>(clsHint);
        if (from != null) {
            for (final Entry<Tin, Integer> e : from) {
                final Tin srcKey = e.getKey();
                if (clsHint.isInstance(srcKey)) {
                    result.put((Tout) srcKey, e.getValue());
                }
            }
        }
        return result;
    }


    @SuppressWarnings("unchecked")
    public static <Tin extends InventoryItem, Tout extends InventoryItem> ItemPool<Tout> createFrom(final Iterable<Tin> from, final Class<Tout> clsHint) {
        final ItemPool<Tout> result = new ItemPool<Tout>(clsHint);
        if (from != null) {
            for (final Tin srcKey : from) {
                if (clsHint.isInstance(srcKey)) {
                    result.put((Tout) srcKey, Integer.valueOf(1));
                }
            }
        }
        return result;
    }

    // get
    /**
     * 
     * Get item view.
     * 
     * @return a ItemPoolView
     */
    public ItemPoolView<T> getView() {
        return new ItemPoolView<T>(Collections.unmodifiableMap(this.getItems()), this.getMyClass());
    }

    // Items manipulation
    /**
     * 
     * Add a single item.
     * 
     * @param item
     *            a T
     */
    public void add(final T item) {
        this.add(item, 1);
    }

    /**
     * 
     * Add multiple items.
     * 
     * @param item
     *            a T
     * @param amount
     *            a int
     */
    public void add(final T item, final int amount) {
        if (amount <= 0) {
            return;
        }
        this.getItems().put(item, Integer.valueOf(this.count(item) + amount));
        this.isListInSync = false;
    }

    public void put(final T item, final int amount) {
        this.getItems().put(item, amount);
        this.isListInSync = false;
    }

    /**
     * addAllFlat.
     * 
     * @param <U>
     *            a InventoryItem
     * @param items
     *            a Iterable<U>
     */
    @SuppressWarnings("unchecked")
    public <U extends InventoryItem> void addAllFlat(final Iterable<U> items) {
        for (final U cr : items) {
            if (this.getMyClass().isInstance(cr)) {
                this.add((T) cr);
            }
        }
        this.isListInSync = false;
    }

    /**
     * addAll.
     * 
     * @param <U>
     *            an InventoryItem
     * @param map
     *            a Iterable<Entry<U, Integer>>
     */
    @SuppressWarnings("unchecked")
    public <U extends InventoryItem> void addAll(final Iterable<Entry<U, Integer>> map) {
        Class<T> myClass = this.getMyClass();
        for (final Entry<U, Integer> e : map) {
            if (myClass.isInstance(e.getKey())) {
                this.add((T) e.getKey(), e.getValue());
            }
        }
        this.isListInSync = false;
    }

    /**
     * 
     * Remove.
     * 
     * @param item
     *            a T
     */
    public boolean remove(final T item) {
        return this.remove(item, 1);
    }

    /**
     * 
     * Remove.
     * 
     * @param item
     *            a T
     * @param amount
     *            a int
     */
    public boolean remove(final T item, final int amount) {
        final int count = this.count(item);
        if ((count == 0) || (amount <= 0)) {
            return false;
        }
        if (count <= amount) {
            this.getItems().remove(item);
        } else {
            this.getItems().put(item, count - amount);
        }
        this.isListInSync = false;
        return true;
    }

    public boolean removeAll(final T item) {
        return this.getItems().remove(item) != null;
    }
    
    /**
     * 
     * RemoveAll.
     * 
     * @param map
     *            a T
     */
    public void removeAll(final Iterable<Entry<T, Integer>> map) {
        for (final Entry<T, Integer> e : map) {
            this.remove(e.getKey(), e.getValue());
        }
        // need not set out-of-sync: either remove did set, or nothing was removed
    }

    /**
     * 
     * TODO: Write javadoc for this method.
     * @param flat Iterable<T>
     */
    public void removeAllFlat(final Iterable<T> flat) {
        for (final T e : flat) {
            this.remove(e);
        }
        // need not set out-of-sync: either remove did set, or nothing was removed
    }

    /**
     * 
     * Clear.
     */
    public void clear() {
        this.getItems().clear();
        this.isListInSync = false;
    }
}
