
package forge;


import static java.util.Collections.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class Deck implements java.io.Serializable {
    private static final long serialVersionUID = -2188987217361601903L;
    
    //gameType is from Constant.GameType, like Constant.GameType.Regular
    private String            name, comment, deckType;
    
    private List<String>      main, sideboard, mainView, sideboardView;
    
    //gameType is from Constant.GameType, like Constant.GameType.Regular
    public Deck(String gameType) {
        setDeckType(gameType);
        name = "";
        
        main = new ArrayList<String>();
        mainView = unmodifiableList(main);
        
        sideboard = new ArrayList<String>();
        sideboardView = unmodifiableList(sideboard);
    }
    
    public Deck(String deckType, List<String> main, List<String> sideboard, String name) {
        this.deckType = deckType;
        this.name = name;
        
        this.main = main;
        mainView = unmodifiableList(main);
        
        this.sideboard = main;
        sideboardView = unmodifiableList(sideboard);
    }
    
    public List<String> getMain() {
        return mainView;
    }
    
    public List<String> getSideboard() {
        return sideboardView;
    }
    
    public String getDeckType() {
        return deckType;
    }
    
    //can only call this method ONCE
    private void setDeckType(String deckType) {
        if(this.deckType != null) throw new IllegalStateException(
                "Deck : setDeckType() error, deck type has already been set");
        
        if(!Constant.GameType.GameTypes.contains(deckType)) throw new RuntimeException(
                "Deck : setDeckType() error, invalid deck type - " + deckType);
        
        this.deckType = deckType;
    }
    
    public void setName(String s) {
        name = s;
    }
    
    public String getName() {
        return name;
    }//may return null
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public String getComment() {
        return comment;
    }
    
    public void addMain(String cardName) {
        main.add(cardName);
    }
    
    public int countMain() {
        return main.size();
    }
    
    public String getMain(int index) {
        return main.get(index).toString();
    }
    
    public String removeMain(int index) {
        return main.remove(index).toString();
    }
    
    public void addSideboard(String cardName) {
        sideboard.add(cardName);
    }
    
    public int countSideboard() {
        return sideboard.size();
    }
    
    public String getSideboard(int index) {
        return sideboard.get(index).toString();
    }
    
    public String removeSideboard(int index) {
        return sideboard.remove(index).toString();
    }
    
    public boolean isDraft() {
        return deckType.equals(Constant.GameType.Draft);
    }
    
    public boolean isSealed() {
        return deckType.equals(Constant.GameType.Sealed);
    }
    
    public boolean isRegular() {
        return deckType.equals(Constant.GameType.Constructed);
    }
    
    public int hashcode() {
        return getName().hashCode();
    }
    
    @Override
    public String toString() {
        return getName();
    }
}


class DeckSort implements Comparator<Object>, java.io.Serializable {
    public int compare(Object a, Object b) {
        String a1 = ((Deck) a).getName();
        String b1 = ((Deck) b).getName();
        
        return a1.compareTo(b1);
    }
}
