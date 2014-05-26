package forge.menu;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

import forge.Forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FEvent.FEventType;
import forge.util.Utils;

public class FMenuItem extends FDisplayObject {
    public static final float HEIGHT = Utils.AVG_FINGER_HEIGHT * 0.8f;
    protected static final float GAP_X = HEIGHT * 0.1f;
    private static final float ICON_SIZE = ((int)((HEIGHT - 2 * GAP_X) / 20f)) * 20; //round down to nearest multiple of 20

    private static final FSkinFont FONT = FSkinFont.get(12);
    protected static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinColor PRESSED_COLOR = FSkinColor.get(Colors.CLR_ACTIVE).alphaColor(0.9f);
    private static final FSkinColor SEPARATOR_COLOR = FORE_COLOR.alphaColor(0.5f);

    private final String text;
    private final FImage icon;
    private final FEventHandler handler;
    private boolean pressed;
    private boolean allowForIcon;
    private float textWidth;

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

    public boolean hasIcon() {
        return icon != null;
    }

    public void setAllowForIcon(boolean allowForIcon0) {
        allowForIcon = allowForIcon0;
    }

    public float getMinWidth() {
        float width = textWidth + 2 * GAP_X;
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
        return pressed;
    }

    @Override
    public void draw(Graphics g) {
        float w = getWidth();
        float h = HEIGHT;

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

        g.drawText(text, FONT, FORE_COLOR, x, 0, w - x - GAP_X, h, false, HAlignment.LEFT, true);

        //draw separator line
        g.drawLine(1, SEPARATOR_COLOR, 0, h, w, h);
    }
}
