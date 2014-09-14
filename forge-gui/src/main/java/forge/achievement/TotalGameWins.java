package forge.achievement;

import forge.game.Game;
import forge.game.player.Player;

public class TotalGameWins extends Achievement {
    public TotalGameWins(int bronze0, int silver0, int gold0) {
        super("Total Game Wins", false, true,
            String.format("Win %d games.", bronze0), bronze0,
            String.format("Win %d games.", silver0), silver0,
            String.format("Win %d games.", gold0), gold0);
    }

    @Override
    protected int evaluate(Player player, Game game, int current) {
        if (player.getOutcome().hasWon()) {
            return current + 1;
        }
        return current;
    }
}
