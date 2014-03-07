package forge.toolbox;

import com.badlogic.gdx.graphics.Color;

import forge.Forge.Graphics;

public class FCardPanel extends FDisplayObject {
    public final static float ASPECT_RATIO = 3.5f / 2.5f;
    private static final float PADDING = 3; //scale to leave vertical space between

    @Override
    public void draw(Graphics g) {
        float y = PADDING;
        float h = getHeight() - 2 * y;
        float w = h / ASPECT_RATIO;
        float x = (getWidth() - w) / 2;

        g.fillRect(Color.BLUE, x, y, w, h);
    }
}
