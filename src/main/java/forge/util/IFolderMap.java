package forge.util;


/** 
 * TODO: Write javadoc for this type.
 *
 * @param <T>
 */
public interface IFolderMap<T extends IHasName> extends IFolderMapView<T> {

    /**
     * <p>
     * addDeck.
     * </p>
     * 
     * @param deck
     *            a {@link forge.deck.Deck} object.
     */
    public abstract void add(final T deck);

    /**
     * <p>
     * deleteDeck.
     * </p>
     * 
     * @param deckName
     *            a {@link java.lang.String} object.
     */
    public abstract void delete(final String deckName);

    /**
     * <p>
     * isUnique.
     * </p>
     * 
     * @param deckName
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public abstract boolean isUnique(final String name);

}
