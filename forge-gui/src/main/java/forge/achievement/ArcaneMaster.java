package forge.achievement;

import forge.game.Game;
import forge.game.player.Player;

public class ArcaneMaster extends Achievement {
    public ArcaneMaster() {
        super("ArcaneMaster", "Arcane Master", "Win a game without casting",
            "more than 3 spells", 3,
            "more than 2 spells", 2,
            "more than 1 spell", 1,
            "any spells", 0);
        best = Integer.MAX_VALUE; //initialize best to max value so any amount is smaller
    }

    @Override
    public boolean needSave() {
        return best < Integer.MAX_VALUE;
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            return player.getAchievementTracker().spellsCast;
        }
        return Integer.MAX_VALUE; //indicate that player didn't win
    }

    @Override
    public String getSubTitle() {
        if (best < Integer.MAX_VALUE) {
            return "Best: " + best + " Spell" + (best != 1 ? "s" : "");
        }
        return null;
    }
}
