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

import com.google.common.collect.Maps;
import forge.item.InventoryItem;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import java.util.stream.Collector;

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
                    result.add((Tout) srcKey, 1);
                }
            }
        }
        return result;
    }

    public static <T extends InventoryItem> Collector<T, ?, ItemPool<T>> collector(Class<T> cls) {
        return new Collector<T, ItemPool<T>, ItemPool<T>>() {
            @Override
            public Supplier<ItemPool<T>> supplier() {
                return () -> new ItemPool<T>(cls);
            }

            @Override
            public BiConsumer<ItemPool<T>, T> accumulator() {
                return (pool, item) -> {
                    if (cls.isInstance(item)) pool.add(cls.cast(item), 1);
                };
            }

            @Override
            public BinaryOperator<ItemPool<T>> combiner() {
                return (first, second) -> {
                    first.addAll(second);
                    return first;
                };
            }

            @Override public Function<ItemPool<T>, ItemPool<T>> finisher() {
                return Function.identity();
            }
            @Override public Set<Characteristics> characteristics() {
                return EnumSet.of(Characteristics.IDENTITY_FINISH);
            }
        };
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
        return boxed == null ? 0 : boxed;
    }

    public final int countAll() {
        int count = 0;
        for (Entry<T, Integer> e : this) {
            count += e.getValue();
        }
        return count;
    }

    public int countAll(Predicate<T> condition){
        int count = 0;
        for (Integer v : Maps.filterKeys(this.items, condition::test).values())
            count += v;
        return count;

    }

    @SuppressWarnings("unchecked")
    public final <U extends InventoryItem> int countAll(Predicate<? super U> condition, Class<U> cls) {
        int count = 0;
        Map<T, Integer> matchingKeys = Maps.filterKeys(this.items, item -> cls.isInstance(item) && (condition.test((U)item)));
        for (Integer i : matchingKeys.values()) {
            count += i;
        }
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

    public void removeIf(Predicate<T> filter) {
        items.keySet().removeIf(filter);
    }

    public void retainIf(Predicate<T> filter) {
        items.keySet().removeIf(filter.negate());
    }

    public T find(Predicate<T> filter) {
        return items.keySet().stream().filter(filter).findFirst().orElse(null);
    }

    public void clear() {
        items.clear();
    }

    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof ItemPool ip) &&
                (this.items.equals(ip.items));
    }

    /**
     * Applies a predicate to this ItemPool's entries.
     *
     * @param predicate the Predicate to apply to this ItemPool
     * @return a new ItemPool made from this ItemPool with only the items that agree with the provided Predicate
     */
    public ItemPool<T> getFilteredPool(Predicate<T> predicate) {
        ItemPool<T> filteredPool = new ItemPool<>(myClass);
        for (T c : this.items.keySet()) {
            if (predicate.test(c))
                filteredPool.add(c, this.items.get(c));
        }
        return filteredPool;
    }

    /**
     * Returns the items in this pool that exceed the quantity available in the provided super-set.
     * <p>
     * For example, if you have a card pool and a deck that must be made using only cards from that pool,
     * you can call deck.skimOverflow(cardPool), and this will return the entries in the deck which exceed the
     * available quantity in the pool. Those cards could then be removed from the deck via removeAll, or added
     * to the pool via addAll.
     */
    public ItemPool<T> skimOverflow(ItemPool<T> superSet) {
        ItemPool<T> out = new ItemPool<>(this.myClass);
        for(Entry<T, Integer> entry : this.items.entrySet()) {
            int count = Math.max(0, entry.getValue() - superSet.count(entry.getKey()));
            if(count > 0)
                out.add(entry.getKey(), count);
        }
        return out;
    }
}
