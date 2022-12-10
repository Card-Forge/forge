package forge.menu;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.toolbox.FDisplayObject;
import forge.util.Utils;

public class FMenuTab extends FDisplayObject {
    public static final FSkinFont FONT = FSkinFont.get(12);
    boolean iconOnly = false;
    boolean active = false;
    private static FSkinColor getSelBackColor() {
        if (Forge.isMobileAdventureMode)
            return FSkinColor.get(Colors.ADV_CLR_ACTIVE);
        return FSkinColor.get(Colors.CLR_ACTIVE);
    }
    private static FSkinColor getSelBorderColor() {
        return FDropDown.getBorderColor();
    }
    private static FSkinColor getSelForeColor() {
        if (Forge.isMobileAdventureMode)
            return FSkinColor.get(Colors.ADV_CLR_TEXT);
        return FSkinColor.get(Colors.CLR_TEXT);
    }
    private static FSkinColor getForeColor() {
        return getSelForeColor().alphaColor(0.5f);
    }
    private static FSkinColor getSeparatorColor() {
        return getSelForeColor().alphaColor(0.3f);
    }
    public static final float PADDING = Utils.scale(2);
    private static final float SEPARATOR_WIDTH = Utils.scale(1);

    private final FMenuBar menuBar;
    private final FDropDown dropDown;

    private String text;
    private float minWidth;
    private int index;

    public FMenuTab(String text0, FMenuBar menuBar0, FDropDown dropDown0, int index0, boolean iconOnly0) {
        menuBar = menuBar0;
        dropDown = dropDown0;
        index = index0;
        iconOnly = iconOnly0;
        setText(text0);
    }

    @Override
    public boolean tap(float x, float y, int count) {
        if (dropDown.isVisible()) {
            dropDown.hide();
        }
        else {
            dropDown.show();
        }
        return true;
    }
    public void hideDropDown() {
        if (dropDown.isVisible())
            dropDown.hide();
    }
    public void showDropDown() {
        if (!dropDown.isVisible())
            dropDown.show();
    }

    @Override
    public boolean keyDown(int keyCode) {
        if (Forge.hasGamepad() && dropDown.isVisible()) {
            if (keyCode == Input.Keys.DPAD_UP)
                dropDown.setPreviousSelected();
            if (keyCode == Input.Keys.DPAD_DOWN)
                dropDown.setNextSelected();
            if (keyCode == Input.Keys.BUTTON_A)
                dropDown.tapChild();
            if (keyCode == Input.Keys.BUTTON_B)
                dropDown.cancel();
        }
        return super.keyDown(keyCode);
    }

    public void setText(String text0) {
        text = text0;
        minWidth = FONT.getBounds(text).width;
        menuBar.revalidate();
    }

    public void setActiveIcon(boolean value) {
        active = value;
    }

    @Override
    public void setVisible(boolean visible0) {
        if (isVisible() == visible0) { return; }
        super.setVisible(visible0);
        if (!visible0) {
            dropDown.hide();
        }
        if (menuBar != null) {
            menuBar.revalidate();
        }
    }

    public float getMinWidth() {
        if (iconOnly) {
            float multiplier = Forge.isLandscapeMode() ? 2.5f : 1.8f;
            return FONT.getLineHeight() * multiplier;
        }
        return minWidth;
    }

    @Override
    public void draw(Graphics g) {
        float x, y, w, h;

        FSkinColor foreColor;
        if (dropDown.isVisible()) {
            x = PADDING; //round so lines show up reliably
            y = PADDING;
            w = getWidth() - 2 * x + 1;
            h = getHeight() - y + 1;

            g.startClip(x, y, w, h);
            g.fillRect(getSelBackColor(), x, y, w, h);
            g.drawRect(2, getSelBorderColor(), x, y, w, h);
            g.endClip();

            foreColor = getSelForeColor();
        }
        else { 
            foreColor = getForeColor();
        }

        //draw right separator
        if (index < menuBar.getTabCount() - 1) {
            x = getWidth();
            y = getHeight() / 4;
            g.drawLine(SEPARATOR_WIDTH, getSeparatorColor(), x, y, x, getHeight() - y);
        }

        x = PADDING;
        y = PADDING;
        w = getWidth() - 2 * PADDING;
        h = getHeight() - 2 * PADDING;
        if (isHovered())
            g.fillRect(getSelBackColor().brighter(), x, y, w, h);
        if (iconOnly) {
            float mod = w * 0.75f;
            FImage icon = active ? FSkinImage.SEE : FSkinImage.UNSEE;
            float scaleW = icon.getWidth() * 0.8f;
            float scaleH = icon.getHeight() * 0.8f;
            g.drawImage(icon, x + w/2 - scaleW/2, y + h/2 - scaleH/2, scaleW, scaleH);
        } else
            g.drawText(text, FONT, foreColor, x, y, w, h, false, Align.center, true);
    }
    public boolean isShowingDropdownMenu(boolean any) {
        if (dropDown == null)
            return false;
        if (any)
            return dropDown.isVisible();
        return dropDown.isVisible() && dropDown instanceof FDropDownMenu;
    }
    public void clearSelected() {
        if (menuBar != null)
            menuBar.clearSelected();
    }
}
