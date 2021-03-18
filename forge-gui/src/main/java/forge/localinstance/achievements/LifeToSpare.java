package forge.localinstance.achievements;

import forge.game.Game;
import forge.game.player.Player;
import forge.util.Localizer;

public class LifeToSpare extends Achievement {
    public LifeToSpare(int bronze0, int silver0, int gold0, int mythic0) {
        super("LifeToSpare", Localizer.getInstance().getMessage("lblLifeToSpare"),
            Localizer.getInstance().getMessage("lblWinGameWith"), 0,
            Localizer.getInstance().getMessage("lblMoreThanStartedLifeN", String.valueOf(bronze0)), bronze0,
            Localizer.getInstance().getMessage("lblMoreThanStartedLifeN", String.valueOf(silver0)), silver0,
            Localizer.getInstance().getMessage("lblMoreThanStartedLifeN", String.valueOf(gold0)), gold0,
            Localizer.getInstance().getMessage("lblMoreThanStartedLifeN", String.valueOf(mythic0)), mythic0
        );
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            int gainedLife = player.getLife() - player.getStartingLife();
            if (gainedLife > 0) {
                return gainedLife;
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
}
