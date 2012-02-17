package forge.util;

import java.util.Collection;


/** 
 * TODO: Write javadoc for this type.
 *
 * @param <T>
 */
public interface IFolderMapView<T extends IHasName> extends Iterable<T> {

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

    /**
     * <p>
     * getDeck.
     * </p>
     * 
     * @param deckName
     *            a {@link java.lang.String} object.
     * @return a {@link forge.deck.Deck} object.
     */
    public abstract T get(final String name);

    /**
     * 
     * Get names of decks.
     * 
     * @param deckType
     *            a GameType
     * @return a ArrayList<String>
     */
    public abstract Collection<String> getNames();


}