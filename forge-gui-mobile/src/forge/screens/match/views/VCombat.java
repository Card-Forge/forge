package forge.screens.match.views;

import forge.menu.FDropDown;

public class VCombat extends FDropDown {

    @Override
    protected boolean autoHide() {
        return false;
    }

    @Override
    protected ScrollBounds updateAndGetPaneSize(float maxWidth, float maxVisibleHeight) {
        
        return new ScrollBounds(maxWidth, maxVisibleHeight);
    }
}
