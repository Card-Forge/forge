package forge.menu;

import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;
import forge.gui.UiCommand;
import forge.gui.interfaces.IButton;
import forge.localinstance.skin.FSkinProp;
import forge.screens.FScreen.Header;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FEvent.FEventType;
import forge.util.Utils;

public class FMenuItem extends FDisplayObject implements IButton {
    public static final float HEIGHT = Utils.AVG_FINGER_HEIGHT * 0.8f;
    protected static final float DIVOT_WIDTH = HEIGHT / 6;
    protected static final float GAP_X = HEIGHT * 0.1f;
    private static final float ICON_SIZE = ((int)((HEIGHT - 2 * GAP_X) / 20f)) * 20; //round down to nearest multiple of 20

    private static final FSkinFont FONT = FSkinFont.get(12);
    protected static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinColor PRESSED_COLOR = FSkinColor.get(Colors.CLR_ACTIVE).alphaColor(0.9f);
    private static final FSkinColor SEPARATOR_COLOR = FORE_COLOR.alphaColor(0.5f);
    private static final FSkinColor TAB_SEPARATOR_COLOR = Header.BACK_COLOR.stepColor(-40);

    private final String text;
    private final FImage icon;
    private final FEventHandler handler;
    private boolean pressed, allowForIcon, selected, tabMode;
    private float textWidth;
    private TextRenderer textRenderer;

    public FMenuItem(String text0, FEventHandler handler0) {
        this(text0, null, handler0, true);
    }
    public FMenuItem(String text0, FEventHandler handler0, boolean enabled0) {
        this(text0, null, handler0, enabled0);
    }
    public FMenuItem(String text0, FImage icon0, FEventHandler handler0) {
        this(text0, icon0, handler0, true);
    }
    public FMenuItem(String text0, FImage icon0, FEventHandler handler0, boolean enabled0) {
        text = text0;
        icon = icon0;
        handler = handler0;
        setEnabled(enabled0);

        textWidth = FONT.getBounds(text).width;
    }

    public String getText() {
        return text;
    }

    public boolean hasIcon() {
        return icon != null;
    }

    public void setAllowForIcon(boolean allowForIcon0) {
        allowForIcon = allowForIcon0;
    }

    public void setTabMode(boolean tabMode0) {
        tabMode = tabMode0;
    }

    public void setTextRenderer(TextRenderer textRenderer0) {
        textRenderer = textRenderer0;
    }

    public float getMinWidth() {
        //pretend there's a divot even if there isn't to provide extra right padding and allow menu items to line up nicer
        float width = textWidth + DIVOT_WIDTH + 4 * GAP_X;
        if (allowForIcon) {
            width += ICON_SIZE + GAP_X;
        }
        return width;
    }

    @Override
    public boolean press(float x, float y) {
        pressed = true;
        return true;
    }

    @Override
    public boolean release(float x, float y) {
        pressed = false;
        return true;
    }

    private final Task handleTapTask = new Task() {
        @Override
        public void run () {
            handler.handleEvent(new FEvent(FMenuItem.this, FEventType.TAP));
        }
    };

    @Override
    public boolean tap(float x, float y, int count) {
        Timer.schedule(handleTapTask, 0.1f); //delay handling tap just long enough for menu to be hidden
        return false; //return false so parent can use event to hide menu
    }

    protected boolean showPressedColor() {
        return (pressed && !tabMode) || selected;
    }

    @Override
    public void draw(Graphics g) {
        float w = getWidth();
        float h = HEIGHT;

        if (isHovered() && !pressed)
            g.fillRect(PRESSED_COLOR.brighter().alphaColor(0.4f), 0, 0, w, h);

        if (showPressedColor()) {
            g.fillRect(PRESSED_COLOR, 0, 0, w, h);
        }

        float x = GAP_X;

        if (allowForIcon) {
            if (icon != null) {
                g.drawImage(icon, x, (h - ICON_SIZE) / 2, ICON_SIZE, ICON_SIZE);
            }
            //account for not having icon but having been given icon size for alignment with other items
            x += ICON_SIZE + GAP_X;
        }

        if (textRenderer == null) {
            g.drawText(text, FONT, FORE_COLOR, x, 0, w - x - GAP_X, h, false, Align.left, true);
        }
        else {
            textRenderer.drawText(g, text, FONT, FORE_COLOR, x, 0, w - x - GAP_X, h, 0, h, false, Align.left, true);
        }

        //draw separator line
        if (tabMode) {
            g.drawLine(1, TAB_SEPARATOR_COLOR, 0, h, w, h);
        }
        else {
            g.drawLine(1, SEPARATOR_COLOR, 0, h, w, h);
        }
    }

    @Override
    public void setText(String text0) {
    }
    @Override
    public boolean isSelected() {
        return selected;
    }
    @Override
    public void setSelected(boolean b0) {
        selected = b0;
    }
    @Override
    public boolean requestFocusInWindow() {
        return false;
    }
    @Override
    public void setCommand(UiCommand command0) {
    }
    @Override
    public void setImage(FSkinProp color) {
    }
    @Override
    public void setTextColor(int r, int g, int b) {
    }
}
