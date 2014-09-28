package forge.achievement;

import forge.game.Game;
import forge.game.player.Player;

public class NeedForSpeed extends Achievement {
    public NeedForSpeed(int bronze0, int silver0, int gold0, int mythic0) {
        super("NeedForSpeed", "Need for Speed", null, Integer.MAX_VALUE,
            String.format("Win a game by turn %d", bronze0), bronze0,
            String.format("Win a game by turn %d", silver0), silver0,
            String.format("Win a game by turn %d", gold0), gold0,
            String.format("Win a game by turn %d", mythic0), mythic0);
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            return player.getTurn();
        }
        return defaultValue; //indicate that player didn't win
    }

    @Override
    protected String getNoun() {
        return "Turn";
    }

    @Override
    protected boolean displayNounBefore() {
        return true;
    }
}
