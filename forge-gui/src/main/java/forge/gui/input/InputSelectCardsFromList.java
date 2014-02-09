package forge.gui.input;

import forge.game.card.Card;

import java.util.Collection;

public class InputSelectCardsFromList extends InputSelectEntitiesFromList<Card> {
    private static final long serialVersionUID = 6230360322294805986L;

    public InputSelectCardsFromList(int cnt, Collection<Card> validCards) {
        super(cnt, cnt, validCards); // to avoid hangs
    }
    
    public InputSelectCardsFromList(int min, int max, Collection<Card> validCards) {
        super(min, max, validCards); // to avoid hangs
    }
    
    public InputSelectCardsFromList(Collection<Card> validCards) {
        super(1, 1, validCards); // to avoid hangs
    }    
}