package forge.toolbox;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;
import forge.toolbox.FEvent.FEventHandler;

public class FTextField extends FDisplayObject {
    protected static final float PADDING = 5;
    protected static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    protected static final FSkinColor BACK_COLOR = FSkinColor.get(Colors.CLR_THEME2);
    private FEventHandler changedHandler;

    private String text;
    private FSkinFont font;
    private HAlignment alignment;

    public FTextField() {
        this("");
    }
    public FTextField(String text0) {
        text = text0;
        setFontSize(14);
        alignment = HAlignment.LEFT;
    }

    public String getText() {
        return text;
    }
    public void setText(String text0) {
        text = text0;
    }

    public HAlignment getAlignment() {
        return alignment;
    }
    public void setAlignment(HAlignment alignment0) {
        alignment = alignment0;
    }

    public void setFontSize(int fontSize0) {
        font = FSkinFont.get(fontSize0);
        setHeight(font.getFont().getCapHeight() * 3);
    }

    public FEventHandler getChangedHandler() {
        return changedHandler;
    }
    public void setChangedHandler(FEventHandler changedHandler0) {
        changedHandler = changedHandler0;
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
        g.drawText(text, font, FORE_COLOR, PADDING, 0, w - PADDING - getRightPadding(), h, false, alignment, true);
        g.drawRect(1, FORE_COLOR, 1, 1, w - 2, h - 2); //allow smooth border to fully display within bounds
    }

    protected float getRightPadding() {
        return PADDING;
    }
}
