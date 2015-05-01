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
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import forge.game.Game;
import forge.player.PlayerControllerHuman;

/**
 * <p>
 * InputControl class.
 * </p>
 *
 * @author Forge
 * @version $Id: InputQueue.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
public class InputQueue extends Observable {
    private final BlockingDeque<InputSynchronized> inputStack = new LinkedBlockingDeque<InputSynchronized>();
    private final Game game;

    public InputQueue(final Game game, final InputProxy inputProxy) {
        this.game = game;
        addObserver(inputProxy);
    }

    public final void updateObservers() {
        setChanged();
        notifyObservers();
    }

    public final Input getInput() {
        return inputStack.isEmpty() ? null : inputStack.peek();
    }

    public final void removeInput(final Input inp) {
        final Input topMostInput = inputStack.isEmpty() ? null : inputStack.pop();

        if (topMostInput != inp) {
            throw new RuntimeException("Cannot remove input " + inp.getClass().getSimpleName() + " because it's not on top of stack. Stack = " + inputStack );
        }
        updateObservers();
    }

    public final Input getActualInput(final PlayerControllerHuman controller) {
        final Input topMost = inputStack.peek(); // incoming input to Control
        if (topMost != null && !game.isGameOver()) {
            return topMost;
        }
        return new InputLockUI(game, this, controller);
    } // getInput()

    // only for debug purposes
    public String printInputStack() {
        return inputStack.toString();
    }

    public void setInput(final InputSynchronized input) {
        //if (HostedMatch.getHumanCount() > 1) { //update current player if needed
            //HostedMatch.setCurrentPlayer(game.getPlayer(input.getOwner()));
        //}
        inputStack.push(input);
        syncPoint();
        updateObservers();
    }

    void syncPoint() {
        synchronized (this) {
            // acquire and release lock, so that actions from Game thread happen before EDT reads their results
        }
    }

    public void onGameOver(final boolean releaseAllInputs) {
        for (final InputSynchronized inp : inputStack) {
            inp.relaseLatchWhenGameIsOver();
            if (!releaseAllInputs) {
                break;
            }
        }
    }
}
