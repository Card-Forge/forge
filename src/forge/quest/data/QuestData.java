package forge.quest.data;

import com.esotericsoftware.minlog.Log;
import com.thoughtworks.xstream.XStream;
import forge.*;
import forge.error.ErrorViewer;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.pet.*;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


//when you create QuestDataOld and AFTER you copy the AI decks over
//you have to call one of these two methods below
//see Gui_QuestOptions for more details

//
//static readAIQuestDeckFiles(QuestDataOld data, ArrayList aiDeckNames)
//OR non-static readAIQuestDeckFiles()
//which reads the files "questDecks-easy", "questDecks-medium","questDecks-hard",
public class QuestData {
    QuestPreferences preferences = null;

    private int rankIndex;
    private int win;
    private int lost;

    private int life;
    private int estatesLevel;
    private int luckyCoinLevel;
    private int sleightOfHandLevel;

    private int gearLevel;

    private int questsPlayed;

    private long credits;
    private int diffIndex;
    private String difficulty;

    private String mode = "";

    private transient Map<String, Deck> aiDecks;
    private transient List<String> easyAIDecks;
    private transient List<String> mediumAIDecks;
    private transient List<String> hardAIDecks;

    private Map<String, Deck> myDecks = new HashMap<String, Deck>();

    //holds String card names
    private List<String> cardPool = new ArrayList<String>();
    private List<String> newCardList = new ArrayList<String>();

    private List<String> shopList = new ArrayList<String>();
    private List<Integer> availableQuests = new ArrayList<Integer>();

    private List<Integer> completedQuests = new ArrayList<Integer>();

    private transient QuestBoosterPack boosterPack;

    //used by shouldAddAdditionalCards()
    private Random random = new Random();

    private transient String[] rankArray;

    public static final String FANTASY = "Fantasy";
    public static final String REALISTIC = "Realistic";

    //TODO: Temporary.
    public boolean useNewQuestUI = false;

    public final QuestPetManager petManager = new QuestPetManager();

