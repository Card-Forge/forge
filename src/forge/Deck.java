package forge;




import java.util.*;


public class Deck implements java.io.Serializable {
    private static final long serialVersionUID = -2188987217361601903L;
    
    //gameType is from Constant.GameType, like Constant.GameType.Regular
    private String            deckType;
    
    private boolean           isRegular;
    private boolean           isSealed;
    private boolean           isDraft;
    
    private ArrayList<String> main             = new ArrayList<String>();
    private ArrayList<String> sideboard        = new ArrayList<String>();
    
    //very important, do NOT change this
    private String            name             = "";
    
    //gameType is from Constant.GameType, like Constant.GameType.Regular
    public Deck(String gameType) {
        deckType = gameType;
        setDeckType(gameType);
    }
    
    public String getDeckType() {
        return deckType;
    }
    
    //can only call this method ONCE
    private final void setDeckType(String deckType) {
        if(isRegular || isSealed || isDraft) throw new RuntimeException(
                "Deck : setDeckType() error, deck type has already been set");
        
        if(deckType.equals(Constant.GameType.Constructed)) isRegular = true;
        else if(deckType.equals(Constant.GameType.Sealed)) isSealed = true;
        else if(deckType.equals(Constant.GameType.Draft)) isDraft = true;
        else throw new RuntimeException("Deck : setDeckType() error, invalid deck type - " + deckType);
    }
    
    public void setName(String s) {
        name = s;
    }
    
    public String getName() {
        return name;
    }//may return null
    
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
        return isDraft;
    }
    
    public boolean isSealed() {
        return isSealed;
    }
    
    public boolean isRegular() {
        return isRegular;
    }
    
    public int hashcode() {
        return getName().hashCode();
    }
    
    public String toString() {
        return getName();
    }
}


class DeckSort implements Comparator<Object> {
    public int compare(Object a, Object b) {
        String a1 = ((Deck) a).getName();
        String b1 = ((Deck) b).getName();
        
        return a1.compareTo(b1);
    }
}
