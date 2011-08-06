
package forge;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import forge.error.ErrorViewer;


//reads and write Deck objects
public class OldDeckIO implements DeckIO {
    private final File          file;
    private ArrayList<Deck>     deckList   = new ArrayList<Deck>();
    
    //key is String of the humans deck
    //data is Deck[] size of 8
    //humans deck is Deck[0]
    private Map<String, Deck[]> boosterMap = new HashMap<String, Deck[]>();
    
    public static void main(String[] args) {
        Deck[] deck = new Deck[8];
        for(int i = 0; i < deck.length; i++) {
            deck[i] = new Deck(Constant.GameType.Draft);
            deck[i].setName("" + i);
        }
        
        deck[0].addMain("Lantern Kami");
        
        //DeckIO io = new DeckIO("booster-decks");
//    io.writeBoosterDeck(deck);
//    io.close();
        
//    io.deleteBoosterDeck("0");
//    System.out.println(io.getBoosterDecks().keySet().size());
//    io.close();
    }
    
    public OldDeckIO(String filename) {
        this(new File(filename));
    }
    
    public OldDeckIO(File file) {
        this.file = file;
        
        readFile();
    }
    
    public boolean isUnique(String deckName) {
        Deck d;
        for(int i = 0; i < deckList.size(); i++) {
            d = deckList.get(i);
            if(d.getName().equals(deckName)) return false;
        }
        return true;
    }
    
    public boolean isUniqueDraft(String deckName) {
        ArrayList<String> key = new ArrayList<String>(boosterMap.keySet());
        
        for(int i = 0; i < key.size(); i++) {
            if(key.get(i).equals(deckName)) return false;
        }
        return true;
    }
    
    public boolean hasName(String deckName) {
        ArrayList<String> string = new ArrayList<String>();
        
        for(int i = 0; i < deckList.size(); i++)
            string.add(deckList.get(i).toString());
        
        Iterator<String> it = boosterMap.keySet().iterator();
        while(it.hasNext())
            string.add(it.next().toString());
        
        return string.contains(deckName);
    }
    
    public Deck readDeck(String deckName) {
        return deckList.get(findDeckIndex(deckName));
    }
    
    private int findDeckIndex(String deckName) {
        int n = -1;
        for(int i = 0; i < deckList.size(); i++)
            if((deckList.get(i)).getName().equals(deckName)) n = i;
        
        if(n == -1) throw new RuntimeException("DeckIO : findDeckIndex() error, deck name not found - " + deckName);
        
        return n;
    }
    
    public void writeDeck(Deck deck) {
        if(deck.getDeckType().equals(Constant.GameType.Draft)) throw new RuntimeException(
                "DeckIO : writeDeck() error, deck type is Draft");
        
        deckList.add(deck);
    }
    
    public void deleteDeck(String deckName) {
        deckList.remove(findDeckIndex(deckName));
    }
    
    public Deck[] readBoosterDeck(String deckName) {
        if(!boosterMap.containsKey(deckName)) throw new RuntimeException(
                "DeckIO : readBoosterDeck() error, deck name not found - " + deckName);
        
        return boosterMap.get(deckName);
    }
    
    public void writeBoosterDeck(Deck[] deck) {
        checkBoosterDeck(deck);
        
        boosterMap.put(deck[0].toString(), deck);
    }//writeBoosterDeck()
    
    public void deleteBoosterDeck(String deckName) {
        if(!boosterMap.containsKey(deckName)) throw new RuntimeException(
                "DeckIO : deleteBoosterDeck() error, deck name not found - " + deckName);
        
        boosterMap.remove(deckName);
    }
    
    private void checkBoosterDeck(Deck[] deck) {
        if(deck == null || deck.length != 8 || deck[0].getName().equals("")
                || (!deck[0].getDeckType().equals(Constant.GameType.Draft))) {
            throw new RuntimeException("DeckIO : checkBoosterDeck() error, invalid deck");
        }
//    for(int i = 0; i < deck.length; i++)
//      if(deck[i].getName().equals(""))
//        throw new RuntimeException("DeckIO : checkBoosterDeck() error, deck does not have name - " +deck[i].getName());
    }//checkBoosterDeck()
    

    public Deck[] getDecks() {
        Deck[] out = new Deck[deckList.size()];
        deckList.toArray(out);
        return out;
    }
    
    public Map<String, Deck[]> getBoosterDecks() {
        return new HashMap<String, Deck[]>(boosterMap);
    }
    
    public void close() {
        writeFile();
    }
    
    @SuppressWarnings("unchecked")
    // ArrayList and Map have unchecked casts
    private void readFile() {
        try {
//~
            // Shouldn't ever get here, but just in case...
            if(file == null) {
                return;
            }
//~
            if(!file.exists()) return;
            
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
            
            deckList = (ArrayList<Deck>) in.readObject();
            boosterMap = (Map<String, Deck[]>) in.readObject();
            
            in.close();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("DeckIO : read() error, " + ex);
        }
    }
    
    private void writeFile() {
        try {
//~
            if(file == null) {
                return;
            }
//~
            if(!file.exists()) file.createNewFile();
            
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
            
            out.writeObject(deckList);
            out.writeObject(boosterMap);
            
            out.flush();
            out.close();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("DeckIO : write() error, " + ex.getMessage());
        }
    }
}//DeckIO
