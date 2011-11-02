package forge.game.limited;

import forge.deck.Deck;
import forge.item.CardPrinted;
import forge.item.ItemPoolView;

/**
 * <p>
 * BoosterDraft interface.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public interface IBoosterDraft {
    /**
     * <p>
     * nextChoice.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    ItemPoolView<CardPrinted> nextChoice();

    /**
     * <p>
     * setChoice.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    void setChoice(CardPrinted c);

    /**
     * <p>
     * hasNextChoice.
     * </p>
     * 
     * @return a boolean.
     */
    boolean hasNextChoice();

    /**
     * <p>
     * getDecks.
     * </p>
     * 
     * @return an array of {@link forge.deck.Deck} objects.
     */
    Deck[] getDecks(); // size 7, all the computers decks

    /** Constant <code>LandSetCode="{}"</code>. */
    String[] LAND_SET_CODE = { "" };

    /**
     * Called when drafting is over - to upload picks.
     */
    void finishedDrafting();

}
