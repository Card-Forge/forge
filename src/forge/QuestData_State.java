
package forge;


/**
 * QuestData_State.java
 * 
 * Created on 26.10.2009
 */


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;


/**
 * The class QuestData_State.
 * 
 * @version V0.0 26.10.2009
 * @author Clemens Koza
 */
public class QuestData_State implements Serializable {
    private static final long serialVersionUID = 7007940230351051937L;
    
    int                       rankIndex, win, lost;
    long 					  credits;
    String                    difficulty;
    
    ArrayList<String>         cardPool, shopList;
    Map<String, Deck>         myDecks, aiDecks;
    
    public QuestData_State() {}
    
    /**
     * This constructor is used by QestData_State in the default package to create a replacement object for the
     * obsolete class.
     */
    public QuestData_State(int rankIndex, int win, int lost, long credits, String difficulty, ArrayList<String> cardPool, ArrayList<String> shopList, Map<String, Deck> myDecks, Map<String, Deck> aiDecks) {
        this.rankIndex = rankIndex;
        this.win = win;
        this.lost = lost;
        this.credits = credits;
        this.difficulty = difficulty;
        this.cardPool = cardPool;
        this.shopList = shopList;
        this.myDecks = myDecks;
        this.aiDecks = aiDecks;
    }
}
