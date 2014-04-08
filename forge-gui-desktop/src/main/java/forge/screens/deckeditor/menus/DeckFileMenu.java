package forge.screens.deckeditor.menus;

import forge.assets.FSkinProp;
import forge.menus.MenuUtil;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.views.VCurrentDeck;
import forge.toolbox.FSkin.SkinnedMenuItem;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

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
        SkinnedMenuItem menuItem = new SkinnedMenuItem("New Deck");
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
        SkinnedMenuItem menuItem = new SkinnedMenuItem("Open Deck");
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
        SkinnedMenuItem menuItem = new SkinnedMenuItem("Import Deck");
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
        SkinnedMenuItem menuItem = new SkinnedMenuItem("Save Deck");
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
        SkinnedMenuItem menuItem = new SkinnedMenuItem("Save Deck As");
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
        SkinnedMenuItem menuItem = new SkinnedMenuItem("Print to HTML file");
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
