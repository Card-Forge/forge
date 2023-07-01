package forge.localinstance.achievements;

import forge.localinstance.properties.ForgeConstants;

public class SealedAchievements extends AchievementCollection {
    public SealedAchievements() {
        super("lblSealedDeck", ForgeConstants.ACHIEVEMENTS_DIR + "sealed.xml", true);
    }

    //add achievements that should appear at the bottom below core achievements for each game mode
    @Override
    protected void addAchievements() {
    }
}
