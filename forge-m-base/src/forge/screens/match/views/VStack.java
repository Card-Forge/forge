package forge.screens.match.views;

import forge.screens.match.views.VHeader.HeaderDropDown;

public class VStack extends HeaderDropDown {

    public VStack() {
    }

    @Override
    public int getCount() {
        return 0;
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
