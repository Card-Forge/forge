package forge.achievement;

import forge.game.Game;
import forge.game.player.Player;

public class LifeToSpare extends Achievement {
    public LifeToSpare(int bronze0, int silver0, int gold0, int mythic0) {
        super("LifeToSpare", "Life to Spare", "Win a game with", 0,
            String.format("%d life more than you started with", bronze0), bronze0,
            String.format("%d life more than you started with", silver0), silver0,
            String.format("%d life more than you started with", gold0), gold0,
            String.format("%d life more than you started with", mythic0), mythic0);
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            int gainedLife = player.getLife() - player.getStartingLife();
            if (gainedLife > 0) {
                return gainedLife;
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
}
