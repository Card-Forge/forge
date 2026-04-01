package forge.screens.match.menus;

import java.util.List;

import javax.swing.JMenu;

import com.google.common.collect.Lists;

import forge.localinstance.properties.ForgePreferences;
import forge.menus.IMenuProvider;
import forge.screens.match.CMatchUI;

/**
 * Gets the menus associated with the Game screen.
 *
 */
public class CMatchUIMenus implements IMenuProvider {

    private final CMatchUI matchUI;
    public CMatchUIMenus(final CMatchUI matchUI) {
        this.matchUI = matchUI;
    }

    @Override
    public List<JMenu> getMenus() {
        final List<JMenu> menus = Lists.newArrayListWithCapacity(3);
        menus.add(new GameMenu(matchUI).getMenu());
        menus.add(new DisplayMenu(matchUI).getMenu());
        if (ForgePreferences.DEV_MODE) {
            menus.add(new DevModeMenu(matchUI.getCDev()).getMenu());
        }
        return menus;
    }
}
