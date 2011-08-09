package forge.gui.input;

import forge.*;

import java.util.LinkedList;
import java.util.Stack;

/**
 * <p>InputControl class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class InputControl extends MyObservable implements java.io.Serializable {
    /** Constant <code>serialVersionUID=3955194449319994301L</code> */
    private static final long serialVersionUID = 3955194449319994301L;

    private Input input;
    /** Constant <code>n=0</code> */
    static int n = 0;
    private Stack<Input> inputStack = new Stack<Input>();
    private Stack<Input> resolvingStack = new Stack<Input>();
    private LinkedList<Input> resolvingQueue = new LinkedList<Input>();

    /**
     * <p>Setter for the field <code>input</code>.</p>
     *
     * @param in a {@link forge.gui.input.Input} object.
     */
    public void setInput(final Input in) {
        if (AllZone.getStack().getResolving() || !(input == null || input instanceof Input_PassPriority))
            inputStack.add(in);
        else
            input = in;
        updateObservers();
    }

    /**
     * <p>Setter for the field <code>input</code>.</p>
     *
     * @param in a {@link forge.gui.input.Input} object.
     * @param bAddToResolving a boolean.
     */
    public void setInput(final Input in, boolean bAddToResolving) {
        // Make this
        if (!bAddToResolving) {
            setInput(in);
            return;
        }

        Input old = input;
        resolvingStack.add(old);
        changeInput(in);
    }

    /**
     * <p>changeInput.</p>
     *
     * @param in a {@link forge.gui.input.Input} object.
     */
    private void changeInput(final Input in) {
        input = in;
        updateObservers();
    }

    /**
     * <p>Getter for the field <code>input</code>.</p>
     *
     * @return a {@link forge.gui.input.Input} object.
     */
    public Input getInput() {
        return input;
    }

    /**
     * <p>clearInput.</p>
     */
    public void clearInput() {
        input = null;
        resolvingQueue.clear();
        inputStack.clear();
    }

    /**
     * <p>resetInput.</p>
     */
    public void resetInput() {
        input = null;
        updateObservers();
    }

    /**
     * <p>resetInput.</p>
     *
     * @param update a boolean.
     */
    public void resetInput(boolean update) {
        input = null;
        if (update)
            updateObservers();
    }

    /**
     * <p>updateInput.</p>
     *
     * @return a {@link forge.gui.input.Input} object.
     */
    public Input updateInput() {
        final String phase = AllZone.getPhase().getPhase();
        final Player playerTurn = AllZone.getPhase().getPlayerTurn();
        final Player priority = AllZone.getPhase().getPriorityPlayer();

        // TODO: this resolving portion needs more work, but fixes Death Cloud issues 
        if (resolvingStack.size() > 0) {
            if (input != null) {
                return input;
            }

            // if an SA is resolving, only change input for something that is part of the resolving SA
            changeInput(resolvingStack.pop());
            return input;
        }

        if (AllZone.getStack().getResolving())
            return null;


        if (input != null)
            return input;

        else if (inputStack.size() > 0) {        // incoming input to Control
            changeInput(inputStack.pop());
            return input;
        }

        if (Phase.getGameBegins() != 0 && AllZone.getPhase().doPhaseEffects()) {
            // Handle begin phase stuff, then start back from the top
            AllZone.getPhase().handleBeginPhase();
            return updateInput();
        }

        // If the Phase we're in doesn't allow for Priority, return null to move to next phase
        if (AllZone.getPhase().isNeedToNextPhase())
            return null;

        // Special Inputs needed for the following phases:        
        if (phase.equals(Constant.Phase.Combat_Declare_Attackers)) {
            AllZone.getStack().freezeStack();

            if (playerTurn.isHuman())
                return new Input_Attack();
        } else if (phase.equals(Constant.Phase.Combat_Declare_Blockers)) {
            AllZone.getStack().freezeStack();
            if (playerTurn.isHuman()) {
                AllZone.getComputer().getComputer().declare_blockers();
                return null;
            } else {
                if (AllZone.getCombat().getAttackers().length == 0) {
                    // no active attackers, skip the Blocking phase
                    AllZone.getPhase().setNeedToNextPhase(true);
                    return null;
                } else
                    return new Input_Block();
            }
        } else if (phase.equals(Constant.Phase.Cleanup))    // Player needs to discard
            if (AllZone.getStack().size() == 0)    // fall through to resolve things like Madness
                return new Input_Cleanup();

        // *********************
        // Special phases handled above, everything else is handled simply by priority

        if (priority.isHuman()) {
            boolean skip = AllZone.getPhase().doSkipPhase();
            AllZone.getPhase().setSkipPhase(false);
            if (AllZone.getStack().size() == 0 && !AllZone.getDisplay().stopAtPhase(playerTurn, phase) && skip) {
                AllZone.getPhase().passPriority();
                return null;
            } else
                return new Input_PassPriority();
        } else if (playerTurn.isComputer())
            return AllZone.getComputer();
        else {
            AllZone.getComputer().getComputer().stack_not_empty();
            return null;
        }
    }//getInput()
}//InputControl
