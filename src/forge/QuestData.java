
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
    public final String           EASY            = "Easy";
    public final String           MEDIUM          = "Medium";
    public final String           HARD            = "Hard";
    public final String           VERY_HARD       = "Very Hard";
    
    //easy, medium, hard, very hard
    //easy gets a new booster pack after 1 wins
    //medium gets a new booster pack after after 1 wins, etc...
    //hard gets a new booster pack after after 2 wins, etc...
    //very hard gets a new booster pack after after 2 wins, etc...
    //these numbers are just guesses
    private int[]                 addCardsArray   = {1, 1, 2, 2};
    
    //easy, medium, hard, very hard
    //easy gets a new "rank" after 1 wins
    //medium, gets a new "rank" after 2 wins
    //these numbers are just guesses
    private int[]                 rankChangeArray = {1, 2, 3, 4};
    
    private int                   rankIndex;
    private int                   win;
    private int                   lost;
    
    private String                difficulty;
    

    private ArrayList<String>     easyAIDecks;
    private ArrayList<String>     mediumAIDecks;
    private ArrayList<String>     hardAIDecks;
    
    private HashMap<String, Deck> myDecks         = new HashMap<String, Deck>();
    private HashMap<String, Deck> aiDecks         = new HashMap<String, Deck>();
    
    //holds String card names
    private ArrayList<String>     cardPool        = new ArrayList<String>();
    private ArrayList<String>     newCardList     = new ArrayList<String>();
    
    private QuestData_BoosterPack boosterPack     = new QuestData_BoosterPack();
    
    //used by shouldAddAdditionalCards()
    private Random                random          = new Random();
    
    //feel free to change this to something funnier
    private String[]              rankArray       = {
            "Level 0 - Confused Wizard", "Level 1 - Mana Mage", "Level 2 - Death by Megrim",
            "Level 3 - Shattered the Competition", "Level 4 - Black Knighted", "Level 5 - Shockingly Good",
            "Level 6 - Regressed into Timmy", "Level 7 - Loves Blue Control", "Level 8 - Immobilized by Fear",
            "Level 9 - Lands = Friends", "Saltblasted for your talent", "Serra Angel is your girlfriend",};
    
    /*  
      private String[] rankArray =
      {
        "Level 0 - Interested Newbie"  ,
        "Level 1 - Card Flopper"       ,
        "Level 2 - Friday Night Winner",
        "Level 3 - Store Champion"     ,
        "Level 4 - Card Crusher"       ,
        "Level 5 - PTQ Player"         ,
        "Level 6 - PTQ Winner"         ,
        "Level 7 - Pro Wannabe"        ,
        "Level 8 - Pro-Tour Winner"    ,
        "Level 9 - Better Than Jon Finkel"  ,
        "Level 10 - World Champion - You Win",
        "Secret Level - Magic God"
      };
    */

    public QuestData() {
        for(int i = 0; i < 40; i++) {
            cardPool.add("Forest");
            cardPool.add("Mountain");
            cardPool.add("Swamp");
            cardPool.add("Island");
            cardPool.add("Plains");
        }//for
    }//QuestData
    
    //adds cards to card pool and sets difficulty
    @SuppressWarnings("unchecked")
    public void newGame(String difficulty) {
        setDifficulty(difficulty);
        
        int[][] totals = { {45, 20, 10}, //easy, 45 common, 20 uncommon, 10 rares
                {40, 15, 10} //everything else
        };
        
        int n = 1;
        if(difficulty.equals(EASY)) n = 0;
        
        ArrayList<String> list = new ArrayList<String>();
        list.addAll(boosterPack.getCommon(totals[n][0]));
        list.addAll(boosterPack.getUncommon(totals[n][1]));
        list.addAll(boosterPack.getRare(totals[n][2]));
        
        //because cardPool already has basic land added to it
        cardPool.addAll(list);
    }
    
    
    public String[] getOpponents() {
        int difficulty[][] = {
        //if getWin() is < 5 return use easyAIDecks
                //if getWin() is < 8 return mediumAIDecks
                //else use hardAIDecks
                {5, 8}, //easy difficulty
                {5, 8}, //medium difficulty
                {10, 20},//hard difficulty
                {10, 20},//very hard difficulty
        };
        
        int index = convertDifficultyToIndex(getDifficulty());
        
        if(getWin() < difficulty[index][0]) return getOpponents(easyAIDecks);
        
        if(getWin() < difficulty[index][1]) return getOpponents(mediumAIDecks);
        
        return getOpponents(hardAIDecks);
    }//getOpponents()
    

    static public void readAIQuestDeckFiles(QuestData data, ArrayList<?> aiDeckNames) {
        data.easyAIDecks = readFile(ForgeProps.getFile(QUEST.EASY), aiDeckNames);
        data.mediumAIDecks = readFile(ForgeProps.getFile(QUEST.MEDIUM), aiDeckNames);
        data.hardAIDecks = readFile(ForgeProps.getFile(QUEST.HARD), aiDeckNames);
        
    }
    
    public String[] getOpponents(ArrayList<String> aiDeck) {
        Collections.shuffle(aiDeck);
        
        return new String[] {aiDeck.get(0).toString(), aiDeck.get(1).toString(), aiDeck.get(2).toString()};
        
    }//getOpponents()
    
    private static ArrayList<String> readFile(File file, ArrayList<?> aiDecks) {
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
            if(!aiDecks.contains(list.get(i).toString())) ErrorViewer.showError(new Exception(),
                    "QuestData : readFile() error, file %s contains the invalid ai deck name: %s", file,
                    list.get(i));
        

        return list;
    }//readFile()
    
    public void readAIQuestDeckFiles() {
        readAIQuestDeckFiles(this, ai_getDeckNames());
    }
    
    @SuppressWarnings("unchecked")
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
            data.rankIndex = state.rankIndex;
            data.difficulty = state.difficulty;
            
            data.cardPool = state.cardPool;
            data.myDecks = state.myDecks;
            data.aiDecks = state.aiDecks;
            
            readAIQuestDeckFiles(data, new ArrayList(data.aiDecks.keySet()));
            
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
    @SuppressWarnings("unchecked")
    public void addCards() {
        int nCommon = 7;
        int nUncommon = 3;
        int nRare = 1;
        
        ArrayList<String> newCards = new ArrayList<String>();
        newCards.addAll(boosterPack.getCommon(nCommon));
        newCards.addAll(boosterPack.getUncommon(nUncommon));
        newCards.addAll(boosterPack.getRare(nRare));
        

        cardPool.addAll(newCards);
        
        //getAddedCards() uses newCardList
        newCardList = newCards;
        
    }//addCards()
    
    public void addAdditionalCards() {
        int nRare = 1;
        
        ArrayList<String> newCards = new ArrayList<String>();
        newCards.addAll(boosterPack.getRare(nRare));
        
        cardPool.addAll(newCards);
        //getAddedCards() uses newCardList
        

        newCardList.addAll(newCards);
        
    }
    
    //gets all of the cards that are in the cardpool
    public ArrayList<String> getCards() {
        //copy CardList in order to keep private variables private
        //if we just return cardPool, it could be changed externally
        return new ArrayList<String>(cardPool);
    }
    
    
    public int getTotalNumberOfGames(String difficulty) {
        //-2 because you start a level 1, and the last level is secret
        int numberLevels = rankArray.length - 2;
        int nMatches = rankChangeArray[convertDifficultyToIndex(difficulty)];
        
        return numberLevels * nMatches;
    }
    
    //this changes getRank()
    public void addWin() {
        win++;
        
        int n = convertDifficultyToIndex();
        
        if(win % rankChangeArray[n] == 0) rankIndex++;
    }//addWin()
    
    private int convertDifficultyToIndex() {
        return convertDifficultyToIndex(getDifficulty());
    }
    
    private int convertDifficultyToIndex(String difficulty) {
        String s = difficulty;
        
        if(s.equals(EASY)) return 0;
        if(s.equals(MEDIUM)) return 1;
        if(s.equals(HARD)) return 2;
        
        //VERY_HARD
        return 3;
    }//convertDifficultyToIndex()
    
    public void addLost() {
        lost++;
    }
    
    public int getWin() {
        return win;
    }
    
    public int getLost() {
        return lost;
    }
    
    //should be called first, because the difficultly won't change
    public String getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(String s) {
        difficulty = s;
    }
    
    public String[] getDifficutlyChoices() {
        return new String[] {EASY, MEDIUM, HARD, VERY_HARD};
    }
    
    public String getRank() {
        //is rankIndex too big?
        if(rankIndex == rankArray.length) rankIndex--;
        
        return rankArray[rankIndex];
    }
    
    //add cards after a certain number of wins or loses
    public boolean shouldAddCards(boolean didWin) {
        int n = addCardsArray[convertDifficultyToIndex()];
        
        if(didWin) return getWin() % n == 0;
        else return getLost() % n == 0;
    }
    
    public boolean shouldAddAdditionalCards(boolean didWin) {
        if(didWin) return random.nextBoolean();
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
            state.difficulty = q.difficulty;
            state.rankIndex = q.rankIndex;
            
            state.cardPool = q.cardPool;
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
