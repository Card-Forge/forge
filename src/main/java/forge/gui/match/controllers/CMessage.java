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
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;

import forge.Command;
import forge.game.MatchController;
import forge.gui.InputProxy;
import forge.gui.framework.ICDoc;
import forge.gui.framework.SDisplayUtil;
import forge.gui.match.views.VMessage;

/**
 * Controls the message panel in the match UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public enum CMessage implements ICDoc, Observer {
    /** */
    SINGLETON_INSTANCE;

    private InputProxy inputControl = new InputProxy();
    private Component lastFocusedButton = null;
    private VMessage view = VMessage.SINGLETON_INSTANCE;
    
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
            if (null != view.getParentCell() && view == view.getParentCell().getSelected()) {
                // only record focus changes when we're showing -- otherwise it is due to a tab visibility change
                lastFocusedButton = e.getComponent();
            }
        }
    };
    private MatchController match;

    private void _initButton(JButton button, ActionListener onClick) {
        // remove to ensure listeners don't accumulate over many initializations
        button.removeActionListener(onClick);
        button.addActionListener(onClick);
        button.removeFocusListener(onFocus);
        button.addFocusListener(onFocus);
    }
    
    @Override
    public void initialize() {
        _initButton(view.getBtnCancel(), actCancel);
        _initButton(view.getBtnOK(), actOK);
    }

    /**
     * Gets the input control.
     * 
     * @return GuiInput
     */
    public InputProxy getInputControl() {
        return this.inputControl;
    }

    /** @param s0 &emsp; {@link java.lang.String} */
    public void setMessage(String s0) {
        view.getTarMessage().setText(s0);
    }

    /** Flashes animation on input panel if play is currently waiting on input. */
    public void remind() {
        SDisplayUtil.remind(view);
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
     */
    @Override
    public void update(Observable o, Object arg) {
        view.getLblGames().setText(
                match.getGameType().toString() + ": Game #"
                + (match.getPlayedGames().size() + 1)
                + " of " + match.getGamesPerMatch()
                + ", turn " + match.getCurrentGame().getPhaseHandler().getTurn());
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

    /**
     * TODO: Write javadoc for this method.
     * @param match
     */
    public void setModel(MatchController match0) {
        match = match0;
        match.getCurrentGame().getPhaseHandler().addObserver(this);
        update(null, null);
    }
}
