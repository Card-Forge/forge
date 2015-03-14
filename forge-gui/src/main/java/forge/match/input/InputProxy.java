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

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicReference;

import forge.FThreads;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;
import forge.player.PlayerControllerHuman;
import forge.util.ITriggerEvent;

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
    private final PlayerControllerHuman controller;

//    private static final boolean DEBUG_INPUT = true; // false;

    public InputProxy(final PlayerControllerHuman controller0) {
        controller = controller0;
    }

    @Override
    public final void update(final Observable observable, final Object obj) {
        final Input nextInput = controller.getInputQueue().getActualInput(controller);
/*        if(DEBUG_INPUT) 
            System.out.printf("%s ... \t%s on %s, \tstack = %s%n", 
                    FThreads.debugGetStackTraceItem(6, true), nextInput == null ? "null" : nextInput.getClass().getSimpleName(), 
                            game.getPhaseHandler().debugPrintState(), Singletons.getControl().getInputQueue().printInputStack());
*/
        input.set(nextInput);
        if (!(nextInput instanceof InputLockUI)) {
            controller.getGui().setCurrentPlayer(nextInput.getOwner());
        }
        final Runnable showMessage = new Runnable() {
            @Override
            public void run() {
                Input current = getInput(); 
                controller.getInputQueue().syncPoint();
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
        final Input inp = getInput();
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

    public final void selectPlayer(final PlayerView playerView, final ITriggerEvent triggerEvent) {
        final Input inp = getInput();
        if (inp != null) {
            final Player player = controller.getGame().getPlayer(playerView);
            if (player != null) {
                inp.selectPlayer(player, triggerEvent);
            }
        }
    }

    private Card getCard(final CardView cardView) {
        return controller.getGame().getCard(cardView);
    }

    public final String getActivateAction(final CardView cardView) {
        final Input inp = getInput();
        if (inp != null) {
            final Card card = getCard(cardView);
            if (card != null) {
                return inp.getActivateAction(card);
            }
        }
        return null;
    }

    public final boolean selectCard(final CardView cardView, final List<CardView> otherCardViewsToSelect, final ITriggerEvent triggerEvent) {
        final Input inp = getInput();
        //System.out.println("Selectcard " +cardView + " input: "  +inp);
        if (inp != null) {
            final Card card = getCard(cardView);
            if (card != null) {
                List<Card> otherCardsToSelect = null;
                if (otherCardViewsToSelect != null) {
                    for (CardView cv : otherCardViewsToSelect) {
                        final Card c = getCard(cv);
                        if (c != null) {
                            if (otherCardsToSelect == null) {
                                otherCardsToSelect = new ArrayList<Card>();
                            }
                            otherCardsToSelect.add(c);
                        }
                    }
                }
                return inp.selectCard(card, otherCardsToSelect, triggerEvent);
            }
        }
        return false;
    }

    public final void selectAbility(final SpellAbility sa) {
        final Input inp = getInput();
        if (inp != null) {
            if (sa != null) {
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
