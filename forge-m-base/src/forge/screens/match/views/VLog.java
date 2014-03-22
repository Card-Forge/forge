package forge.screens.match.views;

import forge.game.GameLog;
import forge.menu.FDropDown;

public class VLog extends FDropDown {
    private final GameLog model;
    
    public VLog(GameLog model0) {
        model = model0;
    }

    @Override
    protected boolean autoHide() {
        return false;
    }

    @Override
    protected ScrollBounds updateAndGetPaneSize(float maxWidth, float maxVisibleHeight) {
        
        return new ScrollBounds(maxWidth, maxVisibleHeight);
    }
}
