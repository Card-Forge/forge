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
package forge.limited;

import java.util.ArrayList;
import java.util.List;

import forge.GuiBase;
import forge.deck.Deck;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.match.HostedMatch;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.util.Aggregates;

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
    private HostedMatch hostedMatch = null;
    private int rounds;
    private Deck humanDeck;
    private int currentRound;
    private int wins;
    private int losses;
    private boolean gauntletDraft; // Means: Draft game is in Gauntlet-mode, not a single match
    private GameType gauntletType;
    private final List<RegisteredPlayer> aiOpponents = new ArrayList<RegisteredPlayer>();

    public GauntletMini() {
        currentRound = 1;
        gauntletDraft = false;
        wins = 0;
        losses = 0;
        gauntletType = GameType.Sealed; // Assignable in launch();
    }

    /**
     * Resets the tournament.
     */
    public void resetCurrentRound() {
        wins = 0;
        losses = 0;
        currentRound = 1;
    }


    /**
     * Advances the tournament to the next round.
     */
    public void nextRound() {
        System.out.println("Moving from round " + currentRound + " to round " +  (currentRound + 1) + " of " + rounds);
        if (hostedMatch == null) {
            throw new IllegalStateException("Cannot advance round when no match has been hosted.");
        }

        if (currentRound >= rounds) {
            currentRound = rounds - 1;
            return;
        }

        currentRound++;
        hostedMatch.endCurrentGame();
        startRound();
    }

    /**
     *
     * Setup and launch the gauntlet.
     * Note: The AI decks are connected to the human deck.
     *
     * @param rounds0
     *          the number of rounds (opponent decks) in this tournament
     * @param humanDeck0
     *          the human deck for this tournament
     * @param gauntletType0
     *          game type (Sealed, Draft, Constructed...)
     */
    public void launch(final int rounds0, final Deck humanDeck0, final GameType gauntletType0) {
        rounds = rounds0;
        humanDeck = humanDeck0;
        gauntletType = gauntletType0;
        List<Deck> aiDecks;
        if (gauntletType == GameType.Sealed) {
            aiDecks = FModel.getDecks().getSealed().get(humanDeck.getName()).getAiDecks();
        }
        else if (gauntletType == GameType.Draft) {
            gauntletDraft = true;
            aiDecks = FModel.getDecks().getDraft().get(humanDeck.getName()).getAiDecks();
        }
        else {
            throw new IllegalStateException("Cannot launch Gauntlet, game mode not implemented.");
        }
        aiOpponents.clear();

        if (rounds == 1) { //play random opponent if only playing one round
            aiOpponents.add(new RegisteredPlayer(aiDecks.get(Aggregates.randomInt(0, aiDecks.size() - 1))));
        }
        else { //otherwise play opponents in order
            if (rounds > aiDecks.size()) {
                rounds = aiDecks.size(); //don't allow playing same opponent twice
            }
            for (int i = 0; i < rounds; i++) {
                aiOpponents.add(new RegisteredPlayer(aiDecks.get(i)));
            }
        }

        resetCurrentRound();
        startRound();
    }

    /**
     * Starts the tournament.
     */
    private void startRound() {
        final List<RegisteredPlayer> starter = new ArrayList<RegisteredPlayer>();
        final RegisteredPlayer human = new RegisteredPlayer(humanDeck).setPlayer(GamePlayerUtil.getGuiPlayer());
        starter.add(human);
        starter.add(aiOpponents.get(currentRound - 1).setPlayer(GamePlayerUtil.createAiPlayer()));

        hostedMatch = GuiBase.getInterface().hostMatch();
        hostedMatch.startMatch(gauntletType, null, starter, human, GuiBase.getInterface().getNewGuiGame());
    }

    /**
     * Returns the total number of rounds in the tournament.
     * @return int, number of rounds in the tournament
     */
    public final int getRounds() {
        return rounds;
    }

    /**
     * Returns the number of the current round in the tournament.
     * @return int, number of rounds in the tournament
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

    /**
     * Resets the gauntletDraft value.
     */
    public void resetGauntletDraft() {
        gauntletDraft = false;
    }

    /**
     * Draft mode status.
     * @return boolean, gauntletDraft
     */
    public final boolean isGauntletDraft() {
        return gauntletDraft;
    }

}

