package forge.card;

/**
 * <p>CardRarity class.</p>
 *
 * @author Forge
 * @version $Id: CardRarity.java 9708 2011-08-09 19:34:12Z jendave $
 */

public enum CardRarity {
    BasicLand("L"),
    Common("C"),
    Uncommon("U"),
    Rare("R"),
    MythicRare("M"),
    Special("S"), // Timeshifted
    Unknown("?"); // In development

    private final String strValue;
    private CardRarity(final String sValue) {
        strValue = sValue;
    }

    @Override
    public String toString() { return strValue; }

}
