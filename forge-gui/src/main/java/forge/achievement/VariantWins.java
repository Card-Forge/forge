package forge.achievement;

import forge.game.Game;
import forge.game.GameType;
import forge.game.player.Player;
import forge.util.Localizer;

public class VariantWins extends ProgressiveAchievement {
    private GameType variant;

    public VariantWins(GameType variant0, int silver0, int gold0, int mythic0) {
        super(variant0.name(), variant0.toString(), null,
            Localizer.getInstance().getMessage("lblWinNVariantGame", 1, variant0.toString()), 1,
            Localizer.getInstance().getMessage("lblWinNVariantGame", silver0, variant0.toString()), silver0,
            Localizer.getInstance().getMessage("lblWinNVariantGame", gold0, variant0.toString()), gold0,
            Localizer.getInstance().getMessage("lblWinNVariantGame", mythic0, variant0.toString()), mythic0);
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
        return Localizer.getInstance().getMessage("lblWin");
    }
}
