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
import forge.utils.Utils;

public class FMenuItem extends FDisplayObject {
    public static final float HEIGHT = Utils.AVG_FINGER_HEIGHT * 0.8f;
    private static final FSkinFont FONT = FSkinFont.get(12);
    private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinColor PRESSED_COLOR = FSkinColor.get(Colors.CLR_ACTIVE).alphaColor(0.9f);
    private static final FSkinColor SEPARATOR_COLOR = FORE_COLOR.alphaColor(0.5f);

    private final String text;
    private final FImage icon;
    private final FEventHandler handler;
    private boolean pressed;
    private float iconSize;
    private float textWidth;

    public FMenuItem(String text0, FEventHandler handler0) {
        this(text0, null, handler0);
    }
    public FMenuItem(String text0, FImage icon0, FEventHandler handler0) {
        text = text0;
        icon = icon0;
        handler = handler0;

        if (icon != null) {
            iconSize = FONT.getFont().getLineHeight();
        }
        textWidth = FONT.getFont().getBounds(text).width;
    }

    public float getIconSize() {
        return iconSize;
    }

    public void setIconSize(float iconSize0) {
        iconSize = iconSize0;
    }

    public float getMinWidth() {
        float gapX = getGapX();
        float width = textWidth + 2 * gapX;
        if (iconSize > 0) {
            width += iconSize + gapX;
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

    public float getGapX() {
        return (HEIGHT - FONT.getFont().getLineHeight()) / 2;
    }

    @Override
    public final void draw(Graphics g) {
        float w = getWidth();
        float h = HEIGHT;

        if (pressed) {
            g.fillRect(PRESSED_COLOR, 0, 0, w, h);
        }

        float gapX = getGapX();
        float x = gapX;

        if (icon != null) {
            g.drawImage(icon, x, (h - iconSize) / 2, iconSize, iconSize);
        }
        if (iconSize > 0) { //account for not having icon but having been given icon size for alignment with other items
            x += iconSize + gapX;
        }

        g.drawText(text, FONT, FORE_COLOR, x, 0, w - x - gapX, h, false, HAlignment.LEFT, true);

        //draw separator line
        g.drawLine(1, SEPARATOR_COLOR, 0, h, w, h);
    }
}
