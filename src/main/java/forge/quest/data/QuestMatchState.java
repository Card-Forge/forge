package forge.quest.data;

import java.util.ArrayList;
import java.util.List;

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

    public final boolean hasHumanWonLastGame() {
        int iLastGame = gamesPlayed.size() - 1;
        return iLastGame >= 0 ? gamesPlayed.get(iLastGame).isHumanWinner() : false;
    }

    public final boolean isMatchOver() {
        int winsHuman = 0;
        int winsAI = 0;
        int totalGames = 0;

        for (GameSummary game : gamesPlayed) {
            if (game.isAIWinner()) { winsAI++; }
            if (game.isHumanWinner()) { winsHuman++; }
            totalGames++;
        }

        return winsAI >= MIN_GAMES_TO_WIN_MATCH || winsHuman >= MIN_GAMES_TO_WIN_MATCH || totalGames >= GAMES_PER_MATCH;
    }

    public final int getGamesCountWonByHuman() {
        int winsHuman = 0;
        for (GameSummary game : gamesPlayed) {
            if (game.isHumanWinner()) { winsHuman++; }
        }
        return winsHuman;
    }

    public final int getGamesCountLostByHuman() {
        int lossHuman = 0;
        for (GameSummary game : gamesPlayed) {
            if (!game.isHumanWinner()) { lossHuman++; }
        }
        return lossHuman;
    }

    public final boolean isMatchWonByHuman() {
        return getGamesCountWonByHuman() >= MIN_GAMES_TO_WIN_MATCH;
    }

    public final void reset() {
        gamesPlayed.clear();
    }

}
