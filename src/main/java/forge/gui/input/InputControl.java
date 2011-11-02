package forge.gui.input;

import java.util.LinkedList;
import java.util.Stack;

import forge.ComputerAIInput;
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
    private static int n = 0;
    private final Stack<Input> inputStack = new Stack<Input>();
    private final Stack<Input> resolvingStack = new Stack<Input>();
    private final LinkedList<Input> resolvingQueue = new LinkedList<Input>();

    private final FModel model;
    private ComputerAIInput aiInput; // initialized at runtime to be the latest
                                      // object created

    /**
     * TODO Write javadoc for Constructor.
     * 
     * @param fModel
     *            the f model
     */
    public InputControl(final FModel fModel) {
        this.model = fModel;
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
        if (this.model.getGameState().getStack().getResolving()
                || !((this.input == null) || (this.input instanceof InputPassPriority))) {
            this.inputStack.add(in);
        } else {
            this.input = in;
        }
        this.updateObservers();
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
            this.setInput(in);
            return;
        }

        final Input old = this.input;
        this.resolvingStack.add(old);
        this.changeInput(in);
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
        this.input = in;
        this.updateObservers();
    }

    /**
     * <p>
     * Getter for the field <code>input</code>.
     * </p>
     * 
     * @return a {@link forge.gui.input.Input} object.
     */
    public final Input getInput() {
        return this.input;
    }

    /**
     * <p>
     * clearInput.
     * </p>
     */
    public final void clearInput() {
        this.input = null;
        this.resolvingQueue.clear();
        this.inputStack.clear();
    }

    /**
     * <p>
     * resetInput.
     * </p>
     */
    public final void resetInput() {
        this.input = null;
        this.updateObservers();
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
        this.input = null;
        if (update) {
            this.updateObservers();
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
        final String phase = this.model.getGameState().getPhase().getPhase();
        final Player playerTurn = this.model.getGameState().getPhase().getPlayerTurn();
        final Player priority = this.model.getGameState().getPhase().getPriorityPlayer();

        // TODO this resolving portion needs more work, but fixes Death Cloud
        // issues
        if (this.resolvingStack.size() > 0) {
            if (this.input != null) {
                return this.input;
            }

            // if an SA is resolving, only change input for something that is
            // part of the resolving SA
            this.changeInput(this.resolvingStack.pop());
            return this.input;
        }

        if (this.model.getGameState().getStack().getResolving()) {
            return null;
        }

        if (this.input != null) {
            return this.input;
        } else if (this.inputStack.size() > 0) { // incoming input to Control
            this.changeInput(this.inputStack.pop());
            return this.input;
        }

        if ((Phase.getGameBegins() != 0) && this.model.getGameState().getPhase().doPhaseEffects()) {
            // Handle begin phase stuff, then start back from the top
            this.model.getGameState().getPhase().handleBeginPhase();
            return this.updateInput();
        }

        // If the Phase we're in doesn't allow for Priority, return null to move
        // to next phase
        if (this.model.getGameState().getPhase().isNeedToNextPhase()) {
            return null;
        }

        // Special Inputs needed for the following phases:
        if (phase.equals(Constant.Phase.COMBAT_DECLARE_ATTACKERS)) {
            this.model.getGameState().getStack().freezeStack();

            if (playerTurn.isHuman()) {
                return new InputAttack();
            }
        } else if (phase.equals(Constant.Phase.COMBAT_DECLARE_BLOCKERS)) {
            this.model.getGameState().getStack().freezeStack();
            if (playerTurn.isHuman()) {
                this.aiInput.getComputer().declareBlockers();
                return null;
            } else {
                if (this.model.getGameState().getCombat().getAttackers().length == 0) {
                    // no active attackers, skip the Blocking phase
                    this.model.getGameState().getPhase().setNeedToNextPhase(true);
                    return null;
                } else {
                    return new InputBlock();
                }
            }
        } else if (phase.equals(Constant.Phase.CLEANUP)) {
            // discard
            if (this.model.getGameState().getStack().size() == 0) {
                // resolve things
                // like Madness
                return new InputCleanup();
            }
        }

        // *********************
        // Special phases handled above, everything else is handled simply by
        // priority

        if (priority.isHuman()) {
            final boolean skip = this.model.getGameState().getPhase().doSkipPhase();
            this.model.getGameState().getPhase().setSkipPhase(false);
            if ((this.model.getGameState().getStack().size() == 0)
                    && !forge.AllZone.getDisplay().stopAtPhase(playerTurn, phase) && skip) {
                this.model.getGameState().getPhase().passPriority();
                return null;
            } else {
                return new InputPassPriority();
            }
        } else if (playerTurn.isComputer()) {
            return this.aiInput;
        } else {
            this.aiInput.getComputer().stackNotEmpty();
            return null;
        }
    } // getInput()

    /**
     * Sets the computer.
     * 
     * @param computerAI_Input
     *            the new computer
     */
    public final void setComputer(final ComputerAIInput computerAI_Input) {
        this.aiInput = computerAI_Input;
    }
} // InputControl
