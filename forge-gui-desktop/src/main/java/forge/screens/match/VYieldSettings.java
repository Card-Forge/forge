package forge.screens.match;

import forge.Singletons;
import forge.gui.UiCommand;
import forge.interfaces.IGameController;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.toolbox.FButton;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBox;
import forge.toolbox.FLabel;
import forge.util.Localizer;
import forge.view.FDialog;

import javax.swing.JSeparator;

/**
 * Dialog for configuring yield interrupt conditions and automatic suggestions.
 */
@SuppressWarnings("serial")
public class VYieldSettings extends FDialog {
    private static final int PADDING = 10;
    private static final int ROW_HEIGHT = 24;
    private static final int SECTION_GAP = 12;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 26;
    private static final int DROPDOWN_WIDTH = 120;

    private final CMatchUI matchUI;

    public VYieldSettings(CMatchUI matchUI) {
        super();
        this.matchUI = matchUI;
        final Localizer localizer = Localizer.getInstance();
        final ForgePreferences prefs = FModel.getPreferences();

        setTitle(localizer.getMessage("lblYieldSettings"));

        int width = Math.min(Singletons.getView().getFrame().getWidth() * 2 / 3, 480);
        int w = width - 2 * PADDING;
        int x = PADDING;
        int y = PADDING;

        // --- Interrupt Settings section ---
        FLabel lblInterrupt = new FLabel.Builder().text(localizer.getMessage("lblInterruptSettings"))
                .fontStyle(java.awt.Font.BOLD).fontSize(14).build();
        add(lblInterrupt, x, y, w, ROW_HEIGHT);
        y += ROW_HEIGHT + 2;

        y = addCheckbox(x, y, w, localizer.getMessage("lblInterruptOnAttackers"), FPref.YIELD_INTERRUPT_ON_ATTACKERS, prefs);
        y = addCheckbox(x, y, w, localizer.getMessage("lblInterruptOnTargeting"), FPref.YIELD_INTERRUPT_ON_TARGETING, prefs);
        y = addCheckbox(x, y, w, localizer.getMessage("lblInterruptOnMassRemoval"), FPref.YIELD_INTERRUPT_ON_MASS_REMOVAL, prefs);
        y = addCheckbox(x, y, w, localizer.getMessage("lblInterruptOnOpponentSpell"), FPref.YIELD_INTERRUPT_ON_OPPONENT_SPELL, prefs);
        y = addCheckbox(x, y, w, localizer.getMessage("lblInterruptOnTriggers"), FPref.YIELD_INTERRUPT_ON_TRIGGERS, prefs);
        y = addCheckbox(x, y, w, localizer.getMessage("lblInterruptOnReveal"), FPref.YIELD_INTERRUPT_ON_REVEAL, prefs);

        y += SECTION_GAP;
        JSeparator sep = new JSeparator();
        add(sep, x, y, w, 2);
        y += 2 + SECTION_GAP;

        // --- Automatic Suggestions section ---
        FLabel lblSuggestions = new FLabel.Builder().text(localizer.getMessage("lblAutomaticSuggestions"))
                .fontStyle(java.awt.Font.BOLD).fontSize(14).build();
        add(lblSuggestions, x, y, w, ROW_HEIGHT);
        y += ROW_HEIGHT + 2;

        // Stack yield: label + dropdown (Never / Always / Once per stack / Once per turn)
        y = addLabelWithDropdown(x, y, w,
            localizer.getMessage("lblSuggestStackYield"),
            FPref.YIELD_DECLINE_SCOPE_STACK_YIELD,
            new String[] {
                localizer.getMessage("lblDeclScopeNever"),
                localizer.getMessage("lblDeclScopeAlways"),
                localizer.getMessage("lblDeclScopeStack"),
                localizer.getMessage("lblDeclScopeTurn")
            },
            new String[] { "never", "always", "stack", "turn" },
            prefs);

        // No actions: label + dropdown (Never / Always / Once per turn)
        y = addLabelWithDropdown(x, y, w,
            localizer.getMessage("lblSuggestNoActions"),
            FPref.YIELD_DECLINE_SCOPE_NO_ACTIONS,
            new String[] {
                localizer.getMessage("lblDeclScopeNever"),
                localizer.getMessage("lblDeclScopeAlways"),
                localizer.getMessage("lblDeclScopeTurn")
            },
            new String[] { "never", "always", "turn" },
            prefs);

        y += SECTION_GAP;

        y = addCheckbox(x, y, w, localizer.getMessage("lblSuppressOnOwnTurn"), FPref.YIELD_SUPPRESS_ON_OWN_TURN, prefs);
        y = addCheckbox(x, y, w, localizer.getMessage("lblSuppressAfterYield"), FPref.YIELD_SUPPRESS_AFTER_END, prefs);

        y += SECTION_GAP;

        // OK button
        FButton btnOk = new FButton(localizer.getMessage("lblOK"));
        btnOk.setCommand((UiCommand) () -> setVisible(false));
        int btnX = (width - BUTTON_WIDTH) / 2;
        add(btnOk, btnX, y, BUTTON_WIDTH, BUTTON_HEIGHT);
        y += BUTTON_HEIGHT + PADDING;

        this.pack();
        this.setSize(width, y + 3 * PADDING);
    }

    private int addCheckbox(int x, int y, int w, String label, FPref pref, ForgePreferences prefs) {
        FCheckBox cb = new FCheckBox(label, prefs.getPrefBoolean(pref));
        cb.addActionListener(e -> {
            boolean value = cb.isSelected();
            prefs.setPref(pref, value);
            prefs.save();
            IGameController controller = matchUI == null ? null : matchUI.getGameController();
            if (controller != null) {
                controller.setYieldInterruptPref(pref, value);
            }
        });
        add(cb, x, y, w, ROW_HEIGHT);
        return y + ROW_HEIGHT;
    }

    private int addLabelWithDropdown(int x, int y, int w, String label,
            FPref scopePref, String[] displayOptions, String[] valueOptions,
            ForgePreferences prefs) {
        // Label on left
        int lblWidth = w - DROPDOWN_WIDTH - PADDING;
        FLabel lbl = new FLabel.Builder().text(label).fontAlign(javax.swing.SwingConstants.LEFT).build();
        add(lbl, x, y, lblWidth, ROW_HEIGHT);

        // Dropdown on right (force fixed size so all dropdowns match)
        FComboBox<String> combo = new FComboBox<>();
        java.awt.Dimension dropSize = new java.awt.Dimension(DROPDOWN_WIDTH, ROW_HEIGHT);
        combo.setPreferredSize(dropSize);
        combo.setMinimumSize(dropSize);
        combo.setMaximumSize(dropSize);
        for (String opt : displayOptions) {
            combo.addItem(opt);
        }
        // Select current value
        String currentValue = prefs.getPref(scopePref);
        for (int i = 0; i < valueOptions.length; i++) {
            if (valueOptions[i].equals(currentValue)) {
                combo.setSelectedIndex(i);
                break;
            }
        }
        combo.addActionListener(e -> {
            int idx = combo.getSelectedIndex();
            if (idx >= 0 && idx < valueOptions.length) {
                prefs.setPref(scopePref, valueOptions[idx]);
                prefs.save();
            }
        });
        add(combo, x + w - DROPDOWN_WIDTH, y, DROPDOWN_WIDTH, ROW_HEIGHT);

        return y + ROW_HEIGHT;
    }

    public void showDialog() {
        setVisible(true);
        dispose();
    }
}
