/**
 * QuestData_State.java
 * 
 * Created on 26.10.2009
 */

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;


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
    String                    difficulty;
    
    ArrayList<String>         cardPool;
    HashMap<String, Deck>     myDecks, aiDecks;
    
    @SuppressWarnings("unchecked")
    private Object readResolve() throws ObjectStreamException {
        System.out.println("resolving obsolete QuestData_State");
        //removing generic types to make cast between Deck and forge.Deck
        //i'm using Deck in this class because it may be somewhere in the serialization stream
        //it is irrelevant, because readResolve replaces Deck by forge.Deck, and since generics
        //aren't checked at runtime, the cast is ok.
        return new forge.QuestData_State(rankIndex, win, lost, difficulty, cardPool, (HashMap) myDecks,
                (HashMap) aiDecks);
    }
}
