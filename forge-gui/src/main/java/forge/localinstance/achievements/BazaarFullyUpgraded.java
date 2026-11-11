package forge.localinstance.achievements;

import forge.game.Game;
import forge.game.player.Player;

public class BazaarFullyUpgraded extends Achievement {
    public BazaarFullyUpgraded() {
        super("BazaarFullyUpgraded", "Bazaar",
              "Upgrade Bazaar to its highest level",
              "Gonti saves the strangest shelf for you.", 0);
    }

    @Override
    protected int evaluate(Player player, Game game) {
        return 0; // Not used; evaluated via evaluateUpgradeAchievements()
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
