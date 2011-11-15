package forge.quest.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.slightlymagic.maxmtg.Predicate;
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

//when you create QuestDataOld and AFTER you copy the AI decks over
//you have to call one of these two methods below
//see Gui_QuestOptions for more details

/**
 * <p>
 * QuestData class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class QuestData {

    // This field holds the version of the Quest Data
    /** Constant <code>CURRENT_VERSION_NUMBER=2</code>. */
    public static final int CURRENT_VERSION_NUMBER = 2;

    // This field places the version number into QD instance,
    // but only when the object is created through the constructor
    // DO NOT RENAME THIS FIELD
    /** The version number. */
    private int versionNumber = QuestData.CURRENT_VERSION_NUMBER;

    /** The rank index. */
    private int rankIndex; // level

    /** The win. */
    private int win; // number of wins

    /** The lost. */
    private int lost;

    /** The credits. */
    private long credits; // this money is good for all modes

    /** The life. */
    private int life; // for fantasy mode, how much life bought at shop to start game
              // with
    /** The inventory. */
    private QuestInventory inventory = new QuestInventory(); // different gadgets

    /** The pet manager. */
    private QuestPetManager petManager = new QuestPetManager(); // pets that start match
                                                        // with you

    // Diffuculty - they store both index and title
    /** The diff index. */
    private int diffIndex;

    /** The difficulty. */
    private String difficulty;

    // Quest mode - there should be an enum :(
    /** The mode. */
    private String mode = "";

    /** The Constant FANTASY. */
    public static final String FANTASY = "Fantasy";

    /** The Constant REALISTIC. */
    public static final String REALISTIC = "Realistic";

    // Decks collected by player
    /** The my decks. */
    private Map<String, Deck> myDecks = new HashMap<String, Deck>();

    // Cards associated with quest
    /** The card pool. */
    private ItemPool<InventoryItem> cardPool = new ItemPool<InventoryItem>(InventoryItem.class); // player's
                                                                                         // belonging
    /** The shop list. */
    private ItemPool<InventoryItem> shopList = new ItemPool<InventoryItem>(InventoryItem.class); // the
    // current
    // shop
    // list
    /** The new card list. */
    private ItemPool<InventoryItem> newCardList = new ItemPool<InventoryItem>(InventoryItem.class); // cards
    // acquired
    // since
    // last
    // game-win/loss

    // Challenge history
    /** The challenges played. */
    private int challengesPlayed = 0;

    /** The available challenges. */
    private List<Integer> availableChallenges = new ArrayList<Integer>();

    /** The completed challenges. */
    private List<Integer> completedChallenges = new ArrayList<Integer>();

    // Challenges used to be called quests. During the renaming,
    // files could be corrupted. These fields ensure old files still work.
    // These fields should be phased out after a little while.
    // The old files, if played once, are updated automatically to the new
    // system.
    /** The quests played. */
    private int questsPlayed = -1;

    /** The available quests. */
    private List<Integer> availableQuests = null;

    /** The completed quests. */
    private List<Integer> completedQuests = null;

    // own randomizer seed
    private long randomSeed = 0;

    // Utility class to access cards, has access to private fields
    // Moved some methods there that otherwise would make this class even more
    // complex
    private transient QuestUtilCards myCards;

    /** The Constant RANK_TITLES. */
    public static final String[] RANK_TITLES = new String[] { "Level 0 - Confused Wizard", "Level 1 - Mana Mage",
            "Level 2 - Death by Megrim", "Level 3 - Shattered the Competition", "Level 4 - Black Knighted",
            "Level 5 - Shockingly Good", "Level 6 - Regressed into Timmy", "Level 7 - Loves Blue Control",
            "Level 8 - Immobilized by Fear", "Level 9 - Lands = Friends", "Level 10 - Forging new paths",
            "Level 11 - Infect-o-tron", "Level 12 - Great Balls of Fire", "Level 13 - Artifact Schmartifact",
            "Level 14 - Mike Mulligan's The Name", "Level 15 - Fresh Air: Good For The Health",
            "Level 16 - In It For The Love", "Level 17 - Sticks, Stones, Bones", "Level 18 - Credits For Breakfast",
            "Level 19 - Millasaurus", "Level 20 - One-turn Wonder", "Teaching Gandalf a Lesson",
            "What Do You Do With The Other Hand?", "Freelance Sorcerer, Works Weekends",
            "Should We Hire Commentators?", "Saltblasted For Your Talent", "Serra Angel Is Your Girlfriend", };

    /**
     * <p>
     * Constructor for QuestData.
     * </p>
     */
    public QuestData() {
        this.initTransients();
        this.myCards.addBasicLands(this.getCardPool(), QuestPreferences.getStartingBasic(),
                QuestPreferences.getStartingSnowBasic());
        this.randomizeOpponents();
    }

    private void initTransients() {
        // These are helper classes that hold no data.
        this.myCards = new QuestUtilCards(this);

        // to avoid NPE some pools will be created here if they are null
        if (null == this.getNewCardList()) {
            this.setNewCardList(new ItemPool<InventoryItem>(InventoryItem.class));
        }
        if (null == this.getShopList()) {
            this.setShopList(new ItemPool<InventoryItem>(InventoryItem.class));
        }

    }

    /**
     * New game.
     * 
     * @param diff
     *            the diff
     * @param m0de
     *            the m0de
     * @param standardStart
     *            the standard start
     */
    public void newGame(final int diff, final String m0de, final boolean standardStart) {
        this.setDifficulty(diff);

        final Predicate<CardPrinted> filter = standardStart
                ? SetUtils.getStandard().getFilterPrinted()
                : CardPrinted.Predicates.Presets.IS_TRUE;

        this.myCards.setupNewGameCardPool(filter, diff);
        this.setCredits(QuestPreferences.getStartingCredits());

        this.mode = m0de;
        this.life = this.mode.equals(QuestData.FANTASY) ? 15 : 20;
    }

    // All belongings
    /**
     * Gets the inventory.
     * 
     * @return the inventory
     */
    public QuestInventory getInventory() {
        return this.inventory;
    }

    /**
     * Gets the pet manager.
     * 
     * @return the pet manager
     */
    public QuestPetManager getPetManager() {
        return this.petManager;
    }

    // Cards - class uses data from here
    /**
     * Gets the cards.
     * 
     * @return the cards
     */
    public QuestUtilCards getCards() {
        return this.myCards;
    }

    // Challenge performance
    /**
     * Gets the challenges played.
     * 
     * @return the challenges played
     */
    public int getChallengesPlayed() {
        // This should be phased out after a while, when
        // old quest decks have been updated. (changes made 19-9-11)
        if (this.questsPlayed != -1) {
            this.challengesPlayed = this.questsPlayed;
            this.questsPlayed = -1;
        }

        return this.challengesPlayed;
    }

    /**
     * Adds the challenges played.
     */
    public void addChallengesPlayed() {
        this.challengesPlayed++;
    }

    /**
     * Gets the available challenges.
     * 
     * @return the available challenges
     */
    public List<Integer> getAvailableChallenges() {
        // This should be phased out after a while, when
        // old quest decks have been updated. (changes made 19-9-11)
        if (this.availableQuests != null) {
            this.availableChallenges = this.availableQuests;
            this.availableQuests = null;
        }

        return this.availableChallenges != null ? new ArrayList<Integer>(this.availableChallenges) : null;
    }

    /**
     * Sets the available challenges.
     * 
     * @param list
     *            the new available challenges
     */
    public void setAvailableChallenges(final List<Integer> list) {
        this.availableChallenges = list;
    }

    /**
     * Clear available challenges.
     */
    public void clearAvailableChallenges() {
        this.availableChallenges.clear();
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
        // This should be phased out after a while, when
        // old quest decks have been updated. (changes made 19-9-11)
        // Also, poorly named - this should be "getLockedChalleneges" or
        // similar.
        if (this.completedQuests != null) {
            this.completedChallenges = this.completedQuests;
            this.completedQuests = null;
        }

        return this.completedChallenges != null ? new ArrayList<Integer>(this.completedChallenges) : null;
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
     * Adds the lost.
     */
    public void addLost() {
        this.lost++;
    }

    /**
     * Gets the win.
     * 
     * @return the win
     */
    public int getWin() {
        return this.win;
    }

    /**
     * Adds the win.
     */
    public void addWin() { // changes getRank()
        this.win++;

        final int winsToLvlUp = QuestPreferences.getWinsForRankIncrease(this.diffIndex);
        if ((this.win % winsToLvlUp) == 0) {
            this.rankIndex++;
        }
    }

    // Life (only fantasy)
    /**
     * Gets the life.
     * 
     * @return the life
     */
    public int getLife() {
        return this.isFantasy() ? this.life : 20;
    }

    /**
     * Adds the life.
     * 
     * @param n
     *            the n
     */
    public void addLife(final int n) {
        this.life += n;
    }

    // Credits
    /**
     * Adds the credits.
     * 
     * @param c
     *            the c
     */
    public void addCredits(final long c) {
        this.setCredits(this.getCredits() + c);
    }

    /**
     * Subtract credits.
     * 
     * @param c
     *            the c
     */
    public void subtractCredits(final long c) {
        this.setCredits(this.getCredits() > c ? this.getCredits() - c : 0);
    }

    /**
     * Gets the credits.
     * 
     * @return the credits
     */
    public long getCredits() {
        return this.credits;
    }

    // Quest mode
    /**
     * Checks if is fantasy.
     * 
     * @return true, if is fantasy
     */
    public boolean isFantasy() {
        return this.mode.equals(QuestData.FANTASY);
    }

    /**
     * Gets the mode.
     * 
     * @return the mode
     */
    public String getMode() {
        return this.mode == null ? "" : this.mode;
    }

    // Difficulty
    /**
     * Gets the difficulty.
     * 
     * @return the difficulty
     */
    public String getDifficulty() {
        return this.difficulty;
    }

    /**
     * Gets the difficulty index.
     * 
     * @return the difficulty index
     */
    public int getDifficultyIndex() {
        return this.diffIndex;
    }

    /**
     * Sets the difficulty.
     * 
     * @param i
     *            the new difficulty
     */
    public void setDifficulty(final int i) {
        this.diffIndex = i;
        this.difficulty = QuestPreferences.getDifficulty(i);
    }

    /**
     * Guess difficulty index.
     */
    public void guessDifficultyIndex() {
        final String[] diffStr = QuestPreferences.getDifficulty();
        for (int i = 0; i < diffStr.length; i++) {
            if (this.difficulty.equals(diffStr[i])) {
                this.diffIndex = i;
            }
        }
    }

    // Level, read-only ( note: it increments in addWin() )
    /**
     * Gets the level.
     * 
     * @return the level
     */
    public int getLevel() {
        return this.rankIndex;
    }

    /**
     * Gets the rank.
     * 
     * @return the rank
     */
    public String getRank() {
        if (this.rankIndex >= QuestData.RANK_TITLES.length) {
            this.rankIndex = QuestData.RANK_TITLES.length - 1;
        }
        return QuestData.RANK_TITLES[this.rankIndex];
    }

    // decks management
    /**
     * Gets the deck names.
     * 
     * @return the deck names
     */
    public List<String> getDeckNames() {
        return new ArrayList<String>(this.getMyDecks().keySet());
    }

    /**
     * Removes the deck.
     * 
     * @param deckName
     *            the deck name
     */
    public void removeDeck(final String deckName) {
        this.getMyDecks().remove(deckName);
    }

    /**
     * Adds the deck.
     * 
     * @param d
     *            the d
     */
    public void addDeck(final Deck d) {
        this.getMyDecks().put(d.getName(), d);
    }

    /**
     * <p>
     * getDeck.
     * </p>
     * 
     * @param deckName
     *            a {@link java.lang.String} object.
     * @return a {@link forge.deck.Deck} object.
     */
    public Deck getDeck(final String deckName) {
        if (!this.getMyDecks().containsKey(deckName)) {
            ErrorViewer.showError(new Exception(),
                    "QuestData : getDeckFromMap(String deckName) error, deck name not found - %s", deckName);
        }
        final Deck d = this.getMyDecks().get(deckName);
        d.clearSideboard();
        return d;
    }

    // randomizer - related
    /**
     * Gets the random seed.
     * 
     * @return the random seed
     */
    public long getRandomSeed() {
        return this.randomSeed;
    }

    /**
     * This method should be called whenever the opponents should change.
     */
    public void randomizeOpponents() {
        this.randomSeed = MyRandom.getRandom().nextLong();
    }

    // SERIALIZATION - related things

    // This must be called by XML-serializer via reflection
    /**
     * Read resolve.
     * 
     * @return the object
     */
    public Object readResolve() {
        this.initTransients();
        return this;
    }

    /**
     * Checks for save file.
     * 
     * @return true, if successful
     */
    public boolean hasSaveFile() {
        return ForgeProps.getFile(NewConstants.Quest.DATA).exists()
                || ForgeProps.getFile(NewConstants.Quest.XMLDATA).exists();
    }

    /**
     * Save data.
     */
    public void saveData() {
        QuestDataIO.saveData(this);
    }

    /**
     * @return the cardPool
     */
    public ItemPool<InventoryItem> getCardPool() {
        return cardPool;
    }

    /**
     * @param cardPool the cardPool to set
     */
    public void setCardPool(ItemPool<InventoryItem> cardPool) {
        this.cardPool = cardPool; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the shopList
     */
    public ItemPool<InventoryItem> getShopList() {
        return shopList;
    }

    /**
     * @param shopList the shopList to set
     */
    public void setShopList(ItemPool<InventoryItem> shopList) {
        this.shopList = shopList; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the newCardList
     */
    public ItemPool<InventoryItem> getNewCardList() {
        return newCardList;
    }

    /**
     * @param newCardList the newCardList to set
     */
    public void setNewCardList(ItemPool<InventoryItem> newCardList) {
        this.newCardList = newCardList; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the myDecks
     */
    public Map<String, Deck> getMyDecks() {
        return myDecks;
    }

    /**
     * @param myDecks the myDecks to set
     */
    public void setMyDecks(Map<String, Deck> myDecks) {
        this.myDecks = myDecks; // TODO: Add 0 to parameter's name.
    }

    /**
     * @param inventory the inventory to set
     */
    public void setInventory(QuestInventory inventory) {
        this.inventory = inventory; // TODO: Add 0 to parameter's name.
    }

    /**
     * @param credits the credits to set
     */
    public void setCredits(long credits) {
        this.credits = credits; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the versionNumber
     */
    public int getVersionNumber() {
        return versionNumber;
    }

    /**
     * @param versionNumber the versionNumber to set
     */
    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber; // TODO: Add 0 to parameter's name.
    }
}
