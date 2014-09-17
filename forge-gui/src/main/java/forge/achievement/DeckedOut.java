package forge.achievement;

import forge.game.Game;
import forge.game.player.GameLossReason;
import forge.game.player.Player;

public class DeckedOut extends Achievement {
    public DeckedOut(int silver0, int gold0) {
        super("Decked Out",
            "Win a game from opponent drawing into an empty library.", Integer.MAX_VALUE - 1,
            String.format("Win a game this way by turn %d.", silver0), silver0,
            String.format("Win a game this way by turn %d.", gold0), gold0);
        best = Integer.MAX_VALUE;
    }

    @Override
    public boolean needSave() {
        return best < Integer.MAX_VALUE;
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            Player opponent = getSingleOpponent(player);
            if (opponent != null && opponent.getOutcome().lossState == GameLossReason.Milled) {
                return player.getTurn();
            }
        }
        return Integer.MAX_VALUE;
    }

    @Override
    public String getSubTitle() {
        if (best < Integer.MAX_VALUE) {
            return "Best: Turn " + best;
        }
        return null;
    }
}
