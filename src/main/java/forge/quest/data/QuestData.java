package forge.quest.data;

import forge.MyRandom;
import forge.SetUtils;
import forge.deck.Deck;
import forge.error.ErrorViewer;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.item.QuestInventory;
import forge.quest.data.pet.QuestPetManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.slightlymagic.maxmtg.Predicate;

//when you create QuestDataOld and AFTER you copy the AI decks over
//you have to call one of these two methods below
//see Gui_QuestOptions for more details

/**
 * <p>QuestData class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public final class QuestData {

    //This field holds the version of the Quest Data
    /** Constant <code>CURRENT_VERSION_NUMBER=2</code> */
    public static final int CURRENT_VERSION_NUMBER = 2;

    //This field places the version number into QD instance,
    //but only when the object is created through the constructor
    //DO NOT RENAME THIS FIELD
    int versionNumber = CURRENT_VERSION_NUMBER;

    int rankIndex; // level
    int win; // number of wins
    int lost;

    long credits; // this money is good for all modes
    
    int life; // for fantasy mode, how much life bought at shop to start game with
    QuestInventory inventory = new QuestInventory(); // different gadgets
    QuestPetManager petManager = new QuestPetManager(); // pets that start match with you


    // Diffuculty - they store both index and title
    int diffIndex;
    String difficulty;

    // Quest mode - there should be an enum :(
    String mode = "";
    public static final String FANTASY = "Fantasy";
    public static final String REALISTIC = "Realistic";

    // Decks collected by player
    Map<String, Deck> myDecks = new HashMap<String, Deck>();

    // Cards associated with quest
    ItemPool<InventoryItem> cardPool = new ItemPool<InventoryItem>(InventoryItem.class);     // player's belonging
    ItemPool<InventoryItem> shopList = new ItemPool<InventoryItem>(InventoryItem.class);     // the current shop list
    ItemPool<InventoryItem> newCardList = new ItemPool<InventoryItem>(InventoryItem.class);  // cards acquired since last game-win/loss

    // Challenge history
    int challengesPlayed = 0;
    List<Integer> availableChallenges = new ArrayList<Integer>();
    List<Integer> completedChallenges = new ArrayList<Integer>();
    
    // Challenges used to be called quests.  During the renaming, 
    // files could be corrupted.  These fields ensure old files still work.
    // These fields should be phased out after a little while.
    // The old files, if played once, are updated automatically to the new system.
    int questsPlayed = -1;
    List<Integer> availableQuests = null;
    List<Integer> completedQuests = null;

    // own randomizer seed
    private long randomSeed = 0;

    // Utility class to access cards, has access to private fields
    // Moved some methods there that otherwise would make this class even more complex
    private transient QuestUtilCards myCards;
    private transient QuestUtilRewards myRewards;


    public static final String[] RANK_TITLES  = new String[]{
        "Level 0 - Confused Wizard",
        "Level 1 - Mana Mage",
        "Level 2 - Death by Megrim",
        "Level 3 - Shattered the Competition",
        "Level 4 - Black Knighted",
        "Level 5 - Shockingly Good",
        "Level 6 - Regressed into Timmy",
        "Level 7 - Loves Blue Control",
        "Level 8 - Immobilized by Fear",
        "Level 9 - Lands = Friends",
        "Level 10 - Forging new paths",
        "Level 11 - Infect-o-tron",
        "Level 12 - Great Balls of Fire",
        "Level 13 - Artifact Schmartifact",
        "Level 14 - Mike Mulligan's The Name",
        "Level 15 - Fresh Air: Good For The Health",
        "Level 16 - In It For The Love",
        "Level 17 - Sticks, Stones, Bones",
        "Level 18 - Credits For Breakfast",
        "Level 19 - Millasaurus",
        "Level 20 - One-turn Wonder",
        "Teaching Gandalf a Lesson",
        "What Do You Do With The Other Hand?",
        "Freelance Sorcerer, Works Weekends",
        "Should We Hire Commentators?",
        "Saltblasted For Your Talent",
        "Serra Angel Is Your Girlfriend",
        };

    /**
     * <p>Constructor for QuestData.</p>
     */
    public QuestData() {
        initTransients();
        myCards.addBasicLands(cardPool, QuestPreferences.getStartingBasic(), QuestPreferences.getStartingSnowBasic());
        randomizeOpponents();
    }

    private void initTransients() {
        // These are helper classes that hold no data.
        myCards = new QuestUtilCards(this);
        myRewards = new QuestUtilRewards(this);

        // to avoid NPE some pools will be created here if they are null
        if (null == newCardList) { newCardList = new ItemPool<InventoryItem>(InventoryItem.class); }
        if (null == shopList) { shopList = new ItemPool<InventoryItem>(InventoryItem.class); }

    }

    public void newGame(final int diff, final String m0de, final boolean standardStart) {
        setDifficulty(diff);

        Predicate<CardPrinted> filter = standardStart ? SetUtils.getStandard().getFilterPrinted() : CardPrinted.Predicates.Presets.isTrue;

        myCards.setupNewGameCardPool(filter, diff);
        credits = QuestPreferences.getStartingCredits();

        mode = m0de;
        life = mode.equals(FANTASY) ? 15 : 20;
    }

    // All belongings
    public QuestInventory getInventory() { return inventory; }
    public QuestPetManager getPetManager() { return petManager; }
    // Cards - class uses data from here
    public QuestUtilCards getCards() { return myCards; }
    public QuestUtilRewards getRewards() { return myRewards; }

    // Challenge performance
    public int getChallengesPlayed() {
        // This should be phased out after a while, when
        // old quest decks have been updated. (changes made 19-9-11)
        if(questsPlayed!=-1) { 
            challengesPlayed = questsPlayed;
            questsPlayed = -1; 
        }
        
        return challengesPlayed;
    }
    
    public void addChallengesPlayed() { challengesPlayed++; }

    public List<Integer> getAvailableChallenges() { 
        // This should be phased out after a while, when
        // old quest decks have been updated. (changes made 19-9-11)
        if(availableQuests != null) {
            availableChallenges = availableQuests;
            availableQuests = null;
        }
        
        return availableChallenges != null ? new ArrayList<Integer>(availableChallenges) : null; 
    }
    
    public void setAvailableChallenges(final List<Integer> list) { availableChallenges = list; }
    public void clearAvailableChallenges() { availableChallenges.clear(); }
    
    /**
     * <p>getCompletedChallenges.</p>
     * Returns stored list of non-repeatable challenge IDs.
     * 
     * @return List<Integer>
     */
    public List<Integer> getCompletedChallenges() { 
        // This should be phased out after a while, when
        // old quest decks have been updated. (changes made 19-9-11)
        // Also, poorly named - this should be "getLockedChalleneges" or similar.
        if(completedQuests != null) {
            completedChallenges = completedQuests;
            completedQuests = null;
        }
        
        return completedChallenges != null ? new ArrayList<Integer>(completedChallenges) : null; 
    }
    
    /**
     * <p>addCompletedChallenge.</p>
     * Add non-repeatable challenge ID to list.
     * 
     * @param int i
     */
    
    // Poorly named - this should be "setLockedChalleneges" or similar.
    public void addCompletedChallenge(int i) {
        completedChallenges.add(i);
    }

    // Wins & Losses
    public int getLost() { return lost; }
    public void addLost() { lost++; }
    public int getWin() { return win; }
    public void addWin() { //changes getRank()
        win++;

        int winsToLvlUp = QuestPreferences.getWinsForRankIncrease(diffIndex);
        if (win % winsToLvlUp == 0) { rankIndex++; }
    }

    // Life (only fantasy)
    public int getLife() { return isFantasy() ? life : 20; }
    public void addLife(final int n) { life += n; }

    // Credits
    public void addCredits(final long c) { credits += c; }
    public void subtractCredits(final long c) { credits = credits > c ? credits - c : 0; }
    public long getCredits() { return credits; }

    // Quest mode
    public boolean isFantasy() { return mode.equals(FANTASY); }
    public String getMode() { return mode == null ? "" : mode; }

    // Difficulty
    public String getDifficulty() { return difficulty; }
    public int getDifficultyIndex() { return diffIndex; }

    public void setDifficulty(final int i) {
        diffIndex = i;
        difficulty = QuestPreferences.getDifficulty(i);
    }

    public void guessDifficultyIndex() {
        String[] diffStr = QuestPreferences.getDifficulty();
        for (int i = 0; i < diffStr.length; i++) {
            if (difficulty.equals(diffStr[i])) {
                diffIndex = i;
            }
        }
    }

    // Level, read-only ( note: it increments in addWin() )
    public int getLevel() { return rankIndex; }
    public String getRank() {  
        if (rankIndex >= RANK_TITLES.length) { rankIndex = RANK_TITLES.length - 1; }
        return RANK_TITLES[rankIndex];
    }

    // decks management
    public List<String> getDeckNames() { return new ArrayList<String>(myDecks.keySet()); }
    public void removeDeck(final String deckName) { myDecks.remove(deckName); }
    public void addDeck(final Deck d) { myDecks.put(d.getName(), d); }

    /**
     * <p>getDeck.</p>
     *
     * @param deckName a {@link java.lang.String} object.
     * @return a {@link forge.deck.Deck} object.
     */
    public Deck getDeck(final String deckName) {
        if (!myDecks.containsKey(deckName)) {
            ErrorViewer.showError(new Exception(),
                    "QuestData : getDeckFromMap(String deckName) error, deck name not found - %s", deckName);
        }
        Deck d = myDecks.get(deckName);
        d.clearSideboard();
        return d;
    }

    // randomizer - related
    public long getRandomSeed() { return randomSeed; }

    /**
     * This method should be called whenever the opponents should change.
     */
    public void randomizeOpponents() {
        randomSeed = MyRandom.random.nextLong();
    }

    // SERIALIZATION - related things

    // This must be called by XML-serializer via reflection
    public Object readResolve() {
        initTransients();
        return this;
    }

    public boolean hasSaveFile() {
        return ForgeProps.getFile(NewConstants.QUEST.DATA).exists() ||
               ForgeProps.getFile(NewConstants.QUEST.XMLDATA).exists();
    }

    public void saveData() { QuestDataIO.saveData(this); }
}
