package forge.localinstance.achievements.rogue;

import forge.game.Game;
import forge.game.player.Player;
import forge.localinstance.achievements.Achievement;

public class PlaneboundSlayer extends Achievement {
    public PlaneboundSlayer() {
        super("PlaneboundSlayer", "Planebound Slayer",
              "Defeat every Planebound at least once",
              "Every Planebound knows your name.", 0);
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
