package forge.screens.home.playcommander;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;

import forge.gui.framework.ICDoc;
import forge.menus.IMenuProvider;
import forge.menus.MenuUtil;
import forge.screens.home.CLobby;

public enum CSubmenuPlayCommander implements ICDoc, IMenuProvider {

    SINGLETON_INSTANCE;

    private final VSubmenuPlayCommander view = VSubmenuPlayCommander.SINGLETON_INSTANCE;
    private final CLobby lobby = view.getLobby().getController();

    @Override
    public void register() {
    }

    @Override
    public void update() {
        MenuUtil.setMenuProvider(this);
        lobby.update();
    }

    @Override
    public void initialize() {
        lobby.initialize();
    }

    @Override
    public List<JMenu> getMenus() {
        final List<JMenu> menus = new ArrayList<>();
        return menus;
    }
}
