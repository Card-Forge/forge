package forge.adventure.libgdxgui.menu;

import com.badlogic.gdx.utils.Align;
import forge.adventure.libgdxgui.Forge;
import forge.adventure.libgdxgui.Graphics;
import forge.adventure.libgdxgui.assets.FSkinColor;
import forge.adventure.libgdxgui.assets.FSkinColor.Colors;
import forge.adventure.libgdxgui.assets.FSkinFont;
import forge.adventure.libgdxgui.screens.FScreen;
import forge.adventure.libgdxgui.toolbox.FDisplayObject;
import forge.adventure.libgdxgui.util.TextBounds;
import forge.adventure.libgdxgui.util.Utils;

public class FTooltip extends FDropDown {
    private static final FSkinFont FONT = FSkinFont.get(12);
    private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final float PADDING = Utils.scale(5);

    private FDisplayObject owner;
    private float x, y;
    private final String text;

    public void show(FDisplayObject owner0, float x0, float y0) {
        owner = owner0;
        x = owner.localToScreenX(x0);
        y = owner.localToScreenY(y0);
        show();
    }

    @Override
    protected void updateSizeAndPosition() {
        FScreen screen = Forge.getCurrentScreen();
        float screenWidth = screen.getWidth();
        float screenHeight = screen.getHeight();

        paneSize = updateAndGetPaneSize(screenWidth, screenHeight);
        if (x + paneSize.getWidth() > screenWidth) {
            x = screenWidth - paneSize.getWidth();
        }
        if (y + paneSize.getHeight() > screenHeight) {
            y = screenHeight - paneSize.getHeight();
        }

        setBounds(Math.round(x), Math.round(y), Math.round(paneSize.getWidth()), Math.round(paneSize.getHeight()));
    }

    public FTooltip(String text0) {
        text = text0;
    }

    @Override
    protected boolean autoHide() {
        return true;
    }

    @Override
    protected FDisplayObject getDropDownOwner() {
        return owner;
    }

    @Override
    protected ScrollBounds updateAndGetPaneSize(float maxWidth, float maxVisibleHeight) {
        TextBounds bounds = FONT.getWrappedBounds(text, maxWidth - 2 * PADDING);
        return new ScrollBounds(Math.min(maxWidth, bounds.width + 2 * PADDING), bounds.height + 2 * PADDING);
    }

    @Override
    public void drawBackground(Graphics g) {
        super.drawBackground(g);
        g.drawText(text, FONT, FORE_COLOR, PADDING - getScrollLeft(), PADDING - getScrollTop(), getScrollWidth() - 2 * PADDING, getScrollHeight() - 2 * PADDING, true, Align.left, false);
    }
}
