package forge.localinstance.achievements.rogue;

import forge.game.Game;
import forge.game.player.Player;
import forge.localinstance.achievements.Achievement;

public class TyvarBoonsUnlocked extends Achievement {
    public TyvarBoonsUnlocked() {
        super("TyvarBoonsUnlocked", "Tyvar",
              "Unlock Tyvar's run-start boons",
              "The training begins.", 0);
        setHidden(true);
    }

    @Override
    protected int evaluate(Player player, Game game) {
        return 0; // Not used; evaluated via evaluateNpcBoonUnlockAchievements()
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
