package forge.item;

import java.util.Collections;
import java.util.Map.Entry;

/**
 * <p>
 * CardPool class.
 * </p>
 * Represents a list of cards with amount of each
 * 
 * @param <T>
 *            an Object
 */
public final class ItemPool<T extends InventoryItem> extends ItemPoolView<T> {

    // Constructors here
    /**
     * 
     * ItemPool Constructor.
     * 
     * @param cls
     *            a T
     */
    public ItemPool(final Class<T> cls) {
        super(cls);
    }

    /**
     * 
     * ItemPool Constructor.
     * 
     * @param names
     *            a String
     * @param cls
     *            a T
     */
    @SuppressWarnings("unchecked")
    // conversion here must be safe
    public ItemPool(final Iterable<String> names, final Class<T> cls) {
        super(cls);
        this.addAllCards((Iterable<T>) CardDb.instance().getCards(names));
    }

    /**
     * createFrom method.
     * 
     * @param <Tin>
     *            an InventoryItem
     * @param <Tout>
     *            an InventoryItem
     * @param from
     *            a Tin
     * @param clsHint
     *            a Tout
     * @return InventoryItem
     */
    @SuppressWarnings("unchecked")
    public static <Tin extends InventoryItem, Tout extends InventoryItem> ItemPool<Tout> createFrom(
            final ItemPoolView<Tin> from, final Class<Tout> clsHint) {
        final ItemPool<Tout> result = new ItemPool<Tout>(clsHint);
        if (from != null) {
            for (final Entry<Tin, Integer> e : from) {
                final Tin srcKey = e.getKey();
                if (clsHint.isInstance(srcKey)) {
                    result.put((Tout) srcKey, e.getValue());
                }
            }
        }
        return result;
    }

    /**
     * createFrom method.
     * 
     * @param <Tin>
     *            an InventoryItem
     * @param <Tout>
     *            an InventoryItem
     * @param from
     *            a Iterable<Tin>
     * @param clsHint
     *            a Class<Tout>
     * @return <Tin> an InventoryItem
     */
    @SuppressWarnings("unchecked")
    public static <Tin extends InventoryItem, Tout extends InventoryItem> ItemPool<Tout> createFrom(
            final Iterable<Tin> from, final Class<Tout> clsHint) {
        final ItemPool<Tout> result = new ItemPool<Tout>(clsHint);
        if (from != null) {
            for (final Tin srcKey : from) {
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
     * 
     * @return a ItemPoolView
     */
    public ItemPoolView<T> getView() {
        return new ItemPoolView<T>(Collections.unmodifiableMap(this.getCards()), this.getMyClass());
    }

    // Cards manipulation
    /**
     * 
     * Add Card.
     * 
     * @param card
     *            a T
     */
    public void add(final T card) {
        this.add(card, 1);
    }

    /**
     * 
     * add method.
     * 
     * @param card
     *            a T
     * @param amount
     *            a int
     */
    public void add(final T card, final int amount) {
        if (amount <= 0) {
            return;
        }
        this.getCards().put(card, this.count(card) + amount);
        this.setListInSync(false);
    }

    private void put(final T card, final int amount) {
        this.getCards().put(card, amount);
        this.setListInSync(false);
    }

    /**
     * addAllCards.
     * 
     * @param <U>
     *            a InventoryItem
     * @param cards
     *            a Iterable<U>
     */
    @SuppressWarnings("unchecked")
    public <U extends InventoryItem> void addAllCards(final Iterable<U> cards) {
        for (final U cr : cards) {
            if (this.getMyClass().isInstance(cr)) {
                this.add((T) cr);
            }
        }
        this.setListInSync(false);
    }

    /**
     * addAll.
     * 
     * @param <U>
     *            an InventoryItem
     * @param map
     *            a Iterable<Entry<U, Integer>>
     */
    @SuppressWarnings("unchecked")
    public <U extends InventoryItem> void addAll(final Iterable<Entry<U, Integer>> map) {
        for (final Entry<U, Integer> e : map) {
            if (this.getMyClass().isInstance(e.getKey())) {
                this.add((T) e.getKey(), e.getValue());
            }
        }
        this.setListInSync(false);
    }

    /**
     * 
     * Remove.
     * 
     * @param card
     *            a T
     */
    public void remove(final T card) {
        this.remove(card, 1);
    }

    /**
     * 
     * Remove.
     * 
     * @param card
     *            a T
     * @param amount
     *            a int
     */
    public void remove(final T card, final int amount) {
        final int count = this.count(card);
        if ((count == 0) || (amount <= 0)) {
            return;
        }
        if (count <= amount) {
            this.getCards().remove(card);
        } else {
            this.getCards().put(card, count - amount);
        }
        this.setListInSync(false);
    }

    /**
     * 
     * RemoveAll.
     * 
     * @param map
     *            a T
     */
    public void removeAll(final Iterable<Entry<T, Integer>> map) {
        for (final Entry<T, Integer> e : map) {
            this.remove(e.getKey(), e.getValue());
        }
        this.setListInSync(false);
    }

    /**
     * 
     * Clear.
     */
    public void clear() {
        this.getCards().clear();
        this.setListInSync(false);
    }
}
