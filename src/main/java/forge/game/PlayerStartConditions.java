package forge.game;

import java.util.List;

import forge.Card;
import forge.deck.Deck;


public class PlayerStartConditions {
    private final Deck deck;
    private int startingLife = 20;
    private List<Card> cardsOnTable = null;

    public PlayerStartConditions( Deck deck0 ) {
        deck = deck0;
    }

    public final Deck getDeck() {
        return deck;
    }
    public final int getStartingLife() {
        return startingLife;
    }
    public final List<Card> getCardsOnTable() {
        return cardsOnTable;
    }

    public final void setStartingLife(int startingLife) {
        this.startingLife = startingLife;
    }
    public final void setCardsOnTable(List<Card> cardsOnTable) {
        this.cardsOnTable = cardsOnTable;
    }


}
