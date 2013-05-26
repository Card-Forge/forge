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

import forge.FThreads;
import forge.game.GameAge;
import forge.game.GameState;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.zone.MagicStack;
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

    private final BlockingDeque<Input> inputStack = new LinkedBlockingDeque<Input>();
    private final InputLockUI inputLock;


    public InputQueue() {
        inputLock = new InputLockUI(this);
    }

    /**
     * <p>
     * Setter for the field <code>input</code>.
     * </p>
     * 
     * @param in
     *            a {@link forge.control.input.InputBase} object.
     */
    private final void setInput(final Input in) {
        //System.out.println(in.getClass().getName());
        this.inputStack.push(in);
        // System.out.print("Current: " + input + "; Stack = " + inputStack);
        this.updateObservers();
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
        FThreads.assertExecutedByEdt(false);
        Input topMostInput = inputStack.isEmpty() ? null : inputStack.pop();

        if( topMostInput != inp )
            throw new RuntimeException("Inputs adding/removal into stack is imbalanced! Check your code again!");

        this.updateObservers();
    }

    public final boolean isEmpty() {
        return inputStack.isEmpty();
    }
    
    /**
     * <p>
     * updateInput.
     * </p>
     * 
     * @return a {@link forge.control.input.InputBase} object.
     */
    public final Input getActualInput(GameState game) {
        GameAge age = game.getAge();

        if ( game.isGameOver() )
            return inputLock;
        
        Input topMost = inputStack.peek(); // incoming input to Control
        if (topMost != null )
            return topMost;

        if ( age != GameAge.Play )
            return inputLock;

        final PhaseHandler handler = game.getPhaseHandler();

        final Player playerTurn = handler.getPlayerTurn();
        final Player priority = handler.getPriorityPlayer();
        if (priority == null) 
            throw new RuntimeException("No player has priority!");
        PlayerController pc = priority.getController();

        // If the Phase we're in doesn't allow for Priority, move to next phase
        if (!handler.isPlayerPriorityAllowed()) {
            return pc.getAutoPassPriorityInput();
        }

        // Special Inputs needed for the following phases:
        final PhaseType phase = handler.getPhase();
        final MagicStack stack = game.getStack();
        switch (phase) {
            case COMBAT_DECLARE_ATTACKERS:
                stack.freezeStack();
                if (!playerTurn.getController().mayAutoPass(phase)) {
                    game.getCombat().initiatePossibleDefenders(playerTurn.getOpponents());
                    return playerTurn.getController().getAttackInput();
                }
                break;

            case COMBAT_DECLARE_BLOCKERS:
                stack.freezeStack();

                boolean isAttacked = game.getCombat().isPlayerAttacked(priority);
                return isAttacked ? pc.getBlockInput() : pc.getAutoPassPriorityInput();

            case CLEANUP:
                // discard
                if (stack.isEmpty()) {
                    // resolve things like Madness
                    return pc.getCleanupInput();
                }
                break;
            default:
                break;
        }

        // *********************
        // Special phases handled above, everything else is handled simply by
        // priority

        boolean maySkipPriority = pc.mayAutoPass(phase) || pc.isUiSetToSkipPhase(playerTurn, phase);
        if (game.getStack().isEmpty() && maySkipPriority) {
            return pc.getAutoPassPriorityInput();
        } else
            pc.autoPassCancel(); // probably cancel, since something has happened

         return pc.getDefaultInput();
    } // getInput()


    public void lock() {
        setInput(inputLock);
    }
    
    public void unlock() { 
        if ( inputStack.isEmpty() || inputStack.peek() != inputLock )
            throw new RuntimeException("Trying to unlock input which is not locked! Do check when your threads terminate!");
        removeInput(inputLock);
    }

    // only for debug purposes
    public String printInputStack() {
        return inputStack.toString();
    }
    
    public void setInputAndWait(InputSynchronized input) {
        this.setInput(input);
        input.awaitLatchRelease();
    }

    public void lockAndInvokeGameAction(final Runnable proc) {
        Runnable toRun = new Runnable() {
            @Override
            public void run() {
                InputQueue.this.lock();
                proc.run();
                InputQueue.this.unlock();
            }
        };
        invokeGameAction(toRun);
    }

    public void invokeGameAction(final Runnable proc) {
        if(FThreads.isEDT()) {
            FThreads.invokeInNewThread(proc);
        } else { // this branch is experimental
            proc.run();
        }
    }
    
} // InputControl
