package forge.game.limited;

import forge.card.CardPoolView;
import forge.card.CardPrinted;
import forge.deck.Deck;
import java.util.Map;
import java.util.TreeMap;

/**
 * <p>BoosterDraft interface.</p>
 *
 * @author Forge
 * @version $Id$
 */
public interface BoosterDraft {
    /**
     * <p>nextChoice.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    CardPoolView<CardPrinted> nextChoice();

    /**
     * <p>setChoice.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    void setChoice(CardPrinted c);

    /**
     * <p>hasNextChoice.</p>
     *
     * @return a boolean.
     */
    boolean hasNextChoice();

    /**
     * <p>getDecks.</p>
     *
     * @return an array of {@link forge.deck.Deck} objects.
     */
    Deck[] getDecks(); //size 7, all the computers decks

    /** Constant <code>LandSetCode="{}"</code>. */
    public String LandSetCode[] = {""};
    
    /** Constant <code>draftFormat="{}"</code>. */
    public String draftFormat[] = {""};
    
    /** Constant <code>draftPicks="{}"</code>. */
    public Map<String,Float> draftPicks = new TreeMap<String,Float>();
}






