package forge.screens.match.menus;

import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import com.google.common.collect.Lists;

import forge.gamemodes.net.client.ReplayPlayer;
import forge.localinstance.properties.ForgePreferences;
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
        if (matchUI.isReplay()) {
            // a recording has nothing to concede, undo, yield or cheat with: only a way out
            final JMenu menu = new JMenu("Replay");
            final JMenuItem close = new JMenuItem("Close replay");
            close.addActionListener(e -> ReplayPlayer.stopFor(matchUI, true));
            menu.add(close);
            menus.add(menu);
            return menus;
        }
        menus.add(new GameMenu(matchUI).getMenu());
        if (ForgePreferences.DEV_MODE) {
            menus.add(new DevModeMenu(matchUI.getCDev()).getMenu());
        }
        return menus;
    }
}
