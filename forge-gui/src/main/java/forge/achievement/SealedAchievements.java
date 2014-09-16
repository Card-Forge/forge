package forge.achievement;

import forge.properties.ForgeConstants;

public class SealedAchievements extends AchievementCollection {
    public SealedAchievements() {
        super("Sealed Deck", ForgeConstants.ACHIEVEMENTS_DIR + "sealed.xml");
    }

    //add achievements that should appear at the top above core achievements for each game mode
    @Override
    protected void buildTopShelf() {
    }

    //add achievements that should appear at the bottom below core achievements for each game mode
    @Override
    protected void buildBottomShelf() {
    }
}
