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
    private static final FSkinColor BORDER_COLOR = INACTIVE_COLOR.stepColor(-30);
    private static float MIN_PAN_DELTA = Utils.AVG_FINGER_WIDTH / 2f;
    private static final float PADDING = 5;

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

    public float getAutoSizeWidth() {
        float width;
        float onTextWidth = font.getFont().getBounds(onText).width;
        float offTextWidth = font.getFont().getBounds(offText).width;
        if (onTextWidth > offTextWidth) {
            width = onTextWidth;
        }
        else {
            width = offTextWidth;
        }
        width += 2 * PADDING;
        width *= 1.5f; //leave room for switch to move
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
        float w = getWidth();
        float h = getHeight();

        g.startClip(0, 0, w, h); //start clip to ensure nothing escapes bounds
        g.fillRect(INACTIVE_COLOR, 0, 0, w, h);

        final float x;
        final String text;
        float switchWidth = w * 0.66f;
        if (toggled) {
            x = 0;
            text = onText;
        }
        else {
            x = w - switchWidth;
            text = offText;
        }
        g.fillRect(ACTIVE_COLOR, x, 0, switchWidth, h);
        g.drawText(text, font, FORE_COLOR, x + PADDING, 0, switchWidth - 2 * PADDING, h, false, HAlignment.CENTER, true);

        g.drawRect(1, BORDER_COLOR, 0, 0, w, h);
        g.endClip();
    }
}
