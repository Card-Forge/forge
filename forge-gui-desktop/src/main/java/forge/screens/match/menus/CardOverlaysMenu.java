package forge.screens.match.menus;

import forge.menus.MenuUtil;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.match.CMatchUI;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public final class CardOverlaysMenu {
    private final CMatchUI matchUI;
    public CardOverlaysMenu(final CMatchUI matchUI) {
        this.matchUI = matchUI;
    }

    private static ForgePreferences prefs = FModel.getPreferences();
    private static boolean showOverlays = prefs.getPrefBoolean(FPref.UI_SHOW_CARD_OVERLAYS);

    public JMenu getMenu() {
        JMenu menu = new JMenu("Card Overlays");
        menu.add(getMenuItem_ShowOverlays());
        menu.addSeparator();
        menu.add(getMenuItem_CardOverlay("Card Name", FPref.UI_OVERLAY_CARD_NAME));
        menu.add(getMenuItem_CardOverlay("Mana Cost", FPref.UI_OVERLAY_CARD_MANA_COST));
        menu.add(getMenuItem_CardOverlay("Power/Toughness", FPref.UI_OVERLAY_CARD_POWER));
        menu.add(getMenuItem_CardOverlay("Card Id", FPref.UI_OVERLAY_CARD_ID));
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
        JCheckBoxMenuItem menu = new JCheckBoxMenuItem("Show");
        menu.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_O));
        menu.setState(prefs.getPrefBoolean(FPref.UI_SHOW_CARD_OVERLAYS));
        menu.addActionListener(getShowOverlaysAction());
        return menu;
    }

    private ActionListener getShowOverlaysAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleCardOverlayDisplay((JMenuItem)e.getSource());
            }
        };
    }

    private void toggleCardOverlayDisplay(JMenuItem showMenu) {
        toggleShowOverlaySetting();
        repaintCardOverlays();
        // Enable/disable overlay menu items based on state of "Show" menu.
        for (Component c : ((JPopupMenu)showMenu.getParent()).getComponents()) {
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
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleOverlaySetting(overlaySetting);
                repaintCardOverlays();
            }

        };
    }

    private static void toggleOverlaySetting(FPref overlaySetting) {
        boolean isOverlayEnabled = !prefs.getPrefBoolean(overlaySetting);
        prefs.setPref(overlaySetting, isOverlayEnabled);
        prefs.save();
    }

    private void repaintCardOverlays() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                matchUI.repaintCardOverlays();
            }
        });
    }
}
