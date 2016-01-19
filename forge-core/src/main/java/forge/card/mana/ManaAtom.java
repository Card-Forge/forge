package forge.card.mana;

import forge.card.MagicColor;

/** A bitmask to represent any mana symbol as an integer. */
public abstract class ManaAtom {
    public static final int WHITE = MagicColor.WHITE;
    public static final int BLUE = MagicColor.BLUE; 
    public static final int BLACK = MagicColor.BLACK; 
    public static final int RED = MagicColor.RED;
    public static final int GREEN = MagicColor.GREEN;
    public static final int COLORLESS = 1 << 5;

    public static final byte[] MANATYPES = new byte[] { WHITE, BLUE, BLACK, RED, GREEN, COLORLESS };

    public static final int GENERIC = 1 << 6;

    // Below here skip due to byte conversion shenanigans
    public static final int IS_X = 1 << 8;
    public static final int OR_2_GENERIC = 1 << 9;
    public static final int OR_2_LIFE = 1 << 10;
    public static final int IS_SNOW = 1 << 11;
}