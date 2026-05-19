package forge.screens.match.menus;

import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import forge.control.KeyboardShortcuts;
import forge.gamemodes.match.YieldController;
import forge.gamemodes.match.YieldUpdate;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.match.CMatchUI;
import forge.screens.match.VAutoYieldsAndTriggers;
import forge.screens.match.VYieldSettings;
import forge.toolbox.FSkin.SkinnedCheckBoxMenuItem;
import forge.toolbox.FSkin.SkinnedMenuItem;
import forge.util.Localizer;

/**
 * Returns a JMenu containing core in-game actions for the current match.
 */
public final class GameMenu {
    private static final ForgePreferences prefs = FModel.getPreferences();
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
        menu.add(getMenuItem_YieldSettings());
        final SkinnedCheckBoxMenuItem autoPassItem = getMenuItem_AutoPass();
        menu.add(autoPassItem);
        menu.add(getMenuItem_ClearRememberedAbilityOrders());
        menu.addSeparator();
        menu.addMenuListener(new MenuListener() {
            @Override public void menuSelected(final MenuEvent e) {
                autoPassItem.setState(prefs.getPrefBoolean(FPref.YIELD_AUTO_PASS_NO_ACTIONS));
            }
            @Override public void menuDeselected(final MenuEvent e) {}
            @Override public void menuCanceled(final MenuEvent e) {}
        });
        return menu;
    }

    private SkinnedMenuItem getMenuItem_ClearRememberedAbilityOrders() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblResetSavedAbilityOrders"));
        menuItem.addActionListener(e -> matchUI.getGameController().sendYieldUpdate(new YieldUpdate.ClearAbilityOrders()));
        return menuItem;
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
        menuItem.addActionListener(e -> YieldController.endTurn(matchUI.getGameController(), matchUI.getCurrentPlayer()));
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

    private SkinnedMenuItem getMenuItem_YieldSettings() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblYieldSettings"));
        menuItem.addActionListener(e -> new VYieldSettings(matchUI).showDialog());
        return menuItem;
    }

    private SkinnedCheckBoxMenuItem getMenuItem_AutoPass() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedCheckBoxMenuItem menuItem = new SkinnedCheckBoxMenuItem(localizer.getMessage("lblEnableAutoPass"));
        setAcceleratorFromPref(menuItem, FPref.SHORTCUT_YIELD_AUTO_PASS);
        menuItem.setState(prefs.getPrefBoolean(FPref.YIELD_AUTO_PASS_NO_ACTIONS));
        menuItem.addActionListener(e -> {
            YieldController.toggleAutoPassNoActions(matchUI.getGameController());
            matchUI.getCDock().update();
            menuItem.setState(prefs.getPrefBoolean(FPref.YIELD_AUTO_PASS_NO_ACTIONS));
        });
        return menuItem;
    }

    /** Sets a menu item's accelerator display from a shortcut preference. */
    private static void setAcceleratorFromPref(final JMenuItem menuItem, final FPref pref) {
        final KeyStroke ks = KeyboardShortcuts.getKeyStrokeForPref(pref);
        if (ks != null) {
            menuItem.setAccelerator(ks);
        }
    }
}
