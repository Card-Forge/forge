package forge.card;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import forge.CardList;

import net.slightlymagic.braids.util.lambda.Lambda1;

/**
 * <p>CardPoolView class.</p>
 *
 * @author Forge
 * @version $Id: CardPoolView.java 9708 2011-08-09 19:34:12Z jendave $
 */
public class CardPoolView implements Iterable<Entry<CardPrinted, Integer>> {

    // Field Accessors for select/aggregate operations with filters.
    public final static Lambda1<CardRules, Entry<CardPrinted, Integer>> fnToCard =
        new Lambda1<CardRules, Entry<CardPrinted, Integer>>() {
          @Override public CardRules apply(final Entry<CardPrinted, Integer> from) { return from.getKey().getCard(); }
        };
    public final static Lambda1<CardPrinted, Entry<CardPrinted, Integer>> fnToReference =
        new Lambda1<CardPrinted, Entry<CardPrinted, Integer>>() {
          @Override public CardPrinted apply(final Entry<CardPrinted, Integer> from) { return from.getKey(); }
        };

    public final static Lambda1<Integer, Entry<CardPrinted, Integer>> fnToCount =
        new Lambda1<Integer, Entry<CardPrinted, Integer>>() {
            @Override public Integer apply(final Entry<CardPrinted, Integer> from) { return from.getValue(); }
        };

    // Constructors
    public CardPoolView() { cards = new Hashtable<CardPrinted, Integer>(); }
    public CardPoolView(final Map<CardPrinted, Integer> inMap) { cards = inMap; }

    // Data members
    protected Map<CardPrinted, Integer> cards;

    // same thing as above, it was copied to provide sorting (needed by table views in deck editors) 
    protected List<Entry<CardPrinted, Integer>> cardsListOrdered = new ArrayList<Map.Entry<CardPrinted,Integer>>();
    protected boolean isListInSync = false;

    @Override
    public final Iterator<Entry<CardPrinted, Integer>> iterator() { return cards.entrySet().iterator(); }

    // Cards read only operations
    public final boolean contains(final CardPrinted card) {
        if (cards == null) { return false; }
        return cards.containsKey(card);
    }
    public final int count(final CardPrinted card) {
        if (cards == null) { return 0; }
        Integer boxed = cards.get(card);
        return boxed == null ? 0 : boxed.intValue();
    }
    public final int countAll() {
        int result = 0;
        if (cards != null) { for (Integer n : cards.values()) { result += n; } }
        return result;
    }
    public final int countDistinct() { return cards.size(); }
    public final boolean isEmpty() { return cards == null || cards.isEmpty(); }

    public final List<Entry<CardPrinted, Integer>> getOrderedList() {
        if (!isListInSync) { rebuildOrderedList(); }
        return cardsListOrdered;
    }

    private void rebuildOrderedList() {
        cardsListOrdered.clear();
        if (cards != null) {
            for (Entry<CardPrinted, Integer> e : cards.entrySet()) {
                cardsListOrdered.add(e);
            }
        }
        isListInSync = true;
    }

    public final List<CardPrinted> toFlatList() {
        List<CardPrinted> result = new ArrayList<CardPrinted>();
        for (Entry<CardPrinted, Integer> e : this) {
            for (int i = 0; i < e.getValue(); i++) { result.add(e.getKey()); }
        }
        return result;
    }

    public final CardList toForgeCardList() {
        CardList result = new CardList();
        for (Entry<CardPrinted, Integer> e : this) {
            for (int i = 0; i < e.getValue(); i++) {
                result.add(e.getKey().toForgeCard());
            }
        }
        return result;
    }
}
