package forge.toolbox;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.math.Vector2;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;

public class FTextArea extends FScrollPane {
    private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);

    private String text;
    private FSkinFont font;
    private HAlignment alignment;
    private Vector2 insets;

    public FTextArea() {
        this("");
    }
    public FTextArea(String text0) {
        text = text0;
        font = FSkinFont.get(14);
        alignment = HAlignment.LEFT;
        insets = new Vector2(0, 0);
    }

    public String getText() {
        return text;
    }
    public void setText(String text0) {
        text = text0;
        revalidate();
    }

    public HAlignment getAlignment() {
        return alignment;
    }
    public void setAlignment(HAlignment alignment0) {
        alignment = alignment0;
    }

    public void setFontSize(int fontSize0) {
        font = FSkinFont.get(fontSize0);
        revalidate();
    }

    @Override
    protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        TextBounds bounds = font.getFont().getWrappedBounds(text, visibleWidth - 2 * insets.x);
        return new ScrollBounds(visibleWidth, bounds.height + 2 * insets.y);
    }

    @Override
    public void drawBackground(Graphics g) {
        g.drawText(text, font, FORE_COLOR, insets.x - getScrollLeft(), insets.y - getScrollTop(), getScrollWidth() - 2 * insets.x, getScrollHeight() - 2 * insets.y, true, alignment, false);
    }
}
