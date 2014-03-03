package forge.screens.match.views;

import com.badlogic.gdx.graphics.Color;

import forge.Forge.Graphics;
import forge.screens.FScreen;
import forge.toolbox.FContainer;

public class VLog extends FContainer {
    public static final float HEIGHT = FScreen.BTN_HEIGHT; //TODO: Consider changing this

    @Override
    protected void doLayout(float width, float height) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void drawBackground(Graphics g) {
        float w = getWidth();
        float h = getHeight();
        g.fillRect(Color.MAGENTA, 0, 0, w, h);
    }
}
