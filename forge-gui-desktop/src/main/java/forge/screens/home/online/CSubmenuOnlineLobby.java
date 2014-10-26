package forge.screens.home.online;

import forge.UiCommand;
import forge.gui.framework.ICDoc;
import forge.menus.IMenuProvider;
import forge.menus.MenuUtil;
import javax.swing.*;

import java.util.ArrayList;
import java.util.List;


public enum CSubmenuOnlineLobby implements ICDoc, IMenuProvider {
    SINGLETON_INSTANCE;

    private final VSubmenuOnlineLobby view = VSubmenuOnlineLobby.SINGLETON_INSTANCE;

    @Override
    public void update() {
        MenuUtil.setMenuProvider(this);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                LoginDialog.login();
            }
        });
    }

    @Override
    public void initialize() {
    }

    @Override
    public UiCommand getCommandOnSelect() {
        return null;
    }

    @Override
    public List<JMenu> getMenus() {
        List<JMenu> menus = new ArrayList<JMenu>();
        return menus;
    }
}
