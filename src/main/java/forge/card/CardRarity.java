package forge.card;

/**
 * <p>
 * CardRarity class.
 * </p>
 * 
 * @author Forge
 * @version $Id: CardRarity.java 9708 2011-08-09 19:34:12Z jendave $
 */

public enum CardRarity {

    /** The Basic land. */
    BasicLand("L"),

    /** The Common. */
    Common("C"),

    /** The Uncommon. */
    Uncommon("U"),

    /** The Rare. */
    Rare("R"),

    /** The Mythic rare. */
    MythicRare("M"),

    /** The Special. */
    Special("S"), // Timeshifted
    /** The Unknown. */
    Unknown("?"); // In development

    private final String strValue;

    private CardRarity(final String sValue) {
        strValue = sValue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return strValue;
    }

}
