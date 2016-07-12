package forge.card;

/** 
 * CardAiHints holds all the different types of card hints for AI decks.
 *
 */
public class CardAiHints {

    
    private final boolean isRemovedFromAIDecks;
    private final boolean isRemovedFromRandomDecks;

    private final DeckHints deckHints;
    private final DeckHints deckNeeds;


    public CardAiHints(boolean remAi, boolean remRandom, DeckHints dh, DeckHints dn) {
        isRemovedFromAIDecks = remAi;
        isRemovedFromRandomDecks = remRandom;
        deckHints = dh;
        deckNeeds = dn;
    }

    /**
     * Gets the rem ai decks.
     * 
     * @return the rem ai decks
     */
    public boolean getRemAIDecks() {
        return this.isRemovedFromAIDecks;
    }

    /**
     * Gets the rem random decks.
     * 
     * @return the rem random decks
     */
    public boolean getRemRandomDecks() {
        return this.isRemovedFromRandomDecks;
    }

    /**
     * @return the deckHints
     */
    public DeckHints getDeckHints() {
        return deckHints;
    }

    /**
     * @return the deckHints
     */
    public DeckHints getDeckNeeds() {
        return deckNeeds;
    }

    /**
     * Gets the ai status comparable.
     * 
     * @return the ai status comparable
     */
    public Integer getAiStatusComparable() {
        if (this.isRemovedFromAIDecks && this.isRemovedFromRandomDecks) {
            return Integer.valueOf(3);
        } else if (this.isRemovedFromAIDecks) {
            return Integer.valueOf(4);
        } else if (this.isRemovedFromRandomDecks) {
            return Integer.valueOf(2);
        } else {
            return Integer.valueOf(1);
        }
    }


}
