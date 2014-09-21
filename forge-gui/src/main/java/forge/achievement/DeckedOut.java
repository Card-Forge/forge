package forge.achievement;

import forge.assets.FSkinProp;
import forge.game.Game;
import forge.game.player.GameLossReason;
import forge.game.player.Player;

public class DeckedOut extends Achievement {
    public DeckedOut(int silver0, int gold0, int mythic0) {
        super("Decked Out", "Win a game from opponent",
            "drawing into an empty library", Integer.MAX_VALUE - 1,
            String.format("drawing into an empty library by turn %d", silver0), silver0,
            String.format("drawing into an empty library by turn %d", gold0), gold0,
            String.format("drawing into an empty library by turn %d", mythic0), mythic0,
            FSkinProp.IMG_DECKED_OUT);
        best = Integer.MAX_VALUE;
    }

    @Override
    public boolean needSave() {
        return best < Integer.MAX_VALUE;
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            Player opponent = player.getSingleOpponent();
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
