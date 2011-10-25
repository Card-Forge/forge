package forge.item;

import java.util.Collections;
import java.util.Map.Entry;


/**
 * <p>CardPool class.</p>
 * Represents a list of cards with amount of each
 * @param <T> an Object
 */
public final class ItemPool<T extends InventoryItem> extends ItemPoolView<T>  {

    // Constructors here
    /**
     * 
     * ItemPool Constructor.
     * @param cls a T
     */
    public ItemPool(final Class<T> cls) { super(cls); }

    /**
     * 
     * ItemPool Constructor.
     * @param names a String
     * @param cls a T
     */
    @SuppressWarnings("unchecked") // conversion here must be safe
    public ItemPool(final Iterable<String> names, final Class<T> cls) {
        super(cls);
        addAllCards((Iterable<T>) CardDb.instance().getCards(names));
        }

    /**
     * 
     * createFrom method.
     * @param from a Tin
     * @param clsHint a Tout
     * @param <Tin> an InventoryItem
     * @param <Tout> an InventoryItem
     * @return InventoryItem
     */
    @SuppressWarnings("unchecked")
    public static <Tin extends InventoryItem, Tout extends InventoryItem> ItemPool<Tout>
        createFrom(final ItemPoolView<Tin> from, final Class<Tout> clsHint)
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

    /**
     * 
     * createFrom method.
     * @param from a Iterable<Tin>
     * @param clsHint a Class<Tout>
     * @return <Tin> an InventoryItem
     * @param <Tin> an InventoryItem
     * @param <Tout> an InventoryItem
     */
    @SuppressWarnings("unchecked")
    public static <Tin extends InventoryItem, Tout extends InventoryItem> ItemPool<Tout>
        createFrom(final Iterable<Tin> from, final Class<Tout> clsHint)
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
    /**
     * 
     * Get item view.
     * @return a ItemPoolView
     */
    public ItemPoolView<T> getView() { return new ItemPoolView<T>(Collections.unmodifiableMap(cards), myClass); }

    // Cards manipulation
    /**
     * 
     * Add Card.
     * @param card a T
     */
    public void add(final T card) {
        add(card, 1);
        }
    /**
     * 
     * add method.
     * @param card a T
     * @param amount a int
     */
    public void add(final T card, final int amount) {
        if (amount <= 0) { return; }
        cards.put(card, count(card) + amount);
        isListInSync = false;
    }

    private void put(final T card, final int amount) {
        cards.put(card, amount);
        isListInSync = false;
    }

    /**
     * 
     * addAllCards.
     * @param cards a Iterable<U>
     * @param <U> a InventoryItem
     */
    @SuppressWarnings("unchecked")
    public <U extends InventoryItem> void addAllCards(final Iterable<U> cards) {
        for (U cr : cards) { if (myClass.isInstance(cr)) { add((T) cr); } }
        isListInSync = false;
    }

    /**
     * 
     * addAll.
     * @param map a Iterable<Entry<U, Integer>>
     * @param <U> an InventoryItem
     */
    @SuppressWarnings("unchecked")
    public <U extends InventoryItem> void addAll(final Iterable<Entry<U, Integer>> map) {
        for (Entry<U, Integer> e : map) {
            if (myClass.isInstance(e.getKey())) {
                add((T) e.getKey(), e.getValue());
            }
        }
        isListInSync = false;
    }

    /**
     * 
     * Remove.
     * @param card a T
     */
    public void remove(final T card) {
        remove(card, 1);
        }

    /**
     * 
     * Remove.
     * @param card a T
     * @param amount a int
     */
    public void remove(final T card, final int amount) {
        int count = count(card);
        if (count == 0 || amount <= 0) { return; }
        if (count <= amount) { cards.remove(card); }
        else { cards.put(card, count - amount); }
        isListInSync = false;
    }
    /**
     * 
     * RemoveAll.
     * @param map a T
     */
    public void removeAll(final Iterable<Entry<T, Integer>> map) {
        for (Entry<T, Integer> e : map) { remove(e.getKey(), e.getValue()); }
        isListInSync = false;
    }

    /**
     * 
     * Clear.
     */
    public void clear() { cards.clear(); isListInSync = false; }
}
