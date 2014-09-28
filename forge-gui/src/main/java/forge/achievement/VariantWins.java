package forge.achievement;

import forge.game.Game;
import forge.game.GameType;
import forge.game.player.Player;
import forge.util.Lang;

public class VariantWins extends ProgressiveAchievement {
    private GameType variant;

    public VariantWins(GameType variant0, int silver0, int gold0, int mythic0) {
        super(variant0.name(), variant0.toString(), null,
            "Win " + Lang.nounWithAmount(1, variant0.toString() + " game"), 1,
            "Win " + Lang.nounWithAmount(silver0, variant0.toString() + " game"), silver0,
            "Win " + Lang.nounWithAmount(gold0, variant0.toString() + " game"), gold0,
            "Win " + Lang.nounWithAmount(mythic0, variant0.toString() + " game"), mythic0);
        variant = variant0;
    }

    @Override
    protected boolean eval(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            if (game.getRules().hasAppliedVariant(variant)) {
                return true;
            }
            if (variant == GameType.Archenemy && game.getRules().hasAppliedVariant(GameType.ArchenemyRumble)) {
                return true; //lump Archenemy Rumble into same achievement as Archenemy 
            }
        }
        return false;
    }

    @Override
    protected String getNoun() {
        return "Win";
    }
}
