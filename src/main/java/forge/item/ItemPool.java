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
package forge.item;

import java.util.Collections;
import java.util.Map.Entry;

/**
 * <p>
 * CardPool class.
 * </p>
 * Represents a list of cards with amount of each
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

    public ItemPool(final Class<T> cls, boolean infiniteStock) {
        super(cls, infiniteStock);
    }
    
    /**
     * createFrom method.
     * 
     * @param <Tin>
     *            an InventoryItem
     * @param <Tout>
     *            an InventoryItem
     * @param from
     *            a Tin
     * @param clsHint
     *            a Tout
     * @return InventoryItem
     */
    @SuppressWarnings("unchecked")
    public static <Tin extends InventoryItem, Tout extends InventoryItem> ItemPool<Tout> createFrom(
            final ItemPoolView<Tin> from, final Class<Tout> clsHint, boolean infiniteStock) {
        final ItemPool<Tout> result = new ItemPool<Tout>(clsHint, infiniteStock);
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

    /**
     * createFrom method.
     * 
     * @param <Tin>
     *            an InventoryItem
     * @param <Tout>
     *            an InventoryItem
     * @param from
     *            a Iterable<Tin>
     * @param clsHint
     *            a Class<Tout>
     * @return <Tin> an InventoryItem
     */
    @SuppressWarnings("unchecked")
    public static <Tin extends InventoryItem, Tout extends InventoryItem> ItemPool<Tout> createFrom(
            final Iterable<Tin> from, final Class<Tout> clsHint, boolean infiniteStock) {
        final ItemPool<Tout> result = new ItemPool<Tout>(clsHint, infiniteStock);
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
        return new ItemPoolView<T>(Collections.unmodifiableMap(this.getCards()), this.getMyClass());
    }

    // Cards manipulation
    /**
     * 
     * Add Card.
     * 
     * @param card
     *            a T
     */
    public void add(final T card) {
        this.add(card, 1);
    }

    /**
     * 
     * add method.
     * 
     * @param card
     *            a T
     * @param amount
     *            a int
     */
    public void add(final T card, final int amount) {
        if (amount <= 0) {
            return;
        }
        this.getCards().put(card, this.count(card) + amount);
        this.setListInSync(false);
    }

    private void put(final T card, final int amount) {
        this.getCards().put(card, amount);
        this.setListInSync(false);
    }

    /**
     * addAllCards.
     * 
     * @param <U>
     *            a InventoryItem
     * @param cards
     *            a Iterable<U>
     */
    @SuppressWarnings("unchecked")
    public <U extends InventoryItem> void addAllFlat(final Iterable<U> cards) {
        for (final U cr : cards) {
            if (this.getMyClass().isInstance(cr)) {
                this.add((T) cr);
            }
        }
        this.setListInSync(false);
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
        this.setListInSync(false);
    }

    /**
     * 
     * Remove.
     * 
     * @param card
     *            a T
     */
    public boolean remove(final T card) {
        return this.remove(card, 1);
    }

    /**
     * 
     * Remove.
     * 
     * @param card
     *            a T
     * @param amount
     *            a int
     */
    public boolean remove(final T card, final int amount) {
        final int count = this.count(card);
        if ((count == 0) || (amount <= 0)) {
            return false;
        }
        if (count <= amount) {
            this.getCards().remove(card);
        } else {
            this.getCards().put(card, count - amount);
        }
        this.setListInSync(false);
        return true;
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
        this.getCards().clear();
        this.setListInSync(false);
    }
}
