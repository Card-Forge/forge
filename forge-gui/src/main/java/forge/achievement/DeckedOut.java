package forge.achievement;

import forge.game.Game;
import forge.game.player.GameLossReason;
import forge.game.player.Player;

public class DeckedOut extends Achievement {
    public DeckedOut(int silver0, int gold0, int mythic0) {
        super("DeckedOut", "Decked Out", "Win a game from opponent", Integer.MAX_VALUE,
            "drawing into an empty library", Integer.MAX_VALUE - 1,
            String.format("drawing into an empty library by turn %d", silver0), silver0,
            String.format("drawing into an empty library by turn %d", gold0), gold0,
            String.format("drawing into an empty library by turn %d", mythic0), mythic0);
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
    protected String getNoun() {
        return "Turn";
    }

    @Override
    protected boolean displayNounBefore() {
        return true;
    }
}
