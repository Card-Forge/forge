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

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import forge.item.InventoryItem;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

/**
 * <p>
 * ItemPool class.
 * </p>
 * Represents a list of items with amount of each
 * 
 * @param <T>
 *            an Object
 */
public class ItemPool<T extends InventoryItem> implements Iterable<Entry<T, Integer>>, Serializable {
    private static final long serialVersionUID = 6572047177527559797L;

    /** The fn to printed. */
    public final transient Function<Entry<T, Integer>, T> FN_GET_KEY = new Function<Entry<T, Integer>, T>() {
        @Override
        public T apply(final Entry<T, Integer> from) {
            return from.getKey();
        }
    };

    /** The fn to item name. */
    public final transient Function<Entry<T, Integer>, String> FN_GET_NAME = new Function<Entry<T, Integer>, String>() {
        @Override
        public String apply(final Entry<T, Integer> from) {
            return from.getKey().getName();
        }
    };

    /** The fn to count. */
    public final transient Function<Entry<T, Integer>, Integer> FN_GET_COUNT = new Function<Entry<T, Integer>, Integer>() {
        @Override
        public Integer apply(final Entry<T, Integer> from) {
            return from.getValue();
        }
    };
    
    // Constructors here
    /**
     * 
     * ItemPool Constructor.
     * 
     * @param cls
     *            a T
     */
    public ItemPool(final Class<T> cls) {
        this(new LinkedHashMap<T, Integer>(), cls);
    }

    @SuppressWarnings("unchecked")
    public static <Tin extends InventoryItem, Tout extends InventoryItem> ItemPool<Tout> createFrom(final ItemPool<Tin> from, final Class<Tout> clsHint) {
        final ItemPool<Tout> result = new ItemPool<Tout>(clsHint);
        if (from != null) {
            for (final Entry<Tin, Integer> e : from) {
                final Tin srcKey = e.getKey();
                if (clsHint.isInstance(srcKey)) {
                    result.add((Tout) srcKey, e.getValue());
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
                    result.add((Tout) srcKey, Integer.valueOf(1));
                }
            }
        }
        return result;
    }

    protected ItemPool(final Map<T, Integer> inMap, final Class<T> cls) {
        this.items = inMap;
        this.myClass = cls;
    }

    // Data members
    /** The items. */
    protected final Map<T, Integer> items;

    /** The my class. */
    private final Class<T> myClass; // class does not keep this in runtime by
                                    // itself

    /**
     * iterator.
     * 
     * @return Iterator<Entry<T, Integer>>
     */
    @Override
    public final Iterator<Entry<T, Integer>> iterator() {
        return this.items.entrySet().iterator();
    }

    // Items read only operations
    /**
     * 
     * contains.
     * 
     * @param item
     *            a T
     * @return boolean
     */
    public final boolean contains(final T item) {
        if (this.items == null) {
            return false;
        }
        return this.items.containsKey(item);
    }

    /**
     * 
     * count.
     * 
     * @param item
     *            a T
     * @return int
     */
    public final int count(final T item) {
        if (this.items == null || item == null) {
            return 0;
        }
        final Integer boxed = this.items.get(item);
        return boxed == null ? 0 : boxed.intValue();
    }

    /**
     * 
     * countAll.
     * 
     * @return int
     */
    public final int countAll() {
        return countAll(null, myClass); 
    }

    public final int countAll(Predicate<T> condition) {
        return countAll(condition, myClass); 
    }
    
    public final <U extends InventoryItem> int countAll(Predicate<U> condition, Class<U> cls) {
        int result = 0;
        if (this.items != null) {
            final boolean isSameClass = cls == myClass;
            for (final Entry<T, Integer> kv : this) {
                final T key = kv.getKey();
                @SuppressWarnings("unchecked")
                final U castKey = isSameClass || cls.isInstance(key) ? (U)key : null;
                if (null == condition || castKey != null && condition.apply(castKey))
                    result += kv.getValue();
            }
        }
        return result;
    }
    
    /**
     * 
     * countDistinct.
     * 
     * @return int
     */
    public final int countDistinct() {
        return this.items.size();
    }

    /**
     * 
     * isEmpty.
     * 
     * @return boolean
     */
    public final boolean isEmpty() {
        return (this.items == null) || this.items.isEmpty();
    }

    /**
     * 
     * toFlatList.
     * 
     * @return List<T>
     */
    public final List<T> toFlatList() {
        final List<T> result = new ArrayList<T>();
        for (final Entry<T, Integer> e : this) {
            for (int i = 0; i < e.getValue(); i++) {
                result.add(e.getKey());
            }
        }
        return result;
    }

    /**
     * Gets the my class.
     * 
     * @return the myClass
     */
    public Class<T> getMyClass() {
        return this.myClass;
    }    
    
    // get
    /**
     * 
     * Get item view.
     * 
     * @return a ItemPoolView
     */
    public ItemPool<T> getView() {
        return new ItemPool<T>(Collections.unmodifiableMap(this.items), this.getMyClass());
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
        if (item == null || amount <= 0) {
            return;
        }
        this.items.put(item, Integer.valueOf(this.count(item) + amount));
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
            this.items.remove(item);
        } else {
            this.items.put(item, count - amount);
        }
        return true;
    }

    public boolean removeAll(final T item) {
        return this.items.remove(item) != null;
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
        this.items.clear();
    }
}
