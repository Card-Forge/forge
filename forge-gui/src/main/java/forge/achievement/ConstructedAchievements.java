package forge.achievement;

import forge.properties.ForgeConstants;

public class ConstructedAchievements extends AchievementCollection {
    public ConstructedAchievements() {
        super("Constructed", ForgeConstants.ACHIEVEMENTS_DIR + "constructed.xml");
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
