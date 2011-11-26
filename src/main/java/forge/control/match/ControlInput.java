package forge.control.match;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import forge.AllZone;
import forge.GuiInput;
import forge.view.match.ViewInput;

/** 
 * Child controller - handles operations related to input panel.
 *
 */
public class ControlInput {
    private ViewInput view;

    private GuiInput inputControl;

    /** 
     * Child controller - handles operations related to input panel.
     * @param v &emsp; The Swing component for the input area
     */
    public ControlInput(ViewInput v) {
        view = v;
        inputControl = new GuiInput();
    }

    /** Adds listeners to input area. */
    public void addListeners() {
        view.getBtnCancel().addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent evt) {
                btnCancelActionPerformed(evt);
                view.getBtnOK().requestFocusInWindow();
            }
        });
        //
        view.getBtnOK().addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent evt) {
                btnOKActionPerformed(evt);

                if (AllZone.getPhase().isNeedToNextPhase()) {
                    // moves to next turn
                    AllZone.getPhase().setNeedToNextPhase(false);
                    AllZone.getPhase().nextPhase();
                }
                view.getBtnOK().requestFocusInWindow();
            }
        });
        //
        view.getBtnOK().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent arg0) {
                // TODO make triggers on escape
                int code = arg0.getKeyCode();
                if (code == KeyEvent.VK_ESCAPE) {
                    view.getBtnOK().doClick();
                }
            }
        });
    }

    /**
     * <p>btnCancelActionPerformed.</p>
     * Triggers current cancel action from whichever input controller is being used.
     *
     * @param evt a {@link java.awt.event.ActionEvent} object.
     */
    private void btnCancelActionPerformed(final ActionEvent evt) {
        inputControl.selectButtonCancel();
    }

    /**
     * <p>btnOKActionPerformed.</p>
     * Triggers current OK action from whichever input controller is being used.
     *
     * @param evt a {@link java.awt.event.ActionEvent} object.
     */
    private void btnOKActionPerformed(final ActionEvent evt) {
        inputControl.selectButtonOK();
    }

    /** @return GuiInput */
    public GuiInput getInputControl() {
        return inputControl;
    }
}
