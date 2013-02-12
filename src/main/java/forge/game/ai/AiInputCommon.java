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

import java.util.List;

import com.esotericsoftware.minlog.Log;

import forge.Card;
import forge.card.spellability.SpellAbility;
import forge.control.input.Input;
import forge.game.GameState;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * <p>
 * ComputerAI_Input class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AiInputCommon extends Input {
    /** Constant <code>serialVersionUID=-3091338639571662216L</code>. */
    private static final long serialVersionUID = -3091338639571662216L;

    private final AiController computer;
    private final AIPlayer player; 
    private final GameState game;

    /**
     * <p>
     * Constructor for ComputerAI_Input.
     * </p>
     * 
     * @param iComputer
     *            a {@link forge.game.player.Computer} object.
     */
    public AiInputCommon(final AiController iComputer) {
        this.computer = iComputer;
        player = computer.getPlayer();
        this.game = computer.getGame();
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        // should not think when the game is over
        if (game.isGameOver()) {
            return;
        }

        /*
         * //put this back in ButtonUtil.disableAll();
         * AllZone.getDisplay().showMessage("Phase: " +
         * Singletons.getModel().getGameState().getPhaseHandler().getPhase() + "\nAn error may have occurred. Please
         * send the \"Stack Report\" and the
         * \"Detailed Error Trace\" to the Forge forum.");
         */
        
        final PhaseType phase = game.getPhaseHandler().getPhase();
        
        if (game.getStack().size() > 0) {
            playSpellAbilities(game);
        } else {
            switch(phase) {
                case CLEANUP:
                    if ( game.getPhaseHandler().getPlayerTurn() == player ) {
                        final int size = player.getCardsIn(ZoneType.Hand).size();
                        
                        if (!player.isUnlimitedHandSize()) {
                            final int numDiscards = size - player.getMaxHandSize();
                            player.discard(numDiscards, null);
                        }
                    }
                    break;

                case COMBAT_DECLARE_ATTACKERS:
                    declareAttackers();
                    break;

                case MAIN1:
                case MAIN2:
                    Log.debug("Computer " + phase.toString());
                    playLands();
                    // fall through is intended
                default:
                    playSpellAbilities(game);
                    break;
            }
        }
        player.getController().passPriority();
    } // getMessage();

    /**
     * TODO: Write javadoc for this method.
     */
    private void declareAttackers() {
        // 12/2/10(sol) the decision making here has moved to getAttackers()
        game.setCombat(new AiAttackController(player, player.getOpponent()).getAttackers());

        final List<Card> att = game.getCombat().getAttackers();
        if (!att.isEmpty()) {
            game.getPhaseHandler().setCombat(true);
        }

        for (final Card element : att) {
            // tapping of attackers happens after Propaganda is paid for
            final StringBuilder sb = new StringBuilder();
            sb.append("Computer just assigned ").append(element.getName()).append(" as an attacker.");
            Log.debug(sb.toString());
        }

        player.getZone(ZoneType.Battlefield).updateObservers();

        game.getPhaseHandler().setPlayersPriorityPermission(false);

        // ai is about to attack, cancel all phase skipping
        for (Player p : game.getPlayers()) {
            p.getController().autoPassCancel();
        }
    }

    /**
     * TODO: Write javadoc for this method.
     */
    private void playLands() {
        final Player player = computer.getPlayer();
        List<Card> landsWannaPlay = computer.getLandsToPlay();
        
        while(landsWannaPlay != null && !landsWannaPlay.isEmpty() && player.canPlayLand(null)) {
            Card land = computer.chooseBestLandToPlay(landsWannaPlay);
            landsWannaPlay.remove(land);
            player.playLand(land);
            game.getPhaseHandler().setPriority(player);
        }
    }

    protected void playSpellAbilities(final GameState game)
    {
        SpellAbility sa;
        do { 
            sa = computer.getSpellAbilityToPlay();
            if ( sa == null ) break;
            //System.out.println("Playing sa: " + sa);
            ComputerUtil.handlePlayingSpellAbility(player, sa, game);
        } while ( sa != null );
    }
    

    /* (non-Javadoc)
     * @see forge.control.input.Input#isClassUpdated()
     */
    @Override public void isClassUpdated() { }
}
