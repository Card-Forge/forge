package forge.card;

/**
 * <p>CardColor class.</p>
 *
 * @author Forge
 * @version $Id: CardColor.java 9708 2011-08-09 19:34:12Z jendave $
 */
public final class CardColor implements Comparable<CardColor> {

    public static final byte WHITE = 1 << 1;
    public static final byte BLUE = 1 << 2;
    public static final byte BLACK = 1 << 3;
    public static final byte RED = 1 << 4;
    public static final byte GREEN = 1 << 5;

    private final byte myColor;

    // TODO: some cards state "CardName is %color%" (e.g. pacts of...) - fix this later
    public CardColor(final CardManaCost mana) {
        myColor = mana.getColorProfile();
    }

    public boolean hasAnyColor(final byte colormask) { return (myColor & colormask) != 0; }
    public boolean hasAllColors(final byte colormask) { return (myColor & colormask) == colormask; }

    public int countColors() { byte v = myColor; int c = 0; for (; v != 0; c++) { v &= v - 1; } return c; } // bit count

    public boolean isColorless() { return myColor == 0; }
    public boolean isMulticolor() { return countColors() > 1; }
    public boolean isMonoColor() { return countColors() == 1; }
    public boolean isEqual(final byte color) { return color == myColor; }

    @Override
    public int compareTo(final CardColor other) { return myColor - other.myColor; }

    // Presets
    public boolean hasWhite() { return hasAnyColor(WHITE); }
    public boolean hasBlue() { return hasAnyColor(BLUE); }
    public boolean hasBlack() { return hasAnyColor(BLACK); }
    public boolean hasRed() { return hasAnyColor(RED); }
    public boolean hasGreen() { return hasAnyColor(GREEN); }

    public boolean isWhite() { return isEqual(WHITE); }
    public boolean isBlue() { return isEqual(BLUE); }
    public boolean isBlack() { return isEqual(BLACK); }
    public boolean isRed() { return isEqual(RED); }
    public boolean isGreen() { return isEqual(GREEN); }

    @Override
    public String toString() {
        switch (myColor) {
            case 0: return "";
            case WHITE: return "White"; // Constant.Color.White;
            case BLUE: return "Blue"; // Constant.Color.Blue;
            case BLACK: return "Black"; // Constant.Color.Black;
            case RED: return "Red"; // Constant.Color.Red;
            case GREEN: return "Green"; // Constant.Color.Green;
            default: return "Multi";
        }
    }
}
