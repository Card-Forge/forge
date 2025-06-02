package forge.toolbox;

import forge.Graphics;
import forge.assets.FSkinColor;

public class FPanel extends FScrollPane {
    private FSkinColor backColor;

    public FSkinColor getBackColor() {
        return backColor;
    }
    public void setBackColor(FSkinColor backColor0) {
        backColor = backColor0;
    }

    @Override
    public void drawBackground(Graphics g) {
        if (backColor != null) {
            g.fillRect(backColor, 0, 0, getWidth(), getHeight());
        }
    }

    @Override
    protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        return new ScrollBounds(visibleWidth, visibleHeight);
    }
}
