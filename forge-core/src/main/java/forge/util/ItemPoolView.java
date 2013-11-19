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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import forge.item.InventoryItem;


/**
 * <p>
 * ItemPoolView class.
 * </p>
 * 
 * @param <T>
 *            an InventoryItem
 * @author Forge
 * @version $Id: ItemPoolView.java 9708 2011-08-09 19:34:12Z jendave $
 */
public class ItemPoolView<T extends InventoryItem> implements Iterable<Entry<T, Integer>> {

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

    // Constructors
    public ItemPoolView(final Class<T> cls) {
        this(new Hashtable<T, Integer>(), cls);
    }

    public ItemPoolView(final Map<T, Integer> inMap, final Class<T> cls) {
        this.items = inMap;
        this.myClass = cls;
    }

    // Data members
    /** The items. */
    private final Map<T, Integer> items;

    /** The my class. */
    private final Class<T> myClass; // class does not keep this in runtime by
                                    // itself

    // same thing as above, it was copied to provide sorting (needed by table
    // views in deck editors)
    /** The items ordered. */
    private final transient List<Entry<T, Integer>> itemsOrdered = new ArrayList<Map.Entry<T, Integer>>();

    /** Whether list is in sync. */
    protected transient boolean isListInSync = false;

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
        if (this.items == null) {
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
     * getOrderedList.
     * 
     * @return List<Entry<T, Integer>>
     */
    public final List<Entry<T, Integer>> getOrderedList() {
        if (!this.isListInSync) {
            this.rebuildOrderedList();
        }
        return this.itemsOrdered;
    }

    private void rebuildOrderedList() {
        this.itemsOrdered.clear();
        if (this.items != null) {
            for (final Entry<T, Integer> e : this.items.entrySet()) {
                this.itemsOrdered.add(e);
            }
        }
        this.isListInSync = true;
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
     * Gets the items.
     * 
     * @return the items
     */
    protected Map<T, Integer> getItems() {
        return this.items;
    }

    /**
     * Gets the my class.
     * 
     * @return the myClass
     */
    public Class<T> getMyClass() {
        return this.myClass;
    }


    /**
     * To item list string.
     *
     * @return the iterable
     */
    public Iterable<String> toItemListString() {
        final List<String> list = new ArrayList<String>();
        for (final Entry<T, Integer> e : this.items.entrySet()) {
            list.add(String.format("%d x %s", e.getValue(), e.getKey().getName()));
        }
        return list;
    }
}
