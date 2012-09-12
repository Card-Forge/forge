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
package forge.game.limited;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import forge.Constant;
import forge.Singletons;
import forge.deck.Deck;
import forge.game.GameNew;
import forge.game.GameType;
import forge.gui.SOverlayUtils;

/**
 * <p>
 * GauntletMini class.
 * </p>
 * 
 * @author Forge
 * @version $Id: GauntletMini.java $
 * @since 1.2.xx
 */
public class GauntletMini {

    private int rounds;
    private Deck humanDeck;
    private int currentRound;
    private int wins;
    private int losses;

    // private final String humanName;
    /**
     * TODO: Write javadoc for Constructor.
     */
    public void gauntletMini() {
        currentRound = 1;
        wins = 0;
        losses = 0;
        // humanName = hName;
    }

    /**
     * 
     * Set the number of rounds in the tournament.
     * 
     * @param gameRounds
     *          the number of rounds in the mini tournament
     */

    public void setRounds(int gameRounds) {
        rounds = gameRounds;
    }

    /**
     * 
     * Chooses the human deck for the tournament.
     * Note: The AI decks are connected to the human deck.
     * 
     * @param hDeck
     *          the human deck for this tournament
     */
    public void setHumanDeck(Deck hDeck) {
        humanDeck = hDeck;
    }

    /**
     * Resets the tournament.
     */
    public void resetCurrentRound() {
        wins = 0;
        losses = 0;
        Constant.Runtime.HUMAN_DECK[0] = humanDeck;
        Constant.Runtime.COMPUTER_DECK[0] = Singletons.getModel().getDecks().getSealed().get(humanDeck.getName()).getAiDecks().get(0);
        currentRound = 1;
    }


    /**
     * Advances the tournamen to the next round.
     */
    public void nextRound() {

        // System.out.println("Moving from round " + currentRound + " to round " +  currentRound + 1 + " of " + rounds);
        if (currentRound >= rounds) {
            currentRound = rounds;
            return;
        }

        Constant.Runtime.HUMAN_DECK[0] = humanDeck;
        Constant.Runtime.COMPUTER_DECK[0] = Singletons.getModel().getDecks().getSealed().get(humanDeck.getName()).getAiDecks().get(currentRound);
        currentRound += 1;

    }

    /**
     * Starts the tournament.
     */
    public void launch() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.startGameOverlay();
                SOverlayUtils.showOverlay();
            }
        });

        final SwingWorker<Object, Void> worker = new SwingWorker<Object, Void>() {
            @Override

            public Object doInBackground() {

                Constant.Runtime.setGameType(GameType.Sealed);

                GameNew.newGame(Constant.Runtime.HUMAN_DECK[0], Constant.Runtime.COMPUTER_DECK[0]);

                return null;
            }

            @Override
            public void done() {
                SOverlayUtils.hideOverlay();
            }
        };
        worker.execute();
    }


    /**
     * Returns the total number of rounds in the tournament.
     * @return int, number of rounds in the Sealed Deck tournament
     */
    public final int getRounds() {
        return rounds;
    }

    /**
     * Returns the number of the current round in the tournament.
     * @return int, number of rounds in the Sealed Deck tournament
     */
    public final int getCurrentRound() {
        return currentRound;
    }

    /**
     * Adds a game win to the tournament statistics.
     */
    public void addWin() {
        wins++;
    }

    /**
     * Adds a game loss to the tournament statistics.
     */
    public void addLoss() {
        losses++;
    }

    /**
     * The total number of won games in this tournament.
     * @return int, number of wins
     */
    public final int getWins() {
        return wins;
    }

    /**
     * The total number of lost games in this tournament.
     * @return int, numer of losses
     */
    public final int getLosses() {
        return losses;
    }

}

