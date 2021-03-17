package forge.toolbox;

import com.badlogic.gdx.utils.Align;

import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FEvent.FEventType;
import forge.util.Utils;

public class FToggleSwitch extends FDisplayObject {
    private static final FSkinColor ACTIVE_COLOR = FSkinColor.get(Colors.CLR_ACTIVE);
    private static final FSkinColor PRESSED_COLOR = ACTIVE_COLOR.stepColor(-30);
    private static final FSkinColor INACTIVE_COLOR = FSkinColor.get(Colors.CLR_INACTIVE);
    private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final float BORDER_THICKNESS = Utils.scale(1);
    private static final float INSETS = Utils.scale(2);
    private static final float PADDING = Utils.scale(3);

    private FSkinFont font;
    private String offText, onText;
    private boolean toggled, pressed;
    private FEventHandler changedHandler;

    public FToggleSwitch() {
        this("Off", "On");
    }

    public FToggleSwitch(final String offText0, final String onText0) {
        offText = offText0;
        onText = onText0;
        font = FSkinFont.get(14);
    }

    public String getOffText() {
        return offText;
    }
    public void setOffText(String offText0) {
        offText = offText0;
    }
    public String getOnText() {
        return onText;
    }
    public void setOnText(String onText0) {
        onText = onText0;
    }

    public void setFontSize(int fontSize0) {
        font = FSkinFont.get(fontSize0);
    }

    public boolean isToggled() {
        return toggled;
    }
    public void setToggled(boolean b0) {
        setToggled(b0, false);
    }
    private void setToggled(boolean b0, boolean raiseChangedEvent) {
        if (toggled == b0) { return; }
        toggled = b0;

        if (raiseChangedEvent && changedHandler != null) {
            changedHandler.handleEvent(new FEvent(this, FEventType.CHANGE, b0));
        }
    }

    public FEventHandler getChangedHandler() {
        return changedHandler;
    }
    public void setChangedHandler(FEventHandler changedHandler0) {
        changedHandler = changedHandler0;
    }

    public float getAutoSizeWidth(float height) {
        float width;
        float onTextWidth = font.getBounds(onText).width;
        float offTextWidth = font.getBounds(offText).width;
        if (onTextWidth > offTextWidth) {
            width = onTextWidth;
        }
        else {
            width = offTextWidth;
        }
        width += 2 * (PADDING + INSETS + 1);
        width += height - PADDING; //leave room for switch to move
        return width;
    }

    @Override
    public final boolean press(float x, float y) {
        pressed = true;
        return true;
    }

    @Override
    public final boolean release(float x, float y) {
        pressed = false;
        return true;
    }

    @Override
    public final boolean tap(float x, float y, int count) {
        setToggled(!toggled, true);
        return true;
    }

    //support dragging finger left or right to toggle on/off
    @Override
    public final boolean pan(float x, float y, float deltaX, float deltaY, boolean moreVertical) {
        if (contains(getLeft() + x, getTop() + y)) {
            if (x < getHeight()) {
                setToggled(false, true);
                return true;
            }
            if (x > getWidth() - getHeight()) {
                setToggled(true, true);
                return true;
            }
            pressed = true;
        }
        else {
            pressed = false;
        }
        return false;
    }

    @Override
    public final boolean panStop(float x, float y) {
        if (pressed) {
            pressed = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean fling(float velocityX, float velocityY) {
        return Math.abs(velocityX) > Math.abs(velocityY); //handle fling if more horizontal than vertical
    }

    @Override
    public void draw(Graphics g) {
        float x = BORDER_THICKNESS; //leave a pixel so border displays in full
        float y = BORDER_THICKNESS;
        float w = getWidth() - 2 * x;
        float h = getHeight() - 2 * y;

        g.fillRect(INACTIVE_COLOR, x, y, w, h);
        g.drawRect(BORDER_THICKNESS, FORE_COLOR, x, y, w, h);

        final String text;
        float switchWidth = w - h + PADDING;
        if (toggled) {
            x = w - switchWidth + 1;
            text = onText;
        }
        else {
            text = offText;
        }
        x += INSETS;
        y += INSETS;
        h -= 2 * INSETS;
        w = switchWidth - 2 * INSETS;
        g.fillRect(pressed ? PRESSED_COLOR : ACTIVE_COLOR, x, y, w, h);

        x += PADDING;
        w -= 2 * PADDING;
        g.drawText(text, font, FORE_COLOR, x, y, w, h, false, Align.center, true);
    }
}
