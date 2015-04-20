package forge.screens.match.menus;

import java.util.List;

import javax.swing.JMenu;

import com.google.common.collect.Lists;

import forge.properties.ForgePreferences;
import forge.screens.match.CMatchUI;

/**
 * Gets the menus associated with the Game screen.
 *
 */
public class CMatchUIMenus {

    private final CMatchUI matchUI;
    public CMatchUIMenus(final CMatchUI matchUI) {
        this.matchUI = matchUI;
    }

    public List<JMenu> getMenus() {
        final List<JMenu> menus = Lists.newArrayListWithCapacity(2);
        menus.add(new GameMenu(matchUI).getMenu());
        if (ForgePreferences.DEV_MODE) {
            menus.add(new DevModeMenu(matchUI.getCDev()).getMenu());
        }
        return menus;
    }
}
