package forge.menu;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;
import forge.screens.FScreen;
import forge.toolbox.FDisplayObject;

public class FMenuTab extends FDisplayObject {
    private static final FSkinFont FONT = FSkinFont.get(12);
    private static final FSkinColor SEL_GRADIENT_BOTTOM = FDropDown.BORDER_COLOR;
    private static final FSkinColor SEL_GRADIENT_TOP = SEL_GRADIENT_BOTTOM.stepColor(30);
    private static final FSkinColor SEL_FORE_COLOR = SEL_GRADIENT_BOTTOM.getHighContrastColor();
    private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT).alphaColor(0.5f);

    private final FMenuBar menuBar;
    private final FDropDown dropDown;

    private String text;
    private float minWidth;

    public FMenuTab(String text0, FMenuBar menuBar0, FDropDown dropDown0) {
        menuBar = menuBar0;
        dropDown = dropDown0;
        setText(text0);
    }

    @Override
    public boolean tap(float x, float y, int count) {
        if (dropDown.isVisible()) {
            dropDown.hide();
        }
        else {
            dropDown.show();
        }
        return true;
    }

    public void setText(String text0) {
        text = text0;
        minWidth = FONT.getFont().getBounds(text).width;
        menuBar.revalidate();
    }

    @Override
    public void setVisible(boolean visible0) {
        if (isVisible() == visible0) { return; }
        super.setVisible(visible0);
        if (!visible0) {
            dropDown.hide();
        }
        menuBar.revalidate();
    }

    public float getMinWidth() {
        return minWidth;
    }

    @Override
    public void draw(Graphics g) {
        float x, y, w, h;
        float paddingX = 2;
        float paddingY = 2;

        FSkinColor foreColor;
        if (dropDown.isVisible()) {
            g.fillGradientRect(SEL_GRADIENT_TOP, SEL_GRADIENT_BOTTOM, true, paddingX, paddingY, getWidth() - 2 * paddingX, getHeight() - paddingY);
            foreColor = SEL_FORE_COLOR;
        }
        else { 
            foreColor = FORE_COLOR;
        }

        //draw right separator
        x = getWidth();
        y = getHeight() / 4;
        g.drawLine(1, FScreen.HEADER_LINE_COLOR, x, y, x, getHeight() - y);

        x = paddingX;
        y = paddingY;
        w = getWidth() - 2 * paddingX;
        h = getHeight() - 2 * paddingY;
        g.drawText(text, FONT, foreColor, x, y, w, h, false, HAlignment.CENTER, true);
    }
}
