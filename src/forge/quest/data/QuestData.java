package forge.quest.data;

import forge.*;
import forge.error.ErrorViewer;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.item.QuestInventory;
import forge.quest.data.pet.QuestPetManager;

import java.util.*;


//when you create QuestDataOld and AFTER you copy the AI decks over
//you have to call one of these two methods below
//see Gui_QuestOptions for more details

//
//static readAIQuestDeckFiles(QuestDataOld data, ArrayList aiDeckNames)
//OR non-static readAIQuestDeckFiles()
//which reads the files "questDecks-easy", "questDecks-medium","questDecks-hard",

public class QuestData {
    QuestPreferences preferences = null;

    int rankIndex;
    int win;
    int lost;

    int life;
    private int maxLife;

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

    public static final String FANTASY = "Fantasy";
    public static final String REALISTIC = "Realistic";

    QuestInventory inventory = new QuestInventory();

    //This field holds the version of the Quest Data
    public static final int CURRENT_VERSION_NUMBER = 1;

    //This field places the version number into QD instance,
    //but only when the object is created through the constructor
    //DO NOT RENAME THIS FIELD
    int versionNumber = CURRENT_VERSION_NUMBER;

    QuestPetManager petManager = new QuestPetManager();

    public QuestData() {
        preferences = new QuestPreferences();

        for (int i = 0; i < preferences.getStartingBasic(); i++) {
            cardPool.add("Forest");
            cardPool.add("Mountain");
            cardPool.add("Swamp");
            cardPool.add("Island");
            cardPool.add("Plains");
        }

        for (int i = 0; i < preferences.getStartingSnowBasic(); i++) {
            cardPool.add("Snow-Covered Forest");
            cardPool.add("Snow-Covered Mountain");
            cardPool.add("Snow-Covered Swamp");
            cardPool.add("Snow-Covered Island");
            cardPool.add("Snow-Covered Plains");
        }

        initTransients();
        randomizeOpponents();
    }

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

    public void newGame(int difficulty, String m, boolean standardStart) {
        setDifficulty(difficulty);

        CardList allCards = AllZone.CardFactory.getCards();

        ArrayList<String> list = new ArrayList<String>();

        list.addAll(boosterPack.getQuestStarterDeck(allCards, preferences.getStartingCommons(difficulty),
                preferences.getStartingUncommons(difficulty), preferences.getStartingRares(difficulty), standardStart));

        //because cardPool already has basic land added to it
        cardPool.addAll(list);
        credits = preferences.getStartingCredits();

        mode = m;
        if (mode.equals(FANTASY)) {
            life = 15;
        }
        else {
            life = 20;
        }
    }

    /**
     * This method should be called whenever the opponents should change.
     */
    public void randomizeOpponents() {
        randomSeed = MyRandom.random.nextLong();
    }


    public void saveData() {
        QuestDataIO.saveData(this);
    }


    //returns Strings of the card names

    public List<String> getCardpool() {
        //make a copy so the internal ArrrayList cannot be changed externally
        return new ArrayList<String>(cardPool);
    }

    public List<String> getShopList() {
        if (shopList != null) {
            return new ArrayList<String>(shopList);
        }
        else {
            return null;
        }
    }

    public void setShopList(List<String> list) {
        shopList = list;
    }


    public List<Integer> getAvailableQuests() {
        if (availableQuests != null) {
            return new ArrayList<Integer>(availableQuests);
        }
        else {
            return null;
        }
    }

    public void setAvailableQuests(List<Integer> list) {
        availableQuests = list;
    }

    public void clearAvailableQuests() {
        availableQuests.clear();
    }

    public List<Integer> getCompletedQuests() {
        if (completedQuests != null) {
            return new ArrayList<Integer>(completedQuests);
        }
        else {
            return null;
        }
    }

    public void setCompletedQuests(List<Integer> list) {
        completedQuests = list;
    }


    public void clearShopList() {
        shopList.clear();
    }


    public void removeDeck(String deckName) {
        myDecks.remove(deckName);
    }


