package forge.quest.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.game.GameSummary;

/**
 * <p>
 * QuestMatchState class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */

public class QuestMatchState {

    /** The Constant GAMES_PER_MATCH. */
    public static final int GAMES_PER_MATCH = 3;

    /** The Constant MIN_GAMES_TO_WIN_MATCH. */
    public static final int MIN_GAMES_TO_WIN_MATCH = 2;

    private final List<GameSummary> gamesPlayed = new ArrayList<GameSummary>();

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
    public final GameSummary[] getGamesPlayed() {
        return this.gamesPlayed.toArray(new GameSummary[this.gamesPlayed.size()]);
    }

    /**
     * Gets the games played count.
     * 
     * @return the games played count
     */
    public final int getGamesPlayedCount() {
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
     * @return true, if is match over
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

        return (maxWins >= QuestMatchState.MIN_GAMES_TO_WIN_MATCH) || (totalGames >= QuestMatchState.GAMES_PER_MATCH);
    }

    /**
     * Count games won by.
     * 
     * @param name
     *            the name
     * @return the int
     */
    public final int countGamesWonBy(final String name) {
        int wins = 0;
        for (final GameSummary game : this.gamesPlayed) {
            if (game.isWinner(name)) {
                wins++;
            }
        }
        return wins;
    }

    /**
     * Checks if is match won by.
     * 
     * @param name
     *            the name
     * @return true, if is match won by
     */
    public final boolean isMatchWonBy(final String name) {
        return this.countGamesWonBy(name) >= QuestMatchState.MIN_GAMES_TO_WIN_MATCH;
    }

    /**
     * Reset.
     */
    public final void reset() {
        this.gamesPlayed.clear();
    }

}
