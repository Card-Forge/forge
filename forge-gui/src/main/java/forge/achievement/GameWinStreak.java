package forge.achievement;

import forge.assets.FSkinProp;
import forge.game.Game;
import forge.game.player.Player;

public class GameWinStreak extends Achievement {
    public GameWinStreak(int bronze0, int silver0, int gold0, int mythic0) {
        super("Game Win Streak", null,
            String.format("Win %d games in a row", bronze0), bronze0,
            String.format("Win %d games in a row", silver0), silver0,
            String.format("Win %d games in a row", gold0), gold0,
            String.format("Win %d games in a row", mythic0), mythic0,
            FSkinProp.IMG_GAME_WIN_STREAK);
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            return current + 1;
        }
        return 0; //reset if player didn't win
    }

    @Override
    public String getSubTitle() {
        return "Best: " + best + " Active: " + current;
    }
}
