package forge.achievement;

import forge.properties.ForgeConstants;

public class ChallengeAchievements extends AchievementCollection {
    public static final ChallengeAchievements instance = new ChallengeAchievements();

    private ChallengeAchievements() {
        super("Challenges", ForgeConstants.ACHIEVEMENTS_DIR + "challenges.xml", false);
    }

    @Override
    protected void addSharedAchivements() {
        //prevent including shared achievements
    }

    @Override
    protected void addAchievements() {
        add(new NoCreatures());
        add(new NoSpells());
        add(new NoLands());
        add(new Domain());
    }
}
