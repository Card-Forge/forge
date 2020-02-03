package forge.achievement;

import forge.game.Game;
import forge.game.player.Player;
import forge.util.Localizer;

public class TotalGameWins extends ProgressiveAchievement {
    public TotalGameWins(int bronze0, int silver0, int gold0, int mythic0) {
        super("TotalGameWins", Localizer.getInstance().getMessage("lblTotalGameWins"), null,
            Localizer.getInstance().getMessage("lblWinNGames", String.valueOf(bronze0)), bronze0,
            Localizer.getInstance().getMessage("lblWinNGames", String.valueOf(silver0)), silver0,
            Localizer.getInstance().getMessage("lblWinNGames", String.valueOf(gold0)), gold0,
            Localizer.getInstance().getMessage("lblWinNGames", String.valueOf(mythic0)), mythic0
        );
    }

    @Override
    protected boolean eval(Player player, Game game) {
        return player.getOutcome().hasWon();
    }

    @Override
    protected String getNoun() {
        return Localizer.getInstance().getMessage("lblGame");
    }
}
