package forge.card;

/**
 * <p>CardRarity class.</p>
 *
 * @author Forge
 * @version $Id: CardRarity.java 9708 2011-08-09 19:34:12Z jendave $
 */

public enum CardRarity {
    BasicLand(0, "L"),
    Common(1, "C"),
    Uncommon(2, "U"),
    Rare(3, "R"),
    MythicRare(4, "M"),
    Special(10, "S"); // Timeshifted

    private final int rating;
    private final String strValue;
    private CardRarity(final int value, final String sValue) {
        rating = value;
        strValue = sValue;
    }

    @Override
    public String toString() { return strValue; }

}
