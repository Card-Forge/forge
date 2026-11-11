package forge.localinstance.achievements;

import forge.game.Game;
import forge.game.player.Player;

public class Carrier extends Achievement {
    public Carrier() {
        super("Carrier", "Carrier",
              "Win a Rogue Commander run with 4 or more Carry Cards",
              "Packed for every contingency.", 0);
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
