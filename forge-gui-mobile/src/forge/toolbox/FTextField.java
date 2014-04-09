package forge.toolbox;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;
import forge.toolbox.FEvent.FEventHandler;

public class FTextField extends FDisplayObject {
    private static final int DEFAULT_FONT_SIZE = 14;
    protected static final float PADDING = 5;
    protected static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    protected static final FSkinColor BACK_COLOR = FSkinColor.get(Colors.CLR_THEME2);
    protected static final FSkinColor GHOST_TEXT_COLOR = FORE_COLOR.alphaColor(0.7f);
    private FEventHandler changedHandler;

    public static float getDefaultHeight() {
        return getDefaultHeight(DEFAULT_FONT_SIZE);
    }
    public static float getDefaultHeight(int fontSize) {
        return getDefaultHeight(FSkinFont.get(fontSize));
    }
    public static float getDefaultHeight(FSkinFont font0) {
        return font0.getFont().getCapHeight() * 3;
    }

    private String text;
    private String ghostText;
    private FSkinFont font;
    private HAlignment alignment;

    public FTextField() {
        this("");
    }
    public FTextField(String text0) {
        text = text0;
        ghostText = "";
        setFontSize(DEFAULT_FONT_SIZE);
        alignment = HAlignment.LEFT;
    }

    public String getText() {
        return text;
    }
    public void setText(String text0) {
        text = text0;
    }

    public String getGhostText() {
        return ghostText;
    }
    public void setGhostText(String ghostText0) {
        ghostText = ghostText0;
    }

    public boolean isEmpty() {
        return text.isEmpty();
    }

    public HAlignment getAlignment() {
        return alignment;
    }
    public void setAlignment(HAlignment alignment0) {
        alignment = alignment0;
    }

    public void setFontSize(int fontSize0) {
        font = FSkinFont.get(fontSize0);
        setHeight(getDefaultHeight(font));
    }

    public FEventHandler getChangedHandler() {
        return changedHandler;
    }
    public void setChangedHandler(FEventHandler changedHandler0) {
        changedHandler = changedHandler0;
    }

    public float getAutoSizeWidth() {
        return font.getFont().getBounds(text).width + 2 * PADDING;
    }

    @Override
    public boolean tap(float x, float y, int count) {
        //TODO: Support entering text when tapped
        return true;
    }

    @Override
    public void draw(Graphics g) {
        float w = getWidth();
        float h = getHeight();
        g.fillRect(BACK_COLOR, 0, 0, w, h);
        if (!text.isEmpty()) {
            g.drawText(text, font, FORE_COLOR, PADDING, 0, w - PADDING - getRightPadding(), h, false, alignment, true);
        }
        else if (!ghostText.isEmpty()) {
            g.drawText(ghostText, font, GHOST_TEXT_COLOR, PADDING, 0, w - PADDING - getRightPadding(), h, false, alignment, true);
        }
        g.drawRect(1, FORE_COLOR, 1, 1, w - 2, h - 2); //allow smooth border to fully display within bounds
    }

    protected float getRightPadding() {
        return PADDING;
    }
}
