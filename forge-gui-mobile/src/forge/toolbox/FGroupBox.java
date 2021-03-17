package forge.toolbox;

import com.badlogic.gdx.utils.Align;

import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.util.Utils;

public abstract class FGroupBox extends FContainer {
    private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinFont FONT = FSkinFont.get(16);
    private static final float PADDING = Utils.scale(5);
    private static final float BORDER_THICKNESS = Utils.scale(1);

    private final String caption;

    public FGroupBox(String caption0) {
        caption = caption0;
    }

    @Override
    protected void drawOverlay(Graphics g) {
        float w = getWidth();
        float h = getHeight();
        float x = 2 * PADDING;
        float y = FONT.getCapHeight() / 2;

        g.drawLine(BORDER_THICKNESS, FORE_COLOR, 0, y, 0, h); //draw left border
        g.drawLine(BORDER_THICKNESS, FORE_COLOR, 0, h, w, h); //draw bottom border
        g.drawLine(BORDER_THICKNESS, FORE_COLOR, w, h, w, y); //draw right border

        //draw caption
        g.drawText(caption, FONT, FORE_COLOR, x, 0, w - x - PADDING, h, false, Align.left, false);

        //draw border left of caption
        g.drawLine(BORDER_THICKNESS, FORE_COLOR, 0, y, x, y);

        //draw border right of caption if needed
        float captionEnd = x + FONT.getBounds(caption).width;
        if (captionEnd < w) {
            g.drawLine(BORDER_THICKNESS, FORE_COLOR, captionEnd, y, w, y);
        }
    }

    @Override
    protected void doLayout(float width, float height) {
        float captionHeight = FONT.getLineHeight();
        layoutBox(PADDING, captionHeight + PADDING, getWidth() - 2 * PADDING, getHeight() - captionHeight - 2 * PADDING);
    }

    protected abstract void layoutBox(float x, float y, float w, float h);
}
