package forge;

/**
 * <p>IPlayerZone interface.</p>
 *
 * @author Forge
 * @version $Id$
 */
interface IPlayerZone {
    /**
     * <p>setUpdate.</p>
     *
     * @param b a boolean.
     */
    public void setUpdate(boolean b);

    /**
     * <p>getUpdate.</p>
     *
     * @return a boolean.
     */
    public boolean getUpdate();

    /**
     * <p>size.</p>
     *
     * @return a int.
     */
    public int size();

    /**
     * <p>add.</p>
     *
     * @param o a {@link java.lang.Object} object.
     */
    public void add(Object o);

    /**
     * <p>add.</p>
     *
     * @param c a {@link forge.Card} object.
     * @param index a int.
     */
    public void add(Card c, int index);

    /**
     * <p>addOnce.</p>
     *
     * @param o a {@link java.lang.Object} object.
     */
    public void addOnce(Object o);

    /**
     * <p>get.</p>
     *
     * @param index a int.
     * @return a {@link forge.Card} object.
     */
    public Card get(int index);

    /**
     * <p>remove.</p>
     *
     * @param o a {@link java.lang.Object} object.
     */
    public void remove(Object o);

    /**
     * <p>setCards.</p>
     *
     * @param c an array of {@link forge.Card} objects.
     */
    public void setCards(Card c[]);

    /**
     * <p>getCards.</p>
     *
     * @return an array of {@link forge.Card} objects.
     */
    public Card[] getCards();

    //removes all cards
    /**
     * <p>reset.</p>
     */
    public void reset();

    /**
     * <p>is.</p>
     *
     * @param zone a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean is(String zone);

    /**
     * <p>is.</p>
     *
     * @param zone a {@link java.lang.String} object.
     * @param player a {@link forge.Player} object.
     * @return a boolean.
     */
    public boolean is(String zone, Player player);

    /**
     * <p>getPlayer.</p>
     *
     * @return a {@link forge.Player} object.
     */
    public Player getPlayer();//the Player that owns this zone

    /**
     * <p>getZoneName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getZoneName();//returns the Zone's name like Graveyard

    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String toString();
}

