/**
 * DeckIO.java
 * 
 * Created on 01.11.2009
 */

package forge;


import java.util.Map;


/**
 * The class DeckIO.
 * 
 * @version V0.0 01.11.2009
 * @author Clemens Koza
 */
public interface DeckIO {
    
    public boolean isUnique(String deckName);
    
    public boolean isUniqueDraft(String deckName);
    
    public boolean hasName(String deckName);
    
    public Deck readDeck(String deckName);
    
    public void writeDeck(Deck deck);
    
    public void deleteDeck(String deckName);
    
    public Deck[] readBoosterDeck(String deckName);
    
    public void writeBoosterDeck(Deck[] deck);//writeBoosterDeck()
    
    public void deleteBoosterDeck(String deckName);
    
    public Deck[] getDecks();
    
    public Map<String, Deck[]> getBoosterDecks();
    
    public void close();
    
}
