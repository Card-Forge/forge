package forge.achievement;

import forge.game.Game;
import forge.game.player.GameLossReason;
import forge.game.player.Player;
import forge.util.Localizer;

public class DeckedOut extends Achievement {
    public DeckedOut(int silver0, int gold0, int mythic0) {
        super("DeckedOut", Localizer.getInstance().getMessage("lblDeckedOut"),
            Localizer.getInstance().getMessage("lblWinGameFromOpponent"), Integer.MAX_VALUE,
            Localizer.getInstance().getMessage("lblDrawingEmptyLibrary"), Integer.MAX_VALUE - 1,
            Localizer.getInstance().getMessage("lblDrawingEmptyLibraryByNTurn", String.valueOf(silver0)), silver0,
            Localizer.getInstance().getMessage("lblDrawingEmptyLibraryByNTurn", String.valueOf(gold0)), gold0,
            Localizer.getInstance().getMessage("lblDrawingEmptyLibraryByNTurn", String.valueOf(mythic0)), mythic0
        );
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
