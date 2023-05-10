package forge.toolbox;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.animation.GifAnimation;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;

public class FTextArea extends FScrollPane {
    private String text;
    private FSkinFont font;
    private int alignment;
    private Vector2 insets;
    private FSkinColor textColor;
    private final TextRenderer renderer;
    private GifAnimation animation;
    private boolean centerVertically;

    public FTextArea(boolean parseReminderText0) {
        this(parseReminderText0, "");
    }

    public FTextArea(boolean parseReminderText0, String text0) {
        this(parseReminderText0, text0, null);
    }

    public FTextArea(boolean parseReminderText0, String text0, GifAnimation gifAnimation) {
        text = text0;
        font = FSkinFont.get(14);
        alignment = Align.left;
        insets = new Vector2(1, 1); //prevent text getting cut off by clip
        textColor = FLabel.getDefaultTextColor();
        animation = gifAnimation;
        renderer = new TextRenderer(parseReminderText0);
    }

    public String getText() {
        return text;
    }

    public void setText(String text0) {
        text = text0;
        revalidate();
    }

    public int getAlignment() {
        return alignment;
    }

    public void setAlignment(int alignment0) {
        alignment = alignment0;
    }

    public boolean getCenterVertically() {
        return centerVertically;
    }

    public void setCenterVertically(boolean centerVertically0) {
        centerVertically = centerVertically0;
    }

    public FSkinFont getFont() {
        return font;
    }

    public void setFont(FSkinFont font0) {
        font = font0;
        revalidate();
    }

    public FSkinColor getTextColor() {
        return textColor;
    }

    public void setTextColor(FSkinColor textColor0) {
        textColor = textColor0;
    }

    public float getPreferredHeight(float width) {
        return renderer.getWrappedBounds(text, font, width - 2 * insets.x).height
                + font.getLineHeight() - font.getCapHeight() //need to account for difference in line and cap height
                + 2 * insets.y;
    }

    @Override
    protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        return new ScrollBounds(visibleWidth, getPreferredHeight(visibleWidth));
    }

    @Override
    protected void drawBackground(Graphics g) {
        renderer.drawText(g, text, font, textColor, insets.x - getScrollLeft(), insets.y - getScrollTop(), getScrollWidth() - 2 * insets.x, getScrollHeight() - 2 * insets.y, 0, getHeight(), true, alignment, centerVertically);
    }

    @Override
    public void draw(Graphics g) {
        if (animation != null) {
            float w = getScrollWidth() - 2 * insets.x;
            float multiplier = "extrawide".equalsIgnoreCase(Forge.extrawide) ? 0.5f : 0.6f;
            float h = w * multiplier;
            float y = getPreferredHeight(w);
            animation.draw(g, insets.x - getScrollLeft(), (insets.y - getScrollTop()) + y, w, h);
        }
        super.draw(g);
    }
}
