package forge.screens.match.menus;

import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.SwingUtilities;

import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.menus.MenuUtil;
import forge.model.FModel;
import forge.screens.match.CMatchUI;
import forge.screens.match.controllers.CDock.ArcState;
import forge.screens.match.views.VField;
import forge.toolbox.FSkin.SkinnedCheckBoxMenuItem;
import forge.toolbox.FSkin.SkinnedMenu;
import forge.toolbox.FSkin.SkinnedRadioButtonMenuItem;
import forge.util.Localizer;

/**
 * Returns a JMenu containing options that affect how the match is displayed.
 */
public final class DisplayMenu {
    private final CMatchUI matchUI;
    public DisplayMenu(final CMatchUI matchUI) {
        this.matchUI = matchUI;
    }

    private static final ForgePreferences prefs = FModel.getPreferences();

    public JMenu getMenu() {
        final Localizer localizer = Localizer.getInstance();
        final JMenu menu = new JMenu(localizer.getMessage("lblDisplay"));
        menu.add(getMenuItem_TargetingArcs());
        menu.add(new CardOverlaysMenu(matchUI).getMenu());
        menu.add(getSubmenu_StackGroupPermanents());
        menu.add(getMenuItem_TokensSeparateRow());
        menu.add(getMenuItem_SeparateCombatStacks());
        return menu;
    }

    private SkinnedMenu getMenuItem_TargetingArcs() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenu menu = new SkinnedMenu(localizer.getMessage("lblTargetingArcs"));
        final ButtonGroup group = new ButtonGroup();

        addArcRadio(menu, group, localizer.getMessage("lblOff"), ArcState.OFF);
        addArcRadio(menu, group, localizer.getMessage("lblCardMouseOver"), ArcState.MOUSEOVER);
        addArcRadio(menu, group, localizer.getMessage("lblAlwaysOn"), ArcState.ON);
        return menu;
    }

    private void addArcRadio(final JMenu menu, final ButtonGroup group, final String caption, final ArcState arcState) {
        final SkinnedRadioButtonMenuItem item = MenuUtil.createStayOpenSkinnedRadioButton(caption);
        item.setSelected(arcState == matchUI.getCDock().getArcState());
        item.addActionListener(e -> {
            prefs.setPref(FPref.UI_TARGETING_OVERLAY, String.valueOf(arcState.ordinal()));
            prefs.save();
            matchUI.getCDock().update();
        });
        group.add(item);
        menu.add(item);
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
            final SkinnedRadioButtonMenuItem item = MenuUtil.createStayOpenSkinnedRadioButton(localizer.getMessage(labelKeys[i]));
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
        final SkinnedCheckBoxMenuItem menuItem = MenuUtil.createStayOpenSkinnedCheckBox(localizer.getMessage("cbpTokensSeparateRow"));
        menuItem.setToolTipText(localizer.getMessage("nlTokensSeparateRow"));
        menuItem.setState(prefs.getPrefBoolean(FPref.UI_TOKENS_IN_SEPARATE_ROW));
        menuItem.addActionListener(e -> {
            final boolean enabled = !prefs.getPrefBoolean(FPref.UI_TOKENS_IN_SEPARATE_ROW);
            prefs.setPref(FPref.UI_TOKENS_IN_SEPARATE_ROW, enabled);
            prefs.save();
            relayoutFields();
        });
        return menuItem;
    }

    private SkinnedCheckBoxMenuItem getMenuItem_SeparateCombatStacks() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedCheckBoxMenuItem menuItem = MenuUtil.createStayOpenSkinnedCheckBox(localizer.getMessage("cbSeparateCombatStacks"));
        menuItem.setToolTipText(localizer.getMessage("nlSeparateCombatStacks"));
        menuItem.setState(prefs.getPrefBoolean(FPref.UI_SEPARATE_COMBAT_STACKS));
        menuItem.addActionListener(e -> {
            final boolean enabled = !prefs.getPrefBoolean(FPref.UI_SEPARATE_COMBAT_STACKS);
            prefs.setPref(FPref.UI_SEPARATE_COMBAT_STACKS, enabled);
            prefs.save();
            relayoutFields();
        });
        return menuItem;
    }

    private ActionListener getGroupPermanentsAction(final String value) {
        return e -> {
            prefs.setPref(FPref.UI_GROUP_PERMANENTS, value);
            prefs.save();
            relayoutFields();
        };
    }

    private void relayoutFields() {
        SwingUtilities.invokeLater(() -> {
            for (final VField f : matchUI.getFieldViews()) {
                f.getTabletop().doLayout();
            }
        });
    }
}
