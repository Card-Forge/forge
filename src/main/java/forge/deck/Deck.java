package forge.deck;

import forge.Constant;
import forge.card.CardDb;
import forge.card.CardPool;
import forge.card.CardPoolView;
import forge.card.CardPrinted;

import java.io.Serializable;
import java.util.*;

/**
 * <p>Deck class.</p>
 *
 * @author Forge
 * @version $Id: Deck.java 10183 2011-09-02 22:51:47Z Max mtg $
 */
public final class Deck implements Comparable<Deck>, Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -7478025567887481994L;

    //gameType is from Constant.GameType, like Constant.GameType.Regular

    private Map<String, String> metadata = new HashMap<String, String>();

    private CardPool main;
    private CardPool sideboard;
    private CardPool humanExtraCards;
    private CardPool aiExtraCards;

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
        main = new CardPool();
        sideboard = new CardPool();
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

        this.main = new CardPool(main);
        this.sideboard = new CardPool(sideboard);
    }

    /**
     * <p>Constructor for Deck.</p>
     *
     * @param type a {@link java.lang.String} object.
     */
    public Deck(final String type) {
        this();
        setDeckType(type);
    }

    /**
     * <p>Getter for the field <code>main</code>.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public CardPoolView getMain() {
        return main.getView();
    }

    /**
     * <p>Getter for the field <code>sideboard</code>.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public CardPoolView getSideboard() {
        return sideboard.getView();
    }
    
    /**
     * <p>Getter for the field <code>humanExtraCards</code>.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public CardPoolView getHumanExtraCards() {
        return humanExtraCards.getView();
    }
    
    /**
     * <p>Getter for the field <code>aiExtraCards</code>.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public CardPoolView getAIExtraCards() {
        return aiExtraCards.getView();
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
    public void addMain(final String cardName) { addMain(CardDb.instance().getCard(cardName)); }
    public void addMain(final CardPrinted card) { main.add(card); }
    public void addMain(final CardPoolView list) { main.addAll(list); }
    public void removeMain(final CardPrinted card) { main.remove(card); }
    public void removeMain(final CardPrinted card, final int amount) { main.remove(card, amount); }
    public int countMain() { return main.countAll(); }

    /**
     * <p>addSideboard.</p>
     *
     * @param cardName a {@link java.lang.String} object.
     */
    public final void addSideboard(final String cardName) { addSideboard(CardDb.instance().getCard(cardName)); }
    public final void addSideboard(final CardPrinted card) { sideboard.add(card); }
    public final void addSideboard(final CardPrinted card, final int amount) { sideboard.add(card, amount); }
    public final void addSideboard(final CardPoolView cards) { sideboard.addAll(cards); }

    /**
     * <p>countSideboard.</p>
     *
     * @return a int.
     */
    public int countSideboard() {
        return sideboard.countAll();
    }

    /**
     * <p>removeSideboard.</p>
     *
     * @param index a int.
     * @return a {@link java.lang.String} object.
     */
    public void removeFromSideboard(CardPrinted card) {
        sideboard.remove(card);
    }

    /**
     * <p>addHumanExtraCards.</p>
     *
     * @param cardName a {@link java.lang.String} object.
     */
    public void addHumanExtraCards(final String cardName) { addHumanExtraCards(CardDb.instance().getCard(cardName)); }
    public void addHumanExtraCards(final CardPrinted card) { humanExtraCards.add(card); }
    public void addHumanExtraCards(final CardPoolView list) { humanExtraCards.addAll(list); }
    public void removeHumanExtraCards(final CardPrinted card) { humanExtraCards.remove(card); }
    public void removeHumanExtraCards(final CardPrinted card, final int amount) { humanExtraCards.remove(card, amount); }
    public int countHumanExtraCards() { return main.countAll(); }
    
    /**
     * <p>addAIExtraCards.</p>
     *
     * @param cardName a {@link java.lang.String} object.
     */
    public void addAIExtraCards(final String cardName) { addHumanExtraCards(CardDb.instance().getCard(cardName)); }
    public void addAIExtraCards(final CardPrinted card) { aiExtraCards.add(card); }
    public void addAIExtraCards(final CardPoolView list) { aiExtraCards.addAll(list); }
    public void removeAIExtraCards(final CardPrinted card) { aiExtraCards.remove(card); }
    public void removeAIExtraCards(final CardPrinted card, final int amount) { aiExtraCards.remove(card, amount); }
    public int countAIExtraCards() { return aiExtraCards.countAll(); }
    
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

    public void clearSideboard() {
        sideboard.clear();
    }

    public void clearMain() {
        main.clear();
        
    }

}
