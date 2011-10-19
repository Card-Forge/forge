package forge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;

import forge.Constant.Zone;

/**
 * <p>DefaultPlayerZone class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class DefaultPlayerZone extends PlayerZone implements java.io.Serializable {
    /** Constant <code>serialVersionUID=-5687652485777639176L</code>. */
    private static final long serialVersionUID = -5687652485777639176L;

    protected List<Card> cards = new ArrayList<Card>();
    private final Constant.Zone zoneName;
    private final Player player;
    private boolean update = true;

    private CardList cardsAddedThisTurn = new CardList();
    private ArrayList<Constant.Zone> cardsAddedThisTurnSource = new ArrayList<Constant.Zone>();

    /**
     * <p>Constructor for DefaultPlayerZone.</p>
     *
     * @param zone a {@link java.lang.String} object.
     * @param inPlayer a {@link forge.Player} object.
     */
    public DefaultPlayerZone(final Constant.Zone zone, final Player inPlayer) {
        zoneName = zone;
        player = inPlayer;
    }

    //************ BEGIN - these methods fire updateObservers() *************

    /**
     *
     * @param o a {@link java.lang.Object} object.
     */
    public void add(final Object o) {
        Card c = (Card) o;

        //Immutable cards are usually emblems,effects and the mana pool and we don't want to log those.
        if (!c.isImmutable()) {
            cardsAddedThisTurn.add(c);
            if (AllZone.getZoneOf(c) != null) {
                cardsAddedThisTurnSource.add(AllZone.getZoneOf(c).getZoneType());
            } else {
                cardsAddedThisTurnSource.add(null);
            }
        }

        if (is(Zone.Graveyard)
                && c.hasKeyword("When CARDNAME is put into a graveyard from anywhere, reveal CARDNAME and shuffle it into its owner's library instead.")) {
            PlayerZone lib = c.getOwner().getZone(Constant.Zone.Library);
            lib.add(c);
            c.getOwner().shuffle();
            return;
        }

        if (is(Zone.Graveyard)
                && c.hasKeyword("When CARDNAME is put into a graveyard from anywhere, reveal CARDNAME and its owner shuffles his or her graveyard into his or her library.")) {
            PlayerZone lib = c.getOwner().getZone(Constant.Zone.Library);
            PlayerZone grave = c.getOwner().getZone(Constant.Zone.Graveyard);
            lib.add(c);
            for (Card gc : c.getOwner().getCardsIn(Zone.Graveyard)) {
                lib.add(gc);
            }
            grave.reset();
            c.getOwner().shuffle();
            return;
        }

        if (c.isUnearthed() && (is(Zone.Graveyard) || is(Zone.Hand) || is(Zone.Library))) {
            PlayerZone removed = c.getOwner().getZone(Constant.Zone.Exile);
            removed.add(c);
            c.setUnearthed(false);
            return;
        }

        c.addObserver(this);

        c.setTurnInZone(AllZone.getPhase().getTurn());

        cards.add((Card) c);
        update();
    }

    /**
     *
     * @param ob an Observable
     * @param object an Object
     */
    public final void update(final Observable ob, final Object object) {
        this.update();
    }

    /**
     *
     * @param c a {@link forge.Card} object.
     * @param index a int.
     */
    public final void add(final Card c, final int index) {
        //Immutable cards are usually emblems,effects and the mana pool and we don't want to log those.
        if (!c.isImmutable()) {
            cardsAddedThisTurn.add(c);
            if (AllZone.getZoneOf(c) != null) {
                cardsAddedThisTurnSource.add(AllZone.getZoneOf(c).getZoneType());
            } else {
                cardsAddedThisTurnSource.add(null);
            }
        }

        cards.add(index, c);
        c.setTurnInZone(AllZone.getPhase().getTurn());
        update();
    }
    
    public final boolean contains(Card c) {
        return cards.contains(c);
    }

    /**
     *
     * @param c an Object
     */
    public void remove(final Object c) {
        cards.remove((Card) c);
        update();
    }

    /**
     * <p>Setter for the field <code>cards</code>.</p>
     *
     * @param c an array of {@link forge.Card} objects.
     */
    public final void setCards(final Card[] c) {
        cards = new ArrayList<Card>(Arrays.asList(c));
        update();
    }

    //removes all cards
    /**
     * <p>reset.</p>
     */
    public final void reset() {
        cardsAddedThisTurn.clear();
        cardsAddedThisTurnSource.clear();
        cards.clear();
        update();
    }
    //************ END - these methods fire updateObservers() *************

    /**
     *
     * @param zone a {@link java.lang.String} object.
     * @return a boolean
     */
    public final boolean is(final Constant.Zone zone) {
        return zone.equals(zoneName);
    }
    public final boolean is(final List<Constant.Zone> zones) {
        return zones.contains(zoneName);
    }

    /**
     *
     * @param zone a {@link java.lang.String} object.
     * @param player a {@link forge.Player} object.
     * @return a boolean
     */
    public final boolean is(final Constant.Zone zone, final Player player) {
        return (zone.equals(zoneName) && player.isPlayer(player));
    }

    /**
     * <p>Getter for the field <code>player</code>.</p>
     *
     * @return a {@link forge.Player} object.
     */
    public final Player getPlayer() {
        return player;
    }

    /**
     * <p>Getter for the field <code>zoneName</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final Constant.Zone getZoneType() {
        return zoneName;
    }

    /**
     * <p>size.</p>
     *
     * @return a int.
     */
    public final int size() {
        return cards.size();
    }

    /**
     *
     * @param index a int.
     * @return  a int
     */
    public final Card get(final int index) {
        return (Card) cards.get(index);
    }

    /**
     * <p>Getter for the field <code>cards</code>.</p>
     *
     * @return an array of {@link forge.Card} objects.
     */
    public Card[] getCards() {
        return getCards(true);
    }
    
    @Override
    public Card[] getCards(boolean filter) {
        // Non-Battlefield PlayerZones don't care about the filter
        Card[] c = new Card[cards.size()];
        cards.toArray(c);
        return c;
    }

    public final Card[] getCards(int n) {
        Card[] c = new Card[Math.min(cards.size(),n)];
        for (int i = 0; i < c.length; i++) { c[i] = cards.get(i); }
        return c;
    }    
    
    @Override public boolean isEmpty() {
        return cards.isEmpty(); 
    }    

    /**
     * <p>update.</p>
     */
    public final void update() {
        if (update) {
            updateObservers();
        }
    }

    /**
     *
     * @param b a boolean.
     */
    public final void setUpdate(final boolean b) {
        update = b;
    }

    /**
     * <p>Getter for the field <code>update</code>.</p>
     *
     * @return a boolean.
     */
    public final boolean getUpdate() {
        return update;
    }

    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String toString() {
        return player != null ? String.format("%s %s", player, zoneName) : zoneName.toString();
    }

    /**
     * <p>Getter for the field <code>cardsAddedThisTurn</code>.</p>
     *
     * @param origin a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    public final CardList getCardsAddedThisTurn(final Constant.Zone origin) {
        System.out.print("Request cards put into " + getZoneType() + " from " + origin + ".Amount: ");
        CardList ret = new CardList();
        for (int i = 0; i < cardsAddedThisTurn.size(); i++) {
            if (cardsAddedThisTurnSource.get(i) == origin || origin == null /* former: equals('Any') */) {
                ret.add(cardsAddedThisTurn.get(i));
            }
        }
        System.out.println(ret.size());
        return ret;
    }

    /**
     * <p>resetCardsAddedThisTurn.</p>
     */
    public final void resetCardsAddedThisTurn() {
        cardsAddedThisTurn.clear();
        cardsAddedThisTurnSource.clear();
    }

}
