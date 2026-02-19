package forge.screens.match.menus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedLabel;
import forge.toolbox.FSkin.SkinnedSlider;
import forge.util.Localizer;

/**
 * Submenu under the Game menu for toggling card info popup sections
 * (keyword explanations and related cards) during a match.
 */
public final class CardInfoPopupMenu {
    private static final ForgePreferences prefs = FModel.getPreferences();

    public CardInfoPopupMenu() {
    }

    public JMenu getMenu() {
        final Localizer localizer = Localizer.getInstance();
        final JMenu menu = new JMenu(localizer.getMessage("lblCardInfoPopups"));

        // --- Hover Tooltip section ---
        menu.add(createSectionHeader(localizer.getMessage("lblHoverTooltip")));
        menu.add(getCheckboxItem(localizer.getMessage("lblCardImage"),
                FPref.UI_POPUP_CARD_IMAGE));
        menu.add(getCheckboxItem(localizer.getMessage("lblRelatedCards"),
                FPref.UI_POPUP_RELATED_CARDS));
        menu.add(getCheckboxItem(localizer.getMessage("lblKeywordExplanations"),
                FPref.UI_POPUP_KEYWORD_INFO));

        menu.add(new JSeparator());

        // --- Card Zoom View section ---
        menu.add(createSectionHeader(localizer.getMessage("lblCardZoomView")));
        menu.add(getCheckboxItem(localizer.getMessage("lblRelatedCards"),
                FPref.UI_ZOOM_RELATED_CARDS));
        menu.add(getCheckboxItem(localizer.getMessage("lblKeywordsExplained"),
                FPref.UI_ZOOM_KEYWORD_INFO));

        menu.add(new JSeparator());
        menu.add(buildImageSizePanel(localizer));
        return menu;
    }

    private static JMenuItem createSectionHeader(final String text) {
        final JMenuItem header = new JMenuItem(text);
        header.setEnabled(false);
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        return header;
    }

    private static JCheckBoxMenuItem getCheckboxItem(final String label, final FPref pref) {
        final JCheckBoxMenuItem item = new JCheckBoxMenuItem(label) {
            @Override
            protected void processMouseEvent(final MouseEvent e) {
                if (e.getID() == MouseEvent.MOUSE_RELEASED && contains(e.getPoint())) {
                    doClick(0);
                    setArmed(true);
                } else {
                    super.processMouseEvent(e);
                }
            }
        };
        item.setState(prefs.getPrefBoolean(pref));
        item.addActionListener(e -> {
            final boolean newState = !prefs.getPrefBoolean(pref);
            prefs.setPref(pref, newState);
            prefs.save();
        });
        return item;
    }

    private static JPanel buildImageSizePanel(final Localizer localizer) {
        final Color bg = FSkin.getColor(FSkin.Colors.CLR_THEME2).getColor();
        final Color fg = FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor();
        final int rawValue = prefs.getPrefInt(FPref.UI_POPUP_IMAGE_SIZE);
        final int currentValue = Math.max(100, Math.min(500, rawValue));

        final JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(bg);

        final SkinnedLabel label = new SkinnedLabel();
        label.setText(localizer.getMessage("lblImageSize"));
        label.setForeground(fg);
        label.setFont(FSkin.getFont());
        label.setOpaque(true);
        label.setBackground(bg);

        final SkinnedSlider slider = new SkinnedSlider(SwingConstants.HORIZONTAL, 100, 500, currentValue);
        slider.setMajorTickSpacing(100);
        slider.setMinorTickSpacing(20);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setBackground(bg);
        slider.setForeground(fg);
        slider.setFont(FSkin.getFont());

        slider.addChangeListener(e -> {
            slider.repaint();
            prefs.setPref(FPref.UI_POPUP_IMAGE_SIZE, String.valueOf(slider.getValue()));
            prefs.save();
        });

        panel.add(label, BorderLayout.NORTH);
        panel.add(slider, BorderLayout.CENTER);
        return panel;
    }
}
