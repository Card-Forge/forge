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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Function;
import com.google.common.base.Predicate;


/**
 * <p>
 * CardPoolView class.
 * </p>
 * 
 * @param <T>
 *            an InventoryItem
 * @author Forge
 * @version $Id: CardPoolView.java 9708 2011-08-09 19:34:12Z jendave $
 */
public class ItemPoolView<T extends InventoryItem> implements Iterable<Entry<T, Integer>> {

    /** The fn to printed. */
    public final transient Function<Entry<T, Integer>, T> FN_GET_KEY = new Function<Entry<T, Integer>, T>() {
        @Override
        public T apply(final Entry<T, Integer> from) {
            return from.getKey();
        }
    };

    /** The fn to card name. */
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
        this.cards = inMap;
        this.myClass = cls;
    }

    // Data members
    /** The cards. */
    private final Map<T, Integer> cards;

    /** The my class. */
    private final Class<T> myClass; // class does not keep this in runtime by
                                    // itself

    // same thing as above, it was copied to provide sorting (needed by table
    // views in deck editors)
    /** The cards list ordered. */
    private final transient List<Entry<T, Integer>> cardsListOrdered = new ArrayList<Map.Entry<T, Integer>>();

    /** The is list in sync. */
    private transient boolean isListInSync = false;

    /**
     * iterator.
     * 
     * @return Iterator<Entry<T, Integer>>
     */
    @Override
    public final Iterator<Entry<T, Integer>> iterator() {
        return this.getCards().entrySet().iterator();
    }

    // Cards read only operations
    /**
     * 
     * contains.
     * 
     * @param card
     *            a T
     * @return boolean
     */
    public final boolean contains(final T card) {
        if (this.getCards() == null) {
            return false;
        }
        return this.getCards().containsKey(card);
    }

    /**
     * 
     * count.
     * 
     * @param card
     *            a T
     * @return int
     */
    public final int count(final T card) {
        if (this.getCards() == null) {
            return 0;
        }
        final Integer boxed = this.getCards().get(card);
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
        if (this.getCards() != null) {
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
        return this.getCards().size();
    }

    /**
     * 
     * isEmpty.
     * 
     * @return boolean
     */
    public final boolean isEmpty() {
        return (this.getCards() == null) || this.getCards().isEmpty();
    }

    /**
     * 
     * getOrderedList.
     * 
     * @return List<Entry<T, Integer>>
     */
    public final List<Entry<T, Integer>> getOrderedList() {
        if (!this.isListInSync()) {
            this.rebuildOrderedList();
        }
        return this.cardsListOrdered;
    }

    private void rebuildOrderedList() {
        this.cardsListOrdered.clear();
        if (this.getCards() != null) {
            for (final Entry<T, Integer> e : this.getCards().entrySet()) {
                this.cardsListOrdered.add(e);
            }
        }
        this.setListInSync(true);
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
     * Gets the cards.
     * 
     * @return the cards
     */
    protected Map<T, Integer> getCards() {
        return this.cards;
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
     * Checks if is list in sync.
     * 
     * @return the isListInSync
     */
    public boolean isListInSync() {
        return this.isListInSync;
    }

    /**
     * Sets the list in sync.
     * 
     * @param isListInSync0
     *            the isListInSync to set
     */
    protected void setListInSync(final boolean isListInSync0) {
        this.isListInSync = isListInSync0;
    }


    /**
     * To item list string.
     *
     * @return the iterable
     */
    public Iterable<String> toItemListString() {
        final List<String> list = new ArrayList<String>();
        for (final Entry<T, Integer> e : this.cards.entrySet()) {
            list.add(String.format("%d x %s", e.getValue(), e.getKey().getName()));
        }
        return list;
    }
}