    public void addDeck(Deck d) {
        myDecks.put(d.getName(), d);
    }

    //this Deck object is a Constructed deck
    //deck.getDeckType() is Constant.GameType.Sealed
    //sealed since the card pool is the Deck sideboard

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

    public List<String> getDeckNames() {
        return new ArrayList<String>(myDecks.keySet());
    }


    //get new cards that were added to your card pool by addCards()

    public List<String> getAddedCards() {
        return new ArrayList<String>(newCardList);
    }

    //adds 11 cards, to the current card pool
    //(I chose 11 cards instead of 15 in order to make things more challenging)

    public void addCards() {
        CardList cards = AllZone.CardFactory.getCards();
        int nCommon = preferences.getNumCommon();
        int nUncommon = preferences.getNumUncommon();
        int nRare = preferences.getNumRare();

        ArrayList<String> newCards = new ArrayList<String>();
        newCards.addAll(boosterPack.generateCards(cards, nCommon, Constant.Rarity.Common, null));
        newCards.addAll(boosterPack.generateCards(cards, nUncommon, Constant.Rarity.Uncommon, null));
        newCards.addAll(boosterPack.generateCards(cards, nRare, Constant.Rarity.Rare, null));

        cardPool.addAll(newCards);

        //getAddedCards() uses newCardList
        newCardList = newCards;

    }

    public ArrayList<String> addRandomRare(int n) {
        ArrayList<String> newCards = new ArrayList<String>();
        newCards.addAll(boosterPack.generateCards(AllZone.CardFactory.getCards(), n, Constant.Rarity.Rare, null));

        cardPool.addAll(newCards);
        newCardList.addAll(newCards);

        return newCards;
    }

    public String addRandomRare() {
        return addRandomRare(1).get(0);
    }

    public void addCard(Card c) {
        cardPool.add(c.getName());
    }

    public void addCard(String s) {
        cardPool.add(s);
    }

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

    public void addCardToShopList(Card c) {
        shopList.add(c.getName());
    }

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

    public long getCreditsToAdd(QuestMatchState matchState) {
        long creds = (long) (preferences.getMatchRewardBase() + (preferences.getMatchRewardTotalWins() * win));
        String[] wins = matchState.getWinMethods();
        int[] winTurns = matchState.getWinTurns();
        boolean[] mulliganedToZero = matchState.getMulliganedToZero();

        if (matchState.getLose() == 0) {
            creds += preferences.getMatchRewardNoLosses();
        }

        for (String s : wins) {
            if (s != null) {
                if (s.equals("Poison Counters")) {
                    creds += preferences.getMatchRewardPoisonWinBonus();
                }
                else if (s.equals("Milled")) {
                    creds += preferences.getMatchRewardMilledWinBonus();
                }
                else if (s.equals("Battle of Wits") || s.equals("Felidar Sovereign") || s.equals("Helix Pinnacle") ||
                        s.equals("Epic Struggle") || s.equals("Door to Nothingness") || s.equals("Barren Glory") ||
                        s.equals("Near-Death Experience") || s.equals("Mortal Combat") || s.equals("Test of Endurance")) {
                    creds += preferences.getMatchRewardAltWinBonus();
                }
            }
        }
        for (int i : winTurns) {
            if (i == 1) {
                creds += preferences.getMatchRewardWinFirst();
            }
            else if (i <= 5) {
                creds += preferences.getMatchRewardWinByFifth();
            }
            else if (i <= 10) {
                creds += preferences.getMatchRewardWinByTen();
            }
            else if (i <= 15) {
                creds += preferences.getMatchRewardWinByFifteen();
            }
        }


        for (boolean b : mulliganedToZero) {
            if (b == true) {
                creds += preferences.getMatchMullToZero();
            }
        }

        if (inventory.getItemLevel("Estates") == 1) {
            creds *= 1.1;
        }
        else if (inventory.getItemLevel("Estates") == 2) {
            creds *= 1.15;
        }
        else if (inventory.getItemLevel("Estates") == 3) {
            creds *= 1.2;
        }

        this.addCredits(creds);

        return creds;
    }

