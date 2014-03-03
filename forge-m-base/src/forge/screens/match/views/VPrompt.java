package forge.screens.match.views;

import com.badlogic.gdx.graphics.Color;

import forge.Forge.Graphics;
import forge.toolbox.FContainer;
import forge.utils.Utils;

public class VPrompt extends FContainer {
    public static final float HEIGHT = Utils.AVG_FINGER_HEIGHT;

    @Override
    protected void doLayout(float width, float height) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void drawBackground(Graphics g) {
        float w = getWidth();
        float h = getHeight();
        g.fillRect(Color.ORANGE, 0, 0, w, h);
    }
}
