package forge.game;

import com.google.common.base.Supplier;

import forge.Card;
import forge.deck.Deck;


public class PlayerStartConditions {
    private final Deck originalDeck;
    private Deck currentDeck;
    
    private int startingLife = 20;
    private int startingHand = 7;
    private Supplier<Iterable<Card>> cardsOnBattlefield = null;
    private Supplier<Iterable<Card>> cardsInCommand = null;
    private Supplier<Iterable<Card>> schemes = null;

    public PlayerStartConditions(Deck deck0) {
        originalDeck = deck0;
        currentDeck = originalDeck;
    }

    public final Deck getOriginalDeck() {
        return originalDeck;
    }
    
    public final Deck getCurrentDeck() {
        return currentDeck;
    }
    
    public final int getStartingLife() {
        return startingLife;
    }
    public final Iterable<Card> getCardsOnBattlefield() {
        return cardsOnBattlefield == null ? null : cardsOnBattlefield.get();
    }

    public final void setStartingLife(int startingLife) {
        this.startingLife = startingLife;
    }

    public final void setCardsOnBattlefield(Supplier<Iterable<Card>> cardsOnTable) {
        this.cardsOnBattlefield = cardsOnTable;
    }

    /**
     * @return the startingHand
     */
    public int getStartingHand() {
        return startingHand;
    }

    /**
     * @param startingHand0 the startingHand to set
     */
    public void setStartingHand(int startingHand0) {
        this.startingHand = startingHand0;
    }

    /**
     * @return the cardsInCommand
     */
    public Iterable<Card> getCardsInCommand() {
        return cardsInCommand == null ? null : cardsInCommand.get();
    }

    /**
     * @param cardsInCommand0 the cardsInCommand to set
     */
    public void setCardsInCommand(Supplier<Iterable<Card>> cardsInCommand0) {
        this.cardsInCommand = cardsInCommand0;
    }

    /**
     * @return the schemes
     */
    public Iterable<Card> getSchemes() {
        return schemes == null ? null : schemes.get();
    }

    /**
     * @param schemes0 the schemes to set
     */
    public void setSchemes(Supplier<Iterable<Card>> s) {
        this.schemes = s;
    }

    /**
     * TODO: Write javadoc for this method.
     */
    public void restoreOriginalDeck() {
        currentDeck = originalDeck;
    }


}
