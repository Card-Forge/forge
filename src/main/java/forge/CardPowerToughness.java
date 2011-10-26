package forge;

/**
 * <p>
 * CardPowerToughness class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardPowerToughness {

    private int power;
    private int toughness;
    private long timeStamp = 0;

    /**
     * <p>
     * getTimestamp.
     * </p>
     * 
     * @return a long.
     */
    public final long getTimestamp() {
        return timeStamp;
    }

    /**
     * <p>
     * Constructor for Card_PT.
     * </p>
     * 
     * @param newPower
     *            a int.
     * @param newToughness
     *            a int.
     * @param stamp
     *            a long.
     */
    CardPowerToughness(final int newPower, final int newToughness, final long stamp) {
        power = newPower;
        toughness = newToughness;
        timeStamp = stamp;
    }

    /**
     * 
     * Get Power.
     * 
     * @return int
     */
    public final int getPower() {
        return power;
    }

    /**
     * 
     * Get Toughness.
     * 
     * @return int
     */
    public final int getToughness() {
        return toughness;
    }

    /**
     * <p>
     * equals.
     * </p>
     * 
     * @param newPower
     *            a int.
     * @param newToughness
     *            a int.
     * @param stamp
     *            a long.
     * @return a boolean.
     */
    public final boolean equals(final int newPower, final int newToughness, final long stamp) {
        return timeStamp == stamp && power == newPower && toughness == newToughness;
    }
}
