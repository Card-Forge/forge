package forge.toolbox;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Forge.KeyInputAdapter;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.gui.interfaces.ITextField;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FEvent.FEventType;
import forge.util.TextBounds;
import forge.util.Utils;

public class FTextField extends FDisplayObject implements ITextField {
    private static final FSkinFont DEFAULT_FONT = FSkinFont.get(14);
    private static final float BORDER_THICKNESS = Utils.scale(1);
    public static final float PADDING = Utils.scale(5);
    protected static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    protected static final FSkinColor BACK_COLOR = FSkinColor.get(Colors.CLR_THEME2);
    protected static final FSkinColor GHOST_TEXT_COLOR = FORE_COLOR.alphaColor(0.7f);
    protected static final FSkinColor SEL_COLOR = FSkinColor.get(Colors.CLR_ACTIVE);
    private FEventHandler changedHandler;

    public static float getDefaultHeight() {
        return getDefaultHeight(DEFAULT_FONT);
    }
    public static float getDefaultHeight(FSkinFont font0) {
        return font0.getCapHeight() * 3;
    }

    private String text, ghostText, textBeforeKeyInput;
    protected FSkinFont font, renderedFont;
    private int alignment;
    private int selStart, selLength;
    private boolean isEditing, readOnly;

    private final FPopupMenu contextMenu = new FPopupMenu() {
        @Override
        protected void buildMenu() {
            if (text.length() > 0) {
                if (!readOnly) {
                    addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblCut"), new FEventHandler() {
                        @Override
                        public void handleEvent(FEvent e) {
                            Forge.getClipboard().setContents(getSelectedText());
                            textBeforeKeyInput = text;
                            insertText("");
                            endEdit();
                        }
                    }));
                }
                addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblCopy"), new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        Forge.getClipboard().setContents(getSelectedText());
                    }
                }));
            }
            if (!readOnly) {
                addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblPaste"), new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        textBeforeKeyInput = text;
                        insertText(Forge.getClipboard().getContents());
                        endEdit();
                    }
                }));
            }
        }
    };

    public FTextField() {
        this("");
    }
    public FTextField(String text0) {
        text = text0;
        ghostText = "";
        setFont(DEFAULT_FONT);
        alignment = Align.left;
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

    protected void insertText(String text0) {
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

    public int getAlignment() {
        return alignment;
    }
    public void setAlignment(int alignment0) {
        alignment = alignment0;
    }

    public FSkinFont getFont() {
        return font;
    }
    public void setFont(FSkinFont font0) {
        font = font0;
        renderedFont = font0;
        setHeight(getDefaultHeight(font));
    }

    public boolean isReadOnly() {
        return readOnly;
    }
    public void setReadOnly(boolean readOnly0) {
        readOnly = readOnly0;
    }

    public FEventHandler getChangedHandler() {
        return changedHandler;
    }
    public void setChangedHandler(FEventHandler changedHandler0) {
        changedHandler = changedHandler0;
    }

    public float getAutoSizeWidth() {
        return getLeftPadding() + font.getBounds(text).width + getRightPadding();
    }

    private int getCharIndexAtPoint(float x, float y) {
        float charLeft = getTextLeft();
        if (x < charLeft) {
            return 0;
        }
        if (x >= charLeft + renderedFont.getBounds(text).width) {
            return text.length();
        }

        //find closest character of press
        float charWidth;
        for (int i = 0; i < text.length(); i++) {
            charWidth = renderedFont.getBounds(text.substring(i, i + 1)).width;
            if (x < charLeft + charWidth / 2) {
                return i;
            }
            charLeft += charWidth;
        }
        return text.length();
    }

    @Override
    public boolean press(float x, float y) {
        placeTextCursor(x, y);
        return false;
    }

    public boolean pan(float x, float y, float deltaX, float deltaY, boolean moreVertical) {
        placeTextCursor(x, y);
        return true;
    }

    private void placeTextCursor(float x, float y) {
        if (isEditing) {
            selStart = getCharIndexAtPoint(x, y);
            selLength = 0;
        }
    }

    @Override
    public boolean longPress(float x, float y) {
        if (isEditing) {
            endEdit(); //end previous edit first
        }
        selStart = 0;
        selLength = text.length(); //select text while context menu open
        contextMenu.show(this, x, y);
        return true;
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
        if (readOnly) { return false; }
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
                endEdit();
            }
        });
        isEditing = true;
        return true;
    }

    protected boolean validate() {
        return true;
    }

    protected void endEdit() {
        text = text.trim(); //trim text after editing
        if (!text.equals(textBeforeKeyInput)) {
            if (validate()) {
                if (changedHandler != null) {
                    //handle change event if text changed during input
                    changedHandler.handleEvent(new FEvent(FTextField.this, FEventType.CHANGE, textBeforeKeyInput));
                }
            }
            else { //restore previous text if new text isn't valid
                setText(textBeforeKeyInput);
            }
        }
        isEditing = false;
        selStart = 0;
        selLength = 0;
        textBeforeKeyInput = null;
    }

    @Override
    public void draw(Graphics g) {
        float w = getWidth();
        float h = getHeight();
        boolean drawBackground = !readOnly; //don't draw background or border if read-only

        if (drawBackground) {
            g.fillRect(BACK_COLOR, 0, 0, w, h);
        }

        //determine actual rendered font so selection logic is accurate
        renderedFont = font;
        float availableTextWidth = w - getLeftPadding() - getRightPadding();
        TextBounds textBounds = renderedFont.getMultiLineBounds(text);
        while (textBounds.width > availableTextWidth || textBounds.height > h) {
            if (renderedFont.canShrink()) { //shrink font to fit if possible
                renderedFont = renderedFont.shrink();
                availableTextWidth = w - getLeftPadding() - getRightPadding();
                textBounds = renderedFont.getMultiLineBounds(text);
            }
            else {
                break;
            }
        }

        //draw selection if key input is active or context menu is shown
        if (isEditing || contextMenu.isVisible()) {
            float selLeft = getTextLeft();
            if (selStart > 0) {
                selLeft += renderedFont.getBounds(text.substring(0, selStart)).width;
            }
            float selTop = PADDING;
            float selHeight = h - 2 * PADDING;
            if (selLength == 0) {
                drawText(g, w, h); //draw text behind cursor
                g.drawLine(BORDER_THICKNESS, FORE_COLOR, selLeft, selTop, selLeft, selTop + selHeight);
            }
            else if (selStart == 0 && selLength == text.length()) {
                float selWidth = renderedFont.getBounds(text.substring(selStart, selStart + selLength)).width;
                g.fillRect(SEL_COLOR, selLeft, selTop, selWidth, selHeight);
                drawText(g, w, h); //draw text in front of selection background
            }
        }
        else { //otherwise just draw text
            drawText(g, w, h);
        }

        if (drawBackground) {
            g.drawRect(BORDER_THICKNESS, FORE_COLOR, BORDER_THICKNESS, BORDER_THICKNESS, w - 2 * BORDER_THICKNESS, h - 2 * BORDER_THICKNESS); //allow smooth border to fully display within bounds
        }
    }

    private void drawText(Graphics g, float w, float h) {
        float diff = h - renderedFont.getCapHeight();
        if (diff > 0 && Math.round(diff) % 2 == 1) {
            h++; //if odd difference between height and font height, increment height so text favors displaying closer to bottom
        }
        if (!text.isEmpty()) {
            g.drawText(text, renderedFont, FORE_COLOR, getLeftPadding(), 0, w - getLeftPadding() - getRightPadding(), h, false, alignment, true);
        }
        else if (!ghostText.isEmpty()) {
            g.drawText(ghostText, renderedFont, GHOST_TEXT_COLOR, getLeftPadding(), 0, w - getLeftPadding() - getRightPadding(), h, false, alignment, true);
        }
    }

    protected float getTextLeft() {
        switch (alignment) {
        case Align.left:
        default:
            return getLeftPadding();
        case Align.center:
            return getLeftPadding() + (getWidth() - getRightPadding() - getLeftPadding() - renderedFont.getBounds(text).width) / 2;
        case Align.right:
            return getWidth() - getRightPadding() - renderedFont.getBounds(text).width;
        }
    }

    protected float getLeftPadding() {
        return PADDING;
    }

    protected float getRightPadding() {
        return PADDING;
    }

    @Override
    public boolean requestFocusInWindow() {
        return false;
    }
}
