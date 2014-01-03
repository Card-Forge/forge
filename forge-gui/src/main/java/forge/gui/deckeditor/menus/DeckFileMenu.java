package forge.gui.deckeditor.menus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.views.VCurrentDeck;
import forge.gui.menus.MenuUtil;
import forge.gui.toolbox.FSkin;

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

        JMenu menu = new JMenu("File");
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

    private static JMenuItem menuItem_Save, menuItem_SaveAs;

    public static void updateSaveEnabled() {
        if (menuItem_Save != null) {
            menuItem_Save.setEnabled(CDeckEditorUI.SINGLETON_INSTANCE.hasChanges());
        }
        if (menuItem_SaveAs != null) {
            menuItem_SaveAs.setEnabled(CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController() != null);
        }
    }

    private static JMenuItem getMenuItem_New() {
        JMenuItem menuItem = new JMenuItem("New Deck");
        FSkin.get(menuItem).setIcon(showIcons ? MenuUtil.getMenuIcon(FSkin.InterfaceIcons.ICO_NEW) : null);
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

    private static JMenuItem getMenuItem_Open() {
        JMenuItem menuItem = new JMenuItem("Open Deck");
        FSkin.get(menuItem).setIcon(showIcons ? MenuUtil.getMenuIcon(FSkin.InterfaceIcons.ICO_OPEN) : null);
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

    private static JMenuItem getMenuItem_Import() {
        JMenuItem menuItem = new JMenuItem("Import Deck");
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

    private static JMenuItem getMenuItem_Save() {
        JMenuItem menuItem = new JMenuItem("Save Deck");
        FSkin.get(menuItem).setIcon(showIcons ? MenuUtil.getMenuIcon(FSkin.InterfaceIcons.ICO_SAVE) : null);
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

    private static JMenuItem getMenuItem_SaveAs() {
        JMenuItem menuItem = new JMenuItem("Save Deck As");
        FSkin.get(menuItem).setIcon(showIcons ? MenuUtil.getMenuIcon(FSkin.InterfaceIcons.ICO_SAVEAS) : null);
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

    private static JMenuItem getMenuItem_Print() {
        JMenuItem menuItem = new JMenuItem("Print to HTML file");
        FSkin.get(menuItem).setIcon(showIcons ? MenuUtil.getMenuIcon(FSkin.InterfaceIcons.ICO_PRINT) : null);
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
