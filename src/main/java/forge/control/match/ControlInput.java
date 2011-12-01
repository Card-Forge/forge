/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
    private final ViewInput view;

    private final GuiInput inputControl;

    /**
     * Child controller - handles operations related to input panel.
     * 
     * @param v
     *            &emsp; The Swing component for the input area
     */
    public ControlInput(final ViewInput v) {
        this.view = v;
        this.inputControl = new GuiInput();
    }

    /** Adds listeners to input area. */
    public void addListeners() {
        this.view.getBtnCancel().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent evt) {
                ControlInput.this.btnCancelActionPerformed(evt);
                ControlInput.this.view.getBtnOK().requestFocusInWindow();
            }
        });
        //
        this.view.getBtnOK().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent evt) {
                ControlInput.this.btnOKActionPerformed(evt);

                if (AllZone.getPhase().isNeedToNextPhase()) {
                    // moves to next turn
                    AllZone.getPhase().setNeedToNextPhase(false);
                    AllZone.getPhase().nextPhase();
                }
                ControlInput.this.view.getBtnOK().requestFocusInWindow();
            }
        });
        //
        this.view.getBtnOK().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent arg0) {
                // TODO make triggers on escape
                final int code = arg0.getKeyCode();
                if (code == KeyEvent.VK_ESCAPE) {
                    ControlInput.this.view.getBtnOK().doClick();
                }
            }
        });
    }

    /**
     * <p>
     * btnCancelActionPerformed.
     * </p>
     * Triggers current cancel action from whichever input controller is being
     * used.
     * 
     * @param evt
     *            a {@link java.awt.event.ActionEvent} object.
     */
    private void btnCancelActionPerformed(final ActionEvent evt) {
        this.inputControl.selectButtonCancel();
    }

    /**
     * <p>
     * btnOKActionPerformed.
     * </p>
     * Triggers current OK action from whichever input controller is being used.
     * 
     * @param evt
     *            a {@link java.awt.event.ActionEvent} object.
     */
    private void btnOKActionPerformed(final ActionEvent evt) {
        this.inputControl.selectButtonOK();
    }

    /**
     * Gets the input control.
     * 
     * @return GuiInput
     */
    public GuiInput getInputControl() {
        return this.inputControl;
    }

    /** @return ViewInput */
    public ViewInput getView() {
        return view;
    }
}
