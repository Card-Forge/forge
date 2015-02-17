package forge.screens.match.menus;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;

import forge.properties.ForgePreferences;
import forge.screens.match.CMatchUI;
import forge.screens.match.controllers.CDev;

/**
 * Gets the menus associated with the Game screen.
 *
 */
public class CMatchUIMenus {

    private final boolean SHOW_ICONS = false;

    private final CMatchUI matchUI;
    public CMatchUIMenus(final CMatchUI matchUI) {
        this.matchUI = matchUI;
    }

    public List<JMenu> getMenus(final CDev devController) {
        final List<JMenu> menus = new ArrayList<JMenu>();
        menus.add(new GameMenu(matchUI).getMenu(SHOW_ICONS));
        if (ForgePreferences.DEV_MODE) {
            menus.add(new DevModeMenu(devController).getMenu());
        }
        return menus;
    }
}
