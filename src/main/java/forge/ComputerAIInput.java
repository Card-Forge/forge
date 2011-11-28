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
public class ComputerAIInput extends Input {
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
    public ComputerAIInput(final Computer iComputer) {
        this.computer = iComputer;
    }

    // wrapper method that ComputerAI_StackNotEmpty class calls
    // ad-hoc way for ComptuerAI_StackNotEmpty to get to the Computer class
    /**
     * <p>
     * stackNotEmpty.
     * </p>
     */
    public final void stackNotEmpty() {
        this.computer.stackNotEmpty();
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
        this.think();
    } // getMessage();

    /**
     * <p>
     * Getter for the field <code>computer</code>.
     * </p>
     * 
     * @return a {@link forge.Computer} object.
     */
    public final Computer getComputer() {
        return this.computer;
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
            this.computer.stackNotEmpty();
        } else if (phase.equals(Constant.Phase.MAIN1)) {
            Log.debug("Computer main1");
            this.computer.main1();
        } else if (phase.equals(Constant.Phase.COMBAT_BEGIN)) {
            this.computer.beginCombat();
        } else if (phase.equals(Constant.Phase.COMBAT_DECLARE_ATTACKERS)) {
            this.computer.declareAttackers();
        } else if (phase.equals(Constant.Phase.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)) {
            this.computer.declareAttackersAfter();
        } else if (phase.equals(Constant.Phase.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
            this.computer.declareBlockersAfter();
        } else if (phase.equals(Constant.Phase.COMBAT_END)) {
            this.computer.endOfCombat();
        } else if (phase.equals(Constant.Phase.MAIN2)) {
            Log.debug("Computer main2");
            this.computer.main2();
        } else {
            this.computer.stackNotEmpty();
        }

    } // think
}
