package forge.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.Assets;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FTextureRegionImage;
import forge.toolbox.FDisplayObject;
import forge.util.Utils;

public class FMenuTab extends FDisplayObject implements IUnreadIndicator {
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
    private static final FSkinFont BADGE_FONT = FSkinFont.get(9);
    private static final FSkinColor BADGE_COLOR = FSkinColor.getStandardColor(192, 50, 50);
    private static final FSkinColor BADGE_TEXT_COLOR = FSkinColor.getStandardColor(255, 255, 255);
    private static final long FLASH_DURATION_MS = 900;

    private final FMenuBar menuBar;
    private final FDropDown dropDown;

    private String text;
    private float minWidth;
    private int index;
    private int unreadCount;
    private long flashStartMs;
    private String iconName; //region of the topmenu atlas drawn instead of the text in the compact layout
    private boolean iconShowsCount;
    private FTextureRegionImage topIcon;
    private float topIconRatio = 1f;

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
    public FDropDown getDropDown() { return dropDown; }
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

    public void incrementUnread() {
        unreadCount++;
        flashStartMs = System.currentTimeMillis();
        Gdx.graphics.requestRendering();
    }

    public void clearUnread() {
        if (unreadCount == 0 && flashStartMs == 0) { return; }
        unreadCount = 0;
        flashStartMs = 0;
        Gdx.graphics.requestRendering();
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

    boolean isGlass() {
        if (menuBar == null)
            return false;
        return menuBar.isGlass();
    }

    public float getMinWidth() {
        if (iconOnly) {
            float multiplier = Forge.isLandscapeMode() ? 2.5f : 1.8f;
            return FONT.getLineHeight() * multiplier;
        }
        return minWidth;
    }

    //scratch and constant colours for drawGlass, so a frame allocates nothing (render thread only)
    private static final Color GLASS_HOVER = new Color(1f, 1f, 1f, 0.08f);
    private static final Color GLASS_DIVIDER = new Color(1f, 1f, 1f, 0.10f);
    private static final Color GLASS_TEXT_DIM = new Color(1f, 1f, 1f, 0.6f);
    private static final Color GLASS_GLOW_TOP = new Color(), GLASS_GLOW_BOTTOM = new Color(), GLASS_LINE = new Color();
    private static final Color COUNT_BADGE = new Color(1f, 0.69f, 0.2f, 1f);

    /**
     * Compact layout: names the topmenu atlas region this tab shows as its icon.
     * @param showCount draw the number in the tab's text, like the "(3)" of the stack tab, as a badge on the icon
     */
    public void setTopIcon(String regionName, boolean showCount) {
        iconName = regionName;
        iconShowsCount = showCount;
        topIcon = null;
    }

    //the number in the last brackets of the text, 0 if there is none
    private int getTextCount() {
        final int start = text.lastIndexOf('('), end = text.lastIndexOf(')');
        if (start < 0 || end < start) { return 0; }
        try {
            return Integer.parseInt(text.substring(start + 1, end).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    //draws the tab as its atlas icon; false when there is no icon or the atlas is missing, so the text is drawn instead
    private boolean drawTopIcon(Graphics g, float x, float y, float w, float h, boolean open) {
        if (iconName == null) { return false; }
        if (topIcon == null) {
            final TextureAtlas atlas = Assets.getInstance().getTopMenuAtlas();
            final TextureAtlas.AtlasRegion region = atlas == null ? null : atlas.findRegion(iconName);
            if (region == null) { return false; }
            topIcon = new FTextureRegionImage(region);
            topIconRatio = (float) region.getRegionWidth() / region.getRegionHeight();
        }
        final float ih = Math.min(h * 0.62f, w * 0.9f / topIconRatio);
        final float iw = ih * topIconRatio;
        final float ix = x + (w - iw) / 2, iy = y + (h - ih) / 2;
        final float oldAlpha = g.getfloatAlphaComposite();
        g.setAlphaComposite(open ? 1f : 0.65f); //the icons are white, so alpha is the dim state
        g.drawImage(topIcon, ix, iy, iw, ih);
        g.setAlphaComposite(oldAlpha);
        if (iconShowsCount) {
            final int count = getTextCount();
            if (count > 0) {
                final String label = count > 99 ? "99+" : Integer.toString(count);
                final float d = Math.max(BADGE_FONT.getLineHeight(), BADGE_FONT.getBounds(label).width + 2 * Utils.scale(4));
                final float bx = ix + iw - d / 2, by = iy - d / 3;
                g.fillRect(COUNT_BADGE, bx, by, d, d);
                g.drawText(label, BADGE_FONT, Color.BLACK, bx, by, d, d, false, Align.center, true);
            }
        }
        return true;
    }

    //frosted look: no filled block or border, a soft glow under the open tab with a thin accent line
    private void drawGlass(Graphics g) {
        final float w = getWidth(), h = getHeight();
        final boolean open = dropDown.isVisible();
        final Color accent = getSelBackColor().getColor();
        if (open) {
            GLASS_GLOW_TOP.set(accent.r, accent.g, accent.b, 0f);
            GLASS_GLOW_BOTTOM.set(accent.r, accent.g, accent.b, 0.45f);
            GLASS_LINE.set(accent.r, accent.g, accent.b, 1f);
            g.fillGradientRect(GLASS_GLOW_TOP, GLASS_GLOW_BOTTOM, true, 0, 0, w, h);
            g.fillRect(GLASS_LINE, 0, h - Utils.scale(2), w, Utils.scale(2));
        } else if (isHovered()) {
            g.fillRect(GLASS_HOVER, 0, 0, w, h);
        }
        if (flashStartMs > 0) { //unread message flash
            long elapsed = System.currentTimeMillis() - flashStartMs;
            if (elapsed < FLASH_DURATION_MS) {
                g.fillRect(BADGE_COLOR.alphaColor(0.55f * (1f - (float) elapsed / FLASH_DURATION_MS)), 0, 0, w, h);
                Gdx.graphics.requestRendering();
            } else {
                flashStartMs = 0;
            }
        }
        if (index < menuBar.getTabCount() - 1) { //faint divider
            g.drawLine(SEPARATOR_WIDTH, GLASS_DIVIDER, w, h / 4, w, h - h / 4);
        }
        final float x = PADDING, y = PADDING, tw = w - 2 * PADDING, th = h - 2 * PADDING;
        if (iconOnly) {
            FImage icon = active ? FSkinImage.SEE : FSkinImage.UNSEE;
            //in the compact layout the tab is as wide as the others, so the icon is held to the height of the atlas icons
            float scaleH = Math.min(tw * 0.8f * 0.6f, menuBar.isIconMode() ? th * 0.62f : Float.MAX_VALUE);
            float scaleW = scaleH / 0.6f;
            g.drawImage(icon, x + tw / 2 - scaleW / 2, y + th / 2 - scaleH / 2, scaleW, scaleH);
        } else if (menuBar.isIconMode() && drawTopIcon(g, x, y, tw, th, open)) {
            //drawn as an icon from the topmenu atlas
        } else {
            g.drawText(text, FONT, open ? Color.WHITE : GLASS_TEXT_DIM, x, y, tw, th, false, Align.center, true);
        }
        drawUnreadBadge(g, unreadCount, w);
    }

    @Override
    public void draw(Graphics g) {
        float x, y, w, h;

        FSkinColor foreColor;
        if (menuBar.isGlass()) {
            drawGlass(g);
            return;
        }
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

        //flash overlay while a recent unread message highlights the tab
        if (flashStartMs > 0) {
            long elapsed = System.currentTimeMillis() - flashStartMs;
            if (elapsed < FLASH_DURATION_MS) {
                float alpha = 0.55f * (1f - (float) elapsed / FLASH_DURATION_MS);
                g.fillRect(BADGE_COLOR.alphaColor(alpha), x, y, w, h);
                Gdx.graphics.requestRendering();
            } else {
                flashStartMs = 0;
            }
        }

        if (iconOnly) {
            float mod = w * 0.75f;
            FImage icon = active ? FSkinImage.SEE : FSkinImage.UNSEE;
            float scaleW = w * 0.8f;
            float scaleH = scaleW * 0.6f;
            g.drawImage(icon, x + w/2 - scaleW/2, y + h/2 - scaleH/2, scaleW, scaleH);
        } else
            g.drawText(text, FONT, foreColor, x, y, w, h, false, Align.center, true);

        drawUnreadBadge(g, unreadCount, getWidth());
    }

    /** Draws the unread-message count badge in the top-right corner of a widget of the given width. */
    public static void drawUnreadBadge(Graphics g, int unreadCount, float widgetWidth) {
        if (unreadCount <= 0) { return; }
        String label = unreadCount > 99 ? "99+" : Integer.toString(unreadCount);
        float textW = BADGE_FONT.getBounds(label).width;
        float diameter = Math.max(BADGE_FONT.getLineHeight(), textW + 2 * Utils.scale(4));
        float bx = widgetWidth - diameter - PADDING;
        float by = PADDING;
        g.fillRect(BADGE_COLOR, bx, by, diameter, diameter);
        g.drawText(label, BADGE_FONT, BADGE_TEXT_COLOR, bx, by, diameter, diameter, false, Align.center, true);
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
