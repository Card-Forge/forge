package forge.localinstance.achievements;

import forge.game.Game;
import forge.game.player.Player;

public class GoldHoarder extends Achievement {
    public GoldHoarder() {
        super("GoldHoarder", "Gold Hoarder",
              "Have 15 or more Gold during a Rogue Commander run",
              "Fortune favors the bold.", 0);
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
