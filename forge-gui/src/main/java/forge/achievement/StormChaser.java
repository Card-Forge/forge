package forge.achievement;

import forge.game.Game;
import forge.game.player.Player;

public class StormChaser extends Achievement {
    public StormChaser(int bronze0, int silver0, int gold0, int mythic0) {
        super("StormChaser", "Storm Chaser", "Win a game after casting",
            String.format("%d spells in a single turn", bronze0), bronze0,
            String.format("%d spells in a single turn", silver0), silver0,
            String.format("%d spells in a single turn", gold0), gold0,
            String.format("%d spells in a single turn", mythic0), mythic0);
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            return player.getAchievementTracker().maxStormCount;
        }
        return 0; //indicate that player didn't win
    }

    @Override
    public String getSubTitle() {
        if (best > 0) {
            return "Best: " + best + " Spell" + (best != 1 ? "s" : "");
        }
        return null;
    }
}
