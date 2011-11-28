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

import net.slightlymagic.braids.util.lambda.Lambda1;
import forge.CardList;
import forge.card.CardRules;

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

    // Field Accessors for select/aggregate operations with filters.
    /** The fn to card. */
    private final Lambda1<CardRules, Entry<T, Integer>> fnToCard = new Lambda1<CardRules, Entry<T, Integer>>() {
        @Override
        public CardRules apply(final Entry<T, Integer> from) {
            final T item = from.getKey();
            return item instanceof CardPrinted ? ((CardPrinted) item).getCard() : null;
        }
    };

    /** The fn to printed. */
    private final Lambda1<T, Entry<T, Integer>> fnToPrinted = new Lambda1<T, Entry<T, Integer>>() {
        @Override
        public T apply(final Entry<T, Integer> from) {
            return from.getKey();
        }
    };

    /** The fn to card name. */
    private final Lambda1<String, Entry<T, Integer>> fnToCardName = new Lambda1<String, Entry<T, Integer>>() {
        @Override
        public String apply(final Entry<T, Integer> from) {
            return from.getKey().getName();
        }
    };

    /** The fn to count. */
    private final Lambda1<Integer, Entry<T, Integer>> fnToCount = new Lambda1<Integer, Entry<T, Integer>>() {
        @Override
        public Integer apply(final Entry<T, Integer> from) {
            return from.getValue();
        }
    };

    // Constructors
    /**
     * 
     * ItemPoolView.
     * 
     * @param cls
     *            a Class<T>
     */
    public ItemPoolView(final Class<T> cls) {
        this.setCards(new Hashtable<T, Integer>());
        this.myClass = cls;
    }

    /**
     * 
     * ItemPoolView.
     * 
     * @param inMap
     *            a Map<T, Integer>
     * @param cls
     *            a Class<T>
     */
    public ItemPoolView(final Map<T, Integer> inMap, final Class<T> cls) {
        this.setCards(inMap);
        this.myClass = cls;
    }

    // Data members
    /** The cards. */
    private Map<T, Integer> cards;

    /** The my class. */
    private final Class<T> myClass; // class does not keep this in runtime by
                                    // itself

    // same thing as above, it was copied to provide sorting (needed by table
    // views in deck editors)
    /** The cards list ordered. */
    private final List<Entry<T, Integer>> cardsListOrdered = new ArrayList<Map.Entry<T, Integer>>();

    /** The is list in sync. */
    private boolean isListInSync = false;

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
        int result = 0;
        if (this.getCards() != null) {
            for (final Integer n : this.getCards().values()) {
                result += n;
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
     * 
     * toForgeCardList.
     * 
     * @return CardList
     */
    public final CardList toForgeCardList() {
        final CardList result = new CardList();
        for (final Entry<T, Integer> e : this) {
            if (e.getKey() instanceof CardPrinted) {
                for (int i = 0; i < e.getValue(); i++) {
                    result.add(((CardPrinted) e.getKey()).toForgeCard());
                }
            }
        }
        return result;
    }

    /**
     * Gets the cards.
     * 
     * @return the cards
     */
    public Map<T, Integer> getCards() {
        return this.cards;
    }

    /**
     * Sets the cards.
     * 
     * @param cards
     *            the cards to set
     */
    public void setCards(final Map<T, Integer> cards) {
        this.cards = cards; // TODO: Add 0 to parameter's name.
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
     * @param isListInSync
     *            the isListInSync to set
     */
    public void setListInSync(final boolean isListInSync) {
        this.isListInSync = isListInSync; // TODO: Add 0 to parameter's name.
    }

    /**
     * Gets the fn to card.
     * 
     * @return the fnToCard
     */
    public Lambda1<CardRules, Entry<T, Integer>> getFnToCard() {
        return this.fnToCard;
    }

    /**
     * Gets the fn to card name.
     * 
     * @return the fnToCardName
     */
    public Lambda1<String, Entry<T, Integer>> getFnToCardName() {
        return this.fnToCardName;
    }

    /**
     * Gets the fn to count.
     * 
     * @return the fnToCount
     */
    public Lambda1<Integer, Entry<T, Integer>> getFnToCount() {
        return this.fnToCount;
    }

    /**
     * Gets the fn to printed.
     * 
     * @return the fnToPrinted
     */
    public Lambda1<T, Entry<T, Integer>> getFnToPrinted() {
        return this.fnToPrinted;
    }
}
