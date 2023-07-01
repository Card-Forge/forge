package forge.localinstance.achievements;

import forge.game.Game;
import forge.game.player.Player;
import forge.util.Localizer;

public class ManaScrewed extends Achievement {
    public ManaScrewed() {
        super("ManaScrewed", Localizer.getInstance().getMessage("lblManaScrewed"),
            Localizer.getInstance().getMessage("lblWinGameOnlyPlaing"), Integer.MAX_VALUE,
            Localizer.getInstance().getMessage("lblNLands", String.valueOf(3)), 3,
            Localizer.getInstance().getMessage("lblNLands", String.valueOf(2)), 2,
            Localizer.getInstance().getMessage("lblNLands", String.valueOf(1)), 1,
            Localizer.getInstance().getMessage("lblNLands", String.valueOf(0)), 0
        );
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            return player.getAchievementTracker().landsPlayed;
        }
        return defaultValue; //indicate that player didn't win
    }

    @Override
    protected String getNoun() {
        return Localizer.getInstance().getMessage("lblLand");
    }

    @Override
    protected int performConversion(int value, long timestamp) {
        //throw out any ManaScrewed achievements earned before timestamp support added
        //since there was a bug where it was earned almost always
        if (timestamp == 0) {
            return defaultValue;
        }
        return value;
    }
}
