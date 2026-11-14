package forge.localinstance.achievements;

import forge.game.Game;
import forge.game.player.Player;

public class LegendaryArmy extends Achievement {
    public LegendaryArmy() {
        super("LegendaryArmy", "Legendary Army",
              "Have more than 20 legendary permanents in your Rogue deck",
              "Legends answer the call.", 0);
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
