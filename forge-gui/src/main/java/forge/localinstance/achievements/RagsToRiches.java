package forge.localinstance.achievements;

import forge.game.Game;
import forge.game.player.Player;
import forge.util.Localizer;

public class RagsToRiches extends Achievement {
    public RagsToRiches() {
        super("RagsToRiches", Localizer.getInstance().getMessage("lblRagsToRiches"),
            Localizer.getInstance().getMessage("lblWinGameAfterMulliganingTo"), 7,
            Localizer.getInstance().getMessage("lblNCards", String.valueOf(4)), 4,
            Localizer.getInstance().getMessage("lblNCards", String.valueOf(3)), 3,
            Localizer.getInstance().getMessage("lblNCards", String.valueOf(2)), 2,
            Localizer.getInstance().getMessage("lblNCards", String.valueOf(1)), 1
        );
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon() && player.getAchievementTracker().mulliganTo < defaultValue) {
            return player.getAchievementTracker().mulliganTo;
        }
        return defaultValue; //indicate that player didn't win
    }

    @Override
    protected String getNoun() {
        return Localizer.getInstance().getMessage("lblCard");
    }
}
