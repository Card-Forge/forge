import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;


/**
 * Deck.java
 * 
 * Created on 26.10.2009
 */


/**
 * The class Deck. This class is only here for compatibility with forge versions 10/17 and older. When it is read
 * from the file stream, the object is replaced with an object of type {@link Deck} using {@link #readResolve()}.
 * 
 * @author Clemens Koza
 */
public class Deck implements Serializable {
    private static final long serialVersionUID = -2188987217361601903L;
    
    private String            deckType;
    
    private boolean           isRegular;
    private boolean           isSealed;
    private boolean           isDraft;
    
    private ArrayList<String> main             = new ArrayList<String>();
    private ArrayList<String> sideboard        = new ArrayList<String>();
    
    //very important, do NOT change this
    private String            name             = "";
    
    private Object readResolve() throws ObjectStreamException {
        System.out.println("resolving obsolete Deck");
        forge.Deck d = new forge.Deck(deckType);
        d.setName(name);
        for(String s:main)
            d.addMain(s);
        for(String s:sideboard)
            d.addSideboard(s);
        return d;
    }
}
