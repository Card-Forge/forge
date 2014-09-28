package forge.achievement;

import forge.game.Game;
import forge.game.player.GameLossReason;
import forge.game.player.Player;

public class Poisoned extends Achievement {
    private static final int THRESHOLD = 10;

    public Poisoned(int silver0, int gold0, int mythic0) {
        super("Poisoned", "Poisoned", "Win a game by giving opponent", 0,
            String.format("%d poison counters", THRESHOLD), THRESHOLD,
            String.format("%d poison counters", silver0), silver0,
            String.format("%d poison counters", gold0), gold0,
            String.format("%d poison counters", mythic0), mythic0);
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            Player opponent = player.getSingleOpponent();
            if (opponent != null && opponent.getOutcome().lossState == GameLossReason.Poisoned) {
                return opponent.getPoisonCounters();
            }
        }
        return 0;
    }

    @Override
    protected String getNoun() {
        return "Counter";
    }
}
