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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import com.google.common.collect.Iterables;
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
public class ItemPool<T extends InventoryItem> implements Iterable<Entry<T, Integer>>, Serializable {
    private static final long serialVersionUID = 6572047177527559797L;

    public final transient Function<Entry<T, Integer>, T> FN_GET_KEY = new Function<Entry<T, Integer>, T>() {
        @Override
        public T apply(final Entry<T, Integer> from) {
            return from.getKey();
        }
    };

    public final transient Function<Entry<T, Integer>, String> FN_GET_NAME = new Function<Entry<T, Integer>, String>() {
        @Override
        public String apply(final Entry<T, Integer> from) {
            return from.getKey().getName();
        }
    };

    public final transient Function<Entry<T, Integer>, Integer> FN_GET_COUNT = new Function<Entry<T, Integer>, Integer>() {
        @Override
        public Integer apply(final Entry<T, Integer> from) {
            return from.getValue();
        }
    };

    public ItemPool(final Class<T> cls) {
        this(new ConcurrentHashMap<>(), cls);
    }

    @SuppressWarnings("unchecked")
    public static <Tin extends InventoryItem, Tout extends InventoryItem> ItemPool<Tout> createFrom(final ItemPool<Tin> from, final Class<Tout> clsHint) {
        final ItemPool<Tout> result = new ItemPool<>(clsHint);
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
        final ItemPool<Tout> result = new ItemPool<>(clsHint);
        if (from != null) {
            for (final Tin srcKey : from) {
                if (clsHint.isInstance(srcKey)) {
                    result.add((Tout) srcKey, Integer.valueOf(1));
                }
            }
        }
        return result;
    }

    protected ItemPool(final Map<T, Integer> items0, final Class<T> cls) {
        if (items0 != null) {
            items = items0;
        }
        else {
            items = new ConcurrentHashMap<>();
        }
        myClass = cls;
    }

    // Data members
    protected final Map<T, Integer> items;

    private final Class<T> myClass; //class does not keep this in runtime by itself

    @Override
    public final Iterator<Entry<T, Integer>> iterator() {
        return items.entrySet().iterator();
    }

    public final boolean contains(final T item) {
        return items.containsKey(item);
    }

    public final int count(final T item) {
        if (item == null) {
            return 0;
        }
        final Integer boxed = items.get(item);
        return boxed == null ? 0 : boxed.intValue();
    }

    public final int countAll() {
        int count = 0;
        for (Entry<T, Integer> e : this) {
            count += e.getValue();
        }
        return count;
    }

    public final int countAll(Predicate<T> condition) {
        int count = 0;
        for (Entry<T, Integer> e : this) {
            if (condition.apply(e.getKey())) {
                count += e.getValue();
            }
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    public final <U extends InventoryItem> int countAll(Predicate<U> condition, Class<U> cls) {
        int count = 0;
        Iterable<T> matchingKeys = Iterables.filter(this.items.keySet(), new Predicate<T>() {
            @Override
            public boolean apply(T item) {
                return cls.isInstance(item) && condition.apply((U)item);
            }
        });
        for (T key : matchingKeys)
            count += this.items.get(key);
        return count;
    }

    public final int countDistinct() {
        return items.size();
    }

    public final boolean isEmpty() {
        return items.isEmpty();
    }

    public final List<T> toFlatList() {
        final List<T> result = new ArrayList<>();
        for (final Entry<T, Integer> e : this) {
            for (int i = 0; i < e.getValue(); i++) {
                result.add(e.getKey());
            }
        }
        return result;
    }

    public Map<String, Integer> toNameLookup() {
        final Map<String, Integer> result = new HashMap<>();
        for (final Entry<T, Integer> e : this) {
            result.put(e.getKey().getName(), e.getValue());
        }
        return result;
    }

    public Class<T> getMyClass() {
        return myClass;
    }

    public ItemPool<T> getView() {
        return new ItemPool<>(Collections.unmodifiableMap(items), getMyClass());
    }

    public void add(final T item) {
        add(item, 1);
    }

    public void add(final T item, final int amount) {
        if (item == null || amount <= 0) { return; }

        items.put(item, count(item) + amount);
    }

    public void addAllFlat(final Iterable<T> itms) {
        for (T item : itms) {
            add(item);
        }
    }

    public void addAll(final Iterable<Entry<T, Integer>> map) {
        for (Entry<T, Integer> e : map) {
            add(e.getKey(), e.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    public <U extends InventoryItem> void addAllOfTypeFlat(final Iterable<U> itms) {
        for (U item : itms) {
            if (myClass.isInstance(item)) {
                add((T) item);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <U extends InventoryItem> void addAllOfType(final Iterable<Entry<U, Integer>> map) {
        for (Entry<U, Integer> e : map) {
            if (myClass.isInstance(e.getKey())) {
                add((T) e.getKey(), e.getValue());
            }
        }
    }

    public boolean remove(final T item) {
        return remove(item, 1);
    }

    public boolean remove(final T item, final int amount) {
        final int count = count(item);
        if (count == 0 || amount <= 0) {
            return false;
        }
        if (count <= amount) {
            items.remove(item);
        }
        else {
            items.put(item, count - amount);
        }
        return true;
    }

    public boolean removeAll(final T item) {
        return items.remove(item) != null;
    }

    public void removeAll(final Iterable<Entry<T, Integer>> map) {
        for (final Entry<T, Integer> e : map) {
            remove(e.getKey(), e.getValue());
        }
        // need not set out-of-sync: either remove did set, or nothing was removed
    }

    public void removeAllFlat(final Iterable<T> flat) {
        for (final T e : flat) {
            remove(e);
        }
        // need not set out-of-sync: either remove did set, or nothing was removed
    }

    public void clear() {
        items.clear();
    }
}
