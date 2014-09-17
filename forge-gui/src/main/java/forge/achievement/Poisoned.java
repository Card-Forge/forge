package forge.achievement;

import forge.game.Game;
import forge.game.player.GameLossReason;
import forge.game.player.Player;

public class Poisoned extends Achievement {
    public Poisoned(int bronze0, int silver0, int gold0) {
        super("Poisoned",
            String.format("Win a game by giving opponent %d poison counters.", bronze0), bronze0,
            String.format("Win a game by giving opponent %d poison counters.", silver0), silver0,
            String.format("Win a game by giving opponent %d poison counters.", gold0), gold0);
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            Player opponent = getSingleOpponent(player);
            if (opponent != null && opponent.getOutcome().lossState == GameLossReason.Poisoned) {
                return opponent.getPoisonCounters();
            }
        }
        return 0;
    }

    @Override
    public String getSubTitle() {
        if (best > 0) {
            return "Best: " + best + " Counters";
        }
        return null;
    }
}
