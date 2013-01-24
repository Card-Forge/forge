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
package forge.game.player;

import java.util.List;

import com.esotericsoftware.minlog.Log;

import forge.Singletons;
import forge.card.spellability.SpellAbility;
import forge.control.input.Input;
import forge.game.GameState;
import forge.game.phase.PhaseType;

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
     *            a {@link forge.game.player.Computer} object.
     */
    public ComputerAIInput(final Computer iComputer) {
        this.computer = iComputer;
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        // should not think when the game is over
        if (Singletons.getModel().getGame().isGameOver()) {
            return;
        }

        /*
         * //put this back in ButtonUtil.disableAll();
         * AllZone.getDisplay().showMessage("Phase: " +
         * Singletons.getModel().getGameState().getPhaseHandler().getPhase() + "\nAn error may have occurred. Please
         * send the \"Stack Report\" and the
         * \"Detailed Error Trace\" to the Forge forum.");
         */
        GameState game = Singletons.getModel().getGame();
        final PhaseType phase = game.getPhaseHandler().getPhase();
        
        if (game.getStack().size() > 0) {
            playSpellAbilities(game);
        } else {
            switch(phase) {
                case COMBAT_DECLARE_ATTACKERS:
                    this.computer.declareAttackers();
                    
                    break;

                case MAIN1:
                case MAIN2:
                    Log.debug("Computer " + phase.toString());
                    this.computer.playLands();
                    // fall through is intended
                default:
                    playSpellAbilities(game);
                    break;
            }
        }
        game.getPhaseHandler().passPriority();
    } // getMessage();

    protected void playSpellAbilities(final GameState game)
    {
        Player ai = computer.getPlayer();
        List<SpellAbility> toPlay = computer.getSpellAbilitiesToPlay();
        if ( toPlay != null ) {
            for(SpellAbility sa : toPlay) {
                //System.out.print(sa);
                if (ComputerUtil.canBePlayedAndPayedByAI(ai, sa))
                    ComputerUtil.handlePlayingSpellAbility(ai, sa, game);
            }
        }
    }
    
    /**
     * <p>
     * Getter for the field <code>computer</code>.
     * </p>
     * 
     * @return a {@link forge.game.player.Computer} object.
     */
    public final Computer getComputer() {
        return this.computer;
    }

    /* (non-Javadoc)
     * @see forge.control.input.Input#isClassUpdated()
     */
    @Override public void isClassUpdated() { }
}
