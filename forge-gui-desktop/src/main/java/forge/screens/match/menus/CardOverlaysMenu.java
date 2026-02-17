package forge.screens.match.menus;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.menus.MenuUtil;
import forge.model.FModel;
import forge.screens.match.CMatchUI;
import forge.util.Localizer;

public final class CardOverlaysMenu {
    private final CMatchUI matchUI;
    public CardOverlaysMenu(final CMatchUI matchUI) {
        this.matchUI = matchUI;
    }

    private static ForgePreferences prefs = FModel.getPreferences();
    private static boolean showOverlays = prefs.getPrefBoolean(FPref.UI_SHOW_CARD_OVERLAYS);

    public JMenu getMenu() {
        JMenu menu = new JMenu(Localizer.getInstance().getMessage("lblCardOverlays"));
        menu.add(getMenuItem_ShowOverlays());
        menu.addSeparator();
        menu.add(getMenuItem_CardOverlay(Localizer.getInstance().getMessage("lblCardName"), FPref.UI_OVERLAY_CARD_NAME));
        menu.add(getMenuItem_CardOverlay(Localizer.getInstance().getMessage("lblManaCost"), FPref.UI_OVERLAY_CARD_MANA_COST));
        menu.add(getMenuItem_CardOverlay(Localizer.getInstance().getMessage("lblPerpetualManaCost"), FPref.UI_OVERLAY_CARD_PERPETUAL_MANA_COST));
        menu.add(getMenuItem_CardOverlay(Localizer.getInstance().getMessage("lblPowerOrToughness"), FPref.UI_OVERLAY_CARD_POWER));
        menu.add(getMenuItem_CardOverlay(Localizer.getInstance().getMessage("lblCardID"), FPref.UI_OVERLAY_CARD_ID));
        menu.add(getMenuItem_CardOverlay(Localizer.getInstance().getMessage("lblAbilityIcon"), FPref.UI_OVERLAY_ABILITY_ICONS));
        return menu;
    }

    private JMenuItem getMenuItem_CardOverlay(String menuCaption, FPref pref) {
        JCheckBoxMenuItem menu = new JCheckBoxMenuItem(menuCaption);
        menu.setState(prefs.getPrefBoolean(pref));
        menu.setEnabled(showOverlays);
        menu.addActionListener(getCardOverlaysAction(pref));
        return menu;
    }

    private JMenuItem getMenuItem_ShowOverlays() {
        JCheckBoxMenuItem menu = new JCheckBoxMenuItem(Localizer.getInstance().getMessage("lblShow"));
        menu.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_O));
        menu.setState(prefs.getPrefBoolean(FPref.UI_SHOW_CARD_OVERLAYS));
        menu.addActionListener(getShowOverlaysAction());
        return menu;
    }

    private ActionListener getShowOverlaysAction() {
        return e -> toggleCardOverlayDisplay((JMenuItem)e.getSource());
    }

    private void toggleCardOverlayDisplay(JMenuItem showMenu) {
        toggleShowOverlaySetting();
        repaintCardOverlays();
        // Enable/disable overlay menu items based on state of "Show" menu.
        for (Component c : showMenu.getParent().getComponents()) {
            if (c instanceof JMenuItem) {
                JMenuItem m = (JMenuItem)c;
                if (m != showMenu) {
                    m.setEnabled(prefs.getPrefBoolean(FPref.UI_SHOW_CARD_OVERLAYS));
                }
            }
        }
    }

    private static void toggleShowOverlaySetting() {
        boolean isOverlayEnabled = !prefs.getPrefBoolean(FPref.UI_SHOW_CARD_OVERLAYS);
        prefs.setPref(FPref.UI_SHOW_CARD_OVERLAYS, isOverlayEnabled);
        prefs.save();
    }

    private ActionListener getCardOverlaysAction(final FPref overlaySetting) {
        return e -> {
            toggleOverlaySetting(overlaySetting);
            repaintCardOverlays();
        };
    }

    private static void toggleOverlaySetting(FPref overlaySetting) {
        boolean isOverlayEnabled = !prefs.getPrefBoolean(overlaySetting);
        prefs.setPref(overlaySetting, isOverlayEnabled);
        prefs.save();
    }

    private void repaintCardOverlays() {
        SwingUtilities.invokeLater(matchUI::repaintCardOverlays);
    }
}
