package forge.localinstance.achievements;

import forge.game.Game;
import forge.game.player.Player;
import forge.util.Localizer;

public class StormChaser extends Achievement {
    public StormChaser(int bronze0, int silver0, int gold0, int mythic0) {
        super("StormChaser", Localizer.getInstance().getMessage("lblStormChaser"),
            Localizer.getInstance().getMessage("lblWinGameAfterCasting"), 0,
            Localizer.getInstance().getMessage("lblNSpellInSingleTurn", String.valueOf(bronze0)), bronze0,
            Localizer.getInstance().getMessage("lblNSpellInSingleTurn", String.valueOf(silver0)), silver0,
            Localizer.getInstance().getMessage("lblNSpellInSingleTurn", String.valueOf(gold0)), gold0,
            Localizer.getInstance().getMessage("lblNSpellInSingleTurn", String.valueOf(mythic0)), mythic0
        );
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            return player.getAchievementTracker().maxStormCount;
        }
        return 0; //indicate that player didn't win
    }

    @Override
    protected String getNoun() {
        return Localizer.getInstance().getMessage("lblSpell");
    }
}
