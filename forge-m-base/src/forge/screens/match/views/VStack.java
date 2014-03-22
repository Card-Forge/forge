package forge.screens.match.views;

import forge.game.player.LobbyPlayer;
import forge.game.zone.MagicStack;
import forge.menu.FDropDown;

public class VStack extends FDropDown {
    private final MagicStack model;
    private final LobbyPlayer localPlayer;

    private int stackSize;

    public VStack(MagicStack model0, LobbyPlayer localPlayer0) {
        model = model0;
        localPlayer = localPlayer0;
    }

    @Override
    protected boolean autoHide() {
        return false;
    }

    @Override
    public void update() {
        if (stackSize != model.size()) {
            stackSize = model.size();
            getMenuTab().setText("Stack (" + stackSize + ")");
        }
        super.update();
    }

    @Override
    protected ScrollBounds updateAndGetPaneSize(float maxWidth, float maxVisibleHeight) {

        return new ScrollBounds(maxWidth, maxVisibleHeight);
    }
}
