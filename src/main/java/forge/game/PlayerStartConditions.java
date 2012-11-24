package forge.game;

import com.google.common.base.Supplier;

import forge.Card;
import forge.deck.Deck;


public class PlayerStartConditions {
    private final Deck deck;
    private int startingLife = 20;
    private Supplier<Iterable<Card>> cardsOnTable = null;

    public PlayerStartConditions(Deck deck0) {
        deck = deck0;
    }

    public final Deck getDeck() {
        return deck;
    }
    public final int getStartingLife() {
        return startingLife;
    }
    public final Iterable<Card> getCardsOnTable() {
        return  cardsOnTable == null ? null : cardsOnTable.get();
    }

    public final void setStartingLife(int startingLife) {
        this.startingLife = startingLife;
    }

    public final void setCardsOnTable(Supplier<Iterable<Card>> cardsOnTable) {
        this.cardsOnTable = cardsOnTable;
    }


}
