package forge.quest.data;

import java.util.ArrayList;
import java.util.List;

import forge.Singletons;
import forge.quest.data.QuestPreferences.DifficultyPrefs;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class QuestAchievements {

    // Challenge history
    /** The challenges played. */
    private int challengesPlayed = 0;
    /** */
    private List<Integer> completedChallenges = new ArrayList<Integer>();
    /** */
    private List<Integer> currentChallenges = new ArrayList<Integer>();

    private int win;
    private int winstreakBest = 0;
    private int winstreakCurrent = 0;
    private int lost;

    // Difficulty - will store only index from now.
    private int difficulty;

    /**
     * TODO: Write javadoc for Constructor.
     * @param diff &emsp; int
     */
    public QuestAchievements(int diff) {
        difficulty = diff;
    }

    /**
     * TODO: Write javadoc for Constructor.
     * @param mode
     */
    /**
     * Adds the win.
     */
    public void addWin() { // changes getRank()
        this.win++;
        this.winstreakCurrent++;

        if (this.winstreakCurrent > this.winstreakBest) {
            this.winstreakBest = this.winstreakCurrent;
        }

    }

    // Challenge performance
    /**
     * Gets the challenges played.
     * 
     * @return the challenges played
     */
    public int getChallengesPlayed() {
        return this.challengesPlayed;
    }

    /**
     * Adds the challenges played.
     */
    public void addChallengesPlayed() {
        this.challengesPlayed++;
    }

    /**
     * Returns stored list of non-repeatable challenge IDs.
     * 
     * @return List<Integer>
     */
    public List<Integer> getLockedChallenges() {
        return this.completedChallenges;
    }

    /**
     * <p>
     * addCompletedChallenge.
     * </p>
     * Add non-repeatable challenge ID to list.
     * 
     * @param i
     *            the i
     */
    public void addLockedChallenge(final int i) {
        this.completedChallenges.add(i);
    }

    /**
     * Stores a list of current challenges.
     * 
     * @return List<Integer>
     */
    public List<Integer> getCurrentChallenges() {
        if (this.currentChallenges == null) {
            this.currentChallenges = new ArrayList<Integer>();
        }

        return this.currentChallenges;
    }

    /**
     * Returns the stored list of current challenges.
     * 
     * @param lst0 List<Integer>
     */
    public void setCurrentChallenges(final List<Integer> lst0) {
        this.currentChallenges = lst0;
    }

    /**
     * Adds the lost.
     */
    public void addLost() {
        this.lost++;
        this.winstreakCurrent = 0;
    }
    // Level, read-only ( note: it increments in addWin() )
    /**
     * Gets the level.
     * 
     * @return the level
     */
    public int getLevel() {
        final int winsToLvlUp = Singletons.getModel().getQuestPreferences().getPrefInt(DifficultyPrefs.WINS_RANKUP, difficulty);
        return this.win / winsToLvlUp;
    }
    // Wins & Losses
    /**
     * Gets the lost.
     * 
     * @return the lost
     */
    public int getLost() {
        return this.lost;
    }

    /**
     * Gets the win.
     * 
     * @return the win
     */
    public int getWin() {
        return win;
    }

    /**
     * Gets the win streak best.
     *
     * @return int
     */
    public int getWinStreakBest() {
        return winstreakBest;
    }

    /**
     * Gets the win streak current.
     *
     * @return int
     */
    public int getWinStreakCurrent() {
        return winstreakCurrent;
    }

    /**
     * Gets the difficulty index.
     * 
     * @return the difficulty index
     */
    public int getDifficulty() {
        return this.difficulty;
    }

}
