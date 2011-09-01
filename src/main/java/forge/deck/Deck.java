package forge.deck;

import forge.Card;
import forge.Constant;

import java.io.Serializable;
import java.util.*;

/**
 * <p>Deck class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Deck implements Comparable<Deck>, Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -7478025567887481994L;

    //gameType is from Constant.GameType, like Constant.GameType.Regular

    private Map<String, String> metadata = new HashMap<String, String>();

    private List<String> main;
    private List<String> sideboard;

    /** Constant <code>NAME="Name"</code> */
    public static final String NAME = "Name";
    /** Constant <code>DECK_TYPE="Deck Type"</code> */
    public static final String DECK_TYPE = "Deck Type";
    /** Constant <code>COMMENT="Comment"</code> */
    public static final String COMMENT = "Comment";
    /** Constant <code>DESCRIPTION="Description"</code> */
    public static final String DESCRIPTION = "Description";
    /** Constant <code>DIFFICULTY="Difficulty"</code> */
    public static final String DIFFICULTY = "Difficulty";


    //gameType is from Constant.GameType, like Constant.GameType.Regular
    /**
     * <p>Constructor for Deck.</p>
     */
    public Deck() {
        main = new ArrayList<String>();
        sideboard = new ArrayList<String>();
    }

    /**
     * <p>Constructor for Deck.</p>
     *
     * @param deckType a {@link java.lang.String} object.
     * @param main a {@link java.util.List} object.
     * @param sideboard a {@link java.util.List} object.
     * @param name a {@link java.lang.String} object.
     */
    public Deck(String deckType, List<String> main, List<String> sideboard, String name) {
        setDeckType(deckType);
        setName(name);

        this.main = main;
        this.sideboard = sideboard;
    }

    /**
     * <p>Constructor for Deck.</p>
     *
     * @param type a {@link java.lang.String} object.
     */
    public Deck(String type) {
        this();
        setDeckType(type);
    }

    /**
     * <p>Getter for the field <code>main</code>.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getMain() {
        return Collections.unmodifiableList(main);
    }

    /**
     * <p>Getter for the field <code>sideboard</code>.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getSideboard() {
        return Collections.unmodifiableList(sideboard);
    }

    /**
     * <p>getDeckType.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDeckType() {
        return metadata.get(DECK_TYPE);
    }

    //can only call this method ONCE
    /**
     * <p>setDeckType.</p>
     *
     * @param deckType a {@link java.lang.String} object.
     */
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

    /**
     * <p>setName.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void setName(String s) {
        metadata.put(NAME, s);
    }

    /**
     * <p>getName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return metadata.get(NAME);
    }

    /**
     * <p>setComment.</p>
     *
     * @param comment a {@link java.lang.String} object.
     */
    public void setComment(String comment) {
        metadata.put(COMMENT, comment);
    }

    /**
     * <p>getComment.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getComment() {
        return metadata.get(COMMENT);

    }

    /**
     * <p>addMain.</p>
     *
     * @param cardName a {@link java.lang.String} object.
     */
    public void addMain(String cardName) {
        main.add(cardName);
    }

    /**
     * <p>countMain.</p>
     *
     * @return a int.
     */
    public int countMain() {
        return main.size();
    }

    /**
     * <p>Getter for the field <code>main</code>.</p>
     *
     * @param index a int.
     * @return a {@link java.lang.String} object.
     */
    public String getMain(int index) {
        return main.get(index);
    }

    /**
     * <p>removeMain.</p>
     *
     * @param index a int.
     * @return a {@link java.lang.String} object.
     */
    public String removeMain(int index) {
        return main.remove(index);
    }

    /**
     * <p>removeMain.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public void removeMain(Card c) {
        if (main.contains(c.getName())) {
            int i = main.indexOf(c.getName());
            main.remove(i);
        }
    }

    /**
     * <p>addSideboard.</p>
     *
     * @param cardName a {@link java.lang.String} object.
     */
    public void addSideboard(String cardName) {
        sideboard.add(cardName);
    }

    /**
     * <p>countSideboard.</p>
     *
     * @return a int.
     */
    public int countSideboard() {
        return sideboard.size();
    }

    /**
     * <p>Getter for the field <code>sideboard</code>.</p>
     *
     * @param index a int.
     * @return a {@link java.lang.String} object.
     */
    public String getSideboard(int index) {
        return sideboard.get(index);
    }

    /**
     * <p>removeSideboard.</p>
     *
     * @param index a int.
     * @return a {@link java.lang.String} object.
     */
    public String removeSideboard(int index) {
        return sideboard.remove(index);
    }

    /**
     * <p>isDraft.</p>
     *
     * @return a boolean.
     */
    public boolean isDraft() {
        return getDeckType().equals(Constant.GameType.Draft);
    }

    /**
     * <p>isSealed.</p>
     *
     * @return a boolean.
     */
    public boolean isSealed() {
        return getDeckType().equals(Constant.GameType.Sealed);
    }

    /**
     * <p>isRegular.</p>
     *
     * @return a boolean.
     */
    public boolean isRegular() {
        return getDeckType().equals(Constant.GameType.Constructed);
    }

    /**
     * <p>hashCode.</p>
     *
     * @return a int.
     */
    public int hashCode() {
        return getName().hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getName();
    }


    // The setters and getters below are for Quest decks
    /**
     * <p>setDifficulty.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void setDifficulty(String s) {
        metadata.put(DIFFICULTY, s);
    }

    /**
     * <p>getDifficulty.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDifficulty() {
        return metadata.get(DIFFICULTY);
    }

    /**
     * <p>setDescription.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void setDescription(String s) {
        metadata.put(DESCRIPTION, s);
    }

    /**
     * <p>getDescription.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDescription() {
        return metadata.get(DESCRIPTION);
    }

    /**
     * <p>compareTo.</p>
     *
     * @param d a {@link forge.deck.Deck} object.
     * @return a int.
     */
    public int compareTo(Deck d) {
        return getName().compareTo(d.getName());
    }

    /** {@inheritDoc} */
    public boolean equals(Object o) {
        if (o instanceof Deck) {
            Deck d = (Deck) o;
            return getName().equals(d.getName());
        }
        return false;
    }

    /**
     * <p>Getter for the field <code>metadata</code>.</p>
     *
     * @return a {@link java.util.Set} object.
     */
    public Set<Map.Entry<String, String>> getMetadata() {
        return metadata.entrySet();
    }

    /**
     * <p>Getter for the field <code>metadata</code>.</p>
     *
     * @param key a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @since 1.0.15
     */
    public String getMetadata(String key) {
        if (metadata.containsKey(key))
            return metadata.get(key);
        
        System.err.println("In forge.deck/Deck.java, getMetadata() failed "+
                "for property '"+key+"' in deck '"+getName()+"'.");
        return "";
    }

    /**
     * <p>addMetaData.</p>
     *
     * @param key a {@link java.lang.String} object.
     * @param value a {@link java.lang.String} object.
     */
    public void addMetaData(String key, String value) {
        metadata.put(key, value);
    }
}
