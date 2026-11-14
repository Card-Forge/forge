package forge.localinstance.achievements;

import forge.game.Game;
import forge.game.player.Player;

public class Gifted extends Achievement {
    public Gifted() {
        super("Gifted", "Gifted",
              "Win a Rogue Commander run with 3 or more active Traits",
              "Power leaves its mark.", 0);
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
