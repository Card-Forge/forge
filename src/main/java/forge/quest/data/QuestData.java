package forge.quest.data;

import forge.*;
import forge.deck.Deck;
import forge.error.ErrorViewer;
import forge.game.GameLossReason;
import forge.game.GamePlayerRating;
import forge.game.GameSummary;
import forge.game.PlayerIndex;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.item.QuestInventory;
import forge.quest.data.pet.QuestPetManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.code.jyield.Generator;
import com.google.code.jyield.YieldUtils;


//when you create QuestDataOld and AFTER you copy the AI decks over
//you have to call one of these two methods below
//see Gui_QuestOptions for more details

//
//static readAIQuestDeckFiles(QuestDataOld data, ArrayList aiDeckNames)
//OR non-static readAIQuestDeckFiles()
//which reads the files "questDecks-easy", "questDecks-medium","questDecks-hard",

/**
 * <p>QuestData class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class QuestData {

    int rankIndex;
    int win;
    int lost;

    int life;
    //private int maxLife;

    int questsPlayed;

    long credits;
    int diffIndex;
    String difficulty;

    String mode = "";

    Map<String, Deck> myDecks = new HashMap<String, Deck>();

    //holds String card names
    List<String> cardPool = new ArrayList<String>();
    List<String> newCardList = new ArrayList<String>();

    List<String> shopList = new ArrayList<String>();
    List<Integer> availableQuests = new ArrayList<Integer>();

    List<Integer> completedQuests = new ArrayList<Integer>();

    private transient QuestBoosterPack boosterPack;

    //used by shouldAddAdditionalCards()
    private long randomSeed = 0;

    private transient String[] rankArray;

    /** Constant <code>FANTASY="Fantasy"</code> */
    public static final String FANTASY = "Fantasy";
    /** Constant <code>REALISTIC="Realistic"</code> */
    public static final String REALISTIC = "Realistic";

    QuestInventory inventory = new QuestInventory();

    //This field holds the version of the Quest Data
    /** Constant <code>CURRENT_VERSION_NUMBER=1</code> */
    public static final int CURRENT_VERSION_NUMBER = 1;

    //This field places the version number into QD instance,
    //but only when the object is created through the constructor
    //DO NOT RENAME THIS FIELD
    int versionNumber = CURRENT_VERSION_NUMBER;

    QuestPetManager petManager = new QuestPetManager();

    /**
     * <p>Constructor for QuestData.</p>
     */
    public QuestData() {

        for (int i = 0; i < QuestPreferences.getStartingBasic(); i++) {
            cardPool.add("Forest");
            cardPool.add("Mountain");
            cardPool.add("Swamp");
            cardPool.add("Island");
            cardPool.add("Plains");
        }

        for (int i = 0; i < QuestPreferences.getStartingSnowBasic(); i++) {
            cardPool.add("Snow-Covered Forest");
            cardPool.add("Snow-Covered Mountain");
            cardPool.add("Snow-Covered Swamp");
            cardPool.add("Snow-Covered Island");
            cardPool.add("Snow-Covered Plains");
        }

        initTransients();
        randomizeOpponents();
    }

    /**
     * <p>initTransients.</p>
     */
    private void initTransients() {
        rankArray = new String[]{
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
                "Serra Angel is your girlfriend",};

        boosterPack = new QuestBoosterPack();
    }


    //adds cards to card pool and sets difficulty

    /**
     * <p>newGame.</p>
     *
     * @param difficulty a int.
     * @param m a {@link java.lang.String} object.
     * @param standardStart a boolean.
     */
    public void newGame(int difficulty, String m, boolean standardStart) {
        setDifficulty(difficulty);

        Generator<Card> allCards = YieldUtils.toGenerator(AllZone.getCardFactory());

        ArrayList<String> list = new ArrayList<String>();

        list.addAll(boosterPack.getQuestStarterDeck(allCards, QuestPreferences.getStartingCommons(difficulty),
                QuestPreferences.getStartingUncommons(difficulty), QuestPreferences.getStartingRares(difficulty), standardStart));

        //because cardPool already has basic land added to it
        cardPool.addAll(list);
        credits = QuestPreferences.getStartingCredits();

        mode = m;
        if (mode.equals(FANTASY)) {
            life = 15;
        } else {
            life = 20;
        }
    }

    /**
     * This method should be called whenever the opponents should change.
     */
    public void randomizeOpponents() {
        randomSeed = MyRandom.random.nextLong();
    }


    /**
     * <p>saveData.</p>
     */
    public void saveData() {
        QuestDataIO.saveData(this);
    }


    //returns Strings of the card names

    /**
     * <p>getCardpool.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getCardpool() {
        //make a copy so the internal ArrrayList cannot be changed externally
        return new ArrayList<String>(cardPool);
    }

    /**
     * <p>setCardpool.</p>
     *
     * @param pool a {@link java.util.ArrayList} object.
     * @since 1.0.15
     */
    public void setCardpool(ArrayList<String> pool) {
        cardPool = pool;
    }

    /**
     * <p>Getter for the field <code>shopList</code>.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getShopList() {
        if (shopList != null) {
            return new ArrayList<String>(shopList);
        } else {
            return null;
        }
    }

    /**
     * <p>Setter for the field <code>shopList</code>.</p>
     *
     * @param list a {@link java.util.List} object.
     */
    public void setShopList(List<String> list) {
        shopList = list;
    }


    /**
     * <p>Getter for the field <code>availableQuests</code>.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<Integer> getAvailableQuests() {
        if (availableQuests != null) {
            return new ArrayList<Integer>(availableQuests);
        } else {
            return null;
        }
    }

    /**
     * <p>Setter for the field <code>availableQuests</code>.</p>
     *
     * @param list a {@link java.util.List} object.
     */
    public void setAvailableQuests(List<Integer> list) {
        availableQuests = list;
    }

    /**
     * <p>clearAvailableQuests.</p>
     */
    public void clearAvailableQuests() {
        availableQuests.clear();
    }

    /**
     * <p>Getter for the field <code>completedQuests</code>.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<Integer> getCompletedQuests() {
        if (completedQuests != null) {
            return new ArrayList<Integer>(completedQuests);
        } else {
            return null;
        }
    }

    /**
     * <p>Setter for the field <code>completedQuests</code>.</p>
     *
     * @param list a {@link java.util.List} object.
     */
    public void setCompletedQuests(List<Integer> list) {
        completedQuests = list;
    }


    /**
     * <p>clearShopList.</p>
     */
    public void clearShopList() {
        shopList.clear();
    }


    /**
     * <p>removeDeck.</p>
     *
     * @param deckName a {@link java.lang.String} object.
     */
    public void removeDeck(String deckName) {
        myDecks.remove(deckName);
    }


    /**
     * <p>addDeck.</p>
     *
     * @param d a {@link forge.deck.Deck} object.
     */
    public void addDeck(Deck d) {
        myDecks.put(d.getName(), d);
    }

    //this Deck object is a Constructed deck
    //deck.getDeckType() is Constant.GameType.Sealed
    //sealed since the card pool is the Deck sideboard

    /**
     * <p>getDeck.</p>
     *
     * @param deckName a {@link java.lang.String} object.
     * @return a {@link forge.deck.Deck} object.
     */
    public Deck getDeck(String deckName) {
        //have to always set the card pool aka the Deck sideboard
        //because new cards may have been added to the card pool by addCards()

        if (!myDecks.containsKey(deckName)) {
            ErrorViewer.showError(new Exception(),
                    "QuestData : getDeckFromMap(String deckName) error, deck name not found - %s", deckName);
        }

        Deck d = myDecks.get(deckName);

        //below is probably not needed

        //remove old cards from card pool
        for (int i = 0; i < d.countSideboard(); i++) {
            d.removeSideboard(i);
        }

        //add all cards to card pool
        for (int i = 0; i < cardPool.size(); i++) {
            d.addSideboard(cardPool.get(i));
        }

        return d;
    }


    //returns human player decks
    //returns ArrayList of String deck names

    /**
     * <p>getDeckNames.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getDeckNames() {
        return new ArrayList<String>(myDecks.keySet());
    }


    //get new cards that were added to your card pool by addCards()

    /**
     * <p>getAddedCards.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getAddedCards() {
        return new ArrayList<String>(newCardList);
    }

    //adds 11 cards, to the current card pool
    //(I chose 11 cards instead of 15 in order to make things more challenging)

    /**
     * <p>addCards.</p>
     */
    public ArrayList<String> addCards( List<String> setsFilter ) {
        Generator<Card> cards = YieldUtils.toGenerator(AllZone.getCardFactory());
        if ( setsFilter != null )
            cards = CardFilter.getSets(cards, setsFilter);

        int nCommon = QuestPreferences.getNumCommon();
        int nUncommon = QuestPreferences.getNumUncommon();
        int nRare = QuestPreferences.getNumRare();

        ArrayList<String> newCards = new ArrayList<String>();
        newCards.addAll(boosterPack.generateCards(cards, nCommon, Constant.Rarity.Common, null));
        newCards.addAll(boosterPack.generateCards(cards, nUncommon, Constant.Rarity.Uncommon, null));
        newCards.addAll(boosterPack.generateCards(cards, nRare, Constant.Rarity.Rare, null));

        cardPool.addAll(newCards);

        //getAddedCards() uses newCardList
        newCardList = newCards;
        return newCards;
    }

    /**
     * <p>addRandomRare.</p>
     *
     * @param n a int.
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<String> addRandomRare(int n) {
    	Generator<Card> allCards = YieldUtils.toGenerator(AllZone.getCardFactory());
        ArrayList<String> newCards = new ArrayList<String>();
        newCards.addAll(boosterPack.generateCards(allCards, n, Constant.Rarity.Rare, null));

        cardPool.addAll(newCards);
        newCardList.addAll(newCards);

        return newCards;
    }

    /**
     * <p>addRandomRare.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String addRandomRare() {
        return addRandomRare(1).get(0);
    }

    /**
     * <p>addCard.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public void addCard(Card c) {
        cardPool.add(c.getName());
    }

    /**
     * <p>addCard.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void addCard(String s) {
        cardPool.add(s);
    }

    /**
     * <p>removeCard.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public void removeCard(Card c) {

        String s = c.getName();
        if (!cardPool.contains(s)) {
            return;
        }

        for (int i = 0; i < cardPool.size(); i++) {
            String str = cardPool.get(i);
            if (str.equals(s)) {
                cardPool.remove(i);
                break;
            }
        }
    }

    /**
     * <p>addCardToShopList.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public void addCardToShopList(Card c) {
        shopList.add(c.getName());
    }

    /**
     * <p>removeCardFromShopList.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public void removeCardFromShopList(Card c) {
        String s = c.getName();
        if (!shopList.contains(s)) {
            return;
        }

        for (int i = 0; i < shopList.size(); i++) {
            String str = shopList.get(i);
            if (str.equals(s)) {
                shopList.remove(i);
                break;
            }
        }
    }


    public final int getCreditsRewardForAltWin(final GameLossReason whyAiLost) {
        int rewardAltWinCondition = 0;
        switch (whyAiLost) {
            case LifeReachedZero:
                break; // nothing special here, ordinary kill
            case Milled:
                rewardAltWinCondition = QuestPreferences.getMatchRewardMilledWinBonus();
                break;
            case Poisoned:
                rewardAltWinCondition = QuestPreferences.getMatchRewardPoisonWinBonus();
                break;
            case DidNotLoseYet: // must be player's alternate win condition: felidar, helix pinnacle and like this
                rewardAltWinCondition = QuestPreferences.getMatchRewardAltWinBonus();
                break;
            default: // this .checkstyle forces us to write some idiotic code
                rewardAltWinCondition = 0;
        }
        return rewardAltWinCondition;
    }

    public final int getCreditsRewardForWinByTurn(final int iTurn) {
        if (iTurn == 1) {
            return QuestPreferences.getMatchRewardWinFirst();
        } else if (iTurn <= 5) {
            return QuestPreferences.getMatchRewardWinByFifth();
        } else if (iTurn <= 10) {
            return QuestPreferences.getMatchRewardWinByTen();
        } else if (iTurn <= 15) {
            return QuestPreferences.getMatchRewardWinByFifteen();
        }
        return 0;
    }

    /**
     * <p>getCreditsToAdd.</p>
     *
     * @param matchState a {@link forge.quest.data.QuestMatchState} object.
     * @return a long.
     */
    public final long getCreditsToAdd(final QuestMatchState matchState) {
        long creds = (long) (QuestPreferences.getMatchRewardBase()
                + (QuestPreferences.getMatchRewardTotalWins() * win));

        boolean hasNeverLost = true;
        for (GameSummary game : matchState.getGamesPlayed()) {
            if (game.isAIWinner()) {
                hasNeverLost = true;
                continue; // no rewards for losing a game
            }

            GamePlayerRating aiRating = game.getPlayerRating(PlayerIndex.AI);
            GamePlayerRating humanRating = game.getPlayerRating(PlayerIndex.HUMAN);
            GameLossReason whyAiLost = aiRating.getLossReason();

            creds += getCreditsRewardForAltWin(whyAiLost);
            creds += getCreditsRewardForWinByTurn(game.getTurnGameEnded());

            int cntCardsHumanStartedWith = humanRating.getOpeningHandSize();
            if (0 == cntCardsHumanStartedWith) {
                creds += QuestPreferences.getMatchMullToZero();
            }
        }

        if (hasNeverLost) {
            creds += QuestPreferences.getMatchRewardNoLosses();
        }

        switch(inventory.getItemLevel("Estates")) {
            case 1: creds *= 1.1; break;
            case 2: creds *= 1.15; break;
            case 3: creds *= 1.2; break;
            default: break;
        }

        return creds;
    }

    //gets all of the cards that are in the cardpool

    /**
     * <p>getCards.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getCards() {
        //copy CardList in order to keep private variables private
        //if we just return cardPool, it could be changed externally
        return new ArrayList<String>(cardPool);
    }


    /**
     * <p>getTotalNumberOfGames.</p>
     *
     * @param difficulty a int.
     * @return a int.
     */
    public int getTotalNumberOfGames(int difficulty) {
        //-2 because you start a level 1, and the last level is secret
        int numberLevels = rankArray.length - 2;
        int nMatches = QuestPreferences.getWinsForRankIncrease(difficulty);

        return numberLevels * nMatches;
    }

    //this changes getRank()

    /**
     * <p>addWin.</p>
     */
    public void addWin() {
        win++;

        if (win % QuestPreferences.getWinsForRankIncrease(diffIndex) == 0) {
            rankIndex++;
        }
    }

    /**
     * <p>addLost.</p>
     */
    public void addLost() {
        lost++;
    }

    /**
     * <p>Getter for the field <code>win</code>.</p>
     *
     * @return a int.
     */
    public int getWin() {
        return win;
    }

    /**
     * <p>Getter for the field <code>lost</code>.</p>
     *
     * @return a int.
     */
    public int getLost() {
        return lost;
    }

    //********************FANTASY STUFF START***********************

    /**
     * <p>Setter for the field <code>life</code>.</p>
     *
     * @param n a int.
     */
    public void setLife(int n) {
        life = n;
    }

    /**
     * <p>Getter for the field <code>life</code>.</p>
     *
     * @return a int.
     */
    public int getLife() {
        return life;
    }

    /**
     * <p>addLife.</p>
     *
     * @param n a int.
     */
    public void addLife(int n) {
        life += n;
    }


    /**
     * <p>Getter for the field <code>questsPlayed</code>.</p>
     *
     * @return a int.
     */
    public int getQuestsPlayed() {
        return questsPlayed;
    }

    /**
     * <p>addQuestsPlayed.</p>
     */
    public void addQuestsPlayed() {
        questsPlayed++;
    }

    //********************FANTASY STUFF END***********************

    /**
     * <p>addCredits.</p>
     *
     * @param c a long.
     */
    public void addCredits(long c) {
        credits += c;
    }

    /**
     * <p>subtractCredits.</p>
     *
     * @param c a long.
     */
    public void subtractCredits(long c) {
        credits -= c;
        if (credits < 0) {
            credits = 0;
        }
    }

    /**
     * <p>Getter for the field <code>credits</code>.</p>
     *
     * @return a long.
     */
    public long getCredits() {
        return credits;
    }

    /**
     * <p>Getter for the field <code>mode</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMode() {
        if (mode == null) {
            return "";
        }
        return mode;
    }

    //should be called first, because the difficultly won't change

    /**
     * <p>Getter for the field <code>difficulty</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDifficulty() {
        return difficulty;
    }

    /**
     * <p>getDifficultyIndex.</p>
     *
     * @return a int.
     */
    public int getDifficultyIndex() {
        return diffIndex;
    }

    /**
     * <p>Setter for the field <code>difficulty</code>.</p>
     *
     * @param i a int.
     */
    public void setDifficulty(int i) {
        diffIndex = i;
        difficulty = QuestPreferences.getDifficulty(i);
    }

    /**
     * <p>setDifficultyIndex.</p>
     */
    public void setDifficultyIndex() {
        String[] diffStr = QuestPreferences.getDifficulty();
        for (int i = 0; i < diffStr.length; i++) {
            if (difficulty.equals(diffStr[i])) {
                diffIndex = i;
            }
        }
    }

    /**
     * <p>getDifficultyChoices.</p>
     *
     * @return an array of {@link java.lang.String} objects.
     */
    public String[] getDifficultyChoices() {
        return QuestPreferences.getDifficulty();
    }

    /**
     * <p>getRank.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getRank() {
        //is rankIndex too big?
        if (rankIndex >= rankArray.length) {
            rankIndex = rankArray.length - 1;
        }

        return rankArray[rankIndex];
    }

    /**
     * <p>getLevel.</p>
     *
     * @return a int.
     * @since 1.0.15
     */
    public int getLevel() {
        return rankIndex;
    }

    //add cards after a certain number of wins or losses

    /**
     * <p>shouldAddCards.</p>
     *
     * @param didWin a boolean.
     * @return a boolean.
     */
    public boolean shouldAddCards(boolean didWin) {
        int n = QuestPreferences.getWinsForBooster(diffIndex);

        if (didWin) {
            return getWin() % n == 0;
        } else {
            return getLost() % n == 0;
        }
    }

    /**
     * <p>shouldAddAdditionalCards.</p>
     *
     * @param didWin a boolean.
     * @return a boolean.
     */
    public boolean shouldAddAdditionalCards(boolean didWin) {
        float chance = 0.5f;
        if (inventory.getItemLevel("Lucky Coin") == 1) {
            chance = 0.65f;
        }

        float r = MyRandom.random.nextFloat();

        if (didWin) {
            return r <= chance;
        } else {
            return false;
        }
    }

    /**
     * <p>hasSaveFile.</p>
     *
     * @return a boolean.
     */
    public boolean hasSaveFile() {
        return ForgeProps.getFile(NewConstants.QUEST.DATA).exists() ||
                ForgeProps.getFile(NewConstants.QUEST.XMLDATA).exists();
    }

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link java.lang.String} objects.
     */
    public static void main(String[] args) {
        QuestData q = new QuestData();
        for (int i = 0; i < 20; i++) {
            q.addCards( null );
        }

        for (int i = 0; i < 10; i++) {
            q.saveData();
            q = QuestDataIO.loadData();
        }

        System.exit(1);
    }

    /**
     * <p>Getter for the field <code>petManager</code>.</p>
     *
     * @return a {@link forge.quest.data.pet.QuestPetManager} object.
     */
    public QuestPetManager getPetManager() {
        return petManager;
    }

    //get new cards that were added to your card pool by addCards()

    /**
     * <p>addToNewList.</p>
     *
     * @param added a {@link java.util.ArrayList} object.
     */
    public void addToNewList(ArrayList<String> added) {
        newCardList.addAll(added);
    }

    /**
     * <p>readResolve.</p>
     *
     * @return a {@link java.lang.Object} object.
     */
    public Object readResolve() {
        initTransients();
        return this;
    }

    /**
     * <p>Getter for the field <code>inventory</code>.</p>
     *
     * @return a {@link forge.quest.data.item.QuestInventory} object.
     */
    public QuestInventory getInventory() {
        return inventory;
    }

    /**
     * <p>Getter for the field <code>randomSeed</code>.</p>
     *
     * @return a long.
     */
    public long getRandomSeed() {
        return randomSeed;
    }
}
