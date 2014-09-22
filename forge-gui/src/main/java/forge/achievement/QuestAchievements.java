package forge.achievement;

import forge.properties.ForgeConstants;

public class QuestAchievements extends AchievementCollection {
    public QuestAchievements() {
        super("Quest Mode", ForgeConstants.ACHIEVEMENTS_DIR + "quest.xml", false);
    }

    //add achievements that should appear at the bottom below core achievements for each game mode
    @Override
    protected void addAchievements() {
        add(new Poisoned(15, 25, 40));
        add(new DeckedOut(8, 4, 2));
    }
}
