package forge.quest.data;

import forge.MyRandom;
import forge.SetUtils;
import forge.card.CardPool;
import forge.card.CardPrinted;
import forge.card.InventoryItem;
import forge.deck.Deck;
import forge.error.ErrorViewer;
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

//static readAIQuestDeckFiles(QuestDataOld data, ArrayList aiDeckNames)
//OR non-static readAIQuestDeckFiles()
//which reads the files "questDecks-easy", "questDecks-medium","questDecks-hard",

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
    CardPool<CardPrinted> cardPool = new CardPool<CardPrinted>();     // player's belonging
    CardPool<CardPrinted> shopList = new CardPool<CardPrinted>();     // the current shop list
    CardPool<InventoryItem> newCardList = new CardPool<InventoryItem>();  // cards acquired since last game-win/loss

    // Quests history
    int questsPlayed;
    List<Integer> availableQuests = new ArrayList<Integer>();
    List<Integer> completedQuests = new ArrayList<Integer>();

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
        "Saltblasted for your talent",
        "Serra Angel is your girlfriend",
        };

    /**
     * <p>Constructor for QuestData.</p>
     */
    public QuestData() {
        initTransients();
        myCards.generateBasicLands(QuestPreferences.getStartingBasic(), QuestPreferences.getStartingSnowBasic());
        randomizeOpponents();
    }

    private void initTransients() {
        // These are helper classes that hold no data.
        myCards = new QuestUtilCards(this);
        myRewards = new QuestUtilRewards(this);

        // to avoid NPE some pools will be created here if they are null
        if (null == newCardList) { newCardList = new CardPool<InventoryItem>(); }
        if (null == shopList) { shopList = new CardPool<CardPrinted>(); }

    }

    public void newGame(final int diff, final String m0de, final boolean standardStart) {
        setDifficulty(diff);

        Predicate<CardPrinted> filter = standardStart ? SetUtils.getStandard().getFilterPrinted() : CardPrinted.Predicates.Presets.isTrue;

        myCards.setupNewGameCardPool(filter, diff);
        credits = QuestPreferences.getStartingCredits();

        mode = m0de;
        life = mode.equals(FANTASY) ? 15 : 20;
    }

    // All belongins
    public QuestInventory getInventory() { return inventory; }
    public QuestPetManager getPetManager() { return petManager; }
    // Cards - class uses data from here
    public QuestUtilCards getCards() { return myCards; }
    public QuestUtilRewards getRewards() { return myRewards; }

    // Quests performance
    public int getQuestsPlayed() { return questsPlayed; }
    public void addQuestsPlayed() { questsPlayed++; }

    public List<Integer> getAvailableQuests() { return availableQuests != null ? new ArrayList<Integer>(availableQuests) : null; }
    public void setAvailableQuests(final List<Integer> list) { availableQuests = list; }
    public void clearAvailableQuests() { availableQuests.clear(); }
    public List<Integer> getCompletedQuests() { return completedQuests != null ? new ArrayList<Integer>(completedQuests) : null; }

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

    // SERIALIZATION - relared things

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
