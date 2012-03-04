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

import forge.Constant;
import forge.GuiInput;
import forge.Singletons;
import forge.view.match.ViewMessage;

/**
 * Child controller - handles operations related to message panel.
 * 
 */
public class ControlMessage {
    private final ViewMessage view;

    private final GuiInput inputControl;

    private final ActionListener actCancel, actOK;

    /**
     * Child controller - handles operations related to input panel.
     * 
     * @param v
     *            &emsp; The Swing component for the input area
     */
    public ControlMessage(final ViewMessage v) {
        this.view = v;
        this.inputControl = new GuiInput();

        this.actOK = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent evt) {
                ControlMessage.this.btnOKActionPerformed(evt);

                if (Singletons.getModel().getGameState().getPhaseHandler().isNeedToNextPhase()) {
                    // moves to next turn
                    Singletons.getModel().getGameState().getPhaseHandler().setNeedToNextPhase(false);
                    Singletons.getModel().getGameState().getPhaseHandler().nextPhase();
                }
                ControlMessage.this.view.getBtnOK().requestFocusInWindow();
            }
        };

        this.actCancel = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent evt) {
                ControlMessage.this.btnCancelActionPerformed(evt);
                ControlMessage.this.view.getBtnOK().requestFocusInWindow();
            }
        };

        addListeners();
    }

    /** Adds listeners to input area. */
    public void addListeners() {
        this.view.getBtnCancel().removeActionListener(actCancel);
        this.view.getBtnCancel().addActionListener(actCancel);

        this.view.getBtnOK().removeActionListener(actOK);
        this.view.getBtnOK().addActionListener(actOK);
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

    /** @param s0 &emsp; {@link java.lang.String} */
    public void setMessage(String s0) {
        view.getTarMessage().setText(s0);
    }

    /** Updates count label in input area. */
    public void updateGameCount() {
        view.getLblGames().setText("<html>Game #"
                + (Singletons.getModel().getMatchState().getGamesPlayedCount() + 1)
                + " of " + Singletons.getModel().getMatchState().getGamesPerMatch()
                + "<br>" + Constant.Runtime.getGameType().toString() + " mode</html>");
    }

    /** Flashes animation on input panel if play is currently waiting on input. */
    public void remind() {
        view.remind();
    }
}
