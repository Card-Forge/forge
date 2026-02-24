package forge.screens.match.menus;

import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.toolbox.FButton;
import forge.toolbox.FCheckBox;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedLabel;
import forge.util.Localizer;
import forge.view.FDialog;
import net.miginfocom.swing.MigLayout;

import java.awt.Color;
import java.awt.Dimension;

@SuppressWarnings("serial")
public class CardOverlaySettingsDialog extends FDialog {

    private static final int PADDING = 10;

    private static final FPref[] FIELD_PREFS = {
        FPref.UI_OVERLAY_CARD_NAME,
        FPref.UI_OVERLAY_CARD_MANA_COST,
        FPref.UI_OVERLAY_CARD_PERPETUAL_MANA_COST,
        FPref.UI_OVERLAY_CARD_POWER,
        FPref.UI_OVERLAY_CARD_ID,
        FPref.UI_OVERLAY_ABILITY_ICONS,
    };

    private static final FPref[] HOVER_PREFS = {
        FPref.UI_HOVER_OVERLAY_CARD_NAME,
        FPref.UI_HOVER_OVERLAY_CARD_MANA_COST,
        FPref.UI_HOVER_OVERLAY_CARD_PERPETUAL_MANA_COST,
        FPref.UI_HOVER_OVERLAY_CARD_POWER,
        FPref.UI_HOVER_OVERLAY_CARD_ID,
        FPref.UI_HOVER_OVERLAY_ABILITY_ICONS,
    };

    private static final String[] LABEL_KEYS = {
        "lblCardName",
        "lblManaCost",
        "lblPerpetualManaCost",
        "lblPowerOrToughness",
        "lblCardID",
        "lblAbilityIcon",
    };

    public CardOverlaySettingsDialog(final Runnable onFieldChange) {
        super();
        final Localizer localizer = Localizer.getInstance();
        setTitle(localizer.getMessage("lblCardOverlaySettings").replace("...", ""));

        final ForgePreferences prefs = FModel.getPreferences();
        final Color textColor = FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor();
        final Color sepColor = new Color(textColor.getRed(), textColor.getGreen(),
                textColor.getBlue(), 60);

        // 4 columns: label | field-cb | separator | hover-cb
        final JPanel content = new JPanel(new MigLayout(
                "insets " + PADDING + ", gapy 6",
                "[grow, left]16[90!, center]4[1!]4[90!, center]",
                "[]"));
        content.setOpaque(false);

        // Column headers — use HTML for wrapping in fixed-width columns
        final String headerHtml = "<html><div style='text-align:center'>%s</div></html>";
        content.add(new SkinnedLabel(""));  // empty label column
        final SkinnedLabel fieldHeader = new SkinnedLabel(
                String.format(headerHtml, localizer.getMessage("lblCardOverlaysColumn")));
        fieldHeader.setForeground(textColor);
        fieldHeader.setFont(FSkin.getBoldFont(12));
        content.add(fieldHeader, "center");
        content.add(new SkinnedLabel(""));  // separator column placeholder
        final SkinnedLabel hoverHeader = new SkinnedLabel(
                String.format(headerHtml, localizer.getMessage("lblHoverTooltipOverlaysColumn")));
        hoverHeader.setForeground(textColor);
        hoverHeader.setFont(FSkin.getBoldFont(12));
        content.add(hoverHeader, "center, wrap");

        // Vertical separator spans all rows
        final JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setForeground(sepColor);

        // Rows
        for (int i = 0; i < LABEL_KEYS.length; i++) {
            final FPref fieldPref = FIELD_PREFS[i];
            final FPref hoverPref = HOVER_PREFS[i];

            final SkinnedLabel rowLabel = new SkinnedLabel(
                    localizer.getMessage(LABEL_KEYS[i]));
            rowLabel.setForeground(textColor);
            rowLabel.setFont(FSkin.getFont(14));
            content.add(rowLabel);

            final FCheckBox fieldCb = new FCheckBox();
            fieldCb.setSelected(prefs.getPrefBoolean(fieldPref));
            fieldCb.addActionListener(e -> {
                prefs.setPref(fieldPref, fieldCb.isSelected());
                prefs.save();
                if (onFieldChange != null) {
                    onFieldChange.run();
                }
            });
            content.add(fieldCb, "center");

            // Vertical line in separator column
            final JPanel line = new JPanel();
            line.setOpaque(true);
            line.setBackground(sepColor);
            content.add(line, "w 1!, growy");

            final FCheckBox hoverCb = new FCheckBox();
            hoverCb.setSelected(prefs.getPrefBoolean(hoverPref));
            hoverCb.addActionListener(e -> {
                prefs.setPref(hoverPref, hoverCb.isSelected());
                prefs.save();
            });
            content.add(hoverCb, "center, wrap");
        }

        // Close button
        final FButton btnClose = new FButton(localizer.getMessage("lblClose"));
        btnClose.setCommand(() -> setVisible(false));
        content.add(btnClose, "span, center, gaptop 10, gapbottom 6, w 140!, h 28!");

        // Size to fit content snugly
        final Dimension pref = content.getPreferredSize();
        add(content, PADDING, PADDING, pref.width, pref.height);
        this.pack();
        this.setSize(pref.width + 2 * PADDING, pref.height + 2 * PADDING + getTitleBar().getHeight());
    }

    public static void show(final Runnable onFieldChange) {
        final CardOverlaySettingsDialog dialog = new CardOverlaySettingsDialog(onFieldChange);
        dialog.setVisible(true);
        dialog.dispose();
    }
}
