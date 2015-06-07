package forge.match.input;

import forge.game.card.Card;
import forge.player.PlayerControllerHuman;
import forge.util.collect.FCollectionView;

public class InputSelectCardsFromList extends InputSelectEntitiesFromList<Card> {
    private static final long serialVersionUID = 6230360322294805986L;

    public InputSelectCardsFromList(final PlayerControllerHuman controller, final int cnt, final FCollectionView<Card> validCards) {
        super(controller, cnt, cnt, validCards); // to avoid hangs
    }

    public InputSelectCardsFromList(final PlayerControllerHuman controller, final int min, final int max, final FCollectionView<Card> validCards) {
        super(controller, min, max, validCards); // to avoid hangs
    }

}