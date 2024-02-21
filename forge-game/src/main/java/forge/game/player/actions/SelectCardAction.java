package forge.game.player.actions;

import forge.game.GameEntityView;

public class SelectCardAction extends PlayerAction{
    public SelectCardAction(GameEntityView cardView) {
        super(cardView);
        name = "Select card";
    }


}
