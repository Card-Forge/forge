package forge.achievement;

import forge.properties.ForgeConstants;

public class DraftAchievements extends AchievementCollection {
    public DraftAchievements() {
        super("lblBoosterDraft", ForgeConstants.ACHIEVEMENTS_DIR + "draft.xml", true);
    }

    //add achievements that should appear at the bottom below core achievements for each game mode
    @Override
    protected void addAchievements() {
    }
}
