package forge.item;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;


/**
 * <p>CardPool class.</p>
 * Represents a list of cards with amount of each
 */
public final class ItemPool<T extends InventoryItem> extends ItemPoolView<T>  {

    // Constructors here
    public ItemPool(final Class<T> cls) { super(cls); }

    @SuppressWarnings("unchecked") // conversion here must be safe
    public ItemPool(final List<String> names, final Class<T> cls) { super(cls); addAllCards((Iterable<T>) CardDb.instance().getCards(names)); }

    @SuppressWarnings("unchecked")
    public static <Tin extends InventoryItem, Tout extends InventoryItem> ItemPool<Tout> 
        createFrom(ItemPoolView<Tin> from, Class<Tout> clsHint) 
    {
        ItemPool<Tout> result = new ItemPool<Tout>(clsHint);
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
    public static <Tin extends InventoryItem, Tout extends InventoryItem> ItemPool<Tout>
        createFrom(Iterable<Tin> from, Class<Tout> clsHint)
    {
        ItemPool<Tout> result = new ItemPool<Tout>(clsHint);
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
    public ItemPoolView<T> getView() { return new ItemPoolView<T>(Collections.unmodifiableMap(cards), myClass); }

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
    
    @SuppressWarnings("unchecked")
    public <U extends InventoryItem> void addAllCards(final Iterable<U> cards) {
        for (U cr : cards) { if (myClass.isInstance(cr)) { add((T) cr); } }
        isListInSync = false;
    }

    @SuppressWarnings("unchecked")
    public <U extends InventoryItem> void addAll(final Iterable<Entry<U, Integer>> map) {
        for (Entry<U, Integer> e : map) {
            if (myClass.isInstance(e.getKey())) {
                add((T) e.getKey(), e.getValue());
            }
        }
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
