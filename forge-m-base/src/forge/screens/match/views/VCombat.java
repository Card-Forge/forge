package forge.screens.match.views;

import forge.screens.match.views.VHeader.HeaderDropDown;

public class VCombat extends HeaderDropDown {

    @Override
    public int getCount() {
        return -1;
    }

    @Override
    public void update() {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        return new ScrollBounds(visibleWidth, visibleHeight);
    }
}
