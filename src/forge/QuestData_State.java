
package forge;


/**
 * QuestData_State.java
 * 
 * Created on 26.10.2009
 */


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * The class QuestData_State.
 * 
 * @version V0.0 26.10.2009
 * @author Clemens Koza
 */
public class QuestData_State implements Serializable {
    private static final long serialVersionUID = 7007940230351051937L;
    
    int                       rankIndex, win, lost;
    String                    difficulty;
    
    ArrayList<String>         cardPool;
    HashMap<String, Deck>     myDecks, aiDecks;
    
    public QuestData_State() {}
    
    /**
     * This constructor is used by QestData_State in the default package to create a replacement object for the
     * obsolete class.
     */
    public QuestData_State(int rankIndex, int win, int lost, String difficulty, ArrayList<String> cardPool, HashMap<String, Deck> myDecks, HashMap<String, Deck> aiDecks) {
        this.rankIndex = rankIndex;
        this.win = win;
        this.lost = lost;
        this.difficulty = difficulty;
        this.cardPool = cardPool;
        this.myDecks = myDecks;
        this.aiDecks = aiDecks;
    }
}
