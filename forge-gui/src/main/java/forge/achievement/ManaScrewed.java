package forge.achievement;

import forge.game.Game;
import forge.game.player.Player;

public class ManaScrewed extends Achievement {
    public ManaScrewed() {
        super("ManaScrewed", "Mana Screwed", "Win a game despite playing only", Integer.MAX_VALUE,
            "3 lands", 3,
            "2 lands", 2,
            "1 land", 1,
            "0 lands", 0);
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            return player.getAchievementTracker().landsPlayed;
        }
        return defaultValue; //indicate that player didn't win
    }

    @Override
    protected String getNoun() {
        return "Land";
    }
}
