package forge.gui.match.menus;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import forge.Singletons;
import forge.gui.match.CMatchUI;
import forge.gui.menubar.MenuUtil;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;

public final class CardOverlaysMenu {
    private CardOverlaysMenu() { }

    private static ForgePreferences prefs = Singletons.getModel().getPreferences();
    private static boolean showOverlays = prefs.getPrefBoolean(FPref.UI_SHOW_CARD_OVERLAYS);

    public static JMenu getMenu(boolean showMenuIcons) {
        JMenu menu = new JMenu("Card Overlays");
        menu.add(getMenuItem_ShowOverlays());
        menu.addSeparator();
        menu.add(getMenuItem_CardOverlay("Card Name", FPref.UI_OVERLAY_CARD_NAME));
        menu.add(getMenuItem_CardOverlay("Mana Cost", FPref.UI_OVERLAY_CARD_MANA_COST));
        menu.add(getMenuItem_CardOverlay("Power/Toughness", FPref.UI_OVERLAY_CARD_POWER));
        menu.add(getMenuItem_CardOverlay("Card Id", FPref.UI_OVERLAY_CARD_ID));
        return menu;
    }

    private static JMenuItem getMenuItem_CardOverlay(String menuCaption, FPref pref) {
        JCheckBoxMenuItem menu = new JCheckBoxMenuItem(menuCaption);
        menu.setState(prefs.getPrefBoolean(pref));
        menu.setEnabled(showOverlays);
        menu.addActionListener(getCardOverlaysAction(pref));
        return menu;
    }

    private static JMenuItem getMenuItem_ShowOverlays() {
        JCheckBoxMenuItem menu = new JCheckBoxMenuItem("Show");
        menu.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_O));
        menu.setState(prefs.getPrefBoolean(FPref.UI_SHOW_CARD_OVERLAYS));
        menu.addActionListener(getShowOverlaysAction());
        return menu;
    }

    private static ActionListener getShowOverlaysAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleCardOverlayDisplay((JMenuItem)e.getSource());
            }
        };
    }

    private static void toggleCardOverlayDisplay(JMenuItem showMenu) {
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

    private static ActionListener getCardOverlaysAction(final FPref overlaySetting) {
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

    private static void repaintCardOverlays() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                CMatchUI.SINGLETON_INSTANCE.repaintCardOverlays();
            }
        });
    }
}
