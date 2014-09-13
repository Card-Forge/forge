package forge.achievement;

import forge.game.Game;
import forge.game.player.Player;

public class Overkill extends Achievement {
    public Overkill(int bronze0, int silver0, int gold0) {
        super("Overkill", true,
            String.format("Win game with opponent at -%d life or less.", bronze0), bronze0,
            String.format("Win game with opponent at -%d life or less.", silver0), silver0,
            String.format("Win game with opponent at -%d life or less.", gold0), gold0);
    }

    @Override
    protected int evaluate(Player player, Game game, int current) {
        if (player.getOutcome().hasWon()) {
            Player opponent = getSingleOpponent(player);
            if (opponent != null && opponent.getLife() < 0) {
                return -opponent.getLife();
            }
        }
        return 0;
    }
}
