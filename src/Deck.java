/**
 * Deck.java
 * 
 * Created on 26.10.2009
 */

import java.io.Serializable;
import java.util.ArrayList;


/**
 * The class Deck. This class is only here for compatibility with forge versions 10/17 and older. When it is read
 * from the file stream, the object is replaced with an object of type {@link Deck} using {@link #readResolve()}.
 * 
 * @author Clemens Koza
 */
public class Deck implements Serializable {
    private static final long serialVersionUID = -2188987217361601903L;
    
    private String            deckType;
    
    /*
    private boolean           isRegular;
    private boolean           isSealed;
    private boolean           isDraft;
    */

    private ArrayList<String> main;
    private ArrayList<String> sideboard;
    
    //very important, do NOT change this
    private String            name;
    
    public forge.Deck migrate() {
        return new forge.Deck(deckType, main, sideboard, name);
    }
}
