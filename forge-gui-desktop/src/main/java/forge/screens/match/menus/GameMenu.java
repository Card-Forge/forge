package forge.screens.match.menus;

import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.KeyStroke;

import forge.control.KeyboardShortcuts;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.screens.match.CMatchUI;
import forge.screens.match.VAutoYieldsAndTriggers;
import forge.toolbox.FSkin.SkinnedMenuItem;
import forge.util.Localizer;

/**
 * Returns a JMenu containing core in-game actions for the current match.
 */
public final class GameMenu {
    private final CMatchUI matchUI;
    public GameMenu(final CMatchUI matchUI) {
        this.matchUI = matchUI;
    }

    public JMenu getMenu() {
        final Localizer localizer = Localizer.getInstance();
        final JMenu menu = new JMenu(localizer.getMessage("lblGame"));
        menu.setMnemonic(KeyEvent.VK_G);
        menu.add(getMenuItem_Undo());
        menu.add(getMenuItem_Concede());
        menu.add(getMenuItem_EndTurn());
        menu.add(getMenuItem_AlphaStrike());
        menu.addSeparator();
        menu.add(getMenuItem_ViewDeckList());
        menu.addSeparator();
        menu.add(getMenuItem_AutoYieldsAndTriggers());
        return menu;
    }

    private SkinnedMenuItem getMenuItem_Undo() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblUndo"));
        setAcceleratorFromPref(menuItem, FPref.SHORTCUT_UNDO);
        menuItem.addActionListener(e -> matchUI.getGameController().undoLastAction());
        return menuItem;
    }

    private SkinnedMenuItem getMenuItem_Concede() {
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(matchUI.getConcedeCaption());
        setAcceleratorFromPref(menuItem, FPref.SHORTCUT_CONCEDE);
        menuItem.addActionListener(e -> matchUI.concede());
        return menuItem;
    }

    private SkinnedMenuItem getMenuItem_EndTurn() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblEndTurn"));
        setAcceleratorFromPref(menuItem, FPref.SHORTCUT_ENDTURN);
        menuItem.addActionListener(e -> matchUI.getGameController().passPriorityUntilEndOfTurn());
        return menuItem;
    }

    private SkinnedMenuItem getMenuItem_AlphaStrike() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblAlphaStrike"));
        setAcceleratorFromPref(menuItem, FPref.SHORTCUT_ALPHASTRIKE);
        menuItem.addActionListener(e -> matchUI.getGameController().alphaStrike());
        return menuItem;
    }

    private SkinnedMenuItem getMenuItem_ViewDeckList() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblDeckList"));
        menuItem.addActionListener(e -> matchUI.viewDeckList());
        return menuItem;
    }

    private SkinnedMenuItem getMenuItem_AutoYieldsAndTriggers() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblAutoYieldsAndTriggers"));
        menuItem.addActionListener(e -> new VAutoYieldsAndTriggers(matchUI).showDialog());
        return menuItem;
    }

    /** Sets a menu item's accelerator display from a shortcut preference. */
    private static void setAcceleratorFromPref(final SkinnedMenuItem menuItem, final FPref pref) {
        final KeyStroke ks = KeyboardShortcuts.getKeyStrokeForPref(pref);
        if (ks != null) {
            menuItem.setAccelerator(ks);
        }
    }
}
