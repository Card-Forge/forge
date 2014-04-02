package forge.toolbox;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;
import forge.toolbox.FEvent.FEventHandler;

public class FTextField extends FDisplayObject {
    private static final float PADDING = 3;
    private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private FEventHandler changedHandler;

    private String text;
    private FSkinFont font;
    private HAlignment alignment;

    public FTextField() {
        this("");
    }
    public FTextField(String text0) {
        text = text0;
        font = FSkinFont.get(14);
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
        g.drawText(text, font, FORE_COLOR, PADDING, 0, getWidth() - 2 * PADDING, getHeight(), false, alignment, true);
    }
}
