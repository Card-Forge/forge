package forge.localinstance.achievements;

import forge.game.Game;
import forge.game.player.Player;
import forge.util.Localizer;

public class NeedForSpeed extends Achievement {
    public NeedForSpeed(int bronze0, int silver0, int gold0, int mythic0) {
        super("NeedForSpeed", Localizer.getInstance().getMessage("lblNeedForSpeed"), null, Integer.MAX_VALUE,
            Localizer.getInstance().getMessage("lblWinGameByNTurn", String.valueOf(bronze0)), bronze0,
            Localizer.getInstance().getMessage("lblWinGameByNTurn", String.valueOf(silver0)), silver0,
            Localizer.getInstance().getMessage("lblWinGameByNTurn", String.valueOf(gold0)), gold0,
            Localizer.getInstance().getMessage("lblWinGameByNTurn", String.valueOf(mythic0)), mythic0
        );
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
        return Localizer.getInstance().getMessage("lblTurn");
    }

    @Override
    protected boolean displayNounBefore() {
        return true;
    }
}
