package forge.screens.deckeditor.menus;

import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;

import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.menus.MenuUtil;
import forge.model.FModel;
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
        menu.add(getMenuItem_CopyToClipboard());
        menu.add(new JSeparator());
        menu.add(getMenuItem_EnforceConformity());
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
        return e -> VCurrentDeck.SINGLETON_INSTANCE.getBtnNew().getCommand().run();
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
        return e -> VCurrentDeck.SINGLETON_INSTANCE.getBtnOpen().getCommand().run();
    }

    private static SkinnedMenuItem getMenuItem_Import() {
        final Localizer localizer = Localizer.getInstance();
        SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblImportDeck"));
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_I));
        menuItem.addActionListener(getImportAction());
        return menuItem;
    }

    private static ActionListener getImportAction() {
        return e -> VCurrentDeck.SINGLETON_INSTANCE.getBtnImport().getCommand().run();
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
        return e -> VCurrentDeck.SINGLETON_INSTANCE.getBtnSave().getCommand().run();
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
        return e -> VCurrentDeck.SINGLETON_INSTANCE.getBtnSaveAs().getCommand().run();
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
        return e -> VCurrentDeck.SINGLETON_INSTANCE.getBtnPrintProxies().getCommand().run();
    }

    private static SkinnedMenuItem getMenuItem_CopyToClipboard() {
        final Localizer localizer = Localizer.getInstance();
        SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("btnCopyToClipboard"));
        menuItem.setIcon(showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_CLIPBOARD) : null);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK));
        menuItem.addActionListener(e -> VCurrentDeck.SINGLETON_INSTANCE.getBtnCopyToClipboard().getCommand().run());
        return menuItem;
    }

    private static JCheckBoxMenuItem getMenuItem_EnforceConformity() {
        final Localizer localizer = Localizer.getInstance();
        JCheckBoxMenuItem checkItem = new JCheckBoxMenuItem(localizer.getMessage("cbEnforceDeckLegality"));
        checkItem.setSelected(FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY));
        checkItem.addActionListener(e -> {
            FModel.getPreferences().setPref(FPref.ENFORCE_DECK_LEGALITY, checkItem.isSelected());
            FModel.getPreferences().save();
        });
        return checkItem;
    }
}
