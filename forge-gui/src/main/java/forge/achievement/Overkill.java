package forge.achievement;

import forge.game.Game;
import forge.game.player.Player;

public class Overkill extends Achievement {
    public Overkill(int bronze0, int silver0, int gold0, int mythic0) {
        super("Overkill", "Overkill", "Win a game with opponent at", 0,
            String.format("%d life", bronze0), bronze0,
            String.format("%d life", silver0), silver0,
            String.format("%d life", gold0), gold0,
            String.format("%d life", mythic0), mythic0);
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            Player opponent = player.getSingleOpponent();
            if (opponent != null && opponent.getLife() < 0) {
                return opponent.getLife();
            }
        }
        return 0;
    }

    @Override
    protected String getNoun() {
        return "Life";
    }
    @Override
    protected boolean pluralizeNoun() {
        return false;
    }

    @Override
    protected int performConversion(int value, long timestamp) {
        //perform conversion to handle data from old format before supporting negative thresholds
        if (value > 0) {
            value = -value;
        }
        return value;
    }
}
