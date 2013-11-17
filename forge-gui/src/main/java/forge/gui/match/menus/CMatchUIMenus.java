package forge.gui.match.menus;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;

import forge.Singletons;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;

/**
 * Gets the menus associated with the Game screen.
 *
 */
public class CMatchUIMenus {

    private final boolean SHOW_ICONS = false;
    private static ForgePreferences prefs = Singletons.getModel().getPreferences();

    public List<JMenu> getMenus() {
        List<JMenu> menus = new ArrayList<JMenu>();
        menus.add(GameMenu.getMenu(SHOW_ICONS));
        if (isDevModeEnabled()) {
            menus.add(DevModeMenu.getMenu());
        }
        return menus;
    }

    private boolean isDevModeEnabled() {
        return prefs.getPrefBoolean(FPref.DEV_MODE_ENABLED);
    }
}
