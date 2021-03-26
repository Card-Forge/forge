package forge.localinstance.achievements;

import forge.game.Game;
import forge.game.player.GameLossReason;
import forge.game.player.Player;
import forge.util.Localizer;

public class Poisoned extends Achievement {
    private static final int THRESHOLD = 10;

    public Poisoned(int silver0, int gold0, int mythic0) {
        super("Poisoned", Localizer.getInstance().getMessage("lblPoisoned"), 
            Localizer.getInstance().getMessage("lblWinGameByGivingOppoent"), 0,
            Localizer.getInstance().getMessage("lblNPoisonCounters", String.valueOf(THRESHOLD)), THRESHOLD,
            Localizer.getInstance().getMessage("lblNPoisonCounters", String.valueOf(silver0)), silver0,
            Localizer.getInstance().getMessage("lblNPoisonCounters", String.valueOf(gold0)), gold0,
            Localizer.getInstance().getMessage("lblNPoisonCounters", String.valueOf(mythic0)), mythic0
        );
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            Player opponent = player.getSingleOpponent();
            if (opponent != null && opponent.getOutcome().lossState == GameLossReason.Poisoned) {
                return opponent.getPoisonCounters();
            }
        }
        return 0;
    }

    @Override
    protected String getNoun() {
        return Localizer.getInstance().getMessage("lblCounter");
    }
}
