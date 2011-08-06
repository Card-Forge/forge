
package forge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import com.esotericsoftware.minlog.Log;

import forge.error.ErrorViewer;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;


//when you create QuestData and AFTER you copy the AI decks over
//you have to call one of these two methods below
//see Gui_QuestOptions for more details
//
//static readAIQuestDeckFiles(QuestData data, ArrayList aiDeckNames)
//OR non-static readAIQuestDeckFiles()
//which reads the files "questDecks-easy", "questDecks-medium","questDecks-hard",
public class QuestData implements NewConstants {
	QuestData_Prefs qdPrefs = null;

    private int                   rankIndex;
    private int                   win;
    private int                   lost;
    
    private int 				  plantLevel;
    private int					  wolfPetLevel;
    private int					  crocPetLevel;
    private int 				  birdPetLevel;
    private int					  houndPetLevel;
    
    private String			      selectedPet;
    
    private int 				  life;
    private int 				  estatesLevel;
    private int					  luckyCoinLevel;
    private int					  sleightOfHandLevel;
    private int					  gearLevel;
    
    private int					  questsPlayed;
    
    private long				  credits;
    
    private int					  diffIndex;
    private String             	  difficulty;
    private String				  mode = "";

    private ArrayList<String>     easyAIDecks;
    private ArrayList<String>     mediumAIDecks;
    private ArrayList<String>     hardAIDecks;
    
    private Map<String, Deck>     myDecks         = new HashMap<String, Deck>();
    private Map<String, Deck>     aiDecks         = new HashMap<String, Deck>();
    
    //holds String card names
    private ArrayList<String>     cardPool        = new ArrayList<String>();
    private ArrayList<String>     newCardList     = new ArrayList<String>();
    private ArrayList<String> 	  shopList		  = new ArrayList<String>();
    
    private ArrayList<Integer>    availableQuests = new ArrayList<Integer>();
    private ArrayList<Integer>    completedQuests = new ArrayList<Integer>();
    
    private QuestData_BoosterPack boosterPack     = new QuestData_BoosterPack();
    
    //used by shouldAddAdditionalCards()
    private Random                random          = new Random();
    
    //feel free to change this to something funnier
    private String[]              rankArray       = {
            "Level 0 - Confused Wizard", "Level 1 - Mana Mage", "Level 2 - Death by Megrim",
            "Level 3 - Shattered the Competition", "Level 4 - Black Knighted", "Level 5 - Shockingly Good",
            "Level 6 - Regressed into Timmy", "Level 7 - Loves Blue Control", "Level 8 - Immobilized by Fear",
            "Level 9 - Lands = Friends", "Saltblasted for your talent", "Serra Angel is your girlfriend",};

    public QuestData(){
    	qdPrefs = new QuestData_Prefs();		
    	
        for(int i = 0; i < qdPrefs.getStartingBasic(); i++) {
            cardPool.add("Forest");
            cardPool.add("Mountain");
            cardPool.add("Swamp");
            cardPool.add("Island");
            cardPool.add("Plains");
        }
        
        for(int i = 0; i < qdPrefs.getStartingSnowBasic(); i++) {
            cardPool.add("Snow-Covered Forest");
            cardPool.add("Snow-Covered Mountain");
            cardPool.add("Snow-Covered Swamp");
            cardPool.add("Snow-Covered Island");
            cardPool.add("Snow-Covered Plains");
        }
    }//QuestData
    
