package forge;

import com.esotericsoftware.minlog.Log;

import forge.gui.input.Input;

/**
 * <p>
 * ComputerAI_Input class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ComputerAI_Input extends Input {
    /** Constant <code>serialVersionUID=-3091338639571662216L</code>. */
    private static final long serialVersionUID = -3091338639571662216L;

    private final Computer computer;

    /**
     * <p>
     * Constructor for ComputerAI_Input.
     * </p>
     * 
     * @param iComputer
     *            a {@link forge.Computer} object.
     */
    public ComputerAI_Input(final Computer iComputer) {
        computer = iComputer;
    }

    // wrapper method that ComputerAI_StackNotEmpty class calls
    // ad-hoc way for ComptuerAI_StackNotEmpty to get to the Computer class
    /**
     * <p>
     * stackNotEmpty.
     * </p>
     */
    public final void stackNotEmpty() {
        computer.stackNotEmpty();
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        /*
         * //put this back in ButtonUtil.disableAll();
         * AllZone.getDisplay().showMessage("Phase: " +
         * AllZone.getPhase().getPhase() + "\nAn error may have occurred. Please
         * send the \"Stack Report\" and the
         * \"Detailed Error Trace\" to the Forge forum.");
         */
        think();
    } // getMessage();

    /**
     * <p>
     * Getter for the field <code>computer</code>.
     * </p>
     * 
     * @return a {@link forge.Computer} object.
     */
    public final Computer getComputer() {
        return computer;
    }

    /**
     * <p>
     * think.
     * </p>
     */
    private void think() {
        // TODO instead of setNextPhase, pass priority
        final String phase = AllZone.getPhase().getPhase();

        if (AllZone.getStack().size() > 0) {
            computer.stackNotEmpty();
        } else if (phase.equals(Constant.Phase.MAIN1)) {
            Log.debug("Computer main1");
            computer.main1();
        } else if (phase.equals(Constant.Phase.COMBAT_BEGIN)) {
            computer.beginCombat();
        } else if (phase.equals(Constant.Phase.COMBAT_DECLARE_ATTACKERS)) {
            computer.declareAttackers();
        } else if (phase.equals(Constant.Phase.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)) {
            computer.declareAttackersAfter();
        } else if (phase.equals(Constant.Phase.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
            computer.declareBlockersAfter();
        } else if (phase.equals(Constant.Phase.COMBAT_END)) {
            computer.endOfCombat();
        } else if (phase.equals(Constant.Phase.MAIN2)) {
            Log.debug("Computer main2");
            computer.main2();
        } else {
            computer.stackNotEmpty();
        }

    } // think
}
