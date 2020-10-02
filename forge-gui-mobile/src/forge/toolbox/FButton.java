package forge.toolbox;

import forge.Forge;
import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.utils.Align;
import forge.Graphics;
import forge.UiCommand;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FSkinProp;
import forge.interfaces.IButton;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FEvent.FEventType;
import forge.util.TextBounds;
import forge.util.Utils;

public class FButton extends FDisplayObject implements IButton {
    private static final FSkinColor DEFAULT_FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final float PADDING = Utils.scale(10);

    private FSkinImage imgL, imgM, imgR;
    private String text;
    private FSkinFont font;
    private FSkinColor foreColor = DEFAULT_FORE_COLOR;
    private boolean toggled = false;
    private FEventHandler command;

    public enum Corner {
        None,
        BottomLeft,
        BottomRight,
        BottomMiddle
    }
    private Corner corner = Corner.None;

    /**
     * Instantiates a new FButton.
     */
    public FButton() {
        this("", null);
    }

    public FButton(final String text0) {
        this(text0, null);
    }

    public FButton(final String text0, FEventHandler command0) {
        text = text0;
        command = command0;
        font = FSkinFont.get(14);
        resetImg();
    }

    private boolean hdbuttonskin(){
        return Forge.hdbuttons;
    }

    private void resetImg() {
        imgL = (hdbuttonskin() && FSkinImage.HDBTN_UP_LEFT != null) ? FSkinImage.HDBTN_UP_LEFT : FSkinImage.BTN_UP_LEFT;
        imgM = (hdbuttonskin() && FSkinImage.HDBTN_UP_CENTER != null) ? FSkinImage.HDBTN_UP_CENTER : FSkinImage.BTN_UP_CENTER;
        imgR = (hdbuttonskin() && FSkinImage.HDBTN_UP_RIGHT != null) ? FSkinImage.HDBTN_UP_RIGHT : FSkinImage.BTN_UP_RIGHT;
    }

    public String getText() {
        return text;
    }
    public void setText(String text0) {
        text = text0;
    }

    public FSkinFont getFont() {
        return font;
    }
    public void setFont(FSkinFont font0) {
        font = font0;
    }

    @Override
    public void setEnabled(boolean b0) {
        if (isEnabled() == b0) { return; }
        super.setEnabled(b0);

        if (b0) {
            resetImg();
        }
        else {
            imgL = (hdbuttonskin() && FSkinImage.HDBTN_UP_LEFT != null) ? FSkinImage.HDBTN_UP_LEFT : FSkinImage.BTN_UP_LEFT;
            imgM = (hdbuttonskin() && FSkinImage.HDBTN_UP_CENTER != null) ? FSkinImage.HDBTN_UP_CENTER : FSkinImage.BTN_UP_CENTER;
            imgR = (hdbuttonskin() && FSkinImage.HDBTN_UP_RIGHT != null) ? FSkinImage.HDBTN_UP_RIGHT : FSkinImage.BTN_UP_RIGHT;
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
            imgL = (hdbuttonskin() && FSkinImage.HDBTN_UP_LEFT != null) ? FSkinImage.HDBTN_UP_LEFT : FSkinImage.BTN_UP_LEFT;
            imgM = (hdbuttonskin() && FSkinImage.HDBTN_UP_CENTER != null) ? FSkinImage.HDBTN_UP_CENTER : FSkinImage.BTN_UP_CENTER;
            imgR = (hdbuttonskin() && FSkinImage.HDBTN_UP_RIGHT != null) ? FSkinImage.HDBTN_UP_RIGHT : FSkinImage.BTN_UP_RIGHT;
        }
        else if (isEnabled()) {
            resetImg();
        }
        else {
            imgL = (hdbuttonskin() && FSkinImage.HDBTN_UP_LEFT != null) ? FSkinImage.HDBTN_UP_LEFT : FSkinImage.BTN_UP_LEFT;
            imgM = (hdbuttonskin() && FSkinImage.HDBTN_UP_CENTER != null) ? FSkinImage.HDBTN_UP_CENTER : FSkinImage.BTN_UP_CENTER;
            imgR = (hdbuttonskin() && FSkinImage.HDBTN_UP_RIGHT != null) ? FSkinImage.HDBTN_UP_RIGHT : FSkinImage.BTN_UP_RIGHT;
        }
    }

    public Corner getCorner() {
        return corner;
    }
    public void setCorner(Corner corner0) {
        corner = corner0;
    }

    public void setCommand(FEventHandler command0) {
        command = command0;
    }

    public TextBounds getAutoSizeBounds() {
        TextBounds bounds = new TextBounds();
        bounds.width = font.getBounds(text).width + 2 * PADDING;
        bounds.height = 3 * font.getCapHeight();
        return bounds;
    }

