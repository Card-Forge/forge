package forge.localinstance.achievements;

import forge.game.Game;
import forge.game.player.Player;
import forge.util.Localizer;

public class Overkill extends Achievement {
    public Overkill(int bronze0, int silver0, int gold0, int mythic0) {
        super("Overkill", Localizer.getInstance().getMessage("lblOverkill"),
            Localizer.getInstance().getMessage("lblWinGameWithOppentAt"), 0,
            Localizer.getInstance().getMessage("lblNLife", String.valueOf(bronze0)), bronze0,
            Localizer.getInstance().getMessage("lblNLife", String.valueOf(silver0)), silver0,
            Localizer.getInstance().getMessage("lblNLife", String.valueOf(gold0)), gold0,
            Localizer.getInstance().getMessage("lblNLife", String.valueOf(mythic0)), mythic0
        );
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            Player opponent = player.getSingleOpponent();
            if (opponent != null && opponent.getLife() < 0) {
                return opponent.getLife();
            }
        }
        return 0;
    }

    @Override
    protected String getNoun() {
        return Localizer.getInstance().getMessage("lblLife");
    }
    @Override
    protected boolean pluralizeNoun() {
        return false;
    }

    @Override
    protected int performConversion(int value, long timestamp) {
        //perform conversion to handle data from old format before supporting negative thresholds
        if (value > 0) {
            value = -value;
        }
        return value;
    }
}
