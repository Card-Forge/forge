package forge.gamemodes.match.input;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.player.PlayerControllerHuman;
import forge.util.collect.FCollectionView;

public class InputSelectCardsFromList extends InputSelectEntitiesFromList<Card> {
    private static final long serialVersionUID = 6230360322294805986L;

    public InputSelectCardsFromList(final PlayerControllerHuman controller, final int cnt, final FCollectionView<Card> validCards) {
        this(controller, cnt, cnt, validCards);
    }
    
    public InputSelectCardsFromList(final PlayerControllerHuman controller, final int cnt, final FCollectionView<Card> validCards, final SpellAbility sa) {
    	this(controller, cnt, cnt, validCards, sa);
    }

    public InputSelectCardsFromList(final PlayerControllerHuman controller, final int min, final int max, final FCollectionView<Card> validCards) {
        super(controller, min, max, validCards);
    }

    public InputSelectCardsFromList(final PlayerControllerHuman controller, final int min, final int max, final FCollectionView<Card> validCards, final SpellAbility sa) {
    	super(controller, min, max, validCards, sa);
    }
    
}
