package forge.card.mana;

import forge.card.MagicColor;

/** A bitmask to represent any mana symbol as an integer. */
public abstract class ManaAtom {
    public static final int COLORLESS = 1 << 0;

    /** The Constant WHITE. */
    public static final int WHITE = MagicColor.WHITE; // 1 << 1;

    /** The Constant BLUE. */
    public static final int BLUE = MagicColor.BLUE; // 1 << 2;

    /** The Constant BLACK. */
    public static final int BLACK = MagicColor.BLACK; // 1 << 3;

    /** The Constant RED. */
    public static final int RED = MagicColor.RED; // 1 << 4;

    /** The Constant GREEN. */
    public static final int GREEN = MagicColor.GREEN; // 1 << 5;

    /** The Constant IS_X. */
    public static final int IS_X = 1 << 8;

    /** The Constant OR_2_COLORLESS. */
    public static final int OR_2_COLORLESS = 1 << 9;

    /** The Constant OR_2_LIFE. */
    public static final int OR_2_LIFE = 1 << 10;

    /** The Constant IS_SNOW. */
    public static final int IS_SNOW = 1 << 11;
}