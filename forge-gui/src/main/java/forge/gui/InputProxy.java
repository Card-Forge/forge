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

import forge.FThreads;
import forge.Singletons;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.gui.input.Input;
import forge.gui.input.InputPassPriority;
import forge.gui.toolbox.FOptionPane;

import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicReference;

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
    private AtomicReference<Input> input = new AtomicReference<Input>();
    private Game game = null;

//    private static final boolean DEBUG_INPUT = true; // false;
    
    public void setGame(Game game0) {
        game = game0;
        Singletons.getControl().getInputQueue().addObserver(this);
    }

    public boolean passPriority() {
        Input inp = getInput();
        if (inp != null && inp instanceof InputPassPriority) {
            inp.selectButtonOK();
            return true;
        }

        FThreads.invokeInEdtNowOrLater(new Runnable() {
            @Override
            public void run() {
                FOptionPane.showMessageDialog("Cannot pass priority at this time.");
            }
        });
        return false;
    }

    @Override
    public final void update(final Observable observable, final Object obj) {
        final Input nextInput = Singletons.getControl().getInputQueue().getActualInput(game);
        
/*        if(DEBUG_INPUT) 
            System.out.printf("%s ... \t%s on %s, \tstack = %s%n", 
                    FThreads.debugGetStackTraceItem(6, true), nextInput == null ? "null" : nextInput.getClass().getSimpleName(), 
                            game.getPhaseHandler().debugPrintState(), Singletons.getControl().getInputQueue().printInputStack());
*/
        this.input.set(nextInput);
        Runnable showMessage = new Runnable() {
            @Override public void run() { 
                Input current = getInput(); 
                Singletons.getControl().getInputQueue().syncPoint();
                //System.out.printf("\t%s > showMessage @ %s/%s during %s%n", FThreads.debugGetCurrThreadId(), nextInput.getClass().getSimpleName(), current.getClass().getSimpleName(), game.getPhaseHandler().debugPrintState());
                current.showMessageInitial(); 
            }
        };
        
        FThreads.invokeInEdtLater(showMessage);
    }
    /**
     * <p>
     * selectButtonOK.
     * </p>
     */
    public final void selectButtonOK() {
        Input inp = getInput();
        if (inp != null) {
            inp.selectButtonOK();
        }
    }

    /**
     * <p>
     * selectButtonCancel.
     * </p>
     */
    public final void selectButtonCancel() {
        Input inp = getInput();
        if (inp != null) {
            inp.selectButtonCancel();
        }
    }

    /**
     * <p>
     * selectPlayer.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     */
    public final void selectPlayer(final Player player, final MouseEvent triggerEvent) {
        Input inp = getInput();
        if (inp != null) {
            inp.selectPlayer(player, triggerEvent);
        }
    }

    /**
     * <p>
     * selectCard.
     * </p>
     * 
     * @param card
     *            a {@link forge.game.card.Card} object.
     * @param triggerEvent
     */
    public final void selectCard(final Card card, final MouseEvent triggerEvent) {
        Input inp = getInput();
        if (inp != null) {
            inp.selectCard(card, triggerEvent);
        }
    }

    public final void selectAbility(SpellAbility ab) {
    	Input inp = getInput();
        if (inp != null) {
            inp.selectAbility(ab);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        Input inp = getInput();
        return null == inp ? "(null)" : inp.toString();
    }

    /** @return {@link forge.gui.InputProxy.InputBase} */
    private Input getInput() {
        return this.input.get();
    }
}
