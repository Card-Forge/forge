package forge.quest.data;

import java.util.ArrayList;
import java.util.List;

import forge.Singletons;
import forge.quest.data.QuestPreferences.QPref;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class QuestAchievements {

    // Challenge history
    /** The challenges played. */
    int challengesPlayed = 0;
    /** The completed challenges. */
    List<Integer> completedChallenges = new ArrayList<Integer>();
    /** The win. */
    int win; // number of wins
    int winstreakBest = 0;
    int winstreakCurrent = 0;

    /** The lost. */
    int lost;

    // Difficulty - will store only index from now.
    private int difficulty;
    /**
     * TODO: Write javadoc for Constructor.
     * @param diff
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
     * <p>
     * getCompletedChallenges.
     * </p>
     * Returns stored list of non-repeatable challenge IDs.
     * 
     * @return List<Integer>
     */
    public List<Integer> getCompletedChallenges() {
        return this.completedChallenges;
    }
    /**
     * Adds the challenges played.
     */
    public void addChallengesPlayed() {
        this.challengesPlayed++;
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

    // Poorly named - this should be "setLockedChalleneges" or similar.
    public void addCompletedChallenge(final int i) {
        this.completedChallenges.add(i);
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
        final int winsToLvlUp = Singletons.getModel().getQuestPreferences().getPreferenceInt(QPref.WINS_RANKUP, difficulty);
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
