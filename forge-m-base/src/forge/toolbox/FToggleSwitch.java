package forge.toolbox;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FEvent.FEventType;
import forge.utils.Utils;

public class FToggleSwitch extends FDisplayObject {
    private static final FSkinColor ACTIVE_COLOR = FSkinColor.get(Colors.CLR_ACTIVE);
    private static final FSkinColor INACTIVE_COLOR = FSkinColor.get(Colors.CLR_INACTIVE);
    private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static float MIN_PAN_DELTA = Utils.AVG_FINGER_WIDTH / 2f;
    private static final float INSETS = 2;
    private static final float PADDING = 3;

    private FSkinFont font;
    private final String onText, offText;
    private boolean toggled = false;
    private FEventHandler changedHandler;

    public FToggleSwitch() {
        this("On", "Off");
    }

    public FToggleSwitch(final String onText0, final String offText0) {
        onText = onText0;
        offText = offText0;
        font = FSkinFont.get(14);
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
        float onTextWidth = font.getFont().getBounds(onText).width;
        float offTextWidth = font.getFont().getBounds(offText).width;
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
    public final boolean tap(float x, float y, int count) {
        setToggled(!toggled, true);
        return true;
    }

    //support dragging finger left or right to toggle on/off
    @Override
    public final boolean pan(float x, float y, float deltaX, float deltaY) {
        if (deltaX < -MIN_PAN_DELTA && x < getWidth() / 2) {
            setToggled(true, true);
            return true;
        }
        if (deltaX > MIN_PAN_DELTA && x > getWidth() / 2) {
            setToggled(false, true);
            return true;
        }
        return false;
    }

    @Override
    public void draw(Graphics g) {
        float x = 1; //leave a pixel so border displays in full
        float y = 1;
        float w = getWidth() - 2 * x;
        float h = getHeight() - 2 * x;

        g.fillRect(INACTIVE_COLOR, x, y, w, h);
        g.drawRect(1, FORE_COLOR, x, y, w, h);

        final String text;
        float switchWidth = w - h + PADDING;
        if (toggled) {
            text = onText;
        }
        else {
            x = w - switchWidth + 1;
            text = offText;
        }
        x += INSETS;
        y += INSETS;
        h -= 2 * INSETS;
        w = switchWidth - 2 * INSETS;
        g.fillRect(ACTIVE_COLOR, x, y, w, h);

        x += PADDING;
        w -= 2 * PADDING;
        g.drawText(text, font, FORE_COLOR, x, y, w, h, false, HAlignment.CENTER, true);
    }
}
