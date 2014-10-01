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
package forge.match.input;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicReference;

import forge.FThreads;
import forge.control.FControlGameEventHandler;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.interfaces.IGuiBase;
import forge.util.ITriggerEvent;
import forge.view.CardView;
import forge.view.LocalGameView;
import forge.view.PlayerView;
import forge.view.SpellAbilityView;

/**
 * <p>
 * GuiInput class.
 * </p>
 * 
 * @author Forge
 * @version $Id: InputProxy.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
public class InputProxy implements Observer {

    /** The input. */
    private AtomicReference<Input> input = new AtomicReference<Input>();
    private final LocalGameView gameView;

//    private static final boolean DEBUG_INPUT = true; // false;

    public InputProxy(final LocalGameView gameView0) {
        gameView = gameView0;
    }

    private IGuiBase getGui() {
        return gameView.getGui();
    }

    @Override
    public final void update(final Observable observable, final Object obj) {
        final Input nextInput = gameView.getInputQueue().getActualInput(gameView);
/*        if(DEBUG_INPUT) 
            System.out.printf("%s ... \t%s on %s, \tstack = %s%n", 
                    FThreads.debugGetStackTraceItem(6, true), nextInput == null ? "null" : nextInput.getClass().getSimpleName(), 
                            game.getPhaseHandler().debugPrintState(), Singletons.getControl().getInputQueue().printInputStack());
*/
        input.set(nextInput);
        Runnable showMessage = new Runnable() {
            @Override
            public void run() {
                Input current = getInput(); 
                gameView.getInputQueue().syncPoint();
                //System.out.printf("\t%s > showMessage @ %s/%s during %s%n", FThreads.debugGetCurrThreadId(), nextInput.getClass().getSimpleName(), current.getClass().getSimpleName(), game.getPhaseHandler().debugPrintState());
                current.showMessageInitial(); 
            }
        };
        
        FThreads.invokeInEdtLater(getGui(), showMessage);
    }
    /**
     * <p>
     * selectButtonOK.
     * </p>
     */
    public final void selectButtonOK() {
        Input inp = getInput();
        if (inp != null) {
            FControlGameEventHandler.processEventsAfterInput();
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
            FControlGameEventHandler.processEventsAfterInput();
            inp.selectButtonCancel();
        }
    }

    public final void selectPlayer(final PlayerView playerView, final ITriggerEvent triggerEvent) {
        final Input inp = getInput();
        if (inp != null) {
            final Player player = gameView.getPlayer(playerView);
            if (player != null) {
                FControlGameEventHandler.processEventsAfterInput();
                inp.selectPlayer(player, triggerEvent);
            }
        }
    }

    public final boolean selectCard(final CardView cardView, final ITriggerEvent triggerEvent) {
        final Input inp = getInput();
        if (inp != null) {
            final Card card = gameView.getCard(cardView);
            if (card != null) {
                FControlGameEventHandler.processEventsAfterInput();
                return inp.selectCard(card, triggerEvent);
            }
        }
        return false;
    }

    public final void selectAbility(final SpellAbilityView ab) {
    	final Input inp = getInput();
        if (inp != null) {
            final SpellAbility sa = gameView.getSpellAbility(ab);
            if (sa != null) {
                FControlGameEventHandler.processEventsAfterInput();
                inp.selectAbility(sa);
            }
        }
    }

    public final void alphaStrike() {
        final Input inp = getInput();
        if (inp instanceof InputAttack) {
            ((InputAttack) inp).alphaStrike();
        }
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        Input inp = getInput();
        return null == inp ? "(null)" : inp.toString();
    }

    /** @return {@link forge.gui.InputProxy.InputBase} */
    public Input getInput() {
        return input.get();
    }
}
