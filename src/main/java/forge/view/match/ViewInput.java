package forge.view.match;

import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.control.match.ControlInput;
import forge.gui.skin.FButton;
import forge.gui.skin.FRoundedPanel;
import forge.gui.skin.FSkin;

/** 
 * Assembles Swing components of input area.
 *
 */
@SuppressWarnings("serial")
public class ViewInput extends FRoundedPanel {
    private ControlInput control;
    private JButton btnOK, btnCancel;
    private JTextArea tarMessage;
    private FSkin skin;

    /**
     * Assembles UI for input area (buttons and message panel).
     * 
     */
    public ViewInput() {
        super();
        skin = AllZone.getSkin();
        setToolTipText("Input Area");
        setBackground(skin.getClrTheme());
        setForeground(skin.getClrText());
        setCorners(new boolean[] {false, false, false, true});
        setBorders(new boolean[] {true, false, false, true});

        setLayout(new MigLayout("wrap 2, fill, insets 0, gap 0"));

        // Cancel button
        btnCancel = new FButton("Cancel");
        btnOK = new FButton("OK");

        tarMessage = new JTextArea();
        tarMessage.setOpaque(false);
        tarMessage.setFocusable(false);
        tarMessage.setEditable(false);
        tarMessage.setLineWrap(true);
        tarMessage.setForeground(skin.getClrText());
        tarMessage.setFont(skin.getFont1().deriveFont(Font.PLAIN, 12));
        add(tarMessage, "span 2 1, h 80%!, w 96%!, gapleft 2%, gaptop 1%");
        add(btnOK, "w 47%!, gapright 2%, gapleft 1%");
        add(btnCancel, "w 47%!, gapright 1%");

        // After all components are in place, instantiate controller.
        control = new ControlInput(this);
    }

    /** @return ControlInput */
    public ControlInput getController() {
        return control;
    }

    /** @return JButton */
    public JButton getBtnOK() {
        return btnOK;
    }

    /** @return JButton */
    public JButton getBtnCancel() {
        return btnCancel;
    }

    /** @return JTextArea */
    public JTextArea getTarMessage() {
        return tarMessage;
    }
}
