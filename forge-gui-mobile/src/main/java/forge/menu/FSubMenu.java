package forge.menu;

import forge.Graphics;
import forge.assets.FImage;

public class FSubMenu extends FMenuItem {
    FPopupMenu popupMenu;

    public FSubMenu(String text0, FPopupMenu popupMenu0) {
        this(text0, null, popupMenu0, true);
    }
    public FSubMenu(String text0, FPopupMenu popupMenu0, boolean enabled0) {
        this(text0, null, popupMenu0, enabled0);
    }
    public FSubMenu(String text0, FImage icon0, final FPopupMenu popupMenu0) {
        this(text0, icon0, popupMenu0, true);
    }
    public FSubMenu(String text0, FImage icon0, final FPopupMenu popupMenu0, boolean enabled0) {
        super(text0, icon0, null, enabled0);
        popupMenu = popupMenu0;
    }

    @Override
    protected boolean showPressedColor() {
        return super.showPressedColor() || popupMenu.isVisible();
    }

    @Override
    public boolean tap(float x, float y, int count) {
        if (popupMenu.isVisible()) {
            popupMenu.hide();
        }
        else {
            popupMenu.show(this, getWidth(), 0);
        }
        return true;
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);

        float divotWidth = DIVOT_WIDTH;
        float divotHeight = divotWidth * 2f;
        float x2 = getWidth() - GAP_X - 1;
        float x1 = x2 - divotWidth;
        float x3 = x1;
        float y2 = getHeight() / 2;
        float y1 = y2 - divotHeight / 2;
        float y3 = y2 + divotHeight / 2;
        g.fillTriangle(getForeColor(), x1, y1, x2, y2, x3, y3);
    }
}
