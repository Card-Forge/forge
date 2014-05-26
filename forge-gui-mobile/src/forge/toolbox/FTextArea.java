package forge.toolbox;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.math.Vector2;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;
import forge.assets.TextRenderer;

public class FTextArea extends FScrollPane {
    public static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);

    private String text;
    private FSkinFont font;
    private HAlignment alignment;
    private Vector2 insets;
    private final TextRenderer renderer = new TextRenderer(true);

    public FTextArea() {
        this("");
    }
    public FTextArea(String text0) {
        text = text0;
        font = FSkinFont.get(14);
        alignment = HAlignment.LEFT;
        insets = new Vector2(1, 1); //prevent text getting cut off by clip
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

    public FSkinFont getFont() {
        return font;
    }

    public float getPreferredHeight(float width) {
        return renderer.getWrappedBounds(text, font, width - 2 * insets.x).height
                + font.getFont().getLineHeight() - font.getFont().getCapHeight() //need to account for difference in line and cap height
                + 2 * insets.y;
    }

    @Override
    protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        return new ScrollBounds(visibleWidth, getPreferredHeight(visibleWidth));
    }

    @Override
    public void drawBackground(Graphics g) {
        renderer.drawText(g, text, font, FORE_COLOR, insets.x - getScrollLeft(), insets.y - getScrollTop(), getScrollWidth() - 2 * insets.x, getScrollHeight() - 2 * insets.y, 0, getHeight(), true, alignment, false);
    }
}
