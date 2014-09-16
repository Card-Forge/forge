package forge.achievement;

import forge.game.Game;
import forge.game.GameType;
import forge.game.player.Player;

public class VariantWins extends Achievement {
    private GameType variant;

    public VariantWins(GameType variant0, int silver0, int gold0) {
        super(variant0.toString(),
            String.format("Win a %s game.", variant0.toString()), 1,
            String.format("Win %d %s games.", silver0, variant0.toString()), silver0,
            String.format("Win %d %s games.", gold0, variant0.toString()), gold0);
        variant = variant0;
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            if (game.getRules().hasAppliedVariant(variant)) {
                return current + 1;
            }
            if (variant == GameType.Archenemy && game.getRules().hasAppliedVariant(GameType.ArchenemyRumble)) {
                return current + 1; //lump Archenemy Rumble into same achievement as Archenemy 
            }
        }
        return current;
    }

    @Override
    public String getSubTitle() {
        return current + " Win" + (current != 1 ? "s" : "");
    }
}
