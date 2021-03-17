package forge.localinstance.achievements;

import forge.game.GameType;
import forge.localinstance.properties.ForgeConstants;

public class PlanarConquestAchievements extends AchievementCollection {
    public PlanarConquestAchievements() {
        super("lblPlanarConquest", ForgeConstants.ACHIEVEMENTS_DIR + "planar_conquest.xml", true);
    }

    //add achievements that should appear at the bottom below core achievements for each game mode
    @Override
    protected void addAchievements() {
        add(new VariantWins(GameType.Vanguard, 25, 50, 100));
        add(new VariantWins(GameType.Commander, 25, 50, 100));
        add(new VariantWins(GameType.Planeswalker, 25, 50, 100));
        add(new VariantWins(GameType.Planechase, 25, 50, 100));
        add(new Poisoned(15, 25, 40));
        add(new DeckedOut(8, 4, 2));
        add(new Blackjack(30, 50, 100));
    }
}

