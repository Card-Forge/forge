package forge.deck;

import forge.Card;
import forge.Constant;

import java.util.*;

public class Deck implements Comparable{
    //gameType is from Constant.GameType, like Constant.GameType.Regular

    private Map<String, String> metadata = new HashMap<String, String>();

    private List<String> main;
    private List<String> sideboard;
    private transient List<String> mainView;
    private transient List<String> sideboardView;

    public static final String NAME = "Name";
    public static final String DECK_TYPE = "Deck Type";
    public static final String COMMENT = "Comment";
    public static final String DESCRIPTION = "Description";
    public static final String DIFFICULTY = "Difficulty";


    //gameType is from Constant.GameType, like Constant.GameType.Regular
    public Deck(String gameType) {
        setDeckType(gameType);
        setName("");

        main = new ArrayList<String>();
        mainView = Collections.unmodifiableList(main);

        sideboard = new ArrayList<String>();
        sideboardView = Collections.unmodifiableList(sideboard);
    }

    public Deck(String deckType, List<String> main, List<String> sideboard, String name) {
        setDeckType(deckType);
        setName(name);

        this.main = main;
        mainView = Collections.unmodifiableList(main);

        this.sideboard = main;
        sideboardView = Collections.unmodifiableList(sideboard);
    }

    public List<String> getMain() {
        return mainView;
    }

    public List<String> getSideboard() {
        return sideboardView;
    }

    public String getDeckType() {
        return metadata.get(DECK_TYPE);
    }

    //can only call this method ONCE
    private void setDeckType(String deckType) {
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
    }//may return null

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

    public int compareTo(Object o) {
        if (o instanceof Deck)
        {
            return getName().compareTo(((Deck)o).getName());
        }
        return 0;
    }
}