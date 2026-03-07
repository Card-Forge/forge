package forge.screens.match.menus;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import forge.control.KeyboardShortcuts;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.match.CMatchUI;
import forge.util.Localizer;

public final class CardOverlaysMenu {
    private final CMatchUI matchUI;
    public CardOverlaysMenu(final CMatchUI matchUI) {
        this.matchUI = matchUI;
    }

    private static ForgePreferences prefs = FModel.getPreferences();

    public JMenu getMenu() {
        JMenu menu = new JMenu(Localizer.getInstance().getMessage("lblCardOverlays"));
        menu.add(getMenuItem_ShowOverlays());
        menu.addSeparator();
        JMenuItem settingsItem = new JMenuItem(Localizer.getInstance().getMessage("lblCardOverlaySettings"));
        settingsItem.addActionListener(e ->
                CardOverlaySettingsDialog.show(() ->
                        SwingUtilities.invokeLater(matchUI::repaintCardOverlays)));
        menu.add(settingsItem);
        return menu;
    }

    private JMenuItem getMenuItem_ShowOverlays() {
        JCheckBoxMenuItem menu = new JCheckBoxMenuItem(Localizer.getInstance().getMessage("lblShow"));
        final KeyStroke ks = KeyboardShortcuts.getKeyStrokeForPref(FPref.SHORTCUT_CARDOVERLAYS);
        if (ks != null) { menu.setAccelerator(ks); }
        menu.setState(prefs.getPrefBoolean(FPref.UI_SHOW_CARD_OVERLAYS));
        menu.addActionListener(e -> {
            boolean isOverlayEnabled = !prefs.getPrefBoolean(FPref.UI_SHOW_CARD_OVERLAYS);
            prefs.setPref(FPref.UI_SHOW_CARD_OVERLAYS, isOverlayEnabled);
            prefs.save();
            SwingUtilities.invokeLater(matchUI::repaintCardOverlays);
        });
        return menu;
    }
}
