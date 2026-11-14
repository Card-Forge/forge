package forge.localinstance.achievements;

import forge.game.Game;
import forge.game.player.Player;

public class LifeAbundance extends Achievement {
    public LifeAbundance() {
        super("LifeAbundance", "Life Abundant",
              "Finish a Rogue Commander match with 50 or more life",
              "Overflowing with vitality.", 0);
    }

    @Override
    protected int evaluate(Player player, Game game) {
        return 0; // Not used; evaluated via evaluateRunAchievements()
    }

    @Override
    protected String getNoun() {
        return null;
    }

    @Override
    public String getSubTitle(boolean includeTimestamp) {
        if (includeTimestamp) {
            String formattedTimestamp = getFormattedTimestamp();
            if (formattedTimestamp != null) {
                return "Earned " + formattedTimestamp;
            }
        }
        return null;
    }
}
