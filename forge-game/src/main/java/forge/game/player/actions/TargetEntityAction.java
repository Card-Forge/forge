package forge.game.player.actions;

import forge.game.GameEntityView;

public class TargetEntityAction extends PlayerAction {
    // TODO Add distribution damage/counters
    public TargetEntityAction(GameEntityView cardView) {
        super(cardView);
        name = "Target game entity";
    }
}
