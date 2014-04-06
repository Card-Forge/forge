package forge.menu;

import forge.assets.FImage;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;

public class FSubMenu extends FMenuItem {
    public FSubMenu(String text0, FPopupMenu popupMenu) {
        this(text0, null, popupMenu, true);
    }
    public FSubMenu(String text0, FPopupMenu popupMenu, boolean enabled0) {
        this(text0, null, popupMenu, enabled0);
    }
    public FSubMenu(String text0, FImage icon0, final FPopupMenu popupMenu) {
        this(text0, icon0, popupMenu, true);
    }
    public FSubMenu(String text0, FImage icon0, final FPopupMenu popupMenu, boolean enabled0) {
        super(text0, icon0, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                popupMenu.show(e.getSource(), e.getSource().getWidth(), 0);
            }
        }, enabled0);
    }
}
