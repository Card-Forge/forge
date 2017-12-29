package forge.assets;

import forge.Graphics;

public class FSkinBorder {
    private final FSkinColor color;
    private final float thickness;

    public FSkinBorder(FSkinColor color0, float thickness0) {
        color = color0;
        thickness = thickness0;
    }

    public FSkinColor getColor() {
        return color;
    }

    public float getThickness() {
        return thickness;
    }

    public void draw(Graphics g, float x, float y, float w, float h) {
        x -= thickness;
        y -= thickness;
        w += 2 * thickness;
        h += 2 * thickness;
        g.fillRect(color, x, y, w, h); //draw filled rectangle behind object
    }
}
