package forge.localinstance.achievements;

import forge.game.Game;
import forge.game.player.Player;

public class Speedrunner extends Achievement {
    public Speedrunner() {
        super("Speedrunner", "Speedrunner",
              "Win a Rogue Commander run in under 20 minutes",
              "No time to loot.", 0);
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
