package forge.game.player.actions;

import forge.game.GameEntityView;

public class SelectPlayerAction extends PlayerAction {
    public SelectPlayerAction(GameEntityView playerView) {
        super(playerView);
        name = "Select player";
    }

}