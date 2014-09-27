package forge.achievement;

import forge.game.Game;
import forge.game.player.Player;

public class RagsToRiches extends Achievement {
    private static final int NO_MULLIGAN = 7;

    public RagsToRiches() {
        super("RagsToRiches", "Rags to Riches", "Win a game after mulliganing to",
            "4 cards", 4,
            "3 cards", 3,
            "2 cards", 2,
            "1 card", 1);
        best = NO_MULLIGAN; //initialize best to max value so any amount of mulliganing is smaller
    }

    @Override
    public boolean needSave() {
        return best < NO_MULLIGAN;
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon() && player.getAchievementTracker().mulliganTo < NO_MULLIGAN) {
            return player.getAchievementTracker().mulliganTo;
        }
        return NO_MULLIGAN; //indicate that player didn't win
    }

    @Override
    public String getSubTitle() {
        if (best < NO_MULLIGAN) {
            return "Best: " + best + " cards";
        }
        return null;
    }
}
