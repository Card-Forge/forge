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

import forge.Card;
import forge.FThreads;
import forge.control.input.Input;
import forge.game.GameState;
import forge.game.MatchController;
import forge.game.phase.PhaseHandler;
import forge.game.player.Player;

/**
 * <p>
 * GuiInput class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class InputProxy implements Observer {

    /** The input. */
    private Input input;
    private MatchController match = null;

    public void setMatch(MatchController matchController) {
        match = matchController;
    }
    
    @Override
    public final synchronized void update(final Observable observable, final Object obj) {
        this.input = null;
        final GameState game = match.getCurrentGame();
        final PhaseHandler ph = game.getPhaseHandler();
        
        //System.out.print((FThreads.isEDT() ? "EDT > " : "TRD > ") + ph.debugPrintState());
        if ( match.getInput().isEmpty() && ph.hasPhaseEffects()) {
            //System.out.println(" handle begin phase");
            FThreads.invokeInNewThread(new Runnable() { 
                @Override public void run() { 
                    ph.handleBeginPhase();
                    update(observable, obj); 
                } 
            }, true);
            return;
        }
        
        final Input nextInput = match.getInput().getActualInput(game);
        //System.out.printf(" input is %s \t stack = %s%n", nextInput == null ? "null" : nextInput.getClass().getSimpleName(), match.getInput().printInputStack());
        
        if (nextInput != null) {
            this.input = nextInput;
            FThreads.invokeInEDT(new Runnable() { @Override public void run() { nextInput.showMessage(); } });
        } else if (!ph.isPlayerPriorityAllowed()) {
            ph.getPriorityPlayer().getController().passPriority();
        }
    }
    /**
     * <p>
     * selectButtonOK.
     * </p>
     */
    public final void selectButtonOK() {
        if ( null == input ) return;
        input.selectButtonOK();
    }

    /**
     * <p>
     * selectButtonCancel.
     * </p>
     */
    public final void selectButtonCancel() {
        if ( null == input ) return;
        input.selectButtonCancel();
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
        if ( null == input ) return;
        input.selectPlayer(player);
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
    public final void selectCard(final Card card) {
        if ( null == input ) return;
        input.selectCard(card);
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        if ( null == input ) return "(null)"; 
        return this.input.toString();
    }

    /** @return {@link forge.gui.InputProxy.InputBase} */
    public Input getInput() {
        return this.input;
    }
}