    @Override
    public final boolean press(float x, float y) {
        if (isToggled()) { return true; }

        imgL = (hdbuttonskin() && FSkinImage.HDBTN_UP_LEFT != null) ? FSkinImage.HDBTN_UP_LEFT : FSkinImage.BTN_UP_LEFT;
        imgM = (hdbuttonskin() && FSkinImage.HDBTN_UP_CENTER != null) ? FSkinImage.HDBTN_UP_CENTER : FSkinImage.BTN_UP_CENTER;
        imgR = (hdbuttonskin() && FSkinImage.HDBTN_UP_RIGHT != null) ? FSkinImage.HDBTN_UP_RIGHT : FSkinImage.BTN_UP_RIGHT;

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
        if (command != null) {
            command.handleEvent(new FEvent(this, FEventType.TAP));
        }
        return true;
    }

    public boolean trigger() {
        if (isEnabled() && command != null) {
            command.handleEvent(new FEvent(this, FEventType.TAP));
            return true;
        }
        return false;
    }

    @Override
    public void draw(Graphics g) {
        float x = 0;
        float y = 0;
        float w = getWidth();
        float h = getHeight();

        float cornerButtonWidth = w / 2;
        float cornerButtonHeight = h * 1.5f;
        float cornerTextOffsetX = cornerButtonWidth / 2;
        float cornerTextOffsetY = (cornerButtonHeight - h) / 2;

        if (imgL.getTextureRegion() == null) {
            //handle rendering buttons before textures loaded
            FLabel.drawButtonBackground(g, w, h, imgL == FSkinImage.BTN_DOWN_LEFT);
        }
        else {
            //determine images to draw and text alignment based on which corner button is in (if any)
            switch (corner) {
            case None:
                if (w > 2 * h) {
                    g.drawImage(imgL, 0, 0, h, h);
                    g.drawImage(imgM, h, 0, w - (2 * h), h);
                    g.drawImage(imgR, w - h, 0, h, h);
                }
                else {
                    g.drawImage(imgL, 0, 0, cornerButtonWidth, h);
                    g.drawImage(imgR, cornerButtonWidth, 0, w - cornerButtonWidth, h);
                }
                x += PADDING;
                w -= 2 * PADDING;
                break;
            case BottomLeft:
                g.startClip(x, y, w, h);
                g.drawImage(imgM, 0, 0, cornerButtonWidth, cornerButtonHeight);
                g.drawImage(imgR, cornerButtonWidth, 0, cornerButtonWidth, cornerButtonHeight);
                g.endClip();
                w -= cornerTextOffsetX;
                y += cornerTextOffsetY;
                h -= cornerTextOffsetY;
                break;
            case BottomRight:
                g.startClip(x, y, w, h);
                g.drawImage(imgL, 0, 0, cornerButtonWidth, cornerButtonHeight);
                g.drawImage(imgM, cornerButtonWidth, 0, cornerButtonWidth, cornerButtonHeight);
                g.endClip();
                x += cornerTextOffsetX;
                w -= cornerTextOffsetX;
                y += cornerTextOffsetY;
                h -= cornerTextOffsetY;
                break;
            case BottomMiddle:
                g.startClip(x, y, w, h);
                cornerButtonWidth = w / 3;
                cornerTextOffsetX = cornerButtonWidth / 2;
                g.drawImage(imgL, 0, 0, cornerButtonWidth, cornerButtonHeight);
                g.drawImage(imgM, cornerButtonWidth, 0, w - 2 * cornerButtonWidth, cornerButtonHeight);
                g.drawImage(imgR, w - cornerButtonWidth, 0, cornerButtonWidth, cornerButtonHeight);
                g.endClip();
                x += cornerTextOffsetX / 2;
                w -= cornerTextOffsetX;
                y += cornerTextOffsetY;
                h -= cornerTextOffsetY;
                break;
            }
        }

        String displayText = text;
        if (!StringUtils.isEmpty(displayText)) {
            if (corner == Corner.BottomLeft || corner == Corner.BottomRight) {
                displayText = displayText.replaceFirst(" ", "\n"); //allow second word to wrap if corner button
            }
            g.drawText(displayText, font, foreColor, x, y, w, h, false, Align.center, true);
        }
    }

    @Override
    public boolean isSelected() {
        return isToggled();
    }

    @Override
    public void setSelected(boolean b0) {
        setToggled(b0);
    }

    @Override
    public boolean keyDown(int keyCode) {
        switch (keyCode) {
        case Keys.ENTER:
        case Keys.SPACE:
            return trigger(); //trigger button on Enter or Space
        }
        return false;
    }

    //use FEventHandler one except when references as IButton
    @Override
    public void setCommand(final UiCommand command0) {
        setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                command0.run();
            }
        });
    }

    @Override
    public boolean requestFocusInWindow() {
        return false;
    }

    @Override
    public void setImage(FSkinProp color) {
        foreColor = FSkinColor.get(Colors.fromSkinProp(color));
    }

    @Override
    public void setTextColor(int r, int g, int b) {
        foreColor = FSkinColor.getStandardColor(r, g, b);
    }

    public FSkinColor getForeColor() {
        return foreColor;
    }
}
