package forge.deck;

import forge.PlayerType;
import forge.card.CardDb;
import forge.card.CardPool;
import forge.card.CardPoolView;
import forge.card.CardPrinted;
import forge.game.GameType;

import java.io.Serializable;

/**
 * <p>Deck class.</p>
 * 
 * The set of MTG legal cards that become player's library when the game starts. 
 * Any other data is not part of a deck and should be stored elsewhere.
 * Current fields allowed for deck metadata are Name, Title, Description, Difficulty, Icon, Deck Type.
 *
 * @author Forge
 * @version $Id$
 */
public final class Deck implements Comparable<Deck>, Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -7478025567887481994L;

    //gameType is from Constant.GameType, like GameType.Regular

    private String name;
    private GameType deckType;
    private String comment = null;
    private PlayerType playerType = null;
    
    private CardPool<CardPrinted> main;
    private CardPool<CardPrinted> sideboard;


    //gameType is from Constant.GameType, like GameType.Regular
    /**
     * <p>Constructor for Deck.</p>
     */
    public Deck() {
        main = new CardPool<CardPrinted>();
        sideboard = new CardPool<CardPrinted>();
    }

    /**
     * <p>Constructor for Deck.</p>
     *
     * @param type a {@link java.lang.String} object.
     */
    public Deck(final GameType type) {
        this();
        setDeckType(type);
    }

    /**
     * <p>Getter for the field <code>main</code>.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public CardPoolView<CardPrinted> getMain() {
        return main.getView();
    }

    /**
     * <p>Getter for the field <code>sideboard</code>.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public CardPoolView<CardPrinted> getSideboard() {
        return sideboard.getView();
    }

    /**
     * <p>getDeckType.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public GameType getDeckType() {
        return deckType;
    }

    //can only call this method ONCE
    /**
     * <p>setDeckType.</p>
     *
     * @param deckType a {@link java.lang.String} object.
     */
    void setDeckType(GameType deckType) {
        if (this.getDeckType() != null) {
            throw new IllegalStateException(
                    "Deck : setDeckType() error, deck type has already been set");
        }

        this.deckType = deckType;
    }

    /**
     * <p>setName.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void setName(String s) {
        name = s;
    }

    /**
     * <p>getName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return name;
    }

    /**
     * <p>setComment.</p>
     *
     * @param comment a {@link java.lang.String} object.
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * <p>getComment.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getComment() {
        return comment;
    }

    /**
     * <p>addMain.</p>
     *
     * @param cardName a {@link java.lang.String} object.
     */
    public void addMain(final String cardName) { addMain(CardDb.instance().getCard(cardName)); }
    public void addMain(final CardPrinted card) { main.add(card); }
    public void addMain(final CardPoolView<CardPrinted> list) { main.addAll(list); }
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
    public final void addSideboard(final CardPoolView<CardPrinted> cards) { sideboard.addAll(cards); }

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
     * <p>isDraft.</p>
     *
     * @return a boolean.
     */
    public boolean isDraft() {
        return getDeckType().equals(GameType.Draft);
    }

    /**
     * <p>isSealed.</p>
     *
     * @return a boolean.
     */
    public boolean isSealed() {
        return getDeckType().equals(GameType.Sealed);
    }

    /**
     * <p>isRegular.</p>
     *
     * @return a boolean.
     */
    public boolean isRegular() {
        return getDeckType().equals(GameType.Constructed);
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

    public void clearSideboard() {
        sideboard.clear();
    }

    public void clearMain() {
        main.clear();
        
    }

    public final PlayerType getPlayerType() {
        return playerType;
    }

    public final void setPlayerType(PlayerType recommendedPlayer0) {
        this.playerType = recommendedPlayer0;
    }
}
