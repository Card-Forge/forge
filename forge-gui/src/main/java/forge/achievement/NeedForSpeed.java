package forge.achievement;

import forge.assets.FSkinProp;
import forge.game.Game;
import forge.game.player.Player;

public class NeedForSpeed extends Achievement {
    public NeedForSpeed(int bronze0, int silver0, int gold0, int mythic0) {
        super("Need for Speed", null,
            String.format("Win a game by turn %d", bronze0), bronze0,
            String.format("Win a game by turn %d", silver0), silver0,
            String.format("Win a game by turn %d", gold0), gold0,
            String.format("Win a game by turn %d", mythic0), mythic0,
            FSkinProp.IMG_NEED_FOR_SPEED);
        best = Integer.MAX_VALUE; //initialize best to max value so any
    }

    @Override
    public boolean needSave() {
        return best < Integer.MAX_VALUE;
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            return player.getTurn();
        }
        return Integer.MAX_VALUE; //indicate that player didn't win
    }

    @Override
    public String getSubTitle() {
        if (best < Integer.MAX_VALUE) {
            return "Best: Turn " + best;
        }
        return null;
    }
}
