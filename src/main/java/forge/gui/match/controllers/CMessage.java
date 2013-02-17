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
package forge.gui.match.controllers;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JButton;

import forge.Command;
import forge.game.MatchController;
import forge.gui.GuiInput;
import forge.gui.framework.ICDoc;
import forge.gui.framework.SDisplayUtil;
import forge.gui.match.views.VMessage;

/**
 * Controls the message panel in the match UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public enum CMessage implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private GuiInput inputControl = new GuiInput();
    private Component lastFocusedButton = null;
    
    private final ActionListener actCancel = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent evt) {
            inputControl.selectButtonCancel();
        }
    };
    private final ActionListener actOK = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent evt) {
            inputControl.selectButtonOK();
        }
    };
    
    private final FocusListener onFocus = new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
            if (null != VMessage.SINGLETON_INSTANCE.getParentCell() && VMessage.SINGLETON_INSTANCE == VMessage.SINGLETON_INSTANCE.getParentCell().getSelected()) {
                // only record focus changes when we're showing -- otherwise it is due to a tab visibility change
                lastFocusedButton = e.getComponent();
            }
        }
    };

    private void _initButton(JButton button, ActionListener onClick) {
        // remove to ensure listeners don't accumulate over many initializations
        button.removeActionListener(onClick);
        button.addActionListener(onClick);
        button.removeFocusListener(onFocus);
        button.addFocusListener(onFocus);
    }
    
    @Override
    public void initialize() {
        _initButton(VMessage.SINGLETON_INSTANCE.getBtnCancel(), actCancel);
        _initButton(VMessage.SINGLETON_INSTANCE.getBtnOK(), actOK);
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
        VMessage.SINGLETON_INSTANCE.getTarMessage().setText(s0);
    }

    /** Updates counter label in message area.
     * @param match
     * @param gameState */
    public void updateGameInfo(MatchController match) {
        VMessage.SINGLETON_INSTANCE.getLblGames().setText(
                match.getGameType().toString() + ": Game #"
                + (match.getPlayedGames().size() + 1)
                + " of " + match.getGamesPerMatch()
                + ", turn " + match.getCurrentGame().getPhaseHandler().getTurn());
    }

    /** Flashes animation on input panel if play is currently waiting on input. */
    public void remind() {
        SDisplayUtil.remind(VMessage.SINGLETON_INSTANCE);
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
        // set focus back to button that last had it
        if (null != lastFocusedButton) {
            lastFocusedButton.requestFocusInWindow();
        }
    }
}
