package forge.gui.input;

import java.util.Collection;

import forge.game.card.Card;

public class InputSelectCardsFromList extends InputSelectEntitiesFromList<Card> {
    private static final long serialVersionUID = 6230360322294805986L;

    public InputSelectCardsFromList(int min, int max, Collection<Card> validCards) {
        super(min, max, validCards); // to avoid hangs
    }
    
    public InputSelectCardsFromList(Collection<Card> validCards) {
        super(1, 1, validCards); // to avoid hangs
    }    
}