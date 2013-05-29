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

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import forge.game.Game;
import forge.util.MyObservable;

/**
 * <p>
 * InputControl class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class InputQueue extends MyObservable implements java.io.Serializable {
    /** Constant <code>serialVersionUID=3955194449319994301L</code>. */
    private static final long serialVersionUID = 3955194449319994301L;

    private final BlockingDeque<InputSynchronized> inputStack = new LinkedBlockingDeque<InputSynchronized>();
    private final InputLockUI inputLock;


    public InputQueue() {
        inputLock = new InputLockUI(this);
    }

    /**
     * <p>
     * Getter for the field <code>input</code>.
     * </p>
     * 
     * @return a {@link forge.control.input.InputBase} object.
     */
    public final Input getInput() {
        return inputStack.isEmpty() ? null : this.inputStack.peek();
    }

    /**
     * <p>
     * resetInput.
     * </p>
     * 
     * @param update
     *            a boolean.
     */
    final void removeInput(Input inp) {
        Input topMostInput = inputStack.isEmpty() ? null : inputStack.pop();

        if( topMostInput != inp )
            throw new RuntimeException("Cannot remove input " + inp.getClass().getSimpleName() + " because it's not on top of stack. Stack = " + inputStack );
        updateObservers();
    }

    /**
     * <p>
     * updateInput.
     * </p>
     * 
     * @return a {@link forge.control.input.InputBase} object.
     */
    public final Input getActualInput(Game game) {
        Input topMost = inputStack.peek(); // incoming input to Control
        if (topMost != null && !game.isGameOver() )
            return topMost;

        return inputLock;
    } // getInput()

    // only for debug purposes
    public String printInputStack() {
        return inputStack.toString();
    }
    
    public void setInputAndWait(InputSynchronized input) {
        this.inputStack.push(input);
        this.updateObservers();
        
        input.awaitLatchRelease();
    }

    /**
     * TODO: Write javadoc for this method.
     */
    public void onGameOver() {
        for(InputSynchronized inp : inputStack ) {
            inp.relaseLatchWhenGameIsOver();
        }
    }
    
} // InputControl
