package forge.game.player.actions;

import forge.game.GameEntityView;

public class SelectPlayerAction extends PlayerAction {
    public SelectPlayerAction(GameEntityView playerView) {
        super(playerView, "Select player");
    }

    @Override
    public String describe() {
        return localize("lblMacroActionSelectPlayer", describeEntity());
    }
}
