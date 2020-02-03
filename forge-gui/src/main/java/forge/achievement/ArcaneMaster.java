package forge.achievement;

import forge.game.Game;
import forge.game.GameType;
import forge.game.player.Player;
import forge.util.Localizer;

public class ArcaneMaster extends Achievement {
    public ArcaneMaster() {
        super("ArcaneMaster", Localizer.getInstance().getMessage("lblArcaneMaster"),
            Localizer.getInstance().getMessage("lblWinGameWithOutCasting"), Integer.MAX_VALUE,
            Localizer.getInstance().getMessage("lblMore3Spells"), 3,
            Localizer.getInstance().getMessage("lblMore2Spells"), 2,
            Localizer.getInstance().getMessage("lblMore1Spell"), 1,
            Localizer.getInstance().getMessage("lblAnySpells"), 0
        );
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (game.getRules().hasAppliedVariant(GameType.MomirBasic) || game.getRules().hasAppliedVariant(GameType.MoJhoSto)) {
            return defaultValue; // Momir Basic is exempt from this achievement (custom rules do not require any spellcasting by default)
        }
        if (player.getOutcome().hasWon()) {
            return player.getAchievementTracker().spellsCast;
        }
        return defaultValue; //indicate that player didn't win
    }

    @Override
    protected String getNoun() {
        return  Localizer.getInstance().getMessage("lblSpell");
    }
}
