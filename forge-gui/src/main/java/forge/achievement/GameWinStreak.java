package forge.achievement;

import forge.game.Game;
import forge.game.player.Player;

public class GameWinStreak extends StreakAchievement {
    public GameWinStreak(int bronze0, int silver0, int gold0, int mythic0) {
        super("GameWinStreak", "Game Win Streak", null,
            String.format("Win %d games in a row", bronze0), bronze0,
            String.format("Win %d games in a row", silver0), silver0,
            String.format("Win %d games in a row", gold0), gold0,
            String.format("Win %d games in a row", mythic0), mythic0);
    }

    @Override
    protected Boolean eval(Player player, Game game) {
        return player.getOutcome().hasWon();
    }
}
