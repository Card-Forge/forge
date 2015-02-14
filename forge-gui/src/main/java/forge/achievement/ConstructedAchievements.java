package forge.achievement;

import forge.game.GameType;
import forge.properties.ForgeConstants;

public class ConstructedAchievements extends AchievementCollection {
    public ConstructedAchievements() {
        super("Constructed", ForgeConstants.ACHIEVEMENTS_DIR + "constructed.xml", false);
    }

    //add achievements that should appear at the bottom below core achievements for each game mode
    @Override
    protected void addAchievements() {
        add(new VariantWins(GameType.Vanguard, 25, 50, 100));
        add(new VariantWins(GameType.MomirBasic, 25, 50, 100));
        add(new VariantWins(GameType.Commander, 25, 50, 100));
        add(new VariantWins(GameType.TinyLeaders, 25, 50, 100));
        add(new VariantWins(GameType.Planechase, 25, 50, 100));
        add(new VariantWins(GameType.Archenemy, 25, 50, 100));
        add(new Poisoned(15, 25, 40));
        add(new DeckedOut(8, 4, 2));
        add(new Blackjack(30, 50, 100));
    }
}
