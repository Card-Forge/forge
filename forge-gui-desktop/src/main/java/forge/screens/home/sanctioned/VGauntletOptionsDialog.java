package forge.screens.home.sanctioned;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

/**
 * Simple dialog view for configuring gauntlet options.
 */
public class VGauntletOptionsDialog extends JDialog {
    private final Localizer localizer = Localizer.getInstance();
    private final String titleText = localizer.getMessageorUseDefault("lblGauntletOptions", "Gauntlet Options");
    private final String descriptionText = localizer.getMessageorUseDefault("lblGauntletDescription", "Configure gauntlet options");
    private final String roundsText = localizer.getMessageorUseDefault("lblRounds", "Rounds (numeric):");
    private final String matchLengthText = localizer.getMessageorUseDefault("lblMatchLength", "Games per match:");

    private final JTextField txtRounds = new JTextField(10);
    private final JComboBox<String> cbMatchLength = new JComboBox<>(new String[] {"1","3","5"});

    private final JButton btnOk = new JButton(localizer.getMessageorUseDefault("lblOK", "OK"));
    private final JButton btnCancel = new JButton(localizer.getMessageorUseDefault("lblCancel", "Cancel"));

    public VGauntletOptionsDialog() {
        super();
        setModal(true);
        setTitle(titleText);
        initLayout();
        pack();
        setLocationRelativeTo(null);
    }

    private void initLayout() {
        JPanel pnl = new JPanel(new MigLayout("wrap 2, insets 10"));

        pnl.add(new JLabel(descriptionText), "span, wrap");

        pnl.add(new JLabel(roundsText));
        pnl.add(txtRounds, "growx");

        pnl.add(new JLabel(matchLengthText));
        pnl.add(cbMatchLength, "growx");

        pnl.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(pnl, BorderLayout.CENTER);

        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlButtons.add(btnOk);
        pnlButtons.add(btnCancel);
        getContentPane().add(pnlButtons, BorderLayout.SOUTH);
    }

    public JTextField getTxtRounds() { return txtRounds; }
    public JComboBox<String> getCbMatchLength() { return cbMatchLength; }
    public JButton getBtnOk() { return btnOk; }
    public JButton getBtnCancel() { return btnCancel; }

    public void setRoundsText(final String value) {
        txtRounds.setText(value);
    }

    public void setMatchLength(final String value) {
        cbMatchLength.setSelectedItem(value);
    }

    public String getRoundsText() {
        return txtRounds.getText();
    }

    public String getSelectedMatchLength() {
        final Object selectedItem = cbMatchLength.getSelectedItem();
        return selectedItem == null ? "" : selectedItem.toString();
    }
}
