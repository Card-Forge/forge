package forge.game.limited;


import forge.CardList;

/**
 * Deck built from a Booster Draft.
 * 
 */
public class BoosterDeck extends LimitedDeck {

    private static final long serialVersionUID = -7818685851099321964L;

    /**
     * Constructor.
     * 
     * @param dList
     *            list of cards drafted
     * @param pClrs
     *            colors
     */
    public BoosterDeck(CardList dList, DeckColors pClrs) {
        super(dList, pClrs);
        buildDeck();
    }

}
