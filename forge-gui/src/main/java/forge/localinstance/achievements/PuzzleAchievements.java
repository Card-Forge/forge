package forge.localinstance.achievements;

import forge.localinstance.properties.ForgeConstants;

public class PuzzleAchievements extends AchievementCollection {
    public PuzzleAchievements() {
        super("lblPuzzleMode", ForgeConstants.ACHIEVEMENTS_DIR + "puzzle.xml", false);
    }

    @Override
    protected void addSharedAchievements() {
        //prevent including shared achievements
    }

    @Override
    protected void addAchievements() {
        // TODO: ideally there should be dynamically generated achievements for solving each
        // available puzzle.

        add(new TotalPuzzlesSolved(1, 10, 25, 50));
    }
}