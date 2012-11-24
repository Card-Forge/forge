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
package forge.control.input;

import forge.Card;
import forge.Singletons;
import forge.game.player.Player;
import forge.gui.match.CMatchUI;

/**
 * <p>
 * Abstract Input class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class Input implements java.io.Serializable {
    /** Constant <code>serialVersionUID=-6539552513871194081L</code>. */
    private static final long serialVersionUID = -6539552513871194081L;

    // showMessage() is always the first method called
    /**
     * <p>
     * showMessage.
     * </p>
     */
    public void showMessage() {
        CMatchUI.SINGLETON_INSTANCE.showMessage("Blank Input");
    }

    /**
     * <p>
     * selectCard.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public void selectCard(final Card c) {
    }

    /**
     * <p>
     * selectPlayer.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     */
    public void selectPlayer(final Player player) {
    }

    /**
     * <p>
     * selectButtonOK.
     * </p>
     */
    public void selectButtonOK() {
    }

    /**
     * <p>
     * selectButtonCancel.
     * </p>
     */
    public void selectButtonCancel() {
    }

    // helper methods, since they are used alot
    // to be used by anything in CardFactory like SetTargetInput
    // NOT TO BE USED by Input_Main or any of the "regular" Inputs objects that
    // are not set using AllZone.getInputControl().setInput(Input)
    /**
     * <p>
     * stop.
     * </p>
     */
    public final void stop() {
        // clears a "temp" Input like Input_PayManaCost if there is one
        Singletons.getModel().getMatch().getInput().resetInput();

    }

    // exits the "current" Input and sets the next Input
    /**
     * <p>
     * stopSetNext.
     * </p>
     * 
     * @param in
     *            a {@link forge.control.input.Input} object.
     */
    public final void stopSetNext(final Input in) {
        this.stop();
        Singletons.getModel().getMatch().getInput().setInput(in);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "blank";
    } // returns the Input name like "EmptyStack"


    /**
     * This method is used to mark old descendants of Input
     * TODO: Write javadoc for this method.
     */
    public /*abstract */void isClassUpdated() {
    } //;

}
