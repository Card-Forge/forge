package forge.achievement;

import forge.assets.FSkinProp;
import forge.game.GameType;
import forge.properties.ForgeConstants;

public class ConstructedAchievements extends AchievementCollection {
    public ConstructedAchievements() {
        super("Constructed", ForgeConstants.ACHIEVEMENTS_DIR + "constructed.xml", false);
    }

    //add achievements that should appear at the bottom below core achievements for each game mode
    @Override
    protected void addAchievements() {
        add("Vanguard", new VariantWins(GameType.Vanguard, 25, 50, 100, FSkinProp.IMG_VANGUARD));
        add("MomirBasic", new VariantWins(GameType.MomirBasic, 25, 50, 100, FSkinProp.IMG_MOMIR_BASIC));
        add("Commander", new VariantWins(GameType.Commander, 25, 50, 100, FSkinProp.IMG_COMMANDER));
        add("Planechase", new VariantWins(GameType.Planechase, 25, 50, 100, FSkinProp.IMG_PLANECHASE));
        add("Archenemy", new VariantWins(GameType.Archenemy, 25, 50, 100, FSkinProp.IMG_ARCHENEMY));
        add("Poisoned", new Poisoned(15, 25, 40));
        add("DeckedOut", new DeckedOut(8, 4, 2));
        add("Blackjack", new Blackjack(30, 50, 100));
    }
}
