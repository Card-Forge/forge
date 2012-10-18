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
package forge.gui;

import java.util.Observable;
import java.util.Observer;

import forge.AllZone;
import forge.Card;
import forge.control.input.Input;
import forge.game.GameState;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.util.MyObservable;

/**
 * <p>
 * GuiInput class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class GuiInput extends MyObservable implements Observer {

    /** The input. */
    private Input input;


    /** {@inheritDoc} */
    @Override
    public final void update(final Observable observable, final Object obj) {
        final Input tmp = AllZone.getInputControl().updateInput();
        if (tmp != null) {
            this.setInput(tmp);
        }
    }

    /**
     * <p>
     * Setter for the field <code>input</code>.
     * </p>
     * 
     * @param in
     *            a {@link forge.control.input.Input} object.
     */
    private void setInput(final Input in) {
        this.input = in;
        this.input.showMessage();
    }

    /**
     * <p>
     * showMessage.
     * </p>
     */
    public final void showMessage() {
        this.getInput().showMessage();
    }

    /**
     * <p>
     * selectButtonOK.
     * </p>
     */
    public final void selectButtonOK() {
        this.getInput().selectButtonOK();
    }

    /**
     * <p>
     * selectButtonCancel.
     * </p>
     */
    public final void selectButtonCancel() {
        this.getInput().selectButtonCancel();
    }

    /**
     * <p>
     * selectPlayer.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     */
    public final void selectPlayer(final Player player) {
        this.getInput().selectPlayer(player);
    }

    /**
     * <p>
     * selectCard.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param zone
     *            a {@link forge.game.zone.PlayerZone} object.
     */
    public final void selectCard(final Card card, final PlayerZone zone) {
        this.getInput().selectCard(card, zone);
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        return this.getInput().toString();
    }

    /** @return {@link forge.gui.GuiInput.Input} */
    public Input getInput() {
        return this.input;
    }

    /**
     * TODO: Write javadoc for this method.
     */
    public void subscribe(GameState game) {
        AllZone.getInputControl().addObserver(this);
        game.getStack().addObserver(this);
        game.getPhaseHandler().addObserver(this);
    }
}
