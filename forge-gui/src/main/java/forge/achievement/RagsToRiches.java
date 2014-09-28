package forge.achievement;

import forge.game.Game;
import forge.game.player.Player;

public class RagsToRiches extends Achievement {
    public RagsToRiches() {
        super("RagsToRiches", "Rags to Riches", "Win a game after mulliganing to", 7,
            "4 cards", 4,
            "3 cards", 3,
            "2 cards", 2,
            "1 card", 1);
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon() && player.getAchievementTracker().mulliganTo < defaultValue) {
            return player.getAchievementTracker().mulliganTo;
        }
        return defaultValue; //indicate that player didn't win
    }

    @Override
    protected String getNoun() {
        return "Card";
    }
}
