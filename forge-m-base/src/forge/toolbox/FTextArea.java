package forge.toolbox;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
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

    @Override
    public void drawBackground(Graphics g) {
        g.drawText(text, font, FORE_COLOR, insets.x, insets.y, getWidth() - 2 * insets.x, getHeight() - 2 * insets.y, true, alignment, false);
    }
}
