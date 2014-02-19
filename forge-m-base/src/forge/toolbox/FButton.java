package forge.toolbox;

import java.awt.AlphaComposite;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;

import forge.toolbox.FSkin.SkinImage;

public class FButton extends Widget {
    private SkinImage imgL, imgM, imgR;
    private boolean allImagesPresent = false;
    private String caption;
    private boolean enabled = true;
    private boolean toggled = false;
    private final AlphaComposite disabledComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f);

    /**
     * Instantiates a new FButton.
     */
    public FButton() {
        this("");
    }

    public FButton(final String caption0) {
        caption = caption0;
        /*setOpaque(false);
        setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        setBackground(Color.red);
        setFocusPainted(false);
        setBorder(BorderFactory.createEmptyBorder());
        setContentAreaFilled(false);
        setMargin(new Insets(0, 25, 0, 25));
        setFont(FSkin.getBoldFont(14));*/
        resetImg();
        if ((imgL != null) && (imgM != null) && (imgR != null)) {
            allImagesPresent = true;
        }
    }

    private void resetImg() {
        imgL = FSkin.getImage(FSkin.ButtonImages.IMG_BTN_UP_LEFT);
        imgM = FSkin.getImage(FSkin.ButtonImages.IMG_BTN_UP_CENTER);
        imgR = FSkin.getImage(FSkin.ButtonImages.IMG_BTN_UP_RIGHT);
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
            imgL = FSkin.getImage(FSkin.ButtonImages.IMG_BTN_DISABLED_LEFT);
            imgM = FSkin.getImage(FSkin.ButtonImages.IMG_BTN_DISABLED_CENTER);
            imgR = FSkin.getImage(FSkin.ButtonImages.IMG_BTN_DISABLED_RIGHT);
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

    /** @param b0 &emsp; boolean. */
    public void setToggled(boolean b0) {
        if (toggled == b0) { return; }
        toggled = b0;

        if (toggled) {
            imgL = FSkin.getImage(FSkin.ButtonImages.IMG_BTN_TOGGLE_LEFT);
            imgM = FSkin.getImage(FSkin.ButtonImages.IMG_BTN_TOGGLE_CENTER);
            imgR = FSkin.getImage(FSkin.ButtonImages.IMG_BTN_TOGGLE_RIGHT);
        }
        else if (isEnabled()) {
            resetImg();
        }
        else {
            imgL = FSkin.getImage(FSkin.ButtonImages.IMG_BTN_DISABLED_LEFT);
            imgM = FSkin.getImage(FSkin.ButtonImages.IMG_BTN_DISABLED_CENTER);
            imgR = FSkin.getImage(FSkin.ButtonImages.IMG_BTN_DISABLED_RIGHT);
        }
    }

    @Override
    public void draw(SpriteBatch batch, float parentAlpha) {
        if (!allImagesPresent) {
            return;
        }

        float w = getWidth();
        float h = getHeight();

        Color color = getColor();
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);

        FSkin.drawImage(batch, imgL, 0, 0, h, h);
        FSkin.drawImage(batch, imgM, h, 0, w - (2 * h), h);
        FSkin.drawImage(batch, imgR, w - h, 0, h, h);
    }
}
