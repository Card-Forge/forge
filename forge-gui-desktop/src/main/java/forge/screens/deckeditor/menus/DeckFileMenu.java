package forge.screens.deckeditor.menus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JSeparator;

import forge.localinstance.skin.FSkinProp;
import forge.menus.MenuUtil;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.views.VCurrentDeck;
import forge.toolbox.FSkin.SkinnedMenuItem;
import forge.util.Localizer;

/**
 * Returns a JMenu containing options associated with current game.
 * <p>
 * Replicates options available in Dock tab.
 */
public final class DeckFileMenu {
    private DeckFileMenu() { }

    private static boolean showIcons;

    public static JMenu getMenu(boolean showMenuIcons) {
    	showIcons = showMenuIcons;
        final Localizer localizer = Localizer.getInstance();
        JMenu menu = new JMenu(localizer.getMessage("lblFile"));
        menu.setMnemonic(KeyEvent.VK_F);
        menu.add(getMenuItem_New());
        menu.add(getMenuItem_Open());
        menu.add(getMenuItem_Import());
        menu.add(new JSeparator());
        menu.add(getMenuItem_Save());
        menu.add(getMenuItem_SaveAs());
        menu.add(new JSeparator());
        menu.add(getMenuItem_Print());
        updateSaveEnabled();
        return menu;
    }

    private static SkinnedMenuItem menuItem_Save, menuItem_SaveAs;

    public static void updateSaveEnabled() {
        if (menuItem_Save != null) {
            menuItem_Save.setEnabled(CDeckEditorUI.SINGLETON_INSTANCE.hasChanges());
        }
        if (menuItem_SaveAs != null) {
            menuItem_SaveAs.setEnabled(CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController() != null);
        }
    }

    private static SkinnedMenuItem getMenuItem_New() {
        final Localizer localizer = Localizer.getInstance();
        SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblNewDeck"));
        menuItem.setIcon(showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_NEW) : null);
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_N));
        menuItem.addActionListener(getNewAction());
        return menuItem;
    }

    private static ActionListener getNewAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                VCurrentDeck.SINGLETON_INSTANCE.getBtnNew().getCommand().run();
            }
        };
    }

    private static SkinnedMenuItem getMenuItem_Open() {
        final Localizer localizer = Localizer.getInstance();
        SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblOpenDeck"));
        menuItem.setIcon(showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_OPEN) : null);
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_O));
        menuItem.addActionListener(getOpenAction());
        return menuItem;
    }

    private static ActionListener getOpenAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                VCurrentDeck.SINGLETON_INSTANCE.getBtnOpen().getCommand().run();
            }
        };
    }

    private static SkinnedMenuItem getMenuItem_Import() {
        final Localizer localizer = Localizer.getInstance();
        SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblImportDeck"));
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_I));
        menuItem.addActionListener(getImportAction());
        return menuItem;
    }

    private static ActionListener getImportAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                VCurrentDeck.SINGLETON_INSTANCE.getBtnImport().getCommand().run();
            }
        };
    }

    private static SkinnedMenuItem getMenuItem_Save() {
        final Localizer localizer = Localizer.getInstance();
        SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblSaveDeck"));
        menuItem.setIcon(showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_SAVE) : null);
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_S));
        menuItem.addActionListener(getSaveAction());
        menuItem_Save = menuItem;
        return menuItem;
    }

    private static ActionListener getSaveAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	VCurrentDeck.SINGLETON_INSTANCE.getBtnSave().getCommand().run();
            }
        };
    }

    private static SkinnedMenuItem getMenuItem_SaveAs() {
        final Localizer localizer = Localizer.getInstance();
        SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblSaveDeckAs"));
        menuItem.setIcon(showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_SAVEAS) : null);
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_E));
        menuItem.addActionListener(getSaveAsAction());
        menuItem_SaveAs = menuItem;
        return menuItem;
    }

    private static ActionListener getSaveAsAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                VCurrentDeck.SINGLETON_INSTANCE.getBtnSaveAs().getCommand().run();
            }
        };
    }

    private static SkinnedMenuItem getMenuItem_Print() {
        final Localizer localizer = Localizer.getInstance();
        SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblPrinttoHTMLfile"));
        menuItem.setIcon(showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_PRINT) : null);
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_P));
        menuItem.addActionListener(getPrintAction());
        return menuItem;
    }

    private static ActionListener getPrintAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                VCurrentDeck.SINGLETON_INSTANCE.getBtnPrintProxies().getCommand().run();
            }
        };
    }
}
