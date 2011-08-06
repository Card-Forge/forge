/**
 * QuestData_State.java
 * 
 * Created on 26.10.2009
 */

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


/**
 * The class QuestData_State. This class is only here for compatibility with forge versions 10/17 and older. When
 * it is read from the file stream, the object is replaced with an object of type {@link Deck} using
 * {@link #readResolve()}.
 * 
 * @author Clemens Koza
 */
public class QuestData_State implements Serializable {
    private static final long serialVersionUID = 7007940230351051937L;
    
    int                       rankIndex, win, lost;
    int 				      plantLevel, wolfPetLevel, crocPetLevel, life, estatesLevel, questsPlayed;
    long					  credits;
    String                    difficulty, mode, selectedPet;
    

    ArrayList<Integer>		  availableQuests, completedQuests;
    ArrayList<String>         cardPool, shopList;
    HashMap<String, Deck>     myDecks, aiDecks;
    
    private Object readResolve() throws ObjectStreamException {
//        System.out.println("resolving obsolete QuestData_State");
        Map<String, forge.Deck> myDecks = new HashMap<String, forge.Deck>();
        for(Entry<String, Deck> deck:this.myDecks.entrySet()) {
            myDecks.put(deck.getKey(), deck.getValue().migrate());
        }
        
        Map<String, forge.Deck> aiDecks = new HashMap<String, forge.Deck>();
        for(Entry<String, Deck> deck:this.aiDecks.entrySet()) {
            aiDecks.put(deck.getKey(), deck.getValue().migrate());
        }
        return new forge.QuestData_State(rankIndex, win, lost, plantLevel, wolfPetLevel, crocPetLevel, selectedPet, life, estatesLevel, questsPlayed, availableQuests, completedQuests,credits, difficulty, mode, cardPool, shopList, myDecks, aiDecks);
    }
}
