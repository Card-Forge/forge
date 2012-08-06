package forge.game.limited;


import java.util.List;

import forge.item.CardPrinted;


/**
 * Deck built from a Booster Draft.
 * 
 */
public class BoosterDeck extends LimitedDeck {

    /**
     * Constructor.
     * 
     * @param dList
     *            list of cards drafted
     * @param pClrs
     *            colors
     */
    public BoosterDeck(List<CardPrinted> dList, DeckColors pClrs) {
        super(dList, pClrs);
    }

}
