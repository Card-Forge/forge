package forge.match.input;

import java.util.Collection;

import forge.game.card.Card;
import forge.player.PlayerControllerHuman;

public class InputSelectCardsFromList extends InputSelectEntitiesFromList<Card> {
    private static final long serialVersionUID = 6230360322294805986L;

    public InputSelectCardsFromList(final PlayerControllerHuman controller, final int cnt, final Collection<Card> validCards) {
        super(controller, cnt, cnt, validCards); // to avoid hangs
    }
    
    public InputSelectCardsFromList(final PlayerControllerHuman controller, final int min, final int max, final Collection<Card> validCards) {
        super(controller, min, max, validCards); // to avoid hangs
    }
    
    public InputSelectCardsFromList(final PlayerControllerHuman controller, final Collection<Card> validCards) {
        super(controller, 1, 1, validCards); // to avoid hangs
    }    
}