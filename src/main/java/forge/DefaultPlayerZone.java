package forge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;

import forge.Constant.Zone;

/**
 * <p>
 * DefaultPlayerZone class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class DefaultPlayerZone extends PlayerZone implements java.io.Serializable {
    /** Constant <code>serialVersionUID=-5687652485777639176L</code>. */
    private static final long serialVersionUID = -5687652485777639176L;

    /** The cards. */
    private List<Card> cardList = new ArrayList<Card>();
    private final Constant.Zone zoneName;
    private final Player player;
    private boolean update = true;

    private final CardList cardsAddedThisTurn = new CardList();
    private final ArrayList<Constant.Zone> cardsAddedThisTurnSource = new ArrayList<Constant.Zone>();

    /**
     * <p>
     * Constructor for DefaultPlayerZone.
     * </p>
     * 
     * @param zone
     *            a {@link java.lang.String} object.
     * @param inPlayer
     *            a {@link forge.Player} object.
     */
    public DefaultPlayerZone(final Constant.Zone zone, final Player inPlayer) {
        this.zoneName = zone;
        this.player = inPlayer;
    }

    // ************ BEGIN - these methods fire updateObservers() *************

    /**
     * Adds the.
     * 
     * @param o
     *            a {@link java.lang.Object} object.
     */
    @Override
    public void add(final Object o) {
        final Card c = (Card) o;

        // Immutable cards are usually emblems,effects and the mana pool and we
        // don't want to log those.
        if (!c.isImmutable()) {
            this.cardsAddedThisTurn.add(c);
            PlayerZone zone = AllZone.getZoneOf(c);
            if (zone != null) {
                this.cardsAddedThisTurnSource.add(zone.getZoneType());
            } else {
                this.cardsAddedThisTurnSource.add(null);
            }
        }

        if (this.is(Zone.Graveyard)
                && c.hasKeyword("If CARDNAME would be put into a graveyard "
        + "from anywhere, reveal CARDNAME and shuffle it into its owner's library instead.")) {
            final PlayerZone lib = c.getOwner().getZone(Constant.Zone.Library);
            lib.add(c);
            c.getOwner().shuffle();
            return;
        }

        if (c.isUnearthed() && (this.is(Zone.Graveyard) || this.is(Zone.Hand) || this.is(Zone.Library))) {
            final PlayerZone removed = c.getOwner().getZone(Constant.Zone.Exile);
            removed.add(c);
            c.setUnearthed(false);
            return;
        }

        c.addObserver(this);

        c.setTurnInZone(AllZone.getPhase().getTurn());

        this.getCardList().add(c);
        this.update();
    }

    /**
     * Update.
     * 
     * @param ob
     *            an Observable
     * @param object
     *            an Object
     */
    @Override
    public final void update(final Observable ob, final Object object) {
        this.update();
    }

    /**
     * Adds the.
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param index
     *            a int.
     */
    @Override
    public final void add(final Card c, final int index) {
        // Immutable cards are usually emblems,effects and the mana pool and we
        // don't want to log those.
        if (!c.isImmutable()) {
            this.cardsAddedThisTurn.add(c);
            PlayerZone zone = AllZone.getZoneOf(c);
            if (zone != null) {
                this.cardsAddedThisTurnSource.add(zone.getZoneType());
            } else {
                this.cardsAddedThisTurnSource.add(null);
            }
        }

        this.getCardList().add(index, c);
        c.setTurnInZone(AllZone.getPhase().getTurn());
        this.update();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.IPlayerZone#contains(forge.Card)
     */
    /**
     * @return boolean
     * @param c
     *            Card
     */
    @Override
    public final boolean contains(final Card c) {
        return this.getCardList().contains(c);
    }

    /**
     * Removes the.
     * 
     * @param c
     *            an Object
     */
    @Override
    public void remove(final Object c) {
        this.getCardList().remove(c);
        this.update();
    }

    /**
     * <p>
     * Setter for the field <code>cards</code>.
     * </p>
     * 
     * @param c
     *            an array of {@link forge.Card} objects.
     */
    @Override
    public final void setCards(final Card[] c) {
        this.setCardList(new ArrayList<Card>(Arrays.asList(c)));
        this.update();
    }

    // removes all cards
    /**
     * <p>
     * reset.
     * </p>
     */
    @Override
    public final void reset() {
        this.cardsAddedThisTurn.clear();
        this.cardsAddedThisTurnSource.clear();
        this.getCardList().clear();
        this.update();
    }

    // ************ END - these methods fire updateObservers() *************

    /**
     * Checks if is.
     * 
     * @param zone
     *            a {@link java.lang.String} object.
     * @return a boolean
     */
    @Override
    public final boolean is(final Constant.Zone zone) {
        return zone.equals(this.zoneName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.IPlayerZone#is(java.util.List)
     */
    @Override
    public final boolean is(final List<Constant.Zone> zones) {
        return zones.contains(this.zoneName);
    }

    /**
     * Checks if is.
     * 
     * @param zone
     *            a {@link java.lang.String} object.
     * @param player
     *            a {@link forge.Player} object.
     * @return a boolean
     */
    @Override
    public final boolean is(final Constant.Zone zone, final Player player) {
        return (zone.equals(this.zoneName) && player.isPlayer(player));
    }

    /**
     * <p>
     * Getter for the field <code>player</code>.
     * </p>
     * 
     * @return a {@link forge.Player} object.
     */
    @Override
    public final Player getPlayer() {
        return this.player;
    }

    /**
     * <p>
     * Getter for the field <code>zoneName</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final Constant.Zone getZoneType() {
        return this.zoneName;
    }

    /**
     * <p>
     * size.
     * </p>
     * 
     * @return a int.
     */
    @Override
    public final int size() {
        return this.getCardList().size();
    }

    /**
     * Gets the.
     * 
     * @param index
     *            a int.
     * @return a int
     */
    @Override
    public final Card get(final int index) {
        return this.getCardList().get(index);
    }

    /**
     * <p>
     * Getter for the field <code>cards</code>.
     * </p>
     * 
     * @return an array of {@link forge.Card} objects.
     */
    @Override
    public final Card[] getCards() {
        return this.getCards(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.IPlayerZone#getCards(boolean)
     */
    @Override
    public Card[] getCards(final boolean filter) {
        // Non-Battlefield PlayerZones don't care about the filter
        final Card[] c = new Card[this.getCardList().size()];
        this.getCardList().toArray(c);
        return c;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.IPlayerZone#getCards(int)
     */
    @Override
    public final Card[] getCards(final int n) {
        final Card[] c = new Card[Math.min(this.getCardList().size(), n)];
        for (int i = 0; i < c.length; i++) {
            c[i] = this.getCardList().get(i);
        }
        return c;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.IPlayerZone#isEmpty()
     */
    @Override
    public final boolean isEmpty() {
        return this.getCardList().isEmpty();
    }

    /**
     * <p>
     * update.
     * </p>
     */
    public final void update() {
        if (this.update) {
            this.updateObservers();
        }
    }

    /**
     * Sets the update.
     * 
     * @param b
     *            a boolean.
     */
    @Override
    public final void setUpdate(final boolean b) {
        this.update = b;
    }

    /**
     * <p>
     * Getter for the field <code>update</code>.
     * </p>
     * 
     * @return a boolean.
     */
    @Override
    public final boolean getUpdate() {
        return this.update;
    }

    /**
     * <p>
     * toString.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final String toString() {
        return this.player != null ? String.format("%s %s", this.player, this.zoneName) : this.zoneName.toString();
    }

    /**
     * <p>
     * Getter for the field <code>cardsAddedThisTurn</code>.
     * </p>
     * 
     * @param origin
     *            a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    public final CardList getCardsAddedThisTurn(final Constant.Zone origin) {
        System.out.print("Request cards put into " + this.getZoneType() + " from " + origin + ".Amount: ");
        final CardList ret = new CardList();
        for (int i = 0; i < this.cardsAddedThisTurn.size(); i++) {
            if ((this.cardsAddedThisTurnSource.get(i) == origin) || (origin == null /*
                                                                                     * former
                                                                                     * :
                                                                                     * equals
                                                                                     * (
                                                                                     * 'Any')
                                                                                     */)) {
                ret.add(this.cardsAddedThisTurn.get(i));
            }
        }
        System.out.println(ret.size());
        return ret;
    }

    /**
     * <p>
     * resetCardsAddedThisTurn.
     * </p>
     */
    @Override
    public final void resetCardsAddedThisTurn() {
        this.cardsAddedThisTurn.clear();
        this.cardsAddedThisTurnSource.clear();
    }

    /**
     * @return the cardList
     */
    public List<Card> getCardList() {
        return cardList;
    }

    /**
     * @param cardList the cardList to set
     */
    public void setCardList(List<Card> cardList) {
        this.cardList = cardList; // TODO: Add 0 to parameter's name.
    }

}
