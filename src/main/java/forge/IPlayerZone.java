package forge;

import java.util.List;

/**
 * <p>
 * IPlayerZone interface.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
interface IPlayerZone {
    /**
     * <p>
     * setUpdate.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    void setUpdate(boolean b);

    /**
     * <p>
     * getUpdate.
     * </p>
     * 
     * @return a boolean.
     */
    boolean getUpdate();

    /**
     * <p>
     * size.
     * </p>
     * 
     * @return a int.
     */
    int size();

    /**
     * <p>
     * add.
     * </p>
     * 
     * @param o
     *            a {@link java.lang.Object} object.
     */
    void add(Object o);

    /**
     * <p>
     * add.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param index
     *            a int.
     */
    void add(Card c, int index);

    /**
     * <p>
     * get.
     * </p>
     * 
     * @param index
     *            a int.
     * @return a {@link forge.Card} object.
     */
    Card get(int index);

    /**
     * <p>
     * remove.
     * </p>
     * 
     * @param o
     *            a {@link java.lang.Object} object.
     */
    void remove(Object o);

    /**
     * <p>
     * setCards.
     * </p>
     * 
     * @param c
     *            an array of {@link forge.Card} objects.
     */
    void setCards(Card[] c);

    /**
     * <p>
     * getCards.
     * </p>
     * 
     * @param filter
     *            the filter
     * @return an array of {@link forge.Card} objects.
     */
    Card[] getCards(boolean filter);

    /**
     * Gets the cards.
     * 
     * @return the cards
     */
    Card[] getCards();

    /**
     * Gets the cards.
     * 
     * @param n
     *            the n
     * @return the cards
     */
    Card[] getCards(int n);

    /**
     * Contains.
     * 
     * @param c
     *            the c
     * @return true, if successful
     */
    boolean contains(Card c);

    /**
     * isEmpty returns true if given zone contains no cards.
     * 
     * @return true, if is empty
     */
    boolean isEmpty();

    // removes all cards
    /**
     * <p>
     * reset.
     * </p>
     */
    void reset();

    /**
     * <p>
     * is.
     * </p>
     * 
     * @param zone
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    boolean is(Constant.Zone zone);

    /**
     * Checks if is.
     * 
     * @param zones
     *            the zones
     * @return true, if successful
     */
    boolean is(List<Constant.Zone> zones);

    /**
     * <p>
     * is.
     * </p>
     * 
     * @param zone
     *            a {@link java.lang.String} object.
     * @param player
     *            a {@link forge.Player} object.
     * @return a boolean.
     */
    boolean is(Constant.Zone zone, Player player);

    /**
     * <p>
     * getPlayer.
     * </p>
     * 
     * @return a {@link forge.Player} object.
     */
    Player getPlayer(); // the Player that owns this zone

    /**
     * <p>
     * getZoneName.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    Constant.Zone getZoneType(); // returns the Zone's name like Graveyard

    /**
     * <p>
     * toString.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    String toString();

    /**
     * Reset cards added this turn.
     */
    void resetCardsAddedThisTurn();
}
