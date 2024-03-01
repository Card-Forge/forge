package forge.game.player.actions;

import forge.game.GameEntityView;

public class PayCostAction extends PlayerAction {
    public PayCostAction(GameEntityView cardView) {
        super(cardView);
        name = "Pay cost";
        gameEntityView = cardView;
    }
}
