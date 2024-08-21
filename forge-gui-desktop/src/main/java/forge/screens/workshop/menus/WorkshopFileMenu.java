package forge.screens.workshop.menus;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import forge.localinstance.skin.FSkinProp;
import forge.menus.MenuUtil;
import forge.screens.workshop.controllers.CCardScript;
import forge.toolbox.FSkin.SkinnedMenuItem;

/**
 * Returns a JMenu containing options associated with current game.
 * <p>
 * Replicates options available in Dock tab.
 */
public final class WorkshopFileMenu {
    private WorkshopFileMenu() { }

    private static boolean showIcons;

    public static JMenu getMenu(boolean showMenuIcons) {
    	showIcons = showMenuIcons;

        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        menu.add(getMenuItem_SaveCard());
        return menu;
    }
    
    private static JMenuItem menuItem_SaveCard;
    
    public static void updateSaveEnabled() {
        if (menuItem_SaveCard == null)
            getMenuItem_SaveCard();
    	menuItem_SaveCard.setEnabled(CCardScript.SINGLETON_INSTANCE.hasChanges());
    }

    private static JMenuItem getMenuItem_SaveCard() {
        SkinnedMenuItem menuItem = new SkinnedMenuItem("Save and Apply Card Changes");
        menuItem.setIcon(showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_SAVE) : null);
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_S));
        menuItem.addActionListener(getSaveCardAction());
        menuItem_SaveCard = menuItem;
        updateSaveEnabled();
        return menuItem;
    }

    private static ActionListener getSaveCardAction() {
        return e -> CCardScript.SINGLETON_INSTANCE.saveChanges();
    }
}
