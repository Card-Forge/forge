package forge.game.player.actions;

import forge.game.GameEntityView;

public class ActivateAbilityAction extends PlayerAction{
    public ActivateAbilityAction(GameEntityView cardView) {
        super(cardView);
        name = "Activate ability";
    }
}