    //adds cards to card pool and sets difficulty
    public void newGame(int difficulty, String m) {
        setDifficulty(difficulty);
        
        ArrayList<String> list = new ArrayList<String>();
        list.addAll(boosterPack.getCommon(qdPrefs.getStartingCommons(difficulty)));
        list.addAll(boosterPack.getUncommon(qdPrefs.getStartingUncommons(difficulty)));
        list.addAll(boosterPack.getRare(qdPrefs.getStartingRares(difficulty)));
        
        //because cardPool already has basic land added to it
        cardPool.addAll(list);
        credits = qdPrefs.getStartingCredits();
        
        mode = m;
        if (mode.equals("Fantasy"))
        	life = 15;
        else
        	life = 20;
    }
    
    
    public String[] getOpponents() {     
        int index = getDiffIndex();
        
        if(getWin() < qdPrefs.getWinsForMediumAI(index)) return getOpponents(easyAIDecks);
        
        if(getWin() < qdPrefs.getWinsForHardAI(index)) return getOpponents(mediumAIDecks);
        
        return getOpponents(hardAIDecks);
    }//getOpponents()
    

    static public void readAIQuestDeckFiles(QuestData data, ArrayList<String> aiDeckNames) {
        data.easyAIDecks = readFile(ForgeProps.getFile(QUEST.EASY), aiDeckNames);
        data.mediumAIDecks = readFile(ForgeProps.getFile(QUEST.MEDIUM), aiDeckNames);
        data.hardAIDecks = readFile(ForgeProps.getFile(QUEST.HARD), aiDeckNames);
    }
    
    public void refreshAIQuestDeckFiles(ArrayList<String> aiDeckNames) {
        easyAIDecks = readFile(ForgeProps.getFile(QUEST.EASY), aiDeckNames);
        mediumAIDecks = readFile(ForgeProps.getFile(QUEST.MEDIUM), aiDeckNames);
        hardAIDecks = readFile(ForgeProps.getFile(QUEST.HARD), aiDeckNames);
    }
    
    public String[] getOpponents(ArrayList<String> aiDeck) {
        Collections.shuffle(aiDeck);
        
        return new String[] {aiDeck.get(0).toString(), aiDeck.get(1).toString(), aiDeck.get(2).toString()};
        
    }//getOpponents()
    
    private static ArrayList<String> readFile(File file, ArrayList<String> aiDecks) {
        ArrayList<String> list = FileUtil.readFile(file);
        
        //remove any blank lines
        ArrayList<String> noBlankLines = new ArrayList<String>();
        String s;
        for(int i = 0; i < list.size(); i++) {
            s = list.get(i).toString().trim();
            if(!s.equals("")) noBlankLines.add(s);
        }
        list = noBlankLines;
        
        if(list.size() < 3) ErrorViewer.showError(new Exception(),
                "QuestData : readFile() error, file %s is too short, it must contain at least 3 ai decks names",
                file);
        

        for(int i = 0; i < list.size(); i++)
            /*if(!aiDecks.contains(list.get(i).toString())) ErrorViewer.showError(new Exception(),
                    "QuestData : readFile() error, file %s contains the invalid ai deck name: %s", file,
                    list.get(i));*/
        	if (!aiDecks.contains(list.get(i).toString()))
        	{
        		 aiDecks.add(list.get(i).toString());
        	}
        

        return list;
    }//readFile()
    
    public void readAIQuestDeckFiles() {
        readAIQuestDeckFiles(this, ai_getDeckNames());
    }
    
