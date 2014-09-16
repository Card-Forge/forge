package forge.achievement;

import forge.properties.ForgeConstants;

public class ConstructedAchievements extends AchievementCollection {
    public ConstructedAchievements() {
        super("Constructed", ForgeConstants.ACHIEVEMENTS_DIR + "constructed.xml");
    }

    @Override
    protected void buildAchievementList() {
        add("GameWinStreak", new GameWinStreak(10, 25, 50));
        add("MatchWinStreak", new MatchWinStreak(10, 25, 50));
        add("TotalGameWins", new TotalGameWins(250, 500, 1000));
        add("TotalMatchWins", new TotalMatchWins(100, 250, 500));
        add("Overkill", new Overkill(25, 50, 100));
        add("LifeToSpare", new LifeToSpare(20, 40, 80));
        add("Hellbent", new Hellbent());
    }
}
