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
import forge.toolbox.FTextField;
import forge.util.Localizer;
import forge.view.FDialog;

import javax.swing.JSeparator;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;

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
        JSeparator sep2 = new JSeparator();
        add(sep2, x, y, w, 2);
        y += 2 + SECTION_GAP;

        // --- Speed Settings section ---
        FLabel lblSpeed = new FLabel.Builder().text(localizer.getMessage("lblSpeedSettings"))
                .fontStyle(java.awt.Font.BOLD).fontSize(14).build();
        add(lblSpeed, x, y, w, ROW_HEIGHT);
        y += ROW_HEIGHT + 2;

        y = addTimeoutField(x, y, w, prefs);
        y = addCheckbox(x, y, w, localizer.getMessage("lblSkipPhaseDelay"), FPref.YIELD_SKIP_PHASE_DELAY, prefs);
        y = addCheckbox(x, y, w, localizer.getMessage("lblSkipResolveDelay"), FPref.YIELD_SKIP_RESOLVE_DELAY, prefs);

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

    private int addTimeoutField(int x, int y, int w, ForgePreferences prefs) {
        final Localizer loc = Localizer.getInstance();
        final int fieldWidth = DROPDOWN_WIDTH;
        int lblWidth = w - fieldWidth - PADDING;
        String tooltip = loc.getMessage("lblAutoPassBudgetDesc");
        FLabel lbl = new FLabel.Builder().text(loc.getMessage("lblAutoPassBudgetLabel"))
                .fontAlign(javax.swing.SwingConstants.LEFT).build();
        lbl.setToolTipText(tooltip);
        add(lbl, x, y, lblWidth, ROW_HEIGHT);

        int current = prefs.getPrefInt(FPref.YIELD_AVAILABLE_ACTIONS_BUDGET_MS);
        FTextField field = new FTextField.Builder()
                .ghostText(loc.getMessage("lblDynamic"))
                .tooltip(tooltip)
                .text(current > 0 ? String.valueOf(current) : "")
                .build();
        ((PlainDocument) field.getDocument()).setDocumentFilter(new DigitsOnlyFilter(4));
        field.getDocument().addDocumentListener(new DocumentListener() {
            private void save() {
                String text = field.getText();
                int value = (text == null || text.isEmpty()) ? 0 : Integer.parseInt(text);
                if (value > 9999) value = 9999;
                prefs.setPref(FPref.YIELD_AVAILABLE_ACTIONS_BUDGET_MS, String.valueOf(value));
                prefs.save();
            }
            @Override public void insertUpdate(DocumentEvent e)  { save(); }
            @Override public void removeUpdate(DocumentEvent e)  { save(); }
            @Override public void changedUpdate(DocumentEvent e) { save(); }
        });
        add(field, x + w - fieldWidth, y, fieldWidth, ROW_HEIGHT);
        return y + ROW_HEIGHT;
    }

    private static final class DigitsOnlyFilter extends DocumentFilter {
        private final int maxLength;
        DigitsOnlyFilter(int maxLength) { this.maxLength = maxLength; }
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string == null) return;
            String filtered = scrub(string);
            if (filtered.isEmpty()) return;
            if (fb.getDocument().getLength() + filtered.length() > maxLength) return;
            super.insertString(fb, offset, filtered, attr);
        }
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            String filtered = text == null ? "" : scrub(text);
            int newLength = fb.getDocument().getLength() - length + filtered.length();
            if (newLength > maxLength) return;
            super.replace(fb, offset, length, filtered, attrs);
        }
        private static String scrub(String s) {
            StringBuilder sb = new StringBuilder(s.length());
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (Character.isDigit(c)) sb.append(c);
            }
            return sb.toString();
        }
    }

    public void showDialog() {
        setVisible(true);
        dispose();
    }
}
