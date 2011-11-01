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
    private List<Entry<T, Integer>> cardsListOrdered = new ArrayList<Map.Entry<T, Integer>>();

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
     * @return the cards
     */
    public Map<T, Integer> getCards() {
        return cards;
    }

    /**
     * @param cards the cards to set
     */
    public void setCards(Map<T, Integer> cards) {
        this.cards = cards; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the myClass
     */
    public Class<T> getMyClass() {
        return myClass;
    }

    /**
     * @return the isListInSync
     */
    public boolean isListInSync() {
        return isListInSync;
    }

    /**
     * @param isListInSync the isListInSync to set
     */
    public void setListInSync(boolean isListInSync) {
        this.isListInSync = isListInSync; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the fnToCard
     */
    public Lambda1<CardRules, Entry<T, Integer>> getFnToCard() {
        return fnToCard;
    }

    /**
     * @return the fnToCardName
     */
    public Lambda1<String, Entry<T, Integer>> getFnToCardName() {
        return fnToCardName;
    }

    /**
     * @return the fnToCount
     */
    public Lambda1<Integer, Entry<T, Integer>> getFnToCount() {
        return fnToCount;
    }

    /**
     * @return the fnToPrinted
     */
    public Lambda1<T, Entry<T, Integer>> getFnToPrinted() {
        return fnToPrinted;
    }
}
