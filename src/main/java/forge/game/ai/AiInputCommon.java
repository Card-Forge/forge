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
package forge.game.ai;

import forge.control.input.InputPassPriorityBase;

/**
 * <p>
 * ComputerAI_Input class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AiInputCommon extends InputPassPriorityBase implements AiInput {
    /** Constant <code>serialVersionUID=-3091338639571662216L</code>. */
    private static final long serialVersionUID = -3091338639571662216L;

    private final AiController computer;

    /**
     * <p>
     * Constructor for ComputerAI_Input.
     * </p>
     * 
     * @param iComputer
     *            a {@link forge.game.player.Computer} object.
     */
    public AiInputCommon(final AiController iComputer) {
        super(iComputer.getPlayer());
        this.computer = iComputer;
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        // should not think when the game is over
        if (computer.getGame().isGameOver()) {
            return;
        }

        showMessage(getTurnPhasePriorityMessage());
        
        /*
         * //put this back in ButtonUtil.disableAll();
         * AllZone.getDisplay().showMessage("Phase: " +
         * Singletons.getModel().getGameState().getPhaseHandler().getPhase() +
         * "\nAn error may have occurred. Please send the \"Stack Report\" and
         * the \"Detailed Error Trace\" to the Forge forum.");
         */

        computer.getGame().getInputQueue().LockAndInvokeGameAction(aiActions);

    } // getMessage();

    final Runnable aiActions = new Runnable() {

        @Override
        public void run() {
            computer.onPriorityRecieved();
            pass();
        }
    };
}
