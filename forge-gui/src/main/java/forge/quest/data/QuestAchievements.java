package forge.quest.data;

import forge.model.CardBlock;
import forge.model.FModel;
import forge.quest.QuestEventDraft;
import forge.quest.data.QuestPreferences.DifficultyPrefs;
import forge.quest.data.QuestPreferences.QPref;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class QuestAchievements {

    // Challenge history
    /** The challenges played. */
    private int challengesPlayed = 0;

    private List<String> completedChallenges = new ArrayList<>();
    private List<String> currentChallenges = new ArrayList<>();
    
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
    
    private transient CardBlock nextDraftBlock;

    public QuestAchievements() { //needed for XML serialization
    }

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
		
        win++;
        winstreakCurrent++;

        if (drafts != null) {
			for (QuestEventDraft questEventDraft : drafts) {
				if (drafts.indexOf(questEventDraft) != currentDraft) { //Don't decrement the current draft
					questEventDraft.addWin();
				}
			}
        }

        if (win % FModel.getQuestPreferences().getPrefInt(QPref.WINS_NEW_DRAFT) == 0) {
            draftsToGenerate++;
        }
        
        if (winstreakCurrent > winstreakBest) {
            winstreakBest = winstreakCurrent;
        }
		
    }

    // Challenge performance
    /**
     * Gets the challenges played.
     * 
     * @return the challenges played
     */
    public int getChallengesPlayed() {
        return challengesPlayed;
    }

    /**
     * Adds the challenges played.
     */
    public void addChallengesPlayed() {
        challengesPlayed++;
    }

    /**
     * Returns stored list of non-repeatable challenge IDs.
     * 
     * @return List<Integer>
     */
    public List<String> getLockedChallenges() {
        return completedChallenges;
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
        completedChallenges.add(i);
    }

    /**
     * Stores a list of current challenges.
     * 
     * @return List<Integer>
     */
    public List<String> getCurrentChallenges() {
        if (currentChallenges == null) {
            currentChallenges = new ArrayList<>();
        }

        return currentChallenges;
    }

    /**
     * Returns the stored list of current challenges.
     * 
     * @param lst0 List<Integer>
     */
    public void setCurrentChallenges(final List<String> lst0) {
        currentChallenges = lst0;
    }

    /**
     * Adds the lost.
     */
    public void addLost() {
        lost++;
        winstreakCurrent = 0;
    }

    // Level, read-only ( note: it increments in addWin() )
    /**
     * Gets the level.
     * 
     * @return the level
     */
    public int getLevel() {
        final int winsToLvlUp = FModel.getQuestPreferences().getPrefInt(DifficultyPrefs.WINS_RANKUP, difficulty);
        return win / winsToLvlUp;
    }

    // Wins & Losses
    /**
     * Gets the lost.
     * 
     * @return the lost
     */
    public int getLost() {
        return lost;
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
        return difficulty;
    }

    public QuestEventDraftContainer getDraftEvents() {
        return drafts;
    }

    public void generateDrafts() {
        
        if (drafts == null) {
            drafts = new QuestEventDraftContainer();
            draftsToGenerate = 1;
        }

        Set<QuestEventDraft> toRemove = new HashSet<>();
        for (QuestEventDraft draft : drafts) {
            if (draft.getAge() <= 0
                    && !(currentDraft != -1 && drafts.get(currentDraft) == draft)) {
                toRemove.add(draft);
                break;
            }
        }

        if (!toRemove.isEmpty()) {
            drafts.removeAll(toRemove);
        }

        for (int i = 0; i < draftsToGenerate; i++) {
            QuestEventDraft draft;
            if (nextDraftBlock != null) {
                draft = QuestEventDraft.getDraftOrNull(FModel.getQuest(), nextDraftBlock);
                nextDraftBlock = null;
            } else {
                draft = QuestEventDraft.getRandomDraftOrNull(FModel.getQuest());
            }
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

    public void spendDraftToken(final CardBlock block) {
        if (draftTokens > 0) {
            draftTokens--;
            draftsToGenerate++;
            nextDraftBlock = block;
            generateDrafts();
        }
    }
    
}
