package forge.achievement;

import forge.game.GameType;
import forge.properties.ForgeConstants;

public class ConstructedAchievements extends AchievementCollection {
    public ConstructedAchievements() {
        super("Constructed", ForgeConstants.ACHIEVEMENTS_DIR + "constructed.xml", false);
    }

    //add achievements that should appear at the top above core achievements for each game mode
    @Override
    protected void buildTopShelf() {
    }

    //add achievements that should appear at the bottom below core achievements for each game mode
    @Override
    protected void buildBottomShelf() {
        add("Poisoned", new Poisoned(10, 15, 25));
        add("Decked Out", new DeckedOut(8, 4));
        add("Vanguard", new VariantWins(GameType.Vanguard, 25, 50));
        add("MomirBasic", new VariantWins(GameType.MomirBasic, 25, 50));
        add("Commander", new VariantWins(GameType.Commander, 25, 50));
        add("Planechase", new VariantWins(GameType.Planechase, 25, 50));
        add("Archenemy", new VariantWins(GameType.Archenemy, 25, 50));
    }
}
