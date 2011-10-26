package forge.card;

import forge.Constant;

/**
 * <p>
 * CardColor class.
 * </p>
 * 
 * @author Forge
 * @version $Id: CardColor.java 9708 2011-08-09 19:34:12Z jendave $
 */
public final class CardColor implements Comparable<CardColor> {

    /** The Constant WHITE. */
    public static final byte WHITE = 1 << 1;

    /** The Constant BLUE. */
    public static final byte BLUE = 1 << 2;

    /** The Constant BLACK. */
    public static final byte BLACK = 1 << 3;

    /** The Constant RED. */
    public static final byte RED = 1 << 4;

    /** The Constant GREEN. */
    public static final byte GREEN = 1 << 5;

    private final byte myColor;
    private final int orderWeight;

    // TODO: some cards state "CardName is %color%" (e.g. pacts of...) - fix
    // this later
    /**
     * Instantiates a new card color.
     * 
     * @param mana
     *            the mana
     */
    public CardColor(final CardManaCost mana) {
        myColor = mana.getColorProfile();
        orderWeight = getOrderWeight();
    }

    private CardColor() {
        myColor = 0;
        orderWeight = -1;
    }

    /** The null color. */
    public static CardColor nullColor = new CardColor();

    /**
     * Checks for any color.
     * 
     * @param colormask
     *            the colormask
     * @return true, if successful
     */
    public boolean hasAnyColor(final byte colormask) {
        return (myColor & colormask) != 0;
    }

    /**
     * Checks for all colors.
     * 
     * @param colormask
     *            the colormask
     * @return true, if successful
     */
    public boolean hasAllColors(final byte colormask) {
        return (myColor & colormask) == colormask;
    }

    /**
     * Count colors.
     * 
     * @return the int
     */
    public int countColors() {
        byte v = myColor;
        int c = 0;
        for (; v != 0; c++) {
            v &= v - 1;
        }
        return c;
    } // bit count

    // order has to be: W U B R G multi colorless - same as cards numbering
    // through a set
    /**
     * Gets the order weight.
     * 
     * @return the order weight
     */
    public int getOrderWeight() {
        return myColor == 0 ? 0x400 : (countColors() == 1 ? myColor : 0x200);
    }

    /**
     * Checks if is colorless.
     * 
     * @return true, if is colorless
     */
    public boolean isColorless() {
        return myColor == 0;
    }

    /**
     * Checks if is multicolor.
     * 
     * @return true, if is multicolor
     */
    public boolean isMulticolor() {
        return countColors() > 1;
    }

    /**
     * Checks if is mono color.
     * 
     * @return true, if is mono color
     */
    public boolean isMonoColor() {
        return countColors() == 1;
    }

    /**
     * Checks if is equal.
     * 
     * @param color
     *            the color
     * @return true, if is equal
     */
    public boolean isEqual(final byte color) {
        return color == myColor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final CardColor other) {
        return orderWeight - other.orderWeight;
    }

    // Presets
    /**
     * Checks for white.
     * 
     * @return true, if successful
     */
    public boolean hasWhite() {
        return hasAnyColor(WHITE);
    }

    /**
     * Checks for blue.
     * 
     * @return true, if successful
     */
    public boolean hasBlue() {
        return hasAnyColor(BLUE);
    }

    /**
     * Checks for black.
     * 
     * @return true, if successful
     */
    public boolean hasBlack() {
        return hasAnyColor(BLACK);
    }

    /**
     * Checks for red.
     * 
     * @return true, if successful
     */
    public boolean hasRed() {
        return hasAnyColor(RED);
    }

    /**
     * Checks for green.
     * 
     * @return true, if successful
     */
    public boolean hasGreen() {
        return hasAnyColor(GREEN);
    }

    /**
     * Checks if is white.
     * 
     * @return true, if is white
     */
    public boolean isWhite() {
        return isEqual(WHITE);
    }

    /**
     * Checks if is blue.
     * 
     * @return true, if is blue
     */
    public boolean isBlue() {
        return isEqual(BLUE);
    }

    /**
     * Checks if is black.
     * 
     * @return true, if is black
     */
    public boolean isBlack() {
        return isEqual(BLACK);
    }

    /**
     * Checks if is red.
     * 
     * @return true, if is red
     */
    public boolean isRed() {
        return isEqual(RED);
    }

    /**
     * Checks if is green.
     * 
     * @return true, if is green
     */
    public boolean isGreen() {
        return isEqual(GREEN);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        if (orderWeight == -1) {
            return "n/a";
        }
        switch (myColor) {
        case 0:
            return Constant.Color.Colorless;
        case WHITE:
            return Constant.Color.White;
        case BLUE:
            return Constant.Color.Blue;
        case BLACK:
            return Constant.Color.Black;
        case RED:
            return Constant.Color.Red;
        case GREEN:
            return Constant.Color.Green;
        default:
            return "multi";
        }
    }
}
