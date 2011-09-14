package forge.card;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

/**
 * <p>CardPool class.</p>
 * Represents a list of cards with amount of each
 */
public final class CardPool<T extends InventoryItem> extends CardPoolView<T>  {

    // Constructors here
    public CardPool() { super(); }

    @SuppressWarnings("unchecked") // conversion here must be safe
    public CardPool(final List<String> names) { super(); addAllCards((Iterable<T>) CardDb.instance().getCards(names)); }

    @SuppressWarnings("unchecked")
    public static <Tin extends InventoryItem, Tout extends InventoryItem> CardPool<Tout> 
        createFrom(CardPoolView<Tin> from, Class<Tout> clsHint) 
    {
        CardPool<Tout> result = new CardPool<Tout>();
        if (from != null) {
            for (Entry<Tin, Integer> e : from) {
                Tin srcKey = e.getKey();
                if (clsHint.isInstance(srcKey)) {
                    result.put((Tout) srcKey, e.getValue());
                }
            }
        }
        return result;
    }
    
    @SuppressWarnings("unchecked")
    public static <Tin extends InventoryItem, Tout extends InventoryItem> CardPool<Tout>
        createFrom(Iterable<Tin> from, Class<Tout> clsHint) {
        CardPool<Tout> result = new CardPool<Tout>();
        if (from != null) {
            for (Tin srcKey : from) {
                if (clsHint.isInstance(srcKey)) {
                    result.put((Tout) srcKey, Integer.valueOf(1));
                }
            }
        }
        return result;
    }

    // get
    public CardPoolView<T> getView() { return new CardPoolView<T>(Collections.unmodifiableMap(cards)); }

    // Cards manipulation
    public void add(final T card) { add(card, 1); }
    public void add(final T card, final int amount) {
        if (amount <= 0) { return; }
        cards.put(card, count(card) + amount);
        isListInSync = false;
    }
    private void put(final T card, final int amount) {
        cards.put(card, amount);
        isListInSync = false;
    }    
    public void addAllCards(final Iterable<T> cards) {
        for (T cr : cards) { add(cr); }
        isListInSync = false;
    }
    public void addAll(final Iterable<Entry<T, Integer>> map) {
        for (Entry<T, Integer> e : map) { add(e.getKey(), e.getValue()); }
        isListInSync = false;
    }

    public void remove(final T card) { remove(card, 1); }
    public void remove(final T card, final int amount) {
        int count = count(card);
        if (count == 0 || amount <= 0) { return; }
        if (count <= amount) { cards.remove(card); }
        else { cards.put(card, count - amount); }
        isListInSync = false;
    }
    public void removeAll(final Iterable<Entry<T, Integer>> map) {
        for (Entry<T, Integer> e : map) { remove(e.getKey(), e.getValue()); }
        isListInSync = false;
    }

    public void clear() { cards.clear(); isListInSync = false; }
}
