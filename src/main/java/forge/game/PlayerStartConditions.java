package forge.game;

import com.google.common.base.Supplier;

import forge.Card;
import forge.card.CardRules;
import forge.deck.Deck;
import forge.item.CardPrinted;


public class PlayerStartConditions {
    private final Deck deck;
    private int startingLife = 20;
    private int startingHand = 7;
    private Supplier<Iterable<Card>> cardsOnBattlefield = null;
    private Supplier<Iterable<Card>> cardsInCommand = null;

    public PlayerStartConditions( Deck deck0 ) {
        deck = deck0;
    }

    public final Deck getDeck() {
        return deck;
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


}
