package forge.screens.match.menus;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;

import com.google.common.primitives.Ints;

import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.menus.MenuUtil;
import forge.model.FModel;
import forge.screens.match.CMatchUI;
import forge.screens.match.controllers.CDock.ArcState;
import forge.screens.match.views.VField;
import forge.util.Localizer;

/**
 * Returns a JMenu containing display settings (targeting arcs, card overlays).
 * Extracted from GameMenu to separate gameplay actions from display options.
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
        menu.setMnemonic(KeyEvent.VK_I);

        menu.add(getMenu_TargetingArcs(localizer));
        menu.add(getMenu_CardOverlays(localizer));
        menu.add(getCounterDisplayTypeSubmenu(localizer));
        menu.addSeparator();
        addBattlefieldPrefCheckBox(menu, localizer.getMessage("cbStackCreatures"), FPref.UI_STACK_CREATURES)
                .setToolTipText(localizer.getMessage("nlStackCreatures"));
        addBattlefieldPrefCheckBox(menu, localizer.getMessage("cbTokensInSeparateRow"), FPref.UI_TOKENS_IN_SEPARATE_ROW)
                .setToolTipText(localizer.getMessage("nlTokensInSeparateRow"));

        return menu;
    }

    private JMenu getMenu_TargetingArcs(final Localizer localizer) {
        final JMenu menu = new JMenu(localizer.getMessage("lblTargetingArcs"));

        if (matchUI.getCDock().getArcState() == null) {
            final String arcStateStr = prefs.getPref(FPref.UI_TARGETING_OVERLAY);
            final Integer arcState = Ints.tryParse(arcStateStr);
            matchUI.getCDock().setArcState(ArcState.values()[arcState == null ? 0 : arcState]);
        }

        final ButtonGroup arcGroup = new ButtonGroup();
        for (final ArcState state : new ArcState[]{ArcState.OFF, ArcState.MOUSEOVER, ArcState.ON}) {
            final String label;
            switch (state) {
                case OFF:       label = localizer.getMessage("lblOff"); break;
                case MOUSEOVER: label = localizer.getMessage("lblCardMouseOver"); break;
                case ON:        label = localizer.getMessage("lblAlwaysOn"); break;
                default:        label = state.name(); break;
            }
            final JRadioButtonMenuItem item = MenuUtil.createStayOpenRadioButton(label);
            item.setSelected(state == matchUI.getCDock().getArcState());
            item.addActionListener(e -> {
                prefs.setPref(FPref.UI_TARGETING_OVERLAY, String.valueOf(state.ordinal()));
                prefs.save();
                matchUI.getCDock().setArcState(state);
            });
            arcGroup.add(item);
            menu.add(item);
        }
        return menu;
    }

    private JMenu getMenu_CardOverlays(final Localizer localizer) {
        final JMenu menu = new JMenu(localizer.getMessage("lblCardOverlays"));

        final boolean showOverlays = prefs.getPrefBoolean(FPref.UI_SHOW_CARD_OVERLAYS);
        final List<JCheckBoxMenuItem> overlayItems = new ArrayList<>();

        final JCheckBoxMenuItem showItem = MenuUtil.createStayOpenCheckBox(
                localizer.getMessage("lblShowCardOverlays"));
        showItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_O));
        showItem.setState(showOverlays);
        showItem.addActionListener(e -> {
            final boolean on = showItem.getState();
            prefs.setPref(FPref.UI_SHOW_CARD_OVERLAYS, on);
            prefs.save();
            for (JCheckBoxMenuItem oi : overlayItems) {
                oi.setEnabled(on);
            }
            SwingUtilities.invokeLater(matchUI::repaintCardOverlays);
        });
        menu.add(showItem);

        addOverlayItem(menu, overlayItems, localizer.getMessage("lblCardName"), FPref.UI_OVERLAY_CARD_NAME, showOverlays);
        addOverlayItem(menu, overlayItems, localizer.getMessage("lblManaCost"), FPref.UI_OVERLAY_CARD_MANA_COST, showOverlays);
        addOverlayItem(menu, overlayItems, localizer.getMessage("lblPerpetualManaCost"), FPref.UI_OVERLAY_CARD_PERPETUAL_MANA_COST, showOverlays);
        addOverlayItem(menu, overlayItems, localizer.getMessage("lblPowerOrToughness"), FPref.UI_OVERLAY_CARD_POWER, showOverlays);
        addOverlayItem(menu, overlayItems, localizer.getMessage("lblCardID"), FPref.UI_OVERLAY_CARD_ID, showOverlays);
        addOverlayItem(menu, overlayItems, localizer.getMessage("lblAbilityIcon"), FPref.UI_OVERLAY_ABILITY_ICONS, showOverlays);
        addOverlayItem(menu, overlayItems, localizer.getMessage("cbDisplayFoil"), FPref.UI_OVERLAY_FOIL_EFFECT, showOverlays);

        return menu;
    }

    private JCheckBoxMenuItem addBattlefieldPrefCheckBox(final JMenu menu, final String label, final FPref pref) {
        final JCheckBoxMenuItem item = MenuUtil.createStayOpenCheckBox(label);
        item.setState(prefs.getPrefBoolean(pref));
        item.addActionListener(e -> {
            prefs.setPref(pref, !prefs.getPrefBoolean(pref));
            prefs.save();
            SwingUtilities.invokeLater(() -> {
                for (final VField f : matchUI.getFieldViews()) {
                    f.getTabletop().doLayout();
                }
            });
        });
        menu.add(item);
        return item;
    }

    private void addOverlayItem(final JMenu menu, final List<JCheckBoxMenuItem> overlayItems,
                                final String label, final FPref pref, final boolean enabled) {
        final JCheckBoxMenuItem item = MenuUtil.createStayOpenCheckBox(label);
        item.setState(prefs.getPrefBoolean(pref));
        item.setEnabled(enabled);
        item.addActionListener(e -> {
            prefs.setPref(pref, !prefs.getPrefBoolean(pref));
            prefs.save();
            SwingUtilities.invokeLater(matchUI::repaintCardOverlays);
        });
        overlayItems.add(item);
        menu.add(item);
    }

    private JMenu getCounterDisplayTypeSubmenu(final Localizer localizer) {
        final JMenu submenu = new JMenu(localizer.getMessage("cbpCounterDisplayType"));
        final ButtonGroup group = new ButtonGroup();
        final String current = prefs.getPref(FPref.UI_CARD_COUNTER_DISPLAY_TYPE);

        for (final ForgeConstants.CounterDisplayType type : ForgeConstants.CounterDisplayType.values()) {
            final JRadioButtonMenuItem item = MenuUtil.createStayOpenRadioButton(type.getName());
            item.setSelected(type.getName().equals(current));
            item.addActionListener(e -> {
                prefs.setPref(FPref.UI_CARD_COUNTER_DISPLAY_TYPE, type.getName());
                prefs.save();
                SwingUtilities.invokeLater(matchUI::repaintCardOverlays);
            });
            group.add(item);
            submenu.add(item);
        }
        return submenu;
    }
}
