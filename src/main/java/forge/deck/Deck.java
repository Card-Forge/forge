package forge.deck;

import java.io.Serializable;

import forge.PlayerType;
import forge.game.GameType;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.ItemPool;
import forge.item.ItemPoolView;

/**
 * <p>
 * Deck class.
 * </p>
 * 
 * The set of MTG legal cards that become player's library when the game starts.
 * Any other data is not part of a deck and should be stored elsewhere. Current
 * fields allowed for deck metadata are Name, Title, Description, Difficulty,
 * Icon, Deck Type.
 * 
 * @author Forge
 * @version $Id$
 */
public final class Deck implements Comparable<Deck>, Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -7478025567887481994L;

    // gameType is from Constant.GameType, like GameType.Regular

    private String name;
    private GameType deckType;
    private String comment = null;
    private PlayerType playerType = null;

    private boolean customPool = false;

    private ItemPool<CardPrinted> main;
    private ItemPool<CardPrinted> sideboard;

    // gameType is from Constant.GameType, like GameType.Regular
    /**
     * <p>
     * Constructor for Deck.
     * </p>
     */
    public Deck() {
        this.main = new ItemPool<CardPrinted>(CardPrinted.class);
        this.sideboard = new ItemPool<CardPrinted>(CardPrinted.class);
    }

    /**
     * <p>
     * Constructor for Deck.
     * </p>
     * 
     * @param type
     *            a {@link java.lang.String} object.
     */
    public Deck(final GameType type) {
        this();
        this.setDeckType(type);
    }

    /**
     * <p>
     * Getter for the field <code>main</code>.
     * </p>
     * 
     * @return a {@link java.util.List} object.
     */
    public ItemPoolView<CardPrinted> getMain() {
        return this.main.getView();
    }

    /**
     * <p>
     * Getter for the field <code>sideboard</code>.
     * </p>
     * 
     * @return a {@link java.util.List} object.
     */
    public ItemPoolView<CardPrinted> getSideboard() {
        return this.sideboard.getView();
    }

    /**
     * <p>
     * getDeckType.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public GameType getDeckType() {
        return this.deckType;
    }

    // can only call this method ONCE
    /**
     * <p>
     * setDeckType.
     * </p>
     * 
     * @param deckType
     *            a {@link java.lang.String} object.
     */
    void setDeckType(final GameType deckType) {
        if (this.getDeckType() != null) {
            throw new IllegalStateException("Deck : setDeckType() error, deck type has already been set");
        }

        this.deckType = deckType;
    }

    /**
     * <p>
     * setName.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public void setName(final String s) {
        this.name = s;
    }

    /**
     * <p>
     * getName.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return this.name;
    }

    /**
     * <p>
     * setComment.
     * </p>
     * 
     * @param comment
     *            a {@link java.lang.String} object.
     */
    public void setComment(final String comment) {
        this.comment = comment;
    }

    /**
     * <p>
     * getComment.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getComment() {
        return this.comment;
    }

    /**
     * <p>
     * addMain.
     * </p>
     * 
     * @param cardName
     *            a {@link java.lang.String} object.
     */
    public void addMain(final String cardName) {
        this.addMain(CardDb.instance().getCard(cardName));
    }

    /**
     * Adds the main.
     * 
     * @param card
     *            the card
     */
    public void addMain(final CardPrinted card) {
        this.main.add(card);
    }

    /**
     * Adds the main.
     * 
     * @param card
     *            the card
     * @param amount
     *            the amount
     */
    public void addMain(final CardPrinted card, final int amount) {
        this.main.add(card, amount);
    }

    /**
     * Adds the main.
     * 
     * @param list
     *            the list
     */
    public void addMain(final ItemPoolView<CardPrinted> list) {
        this.main.addAll(list);
    }

    /**
     * Sets the main.
     * 
     * @param cards
     *            the new main
     */
    public void setMain(final Iterable<String> cards) {
        this.main = new ItemPool<CardPrinted>(cards, CardPrinted.class);
    }

    /**
     * Removes the main.
     * 
     * @param card
     *            the card
     */
    public void removeMain(final CardPrinted card) {
        this.main.remove(card);
    }

    /**
     * Removes the main.
     * 
     * @param card
     *            the card
     * @param amount
     *            the amount
     */
    public void removeMain(final CardPrinted card, final int amount) {
        this.main.remove(card, amount);
    }

    /**
     * Count main.
     * 
     * @return the int
     */
    public int countMain() {
        return this.main.countAll();
    }

    /**
     * <p>
     * addSideboard.
     * </p>
     * 
     * @param cardName
     *            a {@link java.lang.String} object.
     */
    public void addSideboard(final String cardName) {
        this.addSideboard(CardDb.instance().getCard(cardName));
    }

    /**
     * Adds the sideboard.
     * 
     * @param card
     *            the card
     */
    public void addSideboard(final CardPrinted card) {
        this.sideboard.add(card);
    }

    /**
     * Adds the sideboard.
     * 
     * @param card
     *            the card
     * @param amount
     *            the amount
     */
    public void addSideboard(final CardPrinted card, final int amount) {
        this.sideboard.add(card, amount);
    }

    /**
     * Adds the sideboard.
     * 
     * @param cards
     *            the cards
     */
    public void addSideboard(final ItemPoolView<CardPrinted> cards) {
        this.sideboard.addAll(cards);
    }

    /**
     * Sets the sideboard.
     * 
     * @param cards
     *            the new sideboard
     */
    public void setSideboard(final Iterable<String> cards) {
        this.sideboard = new ItemPool<CardPrinted>(cards, CardPrinted.class);
    }

    /**
     * <p>
     * countSideboard.
     * </p>
     * 
     * @return a int.
     */
    public int countSideboard() {
        return this.sideboard.countAll();
    }

    /**
     * <p>
     * removeSideboard.
     * </p>
     * 
     * @param card
     *            the card
     * 
     */
    public void removeFromSideboard(final CardPrinted card) {
        this.sideboard.remove(card);
    }

    /**
     * <p>
     * isDraft.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isDraft() {
        return this.getDeckType().equals(GameType.Draft);
    }

    /**
     * <p>
     * isSealed.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isSealed() {
        return this.getDeckType().equals(GameType.Sealed);
    }

    /**
     * <p>
     * isRegular.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isRegular() {
        return this.getDeckType().equals(GameType.Constructed);
    }

    /**
     * <p>
     * hashCode.
     * </p>
     * 
     * @return a int.
     */
    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getName();
    }

    /**
     * <p>
     * compareTo.
     * </p>
     * 
     * @param d
     *            a {@link forge.deck.Deck} object.
     * @return a int.
     */
    @Override
    public int compareTo(final Deck d) {
        return this.getName().compareTo(d.getName());
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object o) {
        if (o instanceof Deck) {
            final Deck d = (Deck) o;
            return this.getName().equals(d.getName());
        }
        return false;
    }

    /**
     * Clear sideboard.
     */
    public void clearSideboard() {
        this.sideboard.clear();
    }

    /**
     * Clear main.
     */
    public void clearMain() {
        this.main.clear();

    }

    /**
     * Gets the player type.
     * 
     * @return the player type
     */
    public PlayerType getPlayerType() {
        return this.playerType;
    }

    /**
     * Sets the player type.
     * 
     * @param recommendedPlayer0
     *            the new player type
     */
    public void setPlayerType(final PlayerType recommendedPlayer0) {
        this.playerType = recommendedPlayer0;
    }

    /**
     * Checks if is custom pool.
     * 
     * @return true, if is custom pool
     */
    public boolean isCustomPool() {
        return this.customPool;
    }

    /**
     * Sets the custom pool.
     * 
     * @param cp
     *            the new custom pool
     */
    public void setCustomPool(final boolean cp) {
        this.customPool = cp;
    }
}
