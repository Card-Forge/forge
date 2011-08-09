package forge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Observable;

/**
 * <p>DefaultPlayerZone class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class DefaultPlayerZone extends PlayerZone implements java.io.Serializable {
    /** Constant <code>serialVersionUID=-5687652485777639176L</code> */
    private static final long serialVersionUID = -5687652485777639176L;

    private ArrayList<Card> cards = new ArrayList<Card>();
    private String zoneName;
    private Player player;
    private boolean update = true;

    private CardList cardsAddedThisTurn = new CardList();
    private ArrayList<String> cardsAddedThisTurnSource = new ArrayList<String>();

    /**
     * <p>Constructor for DefaultPlayerZone.</p>
     *
     * @param zone a {@link java.lang.String} object.
     * @param inPlayer a {@link forge.Player} object.
     */
    public DefaultPlayerZone(String zone, Player inPlayer) {
        zoneName = zone;
        player = inPlayer;
    }

    //************ BEGIN - these methods fire updateObservers() *************

    /**
     *
     * @param o a {@link java.lang.Object} object.
     */
    public void add(Object o) {
        Card c = (Card) o;

        if (!c.isImmutable()) //Immutable cards are usually emblems,effects and the mana pool and we don't want to log those.
        {
            cardsAddedThisTurn.add(c);
            if (AllZone.getZone(c) != null) {
                cardsAddedThisTurnSource.add(AllZone.getZone(c).getZoneName());
            } else {
                cardsAddedThisTurnSource.add("None");
            }
        }

        if (is("Graveyard")
                && c.hasKeyword("When CARDNAME is put into a graveyard from anywhere, reveal CARDNAME and shuffle it into its owner's library instead.")) {
            PlayerZone lib = AllZone.getZone(Constant.Zone.Library, c.getOwner());
            lib.add(c);
            c.getOwner().shuffle();
            return;
        }
        //slight difference from above I guess, the card gets put into the grave first, then shuffled into library.
        //key is that this would trigger abilities that trigger on cards hitting the graveyard
        else if (is("Graveyard")
                && c.hasKeyword("When CARDNAME is put into a graveyard from anywhere, shuffle it into its owner's library.")) {
            PlayerZone lib = AllZone.getZone(Constant.Zone.Library, c.getOwner());
            PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, c.getOwner());

            grave.addOnce(c);
            grave.remove(c);
            lib.add(c);
            c.getOwner().shuffle();
            return;
        }


        if (is("Graveyard")
                && c.hasKeyword("When CARDNAME is put into a graveyard from anywhere, reveal CARDNAME and its owner shuffles his or her graveyard into his or her library.")) {
            PlayerZone lib = AllZone.getZone(Constant.Zone.Library, c.getOwner());
            PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, c.getOwner());
            lib.add(c);
            for (Card gc : AllZoneUtil.getPlayerGraveyard(c.getOwner()))
                lib.add(gc);
            grave.reset();
            c.getOwner().shuffle();
            return;
        }

        if (c.isUnearthed() && (is("Graveyard") || is("Hand") || is("Library"))) {
            PlayerZone removed = AllZone.getZone(Constant.Zone.Exile, c.getOwner());
            removed.add(c);
            c.setUnearthed(false);
            return;
        }


        c.addObserver(this);

        c.setTurnInZone(AllZone.getPhase().getTurn());

        cards.add((Card) c);
        update();
    }

    //hack... use for adding Dread / Serra Avenger to grave

    /**
     *
     * @param o a {@link java.lang.Object} object.
     */
    public void addOnce(Object o) {
        Card c = (Card) o;

        if (!c.isImmutable()) //Immutable cards are usually emblems,effects and the mana pool and we don't want to log those.
        {
            cardsAddedThisTurn.add(c);
            if (AllZone.getZone(c) != null) {
                cardsAddedThisTurnSource.add(AllZone.getZone(c).getZoneName());
            } else {
                cardsAddedThisTurnSource.add("None");
            }
        }

        c.addObserver(this);

        cards.add((Card) c);
        update();
    }

    /**
     *
     * @param ob
     * @param object
     */
    public void update(Observable ob, Object object) {
        this.update();
    }

    /**
     *
     * @param c a {@link forge.Card} object.
     * @param index a int.
     */
    public void add(Card c, int index) {
        if (!c.isImmutable()) //Immutable cards are usually emblems,effects and the mana pool and we don't want to log those.
        {
            cardsAddedThisTurn.add(c);
            if (AllZone.getZone(c) != null) {
                cardsAddedThisTurnSource.add(AllZone.getZone(c).getZoneName());
            } else {
                cardsAddedThisTurnSource.add("None");
            }
        }

        cards.add(index, c);
        c.setTurnInZone(AllZone.getPhase().getTurn());
        update();
    }

    /**
     *
     * @param c
     */
    public void remove(Object c) {
        cards.remove((Card) c);
        update();
    }

    /**
     * <p>Setter for the field <code>cards</code>.</p>
     *
     * @param c an array of {@link forge.Card} objects.
     */
    public void setCards(Card c[]) {
        cards = new ArrayList<Card>(Arrays.asList(c));
        update();
    }

    //removes all cards
    /**
     * <p>reset.</p>
     */
    public void reset() {
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
    public boolean is(String zone) {
        return zone.equals(zoneName);
    }

    /**
     *
     * @param zone a {@link java.lang.String} object.
     * @param player a {@link forge.Player} object.
     * @return a boolean
     */
    public boolean is(String zone, Player player) {
        return (zone.equals(zoneName) && player.isPlayer(player));
    }

    /**
     * <p>Getter for the field <code>player</code>.</p>
     *
     * @return a {@link forge.Player} object.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * <p>Getter for the field <code>zoneName</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getZoneName() {
        return zoneName;
    }

    /**
     * <p>size.</p>
     *
     * @return a int.
     */
    public int size() {
        return cards.size();
    }

    /**
     *
     * @param index a int.
     * @return  a int
     */
    public Card get(int index) {
        return (Card) cards.get(index);
    }

    /**
     * <p>Getter for the field <code>cards</code>.</p>
     *
     * @return an array of {@link forge.Card} objects.
     */
    public Card[] getCards() {
        Card c[] = new Card[cards.size()];
        cards.toArray(c);
        return c;
    }

    /**
     * <p>update.</p>
     */
    public void update() {
        if (update)
            updateObservers();
    }

    /**
     *
     * @param b a boolean.
     */
    public void setUpdate(boolean b) {
        update = b;
    }

    /**
     * <p>Getter for the field <code>update</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getUpdate() {
        return update;
    }

    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (player != null)
            sb.append(player.toString()).append(" ");
        sb.append(zoneName);
        return sb.toString();
    }

    /**
     * <p>Getter for the field <code>cardsAddedThisTurn</code>.</p>
     *
     * @param origin a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList getCardsAddedThisTurn(String origin) {
        System.out.print("Request cards put into " + getZoneName() + " from " + origin + ".Amount: ");
        CardList ret = new CardList();
        for (int i = 0; i < cardsAddedThisTurn.size(); i++) {
            if (origin.equals(cardsAddedThisTurnSource.get(i)) || origin.equals("Any")) {
                ret.add(cardsAddedThisTurn.get(i));
            }
        }
        System.out.println(ret.size());
        return ret;
    }

    /**
     * <p>resetCardsAddedThisTurn.</p>
     */
    public void resetCardsAddedThisTurn() {
        cardsAddedThisTurn.clear();
        cardsAddedThisTurnSource.clear();
    }
}
