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
package forge.control;

import java.util.Observable;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import forge.game.Game;
import forge.gui.input.Input;
import forge.gui.input.InputLockUI;
import forge.gui.input.InputSynchronized;

/**
 * <p>
 * InputControl class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class InputQueue extends Observable {

    private final BlockingDeque<InputSynchronized> inputStack = new LinkedBlockingDeque<InputSynchronized>();
    private final InputLockUI inputLock;


    public InputQueue() {
        inputLock = new InputLockUI(this);
    }

    
    public final void updateObservers() {
        this.setChanged();
        this.notifyObservers();
    }
    
    public final Input getInput() {
        return inputStack.isEmpty() ? null : this.inputStack.peek();
    }

    public final void removeInput(Input inp) {
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
     * @return a {@link forge.gui.input.InputBase} object.
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
        syncPoint();
        this.updateObservers();
        
        input.awaitLatchRelease();
    }
    
    public void setInput(InputSynchronized input) {
        this.inputStack.push(input);
        syncPoint();
        this.updateObservers();
    }
    

    public void syncPoint() { 
        synchronized (inputLock) {
            // acquire and release lock, so that actions from Game thread happen before EDT reads their results  
        }
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
