
package forge;


/**
 * QuestData_State.java
 * 
 * Created on 26.10.2009
 */


import forge.deck.Deck;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;


/**
 * The class QuestData_State.
 * 
 * @version V0.0 26.10.2009
 * @author Clemens Koza
 */


@Deprecated
public class QuestData_State implements Serializable {
    private static final long serialVersionUID = 7007940230351051937L;
    
    int                       rankIndex, win, lost;
    int 					  plantLevel, wolfPetLevel, crocPetLevel, birdPetLevel, houndPetLevel, life, estatesLevel, luckyCoinLevel, sleightOfHandLevel, gearLevel, questsPlayed;
    long 					  credits;
    String                    difficulty, mode, selectedPet;
    
    ArrayList<Integer> 		  availableQuests, completedQuests;
    ArrayList<String>         cardPool, shopList;
    Map<String, Deck>         myDecks, aiDecks;
    
    public QuestData_State() {}
    
    /**
     * This constructor is used by QestData_State in the default package to create a replacement object for the
     * obsolete class.
     */
    public QuestData_State(int rankIndex, int win, int lost, int plantLevel, int wolfPetLevel, int crocPetLevel, int birdPetLevel, int houndPetLevel, String selectedPet, int life, int estatesLevel, int luckyCoinLevel, int sleightOfHandLevel, int gearLevel, int questsPlayed,
    					   ArrayList<Integer> availableQuests, ArrayList<Integer> completedQuests, long credits, String difficulty, String mode, 
    					   ArrayList<String> cardPool, ArrayList<String> shopList, Map<String, Deck> myDecks, Map<String, Deck> aiDecks) {
        this.rankIndex = rankIndex;
        this.win = win;
        this.lost = lost;
        this.plantLevel = plantLevel;
        this.wolfPetLevel = wolfPetLevel;
        this.crocPetLevel = crocPetLevel;
        this.birdPetLevel = birdPetLevel;
        this.houndPetLevel = houndPetLevel;
        this.life = life;
        this.estatesLevel = estatesLevel;
        this.luckyCoinLevel = luckyCoinLevel;
        this.sleightOfHandLevel = sleightOfHandLevel;
        this.gearLevel = gearLevel;
        this.questsPlayed = questsPlayed;
        this.availableQuests = availableQuests;
        this.completedQuests = completedQuests;
        this.credits = credits;
        this.difficulty = difficulty;
        this.mode = mode;
        this.cardPool = cardPool;
        this.shopList = shopList;
        this.myDecks = myDecks;
        this.aiDecks = aiDecks;
    }
}
