package forge.localinstance.achievements;

import forge.game.Game;
import forge.game.player.Player;

public class Roguelike extends Achievement {
    public Roguelike() {
        super("Roguelike", "Roguelike",
              "Win a Rogue Commander run without active Aetherworks",
              "No echoes. No shortcuts.", 0);
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
