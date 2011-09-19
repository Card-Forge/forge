package forge.quest.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.game.GameSummary;


/**
 * <p>QuestMatchState class.</p>
 *
 * @author Forge
 * @version $Id$
 */


public class QuestMatchState {
    public static final int GAMES_PER_MATCH = 3;
    public static final int MIN_GAMES_TO_WIN_MATCH = 2;

    private List<GameSummary> gamesPlayed = new ArrayList<GameSummary>();
    //ArrayList<GameSpecialConditions>

    public final void addGamePlayed(final GameSummary completedGame) {
        gamesPlayed.add(completedGame);
    }

    public final GameSummary[] getGamesPlayed() {
        return gamesPlayed.toArray(new GameSummary[gamesPlayed.size()]);
    }

    public final int getGamesPlayedCount() {
        return gamesPlayed.size();
    }

    public final boolean hasWonLastGame(String playerName) {
        int iLastGame = gamesPlayed.size() - 1;
        return iLastGame >= 0 ? gamesPlayed.get(iLastGame).isWinner(playerName) : false;
    }

    public final boolean isMatchOver() {
        int totalGames = 0;

        Map<String, Integer> winsCount = new HashMap<String, Integer>();
        for (GameSummary game : gamesPlayed) {
            String winner = game.getWinner();
            Integer boxedWins = winsCount.get(winner);
            int wins = boxedWins == null ? 0 : boxedWins.intValue();
            winsCount.put(winner, wins + 1);
            totalGames++;
        }
        
        int maxWins = 0; 
        for (Integer win : winsCount.values()) {
            maxWins = Math.max(maxWins, win);
        }

        return maxWins >= MIN_GAMES_TO_WIN_MATCH || totalGames >= GAMES_PER_MATCH;
    }

    public final int countGamesWonBy(String name) {
        int wins = 0;
        for (GameSummary game : gamesPlayed) {
            if (game.isWinner(name)) { wins++; }
        }
        return wins;
    }

    public final boolean isMatchWonBy(String name) {
        return countGamesWonBy(name) >= MIN_GAMES_TO_WIN_MATCH;
    }

    public final void reset() {
        gamesPlayed.clear();
    }

}
