package forge.gui.input;

import java.util.LinkedList;
import java.util.Stack;

import forge.ComputerAI_Input;
import forge.Constant;
import forge.MyObservable;
import forge.Phase;
import forge.Player;
import forge.model.FModel;

/**
 * <p>
 * InputControl class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class InputControl extends MyObservable implements java.io.Serializable {
    /** Constant <code>serialVersionUID=3955194449319994301L</code>. */
    private static final long serialVersionUID = 3955194449319994301L;

    private Input input;

    /** Constant <code>n=0</code>. */
    static int n = 0;
    private Stack<Input> inputStack = new Stack<Input>();
    private Stack<Input> resolvingStack = new Stack<Input>();
    private LinkedList<Input> resolvingQueue = new LinkedList<Input>();

    private final FModel model;
    private ComputerAI_Input aiInput; // initialized at runtime to be the latest
                                      // object created

    /**
     * TODO Write javadoc for Constructor.
     * 
     * @param fModel
     *            the f model
     */
    public InputControl(final FModel fModel) {
        model = fModel;
    }

    /**
     * <p>
     * Setter for the field <code>input</code>.
     * </p>
     * 
     * @param in
     *            a {@link forge.gui.input.Input} object.
     */
    public final void setInput(final Input in) {
        if (model.getGameState().getStack().getResolving() || !(input == null || input instanceof Input_PassPriority)) {
            inputStack.add(in);
        } else {
            input = in;
        }
        updateObservers();
    }

    /**
     * <p>
     * Setter for the field <code>input</code>.
     * </p>
     * 
     * @param in
     *            a {@link forge.gui.input.Input} object.
     * @param bAddToResolving
     *            a boolean.
     */
    public final void setInput(final Input in, final boolean bAddToResolving) {
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
     * <p>
     * changeInput.
     * </p>
     * 
     * @param in
     *            a {@link forge.gui.input.Input} object.
     */
    private void changeInput(final Input in) {
        input = in;
        updateObservers();
    }

    /**
     * <p>
     * Getter for the field <code>input</code>.
     * </p>
     * 
     * @return a {@link forge.gui.input.Input} object.
     */
    public final Input getInput() {
        return input;
    }

    /**
     * <p>
     * clearInput.
     * </p>
     */
    public final void clearInput() {
        input = null;
        resolvingQueue.clear();
        inputStack.clear();
    }

    /**
     * <p>
     * resetInput.
     * </p>
     */
    public final void resetInput() {
        input = null;
        updateObservers();
    }

    /**
     * <p>
     * resetInput.
     * </p>
     * 
     * @param update
     *            a boolean.
     */
    public final void resetInput(final boolean update) {
        input = null;
        if (update) {
            updateObservers();
        }
    }

    /**
     * <p>
     * updateInput.
     * </p>
     * 
     * @return a {@link forge.gui.input.Input} object.
     */
    public final Input updateInput() {
        final String phase = model.getGameState().getPhase().getPhase();
        final Player playerTurn = model.getGameState().getPhase().getPlayerTurn();
        final Player priority = model.getGameState().getPhase().getPriorityPlayer();

        // TODO this resolving portion needs more work, but fixes Death Cloud
        // issues
        if (resolvingStack.size() > 0) {
            if (input != null) {
                return input;
            }

            // if an SA is resolving, only change input for something that is
            // part of the resolving SA
            changeInput(resolvingStack.pop());
            return input;
        }

        if (model.getGameState().getStack().getResolving()) {
            return null;
        }

        if (input != null) {
            return input;
        } else if (inputStack.size() > 0) { // incoming input to Control
            changeInput(inputStack.pop());
            return input;
        }

        if (Phase.getGameBegins() != 0 && model.getGameState().getPhase().doPhaseEffects()) {
            // Handle begin phase stuff, then start back from the top
            model.getGameState().getPhase().handleBeginPhase();
            return updateInput();
        }

        // If the Phase we're in doesn't allow for Priority, return null to move
        // to next phase
        if (model.getGameState().getPhase().isNeedToNextPhase()) {
            return null;
        }

        // Special Inputs needed for the following phases:
        if (phase.equals(Constant.Phase.Combat_Declare_Attackers)) {
            model.getGameState().getStack().freezeStack();

            if (playerTurn.isHuman()) {
                return new Input_Attack();
            }
        } else if (phase.equals(Constant.Phase.Combat_Declare_Blockers)) {
            model.getGameState().getStack().freezeStack();
            if (playerTurn.isHuman()) {
                aiInput.getComputer().declare_blockers();
                return null;
            } else {
                if (model.getGameState().getCombat().getAttackers().length == 0) {
                    // no active attackers, skip the Blocking phase
                    model.getGameState().getPhase().setNeedToNextPhase(true);
                    return null;
                } else {
                    return new Input_Block();
                }
            }
        } else if (phase.equals(Constant.Phase.Cleanup)) {
            // discard
            if (model.getGameState().getStack().size() == 0) {
                // resolve things
                                                             // like Madness
                return new Input_Cleanup();
            }
        }

        // *********************
        // Special phases handled above, everything else is handled simply by
        // priority

        if (priority.isHuman()) {
            boolean skip = model.getGameState().getPhase().doSkipPhase();
            model.getGameState().getPhase().setSkipPhase(false);
            if (model.getGameState().getStack().size() == 0
                    && !forge.AllZone.getDisplay().stopAtPhase(playerTurn, phase) && skip) {
                model.getGameState().getPhase().passPriority();
                return null;
            } else {
                return new Input_PassPriority();
            }
        } else if (playerTurn.isComputer()) {
            return aiInput;
        } else {
            aiInput.getComputer().stack_not_empty();
            return null;
        }
    }// getInput()

    /**
     * Sets the computer.
     * 
     * @param computerAI_Input
     *            the new computer
     */
    public final void setComputer(final ComputerAI_Input computerAI_Input) {
        aiInput = computerAI_Input;
    }
}// InputControl
