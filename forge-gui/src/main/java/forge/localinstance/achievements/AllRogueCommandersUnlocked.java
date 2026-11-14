package forge.localinstance.achievements;

import forge.game.Game;
import forge.game.player.Player;

public class AllRogueCommandersUnlocked extends Achievement {
    public AllRogueCommandersUnlocked() {
        super("AllRogueCommandersUnlocked", "Full Party",
              "Unlock all Rogue Commanders",
              "Every legend answers the call.", 0);
    }

    @Override
    protected int evaluate(Player player, Game game) {
        return 0; // Not used; evaluated via evaluateCommanderUnlockAchievements()
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
