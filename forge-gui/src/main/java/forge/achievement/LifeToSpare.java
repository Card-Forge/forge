package forge.achievement;

import forge.game.Game;
import forge.game.player.Player;

public class LifeToSpare extends Achievement {
    public LifeToSpare(int bronze0, int silver0, int gold0) {
        super("Life to Spare", true,
            String.format("Win game with %d life more than you started with.", bronze0), bronze0,
            String.format("Win game with %d life more than you started with.", silver0), silver0,
            String.format("Win game with %d life more than you started with.", gold0), gold0);
    }

    @Override
    protected int evaluate(Player player, Game game, int current) {
        if (player.getOutcome().hasWon()) {
            int gainedLife = player.getLife() - player.getStartingLife();
            if (gainedLife > 0) {
                return gainedLife;
            }
        }
        return 0;
    }
}
