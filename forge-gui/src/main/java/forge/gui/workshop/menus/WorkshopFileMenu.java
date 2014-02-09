package forge.gui.workshop.menus;

import forge.gui.menus.MenuUtil;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FSkin.SkinnedMenuItem;
import forge.gui.workshop.controllers.CCardScript;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

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
    	menuItem_SaveCard.setEnabled(CCardScript.SINGLETON_INSTANCE.hasChanges());
    }

    private static JMenuItem getMenuItem_SaveCard() {
        SkinnedMenuItem menuItem = new SkinnedMenuItem("Save and Apply Card Changes");
        menuItem.setIcon(showIcons ? MenuUtil.getMenuIcon(FSkin.InterfaceIcons.ICO_SAVE) : null);
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_S));
        menuItem.addActionListener(getSaveCardAction());
        menuItem_SaveCard = menuItem;
        updateSaveEnabled();
        return menuItem;
    }

    private static ActionListener getSaveCardAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	CCardScript.SINGLETON_INSTANCE.saveChanges();
            }
        };
    }
}
