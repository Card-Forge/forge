package forge.screens.match.views;

import forge.Forge.Graphics;
import forge.assets.FSkinTexture;
import forge.toolbox.FContainer;

public class VField extends FContainer {

    private boolean flipped;

    public VField() {
    }

    public boolean isFlipped() {
        return flipped;
    }
    public void setFlipped(boolean flipped0) {
        flipped = flipped0;
    }

    @Override
    protected void doLayout(float width, float height) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void drawBackground(Graphics g) {
    }
}
