package forge.toolbox;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge;
import forge.Forge.Graphics;
import forge.Forge.KeyInputAdapter;
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
    protected static final FSkinColor SEL_COLOR = FSkinColor.get(Colors.CLR_ACTIVE);
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
    private int selStart;
    private int selLength;
    private boolean keyInputActive;

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
        if (text0 == null) {
            text0 = ""; //don't allow setting null
        }
        text = text0;
        selStart = 0;
        selLength = 0;
    }

    public String getGhostText() {
        return ghostText;
    }
    public void setGhostText(String ghostText0) {
        if (ghostText0 == null) {
            ghostText0 = ""; //don't allow setting null
        }
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

    public int getFontSize() {
        return font.getSize();
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
        selStart = 0; //select all before starting input
        selLength = text.length();

        Forge.startKeyInput(new KeyInputAdapter() {
            @Override
            public FDisplayObject getOwner() {
                return FTextField.this;
            }

            @Override
            public boolean keyTyped(char ch) {
                String newText = String.valueOf(ch);
                if (selStart > 0) {
                    newText = text.substring(0, selStart) + newText;
                }
                int selEnd = selStart + selLength;
                if (selEnd < text.length()) {
                    newText += text.substring(selEnd);
                }
                text = newText;
                selStart++; //put cursor after inserted character
                selLength = 0;
                return true;
            }

            @Override
            public void onInputEnd() {
                keyInputActive = false;
                selStart = 0;
                selLength = 0;
            }
        });
        keyInputActive = true;
        return true;
    }

    @Override
    public void draw(Graphics g) {
        float w = getWidth();
        float h = getHeight();
        g.fillRect(BACK_COLOR, 0, 0, w, h);

        //draw selection if key input is active
        if (keyInputActive) {
            float selLeft = PADDING;
            if (selStart > 0) {
                selLeft += font.getFont().getBounds(text.substring(0, selStart)).width;
            }
            float selTop = PADDING;
            float selHeight = h - 2 * PADDING;
            if (selLength == 0) {
                drawText(g, w, h); //draw text behind cursor
                g.drawLine(1, FORE_COLOR, selLeft, selTop, selLeft, selTop + selLength);
            }
            else if (selStart == 0 && selLength == text.length()) {
                float selWidth = font.getFont().getBounds(text.substring(selStart, selStart + selLength)).width;
                g.fillRect(SEL_COLOR, selLeft, selTop, selWidth, selHeight);
                drawText(g, w, h); //draw text in front of selection background
            }
        }
        else { //otherwise just draw text
            drawText(g, w, h);
        }

        g.drawRect(1, FORE_COLOR, 1, 1, w - 2, h - 2); //allow smooth border to fully display within bounds
    }

    private void drawText(Graphics g, float w, float h) {
        if (!text.isEmpty()) {
            g.drawText(text, font, FORE_COLOR, PADDING, 0, w - PADDING - getRightPadding(), h, false, alignment, true);
        }
        else if (!ghostText.isEmpty()) {
            g.drawText(ghostText, font, GHOST_TEXT_COLOR, PADDING, 0, w - PADDING - getRightPadding(), h, false, alignment, true);
        }
    }

    protected float getRightPadding() {
        return PADDING;
    }
}
