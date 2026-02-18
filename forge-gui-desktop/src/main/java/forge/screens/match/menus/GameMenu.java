package forge.screens.match.menus;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;

import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.menus.MenuUtil;
import forge.model.FModel;
import forge.screens.match.CMatchUI;
import forge.screens.match.VAutoYields;
import forge.toolbox.FSkin.SkinnedMenuItem;
import forge.util.Localizer;

/**
 * Returns a JMenu containing options associated with current game.
 * <p>
 * Replicates options available in Dock tab.
 */
public final class GameMenu {
    private final CMatchUI matchUI;
    public GameMenu(final CMatchUI matchUI) {
        this.matchUI = matchUI;
    }

    private static final boolean showIcons = false;
    private static final ForgePreferences prefs = FModel.getPreferences();

    public JMenu getMenu() {
        final Localizer localizer = Localizer.getInstance();
        final JMenu menu = new JMenu(localizer.getMessage("lblGame"));
        menu.setMnemonic(KeyEvent.VK_G);
        menu.add(getMenuItem_ViewDeckList());
        menu.addSeparator();
        menu.add(getMenuItem_Undo());
        menu.add(getMenuItem_Concede());
        menu.add(getMenuItem_EndTurn());
        menu.add(getMenuItem_AlphaStrike());
        menu.addSeparator();
        menu.add(getMenuItem_AutoYields());
        menu.add(getAutoYieldModeSubmenu(localizer));
        menu.addSeparator();
        MenuUtil.addPrefCheckBox(menu, localizer.getMessage("cbRemindOnPriority"), FPref.UI_REMIND_ON_PRIORITY);
        MenuUtil.addPrefCheckBox(menu, localizer.getMessage("cbEscapeEndsTurn"), FPref.UI_ALLOW_ESC_TO_END_TURN);
        MenuUtil.addPrefCheckBox(menu, localizer.getMessage("lblWarnBeforeEmptyingManaPool"), FPref.UI_MANA_LOST_PROMPT);
        return menu;
    }

    private SkinnedMenuItem getMenuItem_Undo() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblUndo"));
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_Z));
        menuItem.addActionListener(getUndoAction());
        return menuItem;
    }

    private ActionListener getUndoAction() {
        return e -> matchUI.getGameController().undoLastAction();
    }

    private SkinnedMenuItem getMenuItem_Concede() {
        SkinnedMenuItem menuItem = new SkinnedMenuItem(matchUI.getConcedeCaption());
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_CONCEDE) : null));
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_Q));
        menuItem.addActionListener(getConcedeAction());
        return menuItem;
    }

    private ActionListener getConcedeAction() {
        return e -> matchUI.concede();
    }

    private SkinnedMenuItem getMenuItem_AlphaStrike() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblAlphaStrike"));
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_ALPHASTRIKE) : null));
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_A));
        menuItem.addActionListener(getAlphaStrikeAction());
        return menuItem;
    }

    private ActionListener getAlphaStrikeAction() {
        return e -> matchUI.getGameController().alphaStrike();
    }

    private SkinnedMenuItem getMenuItem_EndTurn() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblEndTurn"));
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_ENDTURN) : null));
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_E));
        menuItem.addActionListener(getEndTurnAction());
        return menuItem;
    }

    private ActionListener getEndTurnAction() {
        return e -> matchUI.getGameController().passPriorityUntilEndOfTurn();
    }

    private SkinnedMenuItem getMenuItem_AutoYields() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblAutoYields") + "...");
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_WARNING) : null));
        menuItem.setToolTipText(localizer.getMessage("nlAutoYields"));
        menuItem.addActionListener(getAutoYieldsAction());
        return menuItem;
    }

    private ActionListener getAutoYieldsAction() {
        return e -> {
            final VAutoYields autoYields = new VAutoYields(matchUI);
            autoYields.showAutoYields();
        };
    }

    private SkinnedMenuItem getMenuItem_ViewDeckList() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblViewDeckList"));
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_DECKLIST) : null));
        menuItem.addActionListener(getViewDeckListAction());
        return menuItem;
    }

    private ActionListener getViewDeckListAction() {
        return e -> matchUI.viewDeckList();
    }

    private static JMenu getAutoYieldModeSubmenu(final Localizer localizer) {
        final JMenu submenu = new JMenu(localizer.getMessage("lblAutoYieldMode"));
        submenu.setToolTipText(localizer.getMessage("nlpAutoYieldMode"));
        final ButtonGroup group = new ButtonGroup();
        final String current = prefs.getPref(FPref.UI_AUTO_YIELD_MODE);

        final String[] values = {
            ForgeConstants.AUTO_YIELD_PER_ABILITY,
            ForgeConstants.AUTO_YIELD_PER_CARD
        };
        for (final String value : values) {
            final JRadioButtonMenuItem item = MenuUtil.createStayOpenRadioButton(value);
            item.setSelected(value.equals(current));
            item.addActionListener(e -> {
                prefs.setPref(FPref.UI_AUTO_YIELD_MODE, value);
                prefs.save();
            });
            group.add(item);
            submenu.add(item);
        }
        return submenu;
    }
}
