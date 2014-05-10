package forge.toolbox;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge;
import forge.Forge.Graphics;
import forge.Forge.KeyInputAdapter;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FEvent.FEventType;
import forge.util.Utils;

public class FTextField extends FDisplayObject {
    private static final int DEFAULT_FONT_SIZE = 14;
    private static final float BORDER_THICKNESS = Utils.scaleX(1);
    protected static final float PADDING = Utils.scaleX(5);
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

    private String text, ghostText, textBeforeKeyInput;
    private FSkinFont font;
    private HAlignment alignment;
    private int selStart, selLength;
    private boolean isEditing;

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

    private void insertText(String text0) {
        int insertLength = text0.length();
        if (selStart > 0) {
            text0 = text.substring(0, selStart) + text0;
        }
        int selEnd = selStart + selLength;
        if (selEnd < text.length()) {
            text0 += text.substring(selEnd);
        }
        text = text0;
        selStart += insertLength; //put cursor after inserted text
        selLength = 0;
    }

    public String getSelectedText() {
        if (selLength > 0) {
            return text.substring(selStart, selLength);
        }
        return "";
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
    
    private int getCharIndexAtPoint(float x, float y) {
        float charLeft = PADDING;
        if (x < charLeft) {
            return 0;
        }
        if (x >= charLeft + font.getFont().getBounds(text).width) {
            return text.length();
        }

        //find closest character of press
        float charWidth;
        for (int i = 0; i < text.length(); i++) {
            charWidth = font.getFont().getBounds(text.substring(i, i + 1)).width;
            if (x < charLeft + charWidth / 2) {
                return i;
            }
            charLeft += charWidth;
        }
        return text.length();
    }

    @Override
    public boolean press(float x, float y) {
        if (isEditing) { //support placing text cursor
            selStart = getCharIndexAtPoint(x, y);
            selLength = 0;
        }
        return false;
    }

    @Override
    public boolean tap(float x, float y, int count) {
        return startEdit();
    }

    @Override
    public boolean keyDown(int keyCode) {
        return startEdit(); //start edit if keyDown received while key input not active
    }

    public boolean startEdit() {
        if (isEditing) { return true; } //do nothing if already editing

        selStart = 0; //select all before starting input
        selLength = text.length();
        textBeforeKeyInput = text; //backup text before input to detect changes

        Forge.startKeyInput(new KeyInputAdapter() {
            @Override
            public FDisplayObject getOwner() {
                return FTextField.this;
            }

            @Override
            public boolean allowTouchInput() {
                return true;
            }

            @Override
            public boolean keyTyped(char ch) {
                insertText(String.valueOf(ch));
                return true;
            }

            @Override
            public boolean keyDown(int keyCode) {
                switch (keyCode) {
                case Keys.TAB:
                case Keys.ENTER: //end key input on Tab or Enter
                    Forge.endKeyInput();
                    return true;
                case Keys.ESCAPE:
                    setText(textBeforeKeyInput); //cancel edit on Escape
                    Forge.endKeyInput();
                    return true;
                case Keys.BACKSPACE: //also handles Delete since those are processed the same by libgdx
                    if (text.length() > 0) {
                        if (selLength == 0) { //delete previous or next character if selection empty
                            if (selStart > 0) {
                                selStart--;
                            }
                            selLength = 1;
                        }
                        insertText("");
                    }
                    return true;
                case Keys.LEFT:
                    if (selLength == 0) {
                        if (selStart > 0) {
                            selStart--;
                        }
                    }
                    else {
                        selLength = 0;
                    }
                    return true;
                case Keys.RIGHT:
                    if (selLength == 0) {
                        if (selStart < text.length()) {
                            selStart++;
                        }
                    }
                    else {
                        selStart += selLength;
                        selLength = 0;
                    }
                    return true;
                case Keys.A: //select all on Ctrl+A
                    if (KeyInputAdapter.isCtrlKeyDown()) {
                        selStart = 0;
                        selLength = text.length();
                        return true;
                    }
                    break;
                case Keys.C: //copy on Ctrl+C
                    if (KeyInputAdapter.isCtrlKeyDown()) {
                        if (selLength > 0) {
                            Forge.getClipboard().setContents(getSelectedText());
                        }
                        return true;
                    }
                    break;
                case Keys.V: //paste on Ctrl+V
                    if (KeyInputAdapter.isCtrlKeyDown()) {
                        insertText(Forge.getClipboard().getContents());
                        return true;
                    }
                    break;
                case Keys.X: //cut on Ctrl+X
                    if (KeyInputAdapter.isCtrlKeyDown()) {
                        if (selLength > 0) {
                            Forge.getClipboard().setContents(getSelectedText());
                            insertText("");
                        }
                        return true;
                    }
                    break;
                case Keys.Z: //cancel edit on Ctrl+Z
                    if (KeyInputAdapter.isCtrlKeyDown()) {
                        setText(textBeforeKeyInput);
                        Forge.endKeyInput();
                        return true;
                    }
                    break;
                }
                return false;
            }

            @Override
            public void onInputEnd() {
                if (changedHandler != null && !text.equals(textBeforeKeyInput)) {
                    //handle change event if text changed during input
                    changedHandler.handleEvent(new FEvent(FTextField.this, FEventType.CHANGE, textBeforeKeyInput));
                }
                isEditing = false;
                selStart = 0;
                selLength = 0;
                textBeforeKeyInput = null;
            }
        });
        isEditing = true;
        return true;
    }

    @Override
    public void draw(Graphics g) {
        float w = getWidth();
        float h = getHeight();
        g.fillRect(BACK_COLOR, 0, 0, w, h);

        //draw selection if key input is active
        if (isEditing) {
            float selLeft = PADDING;
            if (selStart > 0) {
                selLeft += font.getFont().getBounds(text.substring(0, selStart)).width;
            }
            float selTop = PADDING;
            float selHeight = h - 2 * PADDING;
            if (selLength == 0) {
                drawText(g, w, h); //draw text behind cursor
                g.drawLine(BORDER_THICKNESS, FORE_COLOR, selLeft, selTop, selLeft, selTop + selHeight);
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

        g.drawRect(BORDER_THICKNESS, FORE_COLOR, BORDER_THICKNESS, BORDER_THICKNESS, w - 2 * BORDER_THICKNESS, h - 2 * BORDER_THICKNESS); //allow smooth border to fully display within bounds
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
