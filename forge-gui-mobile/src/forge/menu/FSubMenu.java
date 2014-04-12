package forge.menu;

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
    public boolean tap(float x, float y, int count) {
        if (popupMenu.isVisible()) {
            popupMenu.hide();
        }
        else {
            popupMenu.show(this, getWidth(), 0);
        }
        return true;
    }
}
