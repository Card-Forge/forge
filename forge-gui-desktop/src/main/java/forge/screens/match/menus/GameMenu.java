package forge.screens.match.menus;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.google.common.primitives.Ints;

import forge.control.KeyboardShortcuts;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.menus.MenuUtil;
import forge.model.FModel;
import forge.screens.match.CMatchUI;
import forge.screens.match.VAutoTriggers;
import forge.screens.match.VAutoYields;
import forge.screens.match.views.VField;
import forge.screens.match.controllers.CDock.ArcState;
import forge.toolbox.FSkin.SkinIcon;
import forge.toolbox.FSkin.SkinnedCheckBoxMenuItem;
import forge.toolbox.FSkin.SkinnedMenu;
import forge.toolbox.FSkin.SkinnedMenuItem;
import forge.toolbox.FSkin.SkinnedRadioButtonMenuItem;
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

    private static ForgePreferences prefs = FModel.getPreferences();
    private static boolean showIcons;

    public JMenu getMenu() {
        final Localizer localizer = Localizer.getInstance();
        final JMenu menu = new JMenu(localizer.getMessage("lblGame"));
        menu.setMnemonic(KeyEvent.VK_G);
        menu.add(getMenuItem_Undo());
        menu.add(getMenuItem_Concede());
        menu.add(getMenuItem_EndTurn());
        menu.add(getMenuItem_AlphaStrike());
        menu.addSeparator();
        menu.add(getMenuItem_TargetingArcs());
        menu.add(new CardOverlaysMenu(matchUI).getMenu());
        menu.add(getSubmenu_StackGroupPermanents());
        menu.add(getMenuItem_TokensSeparateRow());
        menu.add(getMenuItem_SeparateCombatStacks());
        menu.add(getMenuItem_AutoYields());
        menu.add(getMenuItem_AutoTriggers());
        menu.addSeparator();
        menu.add(getMenuItem_ViewDeckList());
        return menu;
    }

    private SkinnedMenuItem getMenuItem_Undo() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblUndo"));
        setAcceleratorFromPref(menuItem, FPref.SHORTCUT_UNDO);
        menuItem.addActionListener(getUndoAction());
        return menuItem;
    }

    private ActionListener getUndoAction() {
        return e -> matchUI.getGameController().undoLastAction();
    }

    private SkinnedMenuItem getMenuItem_Concede() {
        SkinnedMenuItem menuItem = new SkinnedMenuItem(matchUI.getConcedeCaption());
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_CONCEDE) : null));
        setAcceleratorFromPref(menuItem, FPref.SHORTCUT_CONCEDE);
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
        setAcceleratorFromPref(menuItem, FPref.SHORTCUT_ALPHASTRIKE);
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
        setAcceleratorFromPref(menuItem, FPref.SHORTCUT_ENDTURN);
        menuItem.addActionListener(getEndTurnAction());
        return menuItem;
    }

    private ActionListener getEndTurnAction() {
        return e -> matchUI.getGameController().passPriorityUntilEndOfTurn();
    }

    /** Sets a menu item's accelerator display from a shortcut preference. */
    private static void setAcceleratorFromPref(final SkinnedMenuItem menuItem, final FPref pref) {
        final KeyStroke ks = KeyboardShortcuts.getKeyStrokeForPref(pref);
        if (ks != null) {
            menuItem.setAccelerator(ks);
        }
    }

    private SkinnedMenu getMenuItem_TargetingArcs() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenu menu = new SkinnedMenu(localizer.getMessage("lblTargetingArcs"));
        final ButtonGroup group = new ButtonGroup();

        SkinIcon menuIcon = MenuUtil.getMenuIcon(FSkinProp.ICO_ARCSOFF);

        if (matchUI.getCDock().getArcState() == null) {
            final String arcStateStr = FModel.getPreferences().getPref(FPref.UI_TARGETING_OVERLAY);
            final Integer arcState = Ints.tryParse(arcStateStr);
            matchUI.getCDock().setArcState(ArcState.values()[arcState == null ? 0 : arcState]);
        }

        SkinnedRadioButtonMenuItem menuItem;
        menuItem = getTargetingArcRadioButton(localizer.getMessage("lblOff"), FSkinProp.ICO_ARCSOFF, ArcState.OFF);
        if (menuItem.isSelected()) { menuIcon = MenuUtil.getMenuIcon(FSkinProp.ICO_ARCSOFF); }
        group.add(menuItem);
        menu.add(menuItem);
        menuItem = getTargetingArcRadioButton(localizer.getMessage("lblCardMouseOver"), FSkinProp.ICO_ARCSHOVER, ArcState.MOUSEOVER);
        if (menuItem.isSelected()) { menuIcon = MenuUtil.getMenuIcon(FSkinProp.ICO_ARCSHOVER); }
        group.add(menuItem);
        menu.add(menuItem);
        menuItem = getTargetingArcRadioButton(localizer.getMessage("lblAlwaysOn"), FSkinProp.ICO_ARCSON, ArcState.ON);
        if (menuItem.isSelected()) { menuIcon = MenuUtil.getMenuIcon(FSkinProp.ICO_ARCSON); }
        group.add(menuItem);

        menu.setIcon((showIcons ? menuIcon : null));
        menu.add(menuItem);

        return menu;
    }

    private SkinnedRadioButtonMenuItem getTargetingArcRadioButton(final String caption, final FSkinProp icon, final ArcState arcState) {
        final SkinnedRadioButtonMenuItem menuItem = new SkinnedRadioButtonMenuItem(caption);
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(icon) : null));
        menuItem.setSelected(arcState == matchUI.getCDock().getArcState());
        menuItem.addActionListener(getTargetingRadioButtonAction(arcState));
        return menuItem;
    }

    private ActionListener getTargetingRadioButtonAction(final ArcState arcState) {
        return e -> {
            prefs.setPref(FPref.UI_TARGETING_OVERLAY, String.valueOf(arcState.ordinal()));
            prefs.save();
            matchUI.getCDock().setArcState(arcState);
            setTargetingArcMenuIcon((SkinnedRadioButtonMenuItem)e.getSource());
        };
    }

    private static void setTargetingArcMenuIcon(SkinnedRadioButtonMenuItem item) {
        final JPopupMenu pop = (JPopupMenu)item.getParent();
        final JMenu menu = (JMenu)pop.getInvoker();
        menu.setIcon(item.getIcon());
    }

    private SkinnedMenuItem getMenuItem_AutoYields() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblAutoYields"));
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_WARNING) : null));
        menuItem.addActionListener(getAutoYieldsAction());
        return menuItem;
    }

    private ActionListener getAutoYieldsAction() {
        return e -> {
            new VAutoYields(matchUI).showAutoYields();
        };
    }

    private SkinnedMenuItem getMenuItem_AutoTriggers() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblAutoTriggers"));
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_WARNING) : null));
        menuItem.addActionListener(getAutoTriggersAction());
        return menuItem;
    }

    private ActionListener getAutoTriggersAction() {
        return e -> new VAutoTriggers(matchUI).showAutoTriggers();
    }

    private SkinnedMenuItem getMenuItem_ViewDeckList() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblDeckList"));
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_DECKLIST) : null));
        menuItem.addActionListener(getViewDeckListAction());
        return menuItem;
    }

    private ActionListener getViewDeckListAction() {
        return e -> matchUI.viewDeckList();
    }

    private SkinnedMenu getSubmenu_StackGroupPermanents() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenu submenu = new SkinnedMenu(localizer.getMessage("cbpStackGroupPermanents"));
        final ButtonGroup group = new ButtonGroup();
        final String current = prefs.getPref(FPref.UI_GROUP_PERMANENTS);

        final String[] keys = {"default", "stack", "group_creatures", "group_all"};
        final String[] labelKeys = {"lblGroupDefault", "lblGroupStack", "lblGroupCreatures", "lblGroupAll"};
        final String[] tooltipKeys = {"nlGroupDefault", "nlGroupStack", "nlGroupCreatures", "nlGroupAll"};
        for (int i = 0; i < keys.length; i++) {
            SkinnedRadioButtonMenuItem item = new SkinnedRadioButtonMenuItem(localizer.getMessage(labelKeys[i]));
            item.setToolTipText(localizer.getMessage(tooltipKeys[i]));
            item.setSelected(keys[i].equals(current));
            item.addActionListener(getGroupPermanentsAction(keys[i]));
            group.add(item);
            submenu.add(item);
        }
        return submenu;
    }

    private SkinnedCheckBoxMenuItem getMenuItem_TokensSeparateRow() {
        final Localizer localizer = Localizer.getInstance();
        SkinnedCheckBoxMenuItem menuItem = new SkinnedCheckBoxMenuItem(localizer.getMessage("cbpTokensSeparateRow"));
        menuItem.setToolTipText(localizer.getMessage("nlTokensSeparateRow"));
        menuItem.setState(prefs.getPrefBoolean(FPref.UI_TOKENS_IN_SEPARATE_ROW));
        menuItem.addActionListener(e -> {
            final boolean enabled = !prefs.getPrefBoolean(FPref.UI_TOKENS_IN_SEPARATE_ROW);
            prefs.setPref(FPref.UI_TOKENS_IN_SEPARATE_ROW, enabled);
            prefs.save();
            SwingUtilities.invokeLater(() -> {
                for (final VField f : matchUI.getFieldViews()) {
                    f.getTabletop().doLayout();
                }
            });
        });
        return menuItem;
    }

    private SkinnedCheckBoxMenuItem getMenuItem_SeparateCombatStacks() {
        final Localizer localizer = Localizer.getInstance();
        SkinnedCheckBoxMenuItem menuItem = new SkinnedCheckBoxMenuItem(localizer.getMessage("cbSeparateCombatStacks"));
        menuItem.setToolTipText(localizer.getMessage("nlSeparateCombatStacks"));
        menuItem.setState(prefs.getPrefBoolean(FPref.UI_SEPARATE_COMBAT_STACKS));
        menuItem.addActionListener(e -> {
            final boolean enabled = !prefs.getPrefBoolean(FPref.UI_SEPARATE_COMBAT_STACKS);
            prefs.setPref(FPref.UI_SEPARATE_COMBAT_STACKS, enabled);
            prefs.save();
            SwingUtilities.invokeLater(() -> {
                for (final VField f : matchUI.getFieldViews()) {
                    f.getTabletop().doLayout();
                }
            });
        });
        return menuItem;
    }

    private ActionListener getGroupPermanentsAction(final String value) {
        return e -> {
            prefs.setPref(FPref.UI_GROUP_PERMANENTS, value);
            prefs.save();
            SwingUtilities.invokeLater(() -> {
                for (final VField f : matchUI.getFieldViews()) {
                    f.getTabletop().doLayout();
                }
            });
        };
    }
}