    static public QuestData loadData() {
        try {
            //read file "questData"
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(ForgeProps.getFile(QUEST.DATA)));
            Object o = in.readObject();
            in.close();
            
            QuestData_State state = (QuestData_State) o;
            
            QuestData data = new QuestData();
            
            data.win = state.win;
            data.lost = state.lost;
            data.credits = state.credits;
            data.rankIndex = state.rankIndex;
            data.difficulty = state.difficulty;
            
            data.mode = state.mode;
            if (data.mode == null)
            	data.mode = "Realistic";
            
            data.plantLevel = state.plantLevel;
            data.wolfPetLevel = state.wolfPetLevel;
            data.crocPetLevel = state.crocPetLevel;
            data.birdPetLevel = state.birdPetLevel;
            data.houndPetLevel = state.houndPetLevel;
            data.selectedPet  = state.selectedPet;
            data.life = state.life;
            data.estatesLevel = state.estatesLevel;
            data.luckyCoinLevel = state.luckyCoinLevel;
            data.sleightOfHandLevel = state.sleightOfHandLevel;
            data.gearLevel = state.gearLevel;
            data.questsPlayed = state.questsPlayed;
            data.availableQuests = state.availableQuests;
            data.completedQuests = state.completedQuests;
            
            data.shopList = state.shopList;
            data.cardPool = state.cardPool;
            data.myDecks = state.myDecks;
            data.aiDecks = state.aiDecks;
            
            readAIQuestDeckFiles(data, new ArrayList<String>(data.aiDecks.keySet()));
            
            return data;
        }//try
        catch(Exception ex) {
            ErrorViewer.showError(ex, "Error loading Quest Data");
            throw new RuntimeException(ex);
        }
    }//loadData()
    

    //returns Strings of the card names
    public ArrayList<String> getCardpool() {
        //make a copy so the internal ArrrayList cannot be changed externally
        return new ArrayList<String>(cardPool);
    }
    
    public ArrayList<String> getShopList() {
    	if (shopList != null)
    		return new ArrayList<String>(shopList);
    	else 
    		return null;
    }
    
    public void setShopList(ArrayList<String> list)
    {
    	shopList = list;
    }
    

    public ArrayList<Integer> getAvailableQuests() {
    	if (availableQuests != null)
    		return new ArrayList<Integer>(availableQuests);
    	else
    		return null;
    }
    
    public void setAvailableQuests(ArrayList<Integer> list)
    {
    	availableQuests = list;
    }
    
    public void clearAvailableQuests()
    {
    	availableQuests.clear();
    }
    
    public ArrayList<Integer> getCompletedQuests() {
    	if (completedQuests != null)
    		return new ArrayList<Integer>(completedQuests);
    	else
    		return null;
    }
    
    public void setCompletedQuests(ArrayList<Integer> list)
    {
    	completedQuests = list;
    }
    
    
    public void clearShopList() {
    	shopList.clear();
    }
    
    //rename - removeDeck, addDeck
    //copy - addDeck
    
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
        for(int i = 0; i < d.countSideboard(); i++)
            d.removeSideboard(i);
        
        //add all cards to card pool
        for(int i = 0; i < cardPool.size(); i++)
            d.addSideboard(cardPool.get(i).toString());
        
        return d;
    }
    
    //this Deck object is a Constructed deck
    //deck.getDeckType() is Constant.GameType.Constructed
    //constructed because the computer can use any card
    public Deck ai_getDeck(String deckName) {
        return getDeckFromMap(aiDecks, deckName);
    }
    
    public Deck ai_getDeckNewFormat(String deckName) {
    	DeckIO deckIO = new NewDeckIO(ForgeProps.getFile(QUEST.DECKS), true);
    	Deck aiDeck = deckIO.readDeck(deckName);
    	return aiDeck;
    }
    
    
    private Deck getDeckFromMap(Map<String, Deck> map, String deckName) {
        if(!map.containsKey(deckName)) ErrorViewer.showError(new Exception(),
                "QuestData : getDeckFromMap(String deckName) error, deck name not found - %s", deckName);
        
        return map.get(deckName);
    }
    
    //returns human player decks
    //returns ArrayList of String deck names
    public ArrayList<String> getDeckNames() {
        return getDeckNames_String(myDecks);
    }//getDecks()
    
    //returns AI computer decks
    //returns ArrayList of String deck names
    public ArrayList<String> ai_getDeckNames() {
        return getDeckNames_String(aiDecks);
    }
    
    //returns ArrayList of Deck String names
    private ArrayList<String> getDeckNames_String(Map<String, Deck> map) {
        ArrayList<String> out = new ArrayList<String>();
        
        Iterator<String> it = map.keySet().iterator();
        while(it.hasNext())
            out.add(it.next().toString());
        
        return out;
    }
    
    //get new cards that were added to your card pool by addCards()
    public ArrayList<String> getAddedCards() {
        return new ArrayList<String>(newCardList);
    }
    
    //adds 11 cards, to the current card pool
    //(I chose 11 cards instead of 15 in order to make things more challenging)
    public void addCards() {
        int nCommon = qdPrefs.getNumCommon();
        int nUncommon = qdPrefs.getNumUncommon();
        int nRare = qdPrefs.getNumRare();
        
        ArrayList<String> newCards = new ArrayList<String>();
        newCards.addAll(boosterPack.getCommon(nCommon));
        newCards.addAll(boosterPack.getUncommon(nUncommon));
        newCards.addAll(boosterPack.getRare(nRare));
        

        cardPool.addAll(newCards);
        
        //getAddedCards() uses newCardList
        newCardList = newCards;
        
    }//addCards()
    
    public ArrayList<String> getRandomRares(int n, int colorIndex)
    {
    	ArrayList<String> newCards = new ArrayList<String>();
    	newCards.addAll(boosterPack.getRare(n, colorIndex));
    	
    	/*
    	for(String s : newCards ) {
    		Card c = AllZone.CardFactory.getCard(s, AllZone.HumanPlayer);
    		list.add(c);
    	}*/
    	return newCards;
    }
    
    public void addRandomRare(int n)
    {
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
    
    public void addCard(Card c)
    {
    	cardPool.add(c.getName());
    }
    
    public void addCard(String s)
    {
    	cardPool.add(s);
    }
    
    public void removeCard(Card c)
    {
    	
    	String s = c.getName();
    	if (!cardPool.contains(s))
    		return;
    	
    	for(int i=0;i<cardPool.size();i++)
    	{
    		String str = cardPool.get(i);
    		if (str.equals(s)){
    			cardPool.remove(i);
    			break;
    		}
    	}
    }
    
    public void addCardToShopList(Card c)
    {
    	shopList.add(c.getName());
    }
    
    public void removeCardFromShopList(Card c)
    {
    	String s = c.getName();
    	if (!shopList.contains(s))
    		return;
    	
    	for(int i=0;i<shopList.size();i++)
    	{
    		String str = shopList.get(i);
    		if (str.equals(s)){
    			shopList.remove(i);
    			break;
    		}
    	}
    }
    
    public long getCreditsToAdd(WinLose winLose)
    {
    	long creds = (long) (qdPrefs.getMatchRewardBase() + (qdPrefs.getMatchRewardTotalWins() * win));
    	String[] wins = winLose.getWinMethods();
    	int[] winTurns = winLose.getWinTurns();
    	boolean[] mulliganedToZero = winLose.getMulliganedToZero();
    	
    	if (winLose.getLose() == 0)
    		creds += qdPrefs.getMatchRewardNoLosses();
    	
    	for(String s : wins)
    	{
    		if (s != null) {
    			if (s.equals("Poison Counters"))
    				creds += qdPrefs.getMatchRewardPoisonWinBonus();
    			else if (s.equals("Milled"))
    				creds += qdPrefs.getMatchRewardMilledWinBonus();
    			else if (s.equals("Battle of Wits") || s.equals("Felidar Sovereign") || s.equals("Helix Pinnacle") || 
    					 s.equals("Epic Struggle") || s.equals("Door to Nothingness") || s.equals("Barren Glory") ||
    					 s.equals("Near-Death Experience") || s.equals("Mortal Combat") || s.equals("Test of Endurance")) {
	    			creds += qdPrefs.getMatchRewardAltWinBonus();
	    		}
    		}
    	}
    	for (int i : winTurns)
    	{
    		if (i == 1)
    			creds += qdPrefs.getMatchRewardWinFirst();
			else if (i <= 5)
				creds += qdPrefs.getMatchRewardWinByFifth();
			else if (i <= 10)
				creds += qdPrefs.getMatchRewardWinByTen();
			else if (i <= 15)
				creds += qdPrefs.getMatchRewardWinByFifteen();
    	}
    	
    	
    	for (boolean b : mulliganedToZero)
    	{
    		if (b == true)
    			creds += qdPrefs.getMatchMullToZero();
    	}
    	
    	if (getEstatesLevel() == 1)
    		creds*=1.1;
    	else if (getEstatesLevel() == 2)
    		creds*=1.15;
    	else if (getEstatesLevel() == 3)
    		creds*=1.2;
    	
    	this.addCredits(creds);
    	
    	return creds;
    }
    //gets all of the cards that are in the cardpool
    public ArrayList<String> getCards() {
        //copy CardList in order to keep private variables private
        //if we just return cardPool, it could be changed externally
        return new ArrayList<String>(cardPool);
    }
    
    
    public int getTotalNumberOfGames(int difficulty) {
        //-2 because you start a level 1, and the last level is secret
        int numberLevels = rankArray.length - 2;
        int nMatches = qdPrefs.getWinsForRankIncrease(difficulty);
        
        return numberLevels * nMatches;
    }
    
    //this changes getRank()
    public void addWin() {
        win++;
               
        if(win % qdPrefs.getWinsForRankIncrease(diffIndex) == 0) rankIndex++;
    }//addWin()
    
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
    
    public void addPlantLevel()
    {
    	plantLevel++;
    }
    
    public int getPlantLevel()
    {
    	return plantLevel;
    }
    
    public void addWolfPetLevel()
    {
    	wolfPetLevel++;
    }
    
    public int getWolfPetLevel()
    {
    	return wolfPetLevel;
    }
    
    public void addCrocPetLevel()
    {
    	crocPetLevel++;
    }
    
    public int getCrocPetLevel()
    {
    	return crocPetLevel;
    }
    
    public void addBirdPetLevel()
    {
    	birdPetLevel++;
    }
    
    public int getBirdPetLevel()
    {
    	return birdPetLevel;
    }
    
    public void addHoundPetLevel()
    {
    	houndPetLevel++;
    }
    
    public int getHoundPetLevel()
    {
    	return houndPetLevel;
    }
    
    public void setSelectedPet(String s)
    {
    	selectedPet = s;
    }
    
    public String getSelectedPet()
    {
    	return selectedPet;
    }
    
    
    public void setLife(int n)
    {
    	life = n;
    }
    
    public int getLife()
    {
    	return life;
    }
    
    public void addLife(int n)
    {
    	life+=n;
    }
    
    public int getEstatesLevel()
    {
    	return estatesLevel;
    }
    
    public void addEstatesLevel(int n)
    {
    	estatesLevel+=n;
    }
    
    public int getLuckyCoinLevel()
    {
    	return luckyCoinLevel;
    }
    
    public void addLuckyCoinLevel(int n)
    {
    	luckyCoinLevel+=n;
    }
    
    public int getSleightOfHandLevel()
    {
    	return sleightOfHandLevel;
    }
    
    public void addSleightOfHandLevel(int n)
    {
    	sleightOfHandLevel+=n;
    }
    
    public int getGearLevel()
    {
    	return gearLevel;
    }
    
    public void addGearLevel(int n)
    {
    	gearLevel+=n;
    }
    
    public int getQuestsPlayed()
    {
    	return questsPlayed;
    }
    
    public void addQuestsPlayed()
    {
    	questsPlayed++;
    }
    
  //********************FANTASY STUFF END***********************
    
    public void addCredits(long c)
    {
    	credits+=c;
    }
    
    public void subtractCredits(long c)
    {
    	credits-=c;
    	if (credits < 0)
    		credits = 0;
    }
    
    public long getCredits() {
    	return credits;
    }
    
    public String getMode() {
    	if (mode == null)
    		return "";
    	return mode;
    }
    //should be called first, because the difficultly won't change
    public String getDifficulty() {
        return difficulty;
    }
    
    public int getDiffIndex() {
        return diffIndex;
    }
    
    public void setDifficulty(int i) {
    	diffIndex = i;
    	difficulty = qdPrefs.getDifficulty(i);
    }
    
	public void setDifficultyIndex() {
		String[] diffStr = qdPrefs.getDifficulty();
		for(int i = 0; i < diffStr.length; i++)
			if (difficulty.equals(diffStr[i]))
				diffIndex = i;
	}
    
    public String[] getDifficutlyChoices() {
        return qdPrefs.getDifficulty();
    }
    
    public String getRank() {
        //is rankIndex too big?
        if(rankIndex == rankArray.length) rankIndex--;
        
        return rankArray[rankIndex];
    }
    
    //add cards after a certain number of wins or losses
    public boolean shouldAddCards(boolean didWin) {
        int n = qdPrefs.getWinsForBooster(diffIndex);
        
        if(didWin) return getWin() % n == 0;
        else return getLost() % n == 0;
    }
    
    public boolean shouldAddAdditionalCards(boolean didWin) {
    	float chance = 0.5f;
    	if (getLuckyCoinLevel() >= 1)
    		chance = 0.65f;
    	
    	float r = random.nextFloat();
    	Log.debug("Random:" + r);
    	
    	if(didWin) return r <= chance;
        	
        else return false;
    }
    
    
    //opponentName is one of the Strings returned by getOpponents()
    public Deck getOpponentDeck(String opponentName) {
        return null;
    }
    
    public boolean hasSaveFile() {
        //File f = new File(this.saveFileName); // The static field QuestData.saveFileName should be accessed in a static way
        // No warning is given for it below in getBackupFilename
        return ForgeProps.getFile(QUEST.DATA).exists();
    }
    
    //returns somethings like "questData-10"
    //find a new filename
    @SuppressWarnings("unused")
    static private File getBackupFilename() {
        //I made this a long because maybe an int would overflow, but who knows
        File original = ForgeProps.getFile(QUEST.DATA);
        File parent = original.getParentFile();
        String name = original.getName();
        long n = 1;
        
        File f;
        while((f = new File(parent, name + "-" + n)).exists())
            n++;
        
        return f;
    }//getBackupFilename()
    
    static public void saveData(QuestData q) {
        try {
            /*	
                  //rename previous file "questData" to something like questData-23
                  //just in case there is an error when playing the game or saving
                  File file = new File(saveFileName);
                  if(file.exists())
                    file.renameTo(getBackupFilename());
            */

            //setup QuestData_State
            QuestData_State state = new QuestData_State();
            state.win = q.win;
            state.lost = q.lost;
            state.credits = q.credits;
            state.difficulty = q.difficulty;
            state.mode = q.mode;
            state.rankIndex = q.rankIndex;
            
            state.plantLevel = q.plantLevel;
            state.wolfPetLevel = q.wolfPetLevel;
            state.crocPetLevel = q.crocPetLevel;
            state.birdPetLevel = q.birdPetLevel;
            state.houndPetLevel = q.houndPetLevel;
            state.selectedPet = q.selectedPet;
            state.life = q.life;
            state.estatesLevel = q.estatesLevel;
            state.luckyCoinLevel = q.luckyCoinLevel;
            state.sleightOfHandLevel = q.sleightOfHandLevel;
            state.gearLevel = q.gearLevel;
            state.questsPlayed = q.questsPlayed;
            state.availableQuests = q.availableQuests;
            
            state.cardPool = q.cardPool;
            state.shopList = q.shopList;
            state.myDecks = q.myDecks;
            state.aiDecks = q.aiDecks;
            
            //write object
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(ForgeProps.getFile(QUEST.DATA)));
            out.writeObject(state);
            out.flush();
            out.close();
        } catch(Exception ex) {
            ErrorViewer.showError(ex, "Error saving Quest Data");
            throw new RuntimeException(ex);
        }
    }//saveData()
    
    public static void main(String[] args) {
        QuestData q = new QuestData();
        for(int i = 0; i < 20; i++)
            q.addCards();
        
        for(int i = 0; i < 10; i++) {
            QuestData.saveData(q);
            QuestData.loadData();
        }
        
        System.exit(1);
    }
}
