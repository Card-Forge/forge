package forge.card.mana;

import forge.card.MagicColor;

/** A bitmask to represent any mana symbol as an integer. */
public abstract class ManaAtom {
    public static final int COLORLESS = 1 << 6;

    /** The Constant WHITE. */
    public static final int WHITE = MagicColor.WHITE; 

    /** The Constant BLUE. */
    public static final int BLUE = MagicColor.BLUE; 

    /** The Constant BLACK. */
    public static final int BLACK = MagicColor.BLACK; 

    /** The Constant RED. */
    public static final int RED = MagicColor.RED;

    /** The Constant GREEN. */
    public static final int GREEN = MagicColor.GREEN;

    /** The Constant IS_X. */
    public static final int IS_X = 1 << 8;

    /** The Constant OR_2_COLORLESS. */
    public static final int OR_2_COLORLESS = 1 << 9;

    /** The Constant OR_2_LIFE. */
    public static final int OR_2_LIFE = 1 << 10;

    /** The Constant IS_SNOW. */
    public static final int IS_SNOW = 1 << 11;
}