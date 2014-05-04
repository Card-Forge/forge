package forge.screens.match.menus;

import forge.properties.ForgePreferences;
import javax.swing.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Gets the menus associated with the Game screen.
 *
 */
public class CMatchUIMenus {

    private final boolean SHOW_ICONS = false;

    public List<JMenu> getMenus() {
        List<JMenu> menus = new ArrayList<JMenu>();
        menus.add(GameMenu.getMenu(SHOW_ICONS));
        if (ForgePreferences.DEV_MODE) {
            menus.add(DevModeMenu.getMenu());
        }
        return menus;
    }
}
