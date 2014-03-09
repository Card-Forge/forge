package forge.toolbox;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;

public class FButton extends FDisplayObject {
    private static final int insetX = 25;
    private static final FSkinColor foreColor = FSkinColor.get(Colors.CLR_TEXT);

    private FSkinImage imgL, imgM, imgR;
    private String text;
    private FSkinFont font;
    private boolean toggled = false;
    private Runnable command;

    /**
     * Instantiates a new FButton.
     */
    public FButton() {
        this("", null);
    }

    public FButton(final String text0) {
        this(text0, null);
    }

    public FButton(final String text0, Runnable command0) {
        text = text0;
        command = command0;
        font = FSkinFont.get(14);
        resetImg();
    }

    private void resetImg() {
        imgL = FSkinImage.BTN_UP_LEFT;
        imgM = FSkinImage.BTN_UP_CENTER;
        imgR = FSkinImage.BTN_UP_RIGHT;
    }

    public String getText() {
        return text;
    }
    public void setText(String text0) {
        text = text0;
    }

    @Override
    public void setEnabled(boolean b0) {
        if (isEnabled() == b0) { return; }
        super.setEnabled(b0);

        if (b0) {
            resetImg();
        }
        else {
            imgL = FSkinImage.BTN_DISABLED_LEFT;
            imgM = FSkinImage.BTN_DISABLED_CENTER;
            imgR = FSkinImage.BTN_DISABLED_RIGHT;
        }
    }

    /** 
     * Button toggle state, for a "permanently pressed" functionality, e.g. as a tab.
     * 
     * @return boolean
     */
    public boolean isToggled() {
        return toggled;
    }
    public void setToggled(boolean b0) {
        if (toggled == b0) { return; }
        toggled = b0;

        if (toggled) {
            imgL = FSkinImage.BTN_TOGGLE_LEFT;
            imgM = FSkinImage.BTN_TOGGLE_CENTER;
            imgR = FSkinImage.BTN_TOGGLE_RIGHT;
        }
        else if (isEnabled()) {
            resetImg();
        }
        else {
            imgL = FSkinImage.BTN_DISABLED_LEFT;
            imgM = FSkinImage.BTN_DISABLED_CENTER;
            imgR = FSkinImage.BTN_DISABLED_RIGHT;
        }
    }

    public void setCommand(Runnable command0) {
        command = command0;
    }

    @Override
    public final boolean press(float x, float y) {
        if (isToggled()) { return true; }
        imgL = FSkinImage.BTN_DOWN_LEFT;
        imgM = FSkinImage.BTN_DOWN_CENTER;
        imgR = FSkinImage.BTN_DOWN_RIGHT;
        return true;
    }

    @Override
    public final boolean release(float x, float y) {
        if (isToggled()) { return true; }
        resetImg();
        return true;
    }

    @Override
    public final boolean tap(float x, float y, int count) {
        if (count == 1 && command != null) {
            command.run();
        }
        return true;
    }

    @Override
    public void draw(Graphics g) {
        float w = getWidth();
        float h = getHeight();

        if (w > 2 * h) {
            g.drawImage(imgL, 0, 0, h, h);
            g.drawImage(imgM, h, 0, w - (2 * h), h);
            g.drawImage(imgR, w - h, 0, h, h);
        }
        else {
            float buttonWidth = w / 2;
            g.drawImage(imgL, 0, 0, buttonWidth, h);
            g.drawImage(imgR, buttonWidth, 0, w - buttonWidth, h);
        }
        if (!StringUtils.isEmpty(text)) {
            g.drawText(text, font, foreColor, insetX, 0, w - 2 * insetX, h, false, HAlignment.CENTER, true);
        }
    }
}