    //This field stores the version number of the QuestData format
    private static int VERSION_NUMBER = 0;

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
    }

    private void initTransients() {
        aiDecks = new HashMap<String, Deck>();
        List<String> aiDeckNames = ai_getDeckNames();
        easyAIDecks = readFile(ForgeProps.getFile(NewConstants.QUEST.EASY), aiDeckNames);
        mediumAIDecks = readFile(ForgeProps.getFile(NewConstants.QUEST.MEDIUM), aiDeckNames);
        hardAIDecks = readFile(ForgeProps.getFile(NewConstants.QUEST.HARD), aiDeckNames);

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
    public void newGame(int difficulty, String m) {
        setDifficulty(difficulty);

        ArrayList<String> list = new ArrayList<String>();
        list.addAll(boosterPack.getCommon(preferences.getStartingCommons(difficulty)));
        list.addAll(boosterPack.getUncommon(preferences.getStartingUncommons(difficulty)));
        list.addAll(boosterPack.getRare(preferences.getStartingRares(difficulty)));

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

    public String[] getOpponents() {
        int index = getDifficultyIndex();

        if (getWin() < preferences.getWinsForMediumAI(index)) {
            return getOpponents(easyAIDecks);
        }

        if (getWin() < preferences.getWinsForHardAI(index)) {
            return getOpponents(mediumAIDecks);
        }

        return getOpponents(hardAIDecks);
    }

    public String[] getOpponents(List<String> aiDeck) {
        Collections.shuffle(aiDeck);

        return new String[]{aiDeck.get(0), aiDeck.get(1), aiDeck.get(2)};

    }

    private static List<String> readFile(File file, List<String> aiDecks) {
        ArrayList<String> list = FileUtil.readFile(file);

        //remove any blank lines
        ArrayList<String> noBlankLines = new ArrayList<String>();
        String s;
        for (String aList : list) {
            s = aList.trim();
            if (!s.equals("")) {
                noBlankLines.add(s);
            }
        }
        list = noBlankLines;

        if (list.size() < 3) {
            ErrorViewer.showError(new Exception(),
                    "QuestData : readFile() error, file %s is too short, it must contain at least 3 ai decks names",
                    file);
        }


        for (String aList : list) {
            if (!aiDecks.contains(aList)) {
                aiDecks.add(aList);
            }
        }


        return list;
    }


    static public QuestData loadData() {
        try {
            //read file "questData"
            QuestData data;

            File xmlSaveFile = ForgeProps.getFile(NewConstants.QUEST.XMLDATA);

            //if the new file format does not exist, convert the old one and save it as the new copy

            if (!xmlSaveFile.exists()) {
                data = convertSaveFormat();
                data.saveData();
            }

            else {
                BufferedInputStream bin = new BufferedInputStream(new FileInputStream(xmlSaveFile));
                GZIPInputStream zin = new GZIPInputStream(bin);

                XStream xStream = new XStream();

                data = (QuestData) xStream.fromXML(zin);

                zin.close();
            }
            return data;
        }

        catch (Exception ex) {
            ErrorViewer.showError(ex, "Error loading Quest Data");
            throw new RuntimeException(ex);
        }
    }

    public void saveData() {
        try {
            File f = ForgeProps.getFile(NewConstants.QUEST.XMLDATA);
            BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(f));
            GZIPOutputStream zout = new GZIPOutputStream(bout);

            XStream xStream = new XStream();
            xStream.toXML(this, zout);

            zout.flush();
            zout.close();
        } catch (Exception ex) {
            ErrorViewer.showError(ex, "Error saving Quest Data");
            throw new RuntimeException(ex);
        }
        }

    @SuppressWarnings({"deprecation"})
    private static QuestData convertSaveFormat() {
        forge.QuestData oldData = forge.QuestData.loadData();
        QuestData newData = new QuestData();


        newData.difficulty = oldData.getDifficulty();
        newData.diffIndex = oldData.getDiffIndex();
        newData.rankIndex = oldData.getWin() / newData.preferences.getWinsForRankIncrease(newData.diffIndex);

        newData.win = oldData.getWin();
        newData.lost = oldData.getLost();

        newData.life = oldData.getLife();
        newData.estatesLevel = oldData.getEstatesLevel();
        newData.luckyCoinLevel = oldData.getLuckyCoinLevel();
        newData.sleightOfHandLevel = oldData.getSleightOfHandLevel();
        newData.gearLevel = oldData.getGearLevel();
        newData.questsPlayed = oldData.getQuestsPlayed();
        newData.credits = oldData.getCredits();
        newData.mode = oldData.getMode();

        newData.myDecks = new HashMap<String, Deck>();
        for (String deckName : oldData.getDeckNames()) {
            newData.myDecks.put(deckName, oldData.getDeck(deckName));
        }

        newData.cardPool = oldData.getCardpool();
        newData.newCardList = oldData.getAddedCards();
        newData.shopList = oldData.getShopList();

        newData.availableQuests = oldData.getAvailableQuests();
        newData.completedQuests = oldData.getCompletedQuests();

        QuestPetAbstract newPet;

        if (oldData.getBirdPetLevel() > 0) {
            newPet = new QuestPetBird();
            newPet.setLevel(oldData.getBirdPetLevel());
            newData.petManager.addPet(newPet);
        }
        if (oldData.getHoundPetLevel() > 0) {
            newPet = new QuestPetHound();
            newPet.setLevel(oldData.getHoundPetLevel());
            newData.petManager.addPet(newPet);
        }
        if (oldData.getWolfPetLevel() > 0) {
            newPet = new QuestPetWolf();
            newPet.setLevel(oldData.getWolfPetLevel());
            newData.petManager.addPet(newPet);
        }
        if (oldData.getCrocPetLevel() > 0) {
            newPet = new QuestPetCrocodile();
            newPet.setLevel(oldData.getCrocPetLevel());
            newData.petManager.addPet(newPet);
        }
        if (oldData.getPlantLevel() > 0) {
            newPet = new QuestPetPlant();
            newPet.setLevel(oldData.getPlantLevel());
            newData.petManager.getPlant().setLevel(oldData.getPlantLevel());
        }

        newData.getPetManager().setSelectedPet(null);

        return newData;
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

    public void ai_removeDeck(String deckName) {
        aiDecks.remove(deckName);
    }

    public void addDeck(Deck d) {
        myDecks.put(d.getName(), d);
    }

    public void ai_addDeck(Deck d) {
        aiDecks.put(d.getName(), d);
    }

    //this Deck object is a Constructed deck
    //deck.getDeckType() is Constant.GameType.Sealed
    //sealed since the card pool is the Deck sideboard
    public Deck getDeck(String deckName) {
        //have to always set the card pool aka the Deck sideboard
        //because new cards may have been added to the card pool by addCards()

        //this sets the cards in Deck main
        Deck d = getDeckFromMap(myDecks, deckName);

        //below is probably not needed

        //remove old cards from card pool
        for (int i = 0; i < d.countSideboard(); i++) {
            d.removeSideboard(i);
        }

        //add all cards to card pool
        for (int i = 0; i < cardPool.size(); i++) {
            d.addSideboard(cardPool.get(i).toString());
        }

        return d;
    }

    //this Deck object is a Constructed deck
    //deck.getDeckType() is Constant.GameType.Constructed
    //constructed because the computer can use any card
    public Deck ai_getDeck(String deckName) {
        return getDeckFromMap(aiDecks, deckName);
    }

    public Deck ai_getDeckNewFormat(String deckName) {
        DeckIO deckIO = new NewDeckIO(ForgeProps.getFile(NewConstants.QUEST.DECKS), true);
        Deck aiDeck = deckIO.readDeck(deckName);
        return aiDeck;
    }


    private Deck getDeckFromMap(Map<String, Deck> map, String deckName) {
        if (!map.containsKey(deckName)) {
            ErrorViewer.showError(new Exception(),
                    "QuestData : getDeckFromMap(String deckName) error, deck name not found - %s", deckName);
        }

        return map.get(deckName);
    }

    //returns human player decks
    //returns ArrayList of String deck names
    public List<String> getDeckNames() {
        return getDeckNames_String(myDecks);
    }

    //returns AI computer decks
    //returns ArrayList of String deck names
    public List<String> ai_getDeckNames() {
        return getDeckNames_String(aiDecks);
    }

    //returns ArrayList of Deck String names
    private List<String> getDeckNames_String(Map<String, Deck> map) {
        ArrayList<String> out = new ArrayList<String>();

        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            out.add(it.next().toString());
        }

        return out;
    }

    //get new cards that were added to your card pool by addCards()
    public List<String> getAddedCards() {
        return new ArrayList<String>(newCardList);
    }

    //adds 11 cards, to the current card pool
    //(I chose 11 cards instead of 15 in order to make things more challenging)
    public void addCards() {
        int nCommon = preferences.getNumCommon();
        int nUncommon = preferences.getNumUncommon();
        int nRare = preferences.getNumRare();

        ArrayList<String> newCards = new ArrayList<String>();
        newCards.addAll(boosterPack.getCommon(nCommon));
        newCards.addAll(boosterPack.getUncommon(nUncommon));
        newCards.addAll(boosterPack.getRare(nRare));


        cardPool.addAll(newCards);

        //getAddedCards() uses newCardList
        newCardList = newCards;

    }

    public void addRandomRare(int n) {
        ArrayList<String> newCards = new ArrayList<String>();
        newCards.addAll(boosterPack.getRare(n));

        cardPool.addAll(newCards);
        newCardList.addAll(newCards);
    }

    public String addRandomRare() {

        ArrayList<String> newCards = new ArrayList<String>();
        newCards.addAll(boosterPack.getRare(1));

        cardPool.addAll(newCards);
        newCardList.addAll(newCards);

        Card c = AllZone.CardFactory.getCard(newCards.get(0), AllZone.HumanPlayer);
        c.setCurSetCode(c.getMostRecentSet());
        return CardUtil.buildFilename(c);
        //return GuiDisplayUtil.cleanString(newCards.get(0));
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

        if (getEstatesLevel() == 1) {
            creds *= 1.1;
        }
        else if (getEstatesLevel() == 2) {
            creds *= 1.15;
        }
        else if (getEstatesLevel() == 3) {
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

    public int getEstatesLevel() {
        return estatesLevel;
    }

    public void addEstatesLevel(int n) {
        estatesLevel += n;
    }

    public int getLuckyCoinLevel() {
        return luckyCoinLevel;
    }

    public void addLuckyCoinLevel(int n) {
        luckyCoinLevel += n;
    }

    public int getSleightOfHandLevel() {
        return sleightOfHandLevel;
    }

    public void addSleightOfHandLevel(int n) {
        sleightOfHandLevel += n;
    }

    public int getGearLevel() {
        return gearLevel;
    }

    public void addGearLevel(int n) {
        gearLevel += n;
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
        if (getLuckyCoinLevel() >= 1) {
            chance = 0.65f;
        }

        float r = random.nextFloat();
        Log.debug("Random:" + r);

        if (didWin) {
            return r <= chance;
        }

        else {
            return false;
        }
    }

    public boolean hasSaveFile() {
        return ForgeProps.getFile(NewConstants.QUEST.DATA).exists()||
                ForgeProps.getFile(NewConstants.QUEST.XMLDATA).exists();
    }
    
    public static void main(String[] args) {
        QuestData q = new QuestData();
        for (int i = 0; i < 20; i++) {
            q.addCards();
        }

        for (int i = 0; i < 10; i++) {
            q.saveData();
            q = QuestData.loadData();
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
}
