package forge.achievement;

import forge.game.Game;
import forge.game.player.Player;

public class ManaScrewed extends Achievement {
    public ManaScrewed() {
        super("ManaScrewed", "Mana Screwed", "Win a game despite playing only",
            "3 lands", 3,
            "2 lands", 2,
            "1 land", 1,
            "0 lands", 0);
        best = Integer.MAX_VALUE; //initialize best to max value so any amount is smaller
    }

    @Override
    public boolean needSave() {
        return best < Integer.MAX_VALUE;
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            return player.getNumLandsPlayed();
        }
        return Integer.MAX_VALUE; //indicate that player didn't win
    }

    @Override
    public String getSubTitle() {
        if (best < Integer.MAX_VALUE) {
            return "Best: " + best + " Land" + (best != 1 ? "s" : "");
        }
        return null;
    }
}
