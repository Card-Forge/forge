package forge.toolbox;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;

public class FButton extends FDisplayObject {
    private static final int insetX = 25;
    private static final FSkinColor foreColor = FSkinColor.get(Colors.CLR_TEXT);

    private FSkinImage imgL, imgM, imgR;
    private String caption;
    private FSkinFont font;
    private boolean enabled = true;
    private boolean toggled = false;

    /**
     * Instantiates a new FButton.
     */
    public FButton() {
        this("");
    }

    public FButton(final String caption0) {
        caption = caption0;
        font = FSkinFont.get(14);
        resetImg();
    }

    private void resetImg() {
        imgL = FSkinImage.BTN_UP_LEFT;
        imgM = FSkinImage.BTN_UP_CENTER;
        imgR = FSkinImage.BTN_UP_RIGHT;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean b0) {
        if (enabled == b0) { return; }
        enabled = b0;

        if (enabled) {
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

    @Override
    protected void doLayout(float width, float height) {
    }

    @Override
    public void draw(Graphics g) {
        float w = getWidth();
        float h = getHeight();

        g.drawImage(imgL, 0, 0, h, h);
        g.drawImage(imgM, h, 0, w - (2 * h), h);
        g.drawImage(imgR, w - h, 0, h, h);
        if (!caption.isEmpty()) {
            g.drawText(caption, font, foreColor, insetX, 0, w - 2 * insetX, h, false, true, true);
        }
    }
}
