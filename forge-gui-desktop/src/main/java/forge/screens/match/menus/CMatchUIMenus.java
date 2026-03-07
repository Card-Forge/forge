package forge.screens.match.menus;

import java.util.Collections;
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
        final List<JMenu> menus = Lists.newArrayListWithCapacity(2);
        menus.add(new GameMenu(matchUI).getMenu());
        menus.add(new DisplayMenu(matchUI).getMenu());
        return menus;
    }

    @Override
    public List<JMenu> getTrailingMenus() {
        if (ForgePreferences.DEV_MODE) {
            return Collections.singletonList(new DevModeMenu(matchUI.getCDev()).getMenu());
        }
        return Collections.emptyList();
    }
}
