package forge.deck;

import forge.Card;
import forge.Constant;

import java.io.Serializable;
import java.util.*;

public class Deck implements Comparable<Deck>, Serializable{
    /**
	 * 
	 */
	private static final long serialVersionUID = -7478025567887481994L;

	//gameType is from Constant.GameType, like Constant.GameType.Regular

    private Map<String, String> metadata = new HashMap<String, String>();

    private List<String> main;
    private List<String> sideboard;

    public static final String NAME = "Name";
    public static final String DECK_TYPE = "Deck Type";
    public static final String COMMENT = "Comment";
    public static final String DESCRIPTION = "Description";
    public static final String DIFFICULTY = "Difficulty";


    //gameType is from Constant.GameType, like Constant.GameType.Regular
    public Deck() {
        main = new ArrayList<String>();
        sideboard = new ArrayList<String>();
    }

    public Deck(String deckType, List<String> main, List<String> sideboard, String name) {
        setDeckType(deckType);
        setName(name);

        this.main = main;
        this.sideboard = sideboard;
    }

    public Deck(String type) {
        this();
        setDeckType(type);
    }

    public List<String> getMain() {
        return Collections.unmodifiableList(main);
    }

    public List<String> getSideboard() {
        return Collections.unmodifiableList(sideboard);
    }

    public String getDeckType() {
        return metadata.get(DECK_TYPE);
    }

    //can only call this method ONCE
    void setDeckType(String deckType) {
        if (this.getDeckType() != null) {
            throw new IllegalStateException(
                    "Deck : setDeckType() error, deck type has already been set");
        }

        if (!Constant.GameType.GameTypes.contains(deckType)) {
            throw new RuntimeException(
                    "Deck : setDeckType() error, invalid deck type - " + deckType);
        }

        metadata.put(DECK_TYPE, deckType);
    }

    public void setName(String s) {
        metadata.put(NAME, s);
    }

    public String getName() {
        return metadata.get(NAME);
    }

    public void setComment(String comment) {
        metadata.put(COMMENT, comment);
    }

    public String getComment() {
        return metadata.get(COMMENT);

    }

    public void addMain(String cardName) {
        main.add(cardName);
    }

    public int countMain() {
        return main.size();
    }

    public String getMain(int index) {
        return main.get(index);
    }

    public String removeMain(int index) {
        return main.remove(index);
    }

    public void removeMain(Card c) {
        if (main.contains(c.getName())) {
            int i = main.indexOf(c.getName());
            main.remove(i);
        }
    }

    public void addSideboard(String cardName) {
        sideboard.add(cardName);
    }

    public int countSideboard() {
        return sideboard.size();
    }

    public String getSideboard(int index) {
        return sideboard.get(index);
    }

    public String removeSideboard(int index) {
        return sideboard.remove(index);
    }

    public boolean isDraft() {
        return getDeckType().equals(Constant.GameType.Draft);
    }

    public boolean isSealed() {
        return getDeckType().equals(Constant.GameType.Sealed);
    }

    public boolean isRegular() {
        return getDeckType().equals(Constant.GameType.Constructed);
    }

    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public String toString() {
        return getName();
    }


    // The setters and getters below are for Quest decks
    public void setDifficulty(String s) {
        metadata.put(DIFFICULTY, s);
    }

    public String getDifficulty() {
        return metadata.get(DIFFICULTY);
    }

    public void setDescription(String s) {
        metadata.put(DESCRIPTION, s);
    }

    public String getDescription() {
        return metadata.get(DESCRIPTION);
    }

    public int compareTo(Deck d) {
    	return getName().compareTo(d.getName());
    }
    
    public boolean equals(Object o) {
    	if(o instanceof Deck){
    		Deck d = (Deck)o;
    		return getName().equals(d.getName());
    	}
    	return false;
    }

    public Set<Map.Entry<String,String>> getMetadata() {
        return metadata.entrySet();
    }

    public void addMetaData(String key, String value) {
        metadata.put(key, value);
    }
}