package forge.game.limited;


import java.util.List;

import forge.item.PaperCard;


/**
 * Deck built from a Booster Draft.
 * 
 */
public class BoosterDeckBuilder extends LimitedDeckBuilder {

    /**
     * Constructor.
     * 
     * @param dList
     *            list of cards drafted
     * @param pClrs
     *            colors
     */
    public BoosterDeckBuilder(List<PaperCard> dList, DeckColors pClrs) {
        super(dList, pClrs);
    }

}
