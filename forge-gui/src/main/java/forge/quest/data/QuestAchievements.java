package forge.quest.data;

import java.util.ArrayList;
import java.util.List;

import forge.model.FModel;
import forge.quest.QuestEventDraft;
import forge.quest.data.QuestPreferences.DifficultyPrefs;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class QuestAchievements {

    // Challenge history
    /** The challenges played. */
    private int challengesPlayed = 0;

    private List<String> completedChallenges = new ArrayList<String>();
    private List<String> currentChallenges = new ArrayList<String>();
    
    private QuestEventDraftContainer drafts = new QuestEventDraftContainer();
    private int currentDraft = -1;
    private int draftsToGenerate = 1;
    private int draftTokens = 0;

    private int win;
    private int winstreakBest = 0;
    private int winstreakCurrent = 0;
    private int lost;
    
    private int firstPlaceDraftFinishes = 0;
    private int secondPlaceDraftFinishes = 0;
    private int thirdPlaceDraftFinishes = 0;
    private int fourthPlaceDraftFinishes = 0;
    
    // Difficulty - will store only index from now.
    private int difficulty;

    /**
     * TODO: Write javadoc for Constructor.
     * @param diff &emsp; int
     */
    public QuestAchievements(int diff) {
        difficulty = diff;
    }
    
    public void deleteDraft(QuestEventDraft draft) {
        if (currentDraft == drafts.indexOf(draft)) {
            currentDraft = -1;
        }
        drafts.remove(draft);
    }
    
    public void endCurrentTournament(final int place) {
        drafts.remove(drafts.get(currentDraft));
        currentDraft = -1;
        addDraftFinish(place);
        FModel.getQuest().save();
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
        
        for (QuestEventDraft draft : drafts) {
            if (!(currentDraft != -1 && drafts.get(currentDraft) == draft)) {
                draft.addWin();
            }
        }
        
        if (win % 5 == 0) {
            draftsToGenerate++;
        }
        
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
    public List<String> getLockedChallenges() {
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
    public void addLockedChallenge(final String i) {
        this.completedChallenges.add(i);
    }

    /**
     * Stores a list of current challenges.
     * 
     * @return List<Integer>
     */
    public List<String> getCurrentChallenges() {
        if (this.currentChallenges == null) {
            this.currentChallenges = new ArrayList<String>();
        }

        return this.currentChallenges;
    }

    /**
     * Returns the stored list of current challenges.
     * 
     * @param lst0 List<Integer>
     */
    public void setCurrentChallenges(final List<String> lst0) {
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
        final int winsToLvlUp = FModel.getQuestPreferences().getPrefInt(DifficultyPrefs.WINS_RANKUP, difficulty);
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

    public QuestEventDraftContainer getDraftEvents() {
        return drafts;
    }
    
    public void generateDrafts() {
        
        if (drafts == null) {
            drafts = new QuestEventDraftContainer();
            draftsToGenerate = 1;
        }
        
        QuestEventDraft toRemove = null;
        for (QuestEventDraft draft : drafts) {
            if (draft.getAge() <= 0
                    && !(currentDraft != -1 && drafts.get(currentDraft) == draft)) {
                toRemove = draft;
                break;
            }
        }
        
        if (toRemove != null) {
            drafts.remove(toRemove);
        }
        
        for (int i = 0; i < draftsToGenerate; i++) {
            QuestEventDraft draft = QuestEventDraft.getRandomDraftOrNull(FModel.getQuest());
            if (draft != null) {
                drafts.add(draft);
                draftsToGenerate--;
            }
        }
        
        FModel.getQuest().save();
        
    }

    public void addDraftToken() {
        draftTokens++;
    }
    
    public void setCurrentDraft(final QuestEventDraft draft) {
        currentDraft = drafts.indexOf(draft);
    }
    
    public QuestEventDraft getCurrentDraft() {
        if (drafts == null || drafts.size() == 0) {
            return null;
        }
        if (currentDraft > drafts.size() - 1) {
            currentDraft = -1;
            FModel.getQuest().getDraftDecks().delete(QuestEventDraft.DECK_NAME);
            return null;
        }
        return drafts.get(currentDraft);
    }
    
    public int getCurrentDraftIndex() {
        return currentDraft;
    }
    
    public int getWinsForPlace(final int place) {
        
        switch (place) {
            case 1:
                return firstPlaceDraftFinishes;
            case 2:
                return secondPlaceDraftFinishes;
            case 3:
                return thirdPlaceDraftFinishes;
            case 4:
                return fourthPlaceDraftFinishes;
        }
        
        return 0;
        
    }
    
    private void addDraftFinish(final int place) {
        
        switch (place) {
            case 1:
                firstPlaceDraftFinishes++;
                break;
            case 2:
                secondPlaceDraftFinishes++;
                break;
            case 3:
                thirdPlaceDraftFinishes++;
                break;
            case 4:
                fourthPlaceDraftFinishes++;
                break;
        }
        
    }
    
    public int getDraftTokens() {
        return draftTokens;
    }
    
    public void spendDraftToken() {
        if (draftTokens > 0) {
            draftTokens--;
            draftsToGenerate++;
        }
    }

}
