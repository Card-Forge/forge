package forge;

/**
 * Created by IntelliJ IDEA.
 * User: dhudson
 * Date: 6/24/11
 *
 * @author Forge
 * @version $Id$
 */
interface DeckDisplay {
    /**
     * <p>updateDisplay.</p>
     *
     * @param top a {@link forge.CardList} object.
     * @param bottom a {@link forge.CardList} object.
     */
    public void updateDisplay(CardList top, CardList bottom);

    //top shows available card pool
    //if constructed, top shows all cards
    //if sealed, top shows 5 booster packs
    //if draft, top shows cards that were chosen
    /**
     * <p>getTop.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public CardList getTop();

    //bottom shows cards that the user has chosen for his library
    /**
     * <p>getBottom.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public CardList getBottom();

    /**
     * <p>setTitle.</p>
     *
     * @param message a {@link java.lang.String} object.
     */
    public void setTitle(String message);
}
