package forge.card;

import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;

/**
 * <p>CardPool class.</p>
 * Represents a list of cards with amount of each
 */
public final class CardPool extends CardPoolView  {

    // Contructors here
    public CardPool() { super(); }
    public CardPool(final List<String> names) { super(); addAllCards(CardDb.instance().getCards(names)); }

    // Copy ctor will create its own modifiable pool
    @SuppressWarnings("unchecked")
    public CardPool(final CardPoolView from) {
        super();
        cards = new Hashtable<CardPrinted, Integer>();
        cards.putAll(from.cards);
     }
    public CardPool(final Iterable<CardPrinted> list) {
        this(); addAllCards(list);
    }

    // get
    public CardPoolView getView() { return new CardPoolView(Collections.unmodifiableMap(cards)); }

    // Cards manipulation
    public void add(final CardPrinted card) { add(card, 1); }
    public void add(final CardPrinted card, final int amount) {
        cards.put(card, count(card) + amount);
        isListInSync = false;
    }
    public void addAllCards(final Iterable<CardPrinted> cards) {
        for (CardPrinted cr : cards) { add(cr); }
        isListInSync = false;
    }
    public void addAll(final Iterable<Entry<CardPrinted, Integer>> map) {
        for (Entry<CardPrinted, Integer> e : map) { add(e.getKey(), e.getValue()); }
        isListInSync = false;
    }
    public void addAll(final Entry<CardPrinted, Integer>[] map) {
        for (Entry<CardPrinted, Integer> e : map) { add(e.getKey(), e.getValue()); }
        isListInSync = false;
    }

    public void remove(final CardPrinted card) { remove(card, 1); }
    public void remove(final CardPrinted card, final int amount) {
        int count = count(card);
        if (count == 0) { return; }
        if (count <= amount) { cards.remove(card); }
        else { cards.put(card, count - amount); }
        isListInSync = false;
    }
    public void removeAll(final Iterable<Entry<CardPrinted, Integer>> map) {
        for (Entry<CardPrinted, Integer> e : map) { remove(e.getKey(), e.getValue()); }
        isListInSync = false;
    }

    public void clear() { cards.clear(); isListInSync = false; }
}
