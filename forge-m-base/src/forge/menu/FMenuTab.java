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
    private static final FSkinColor SEL_FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinColor FORE_COLOR = SEL_FORE_COLOR.alphaColor(0.5f);

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
            y = 0;
            w = getWidth();
            h = getHeight();
            float yAcross;
            y += paddingY;
            yAcross = y;
            y--;
            h++;
            g.fillRect(FDropDown.BACK_COLOR, 0, paddingY, w, getHeight() - paddingY);
            g.startClip(-1, y, w + 2, h); //use clip to ensure all corners connect
            g.drawLine(1, FScreen.HEADER_LINE_COLOR, 0, yAcross, w, yAcross);
            g.drawLine(1, FScreen.HEADER_LINE_COLOR, 0, y, 0, h);
            g.drawLine(1, FScreen.HEADER_LINE_COLOR, w, y, w, h);
            g.endClip();

            foreColor = SEL_FORE_COLOR;
        }
        else { //draw right separator
            x = getWidth();
            y = getHeight() / 4;
            g.drawLine(1, FScreen.HEADER_LINE_COLOR, x, y, x, getHeight() - y);

            foreColor = FORE_COLOR;
        }

        x = paddingX;
        y = paddingY;
        w = getWidth() - 2 * paddingX;
        h = getHeight() - 2 * paddingY;
        g.drawText(text, FONT, foreColor, x, y, w, h, false, HAlignment.CENTER, true);
    }
}
