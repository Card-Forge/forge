package forge.screens.home.sanctioned;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;

import forge.gui.framework.ICDoc;
import forge.menus.IMenuProvider;
import forge.menus.MenuUtil;
import forge.screens.home.CLobby;

/**
 * Controls the constructed submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuConstructed implements ICDoc, IMenuProvider {
    /** */
    SINGLETON_INSTANCE;

    private final VSubmenuConstructed view = VSubmenuConstructed.SINGLETON_INSTANCE;
    private final CLobby lobby = new CLobby(view.getLobby());

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void update() {
        MenuUtil.setMenuProvider(this);
        lobby.update();
    }

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void initialize() {
        lobby.initialize();
    }

    /* (non-Javadoc)
     * @see forge.gui.menubar.IMenuProvider#getMenus()
     */
    @Override
    public List<JMenu> getMenus() {
        final List<JMenu> menus = new ArrayList<JMenu>();
        menus.add(ConstructedGameMenu.getMenu());
        return menus;
    }

}