    //gets all of the cards that are in the cardpool

    public List<String> getCards() {
        //copy CardList in order to keep private variables private
        //if we just return cardPool, it could be changed externally
        return new ArrayList<String>(cardPool);
    }


    public int getTotalNumberOfGames(int difficulty) {
        //-2 because you start a level 1, and the last level is secret
        int numberLevels = rankArray.length - 2;
        int nMatches = preferences.getWinsForRankIncrease(difficulty);

        return numberLevels * nMatches;
    }

    //this changes getRank()

    public void addWin() {
        win++;

        if (win % preferences.getWinsForRankIncrease(diffIndex) == 0) {
            rankIndex++;
        }
    }

    public void addLost() {
        lost++;
    }

    public int getWin() {
        return win;
    }

    public int getLost() {
        return lost;
    }

    //********************FANTASY STUFF START***********************

    public void setLife(int n) {
        life = n;
    }

    public int getLife() {
        return life;
    }

    public void addLife(int n) {
        life += n;
    }


    public int getQuestsPlayed() {
        return questsPlayed;
    }

    public void addQuestsPlayed() {
        questsPlayed++;
    }

    //********************FANTASY STUFF END***********************

    public void addCredits(long c) {
        credits += c;
    }

    public void subtractCredits(long c) {
        credits -= c;
        if (credits < 0) {
            credits = 0;
        }
    }

    public long getCredits() {
        return credits;
    }

    public String getMode() {
        if (mode == null) {
            return "";
        }
        return mode;
    }

    //should be called first, because the difficultly won't change

    public String getDifficulty() {
        return difficulty;
    }

    public int getDifficultyIndex() {
        return diffIndex;
    }

    public void setDifficulty(int i) {
        diffIndex = i;
        difficulty = preferences.getDifficulty(i);
    }

    public void setDifficultyIndex() {
        String[] diffStr = preferences.getDifficulty();
        for (int i = 0; i < diffStr.length; i++) {
            if (difficulty.equals(diffStr[i])) {
                diffIndex = i;
            }
        }
    }

    public String[] getDifficultyChoices() {
        return preferences.getDifficulty();
    }

    public String getRank() {
        //is rankIndex too big?
        if (rankIndex >= rankArray.length) {
            rankIndex = rankArray.length - 1;
        }

        return rankArray[rankIndex];
    }

    //add cards after a certain number of wins or losses

    public boolean shouldAddCards(boolean didWin) {
        int n = preferences.getWinsForBooster(diffIndex);

        if (didWin) {
            return getWin() % n == 0;
        }
        else {
            return getLost() % n == 0;
        }
    }

    public boolean shouldAddAdditionalCards(boolean didWin) {
        float chance = 0.5f;
        if (inventory.getItemLevel("Lucky Coin") == 1) {
            chance = 0.65f;
        }

        float r = MyRandom.random.nextFloat();

        if (didWin) {
            return r <= chance;
        }

        else {
            return false;
        }
    }

    public boolean hasSaveFile() {
        return ForgeProps.getFile(NewConstants.QUEST.DATA).exists() ||
                ForgeProps.getFile(NewConstants.QUEST.XMLDATA).exists();
    }

    public static void main(String[] args) {
        QuestData q = new QuestData();
        for (int i = 0; i < 20; i++) {
            q.addCards();
        }

        for (int i = 0; i < 10; i++) {
            q.saveData();
            q = QuestDataIO.loadData();
        }

        System.exit(1);
    }

    public QuestPetManager getPetManager() {
        return petManager;
    }

    public QuestPreferences getQuestPreferences() {
        return preferences;
    }

    //get new cards that were added to your card pool by addCards()

    public void addToNewList(ArrayList<String> added) {
        newCardList.addAll(added);
    }

    public Object readResolve() {
        initTransients();
        return this;
    }

    public QuestInventory getInventory() {
        return inventory;
    }

    public QuestPreferences getPreferences() {
        return preferences;
    }

    public long getRandomSeed() {
        return randomSeed;
    }
}
