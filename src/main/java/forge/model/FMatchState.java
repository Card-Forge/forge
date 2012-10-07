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
package forge.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.AllZone;
import forge.Card;
import forge.game.GameSummary;
import forge.game.GameType;
import forge.game.player.Player;
import forge.item.CardDb;
import forge.item.CardPrinted;

/**
 * Represents state of match <i>as a whole</i> - that is, not
 * single games, but the entire set.
 * 
 * @author Forge
 * @version $Id$
 */

public class FMatchState {
    private GameType gameType = GameType.Constructed;
    private int gamesPerMatch = 3;
    private int gamesToWinMatch = 2;

    private final List<GameSummary> gamesPlayed = new ArrayList<GameSummary>();

    private final List<CardPrinted> antesWon = new ArrayList<CardPrinted>();
    private final List<CardPrinted> antesLost = new ArrayList<CardPrinted>();

    // ArrayList<GameSpecialConditions>

    /**
     * Adds the game played.
     * 
     * @param completedGame
     *            the completed game
     */
    public final void addGamePlayed(final GameSummary completedGame) {
        this.gamesPlayed.add(completedGame);
    }

    /**
     * Gets the games played.
     * 
     * @return the games played
     */
    public final List<GameSummary> getGamesPlayed() {
        return this.gamesPlayed;
    }

    /** @return int */
    public int getGamesPerMatch() {
        return gamesPerMatch;
    }

    /** @return int */
    public int getGamesToWinMatch() {
        return gamesToWinMatch;
    }

    /**
     * Gets the games played count.
     *
     * @return java.lang.Integer
     */
    public final Integer getGamesPlayedCount() {
        return this.gamesPlayed.size();
    }

    /**
     * Checks for won last game.
     * 
     * @param playerName
     *            the player name
     * @return true, if successful
     */
    public final boolean hasWonLastGame(final String playerName) {
        final int iLastGame = this.gamesPlayed.size() - 1;
        return iLastGame >= 0 ? this.gamesPlayed.get(iLastGame).isWinner(playerName) : false;
    }

    /**
     * Checks if is match over.
     * 
     * @return true, if match is over
     */
    public final boolean isMatchOver() {
        int totalGames = 0;

        final Map<String, Integer> winsCount = new HashMap<String, Integer>();
        for (final GameSummary game : this.gamesPlayed) {
            final String winner = game.getWinner();
            final Integer boxedWins = winsCount.get(winner);
            final int wins = boxedWins == null ? 0 : boxedWins.intValue();
            winsCount.put(winner, wins + 1);
            totalGames++;
        }

        int maxWins = 0;
        for (final Integer win : winsCount.values()) {
            maxWins = Math.max(maxWins, win);
        }

        return (maxWins >= this.gamesToWinMatch) || (totalGames >= this.gamesPerMatch);
    }

    /**
     * Count games won by.
     * 
     * @param player {@link forge.game.Player}
     * @return java.lang.Integer
     */
    public final int countGamesWonBy(final Player player) {
        int wins = 0;
        for (final GameSummary game : this.gamesPlayed) {
            if (game.isWinner(player.toString())) {
                wins++;
            }
        }
        return wins;
    }

    /**
     * Checks if is match won by.
     * 
     * @param player0
     *            the player
     * @return true, if is match won by
     */
    public final boolean isMatchWonBy(final Player player0) {
        return this.countGamesWonBy(player0) >= this.gamesToWinMatch;
    }

    /**
     * Adds a List<Card> to the antes that have already been won this match.
     *
     * @param antes cards won in ante
     * @return the list
     * @since 1.2.3
     */
    public final List<CardPrinted> addAnteWon(final List<Card> antes) {
        List<CardPrinted> antesPrinted = new ArrayList<CardPrinted>();
        for (Card ante : antes) {
            CardPrinted cp = CardDb.instance().getCard(ante.getName(), ante.getCurSetCode());
            antesWon.add(cp);
            antesPrinted.add(cp);
        }
        return antesPrinted;
    }

    /**
     * Gets a list of all cards won in ante during this match.
     * 
     * @return a list of cards won in ante this match
     */
    public final List<CardPrinted> getAnteWon() {
        return antesWon;
    }

    /**
     * Adds the ante cards won this match to the CardPool (and they get marker as NEW).
     * 
     * @since 1.2.3
     */
    public final void addAnteWonToCardPool() {
        AllZone.getQuest().getCards().addAllCards(antesWon);
    }

    /**
     * Adds a List<Card> to the antes that have already been lost this match.
     *
     * @param antes cards lost in ante
     * @return the list
     * @since 1.2.3
     */
    public final List<CardPrinted> addAnteLost(final List<Card> antes) {
        List<CardPrinted> antesPrinted = new ArrayList<CardPrinted>();
        for (Card ante : antes) {
            CardPrinted cp = CardDb.instance().getCard(ante.getName(), ante.getCurSetCode());
            antesLost.add(cp);
            antesPrinted.add(cp);
        }
        return antesPrinted;
    }

    /**
     * Reset.
     */
    public final void reset() {
        this.gamesPlayed.clear();
        this.antesWon.clear();
        this.antesLost.clear();
    }

    /** @return {@link forge.game.GameType} */
    public GameType getGameType() {
        return gameType;
    }

    /** @param gameType0 {@link forge.game.GameType} */
    public void setGameType(final GameType gameType0) {
        gameType = gameType0;
    }
}
