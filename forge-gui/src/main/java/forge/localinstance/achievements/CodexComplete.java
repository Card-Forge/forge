package forge.localinstance.achievements;

import forge.game.Game;
import forge.game.player.Player;

public class CodexComplete extends Achievement {
    public CodexComplete() {
        super("CodexComplete", "Codex",
              "Complete the Codex",
              "All records have been uncovered.", 0);
    }

    @Override
    protected int evaluate(Player player, Game game) {
        return 0; // Not used; evaluated via evaluateCodexAchievements()
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
