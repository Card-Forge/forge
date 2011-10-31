package forge.card;

/**
 * The Class CardManaCostShard.
 */
public class CardManaCostShard {

    private final int shard;

    /** The cmc. */
    private final int cmc;

    /** The cmpc. */
    private final float cmpc;
    private final String stringValue;

    /** The image key. */
    private final String imageKey;

    /**
     * Instantiates a new card mana cost shard.
     * 
     * @param value
     *            the value
     * @param sValue
     *            the s value
     */
    protected CardManaCostShard(final int value, final String sValue) {
        this(value, sValue, sValue);
    }

    /**
     * Instantiates a new card mana cost shard.
     * 
     * @param value
     *            the value
     * @param sValue
     *            the s value
     * @param imgKey
     *            the img key
     */
    protected CardManaCostShard(final int value, final String sValue, final String imgKey) {
        shard = value;
        cmc = getCMC();
        cmpc = getCmpCost();
        stringValue = sValue;
        imageKey = imgKey;
    }

    /** A bitmask to represent any mana symbol as an integer. */
    public abstract static class Atom {
        // int COLORLESS = 1 << 0;
        /** The Constant WHITE. */
        public static final int WHITE = 1 << 1;

        /** The Constant BLUE. */
        public static final int BLUE = 1 << 2;

        /** The Constant BLACK. */
        public static final int BLACK = 1 << 3;

        /** The Constant RED. */
        public static final int RED = 1 << 4;

        /** The Constant GREEN. */
        public static final int GREEN = 1 << 5;

        /** The Constant IS_X. */
        public static final int IS_X = 1 << 8;

        /** The Constant OR_2_COLORLESS. */
        public static final int OR_2_COLORLESS = 1 << 9;

        /** The Constant OR_2_LIFE. */
        public static final int OR_2_LIFE = 1 << 10;

        /** The Constant IS_SNOW. */
        public static final int IS_SNOW = 1 << 11;

    }

    /*
     * Why boxed values are here? They is meant to be constant and require no
     * further boxing if added into an arraylist or something else, with generic
     * parameters that would require Object's descendant.
     * 
     * Choosing between "let's have some boxing" and "let's have some unboxing",
     * I choose the latter, because memory for boxed objects will be taken from
     * heap, while unboxed values will lay on stack, which is faster
     */
    /** The Constant X. */
    public static final CardManaCostShard X = new CardManaCostShard(Atom.IS_X, "X");

    /** The Constant S. */
    public static final CardManaCostShard S = new CardManaCostShard(Atom.IS_SNOW, "S");

    /** The Constant WHITE. */
    public static final CardManaCostShard WHITE = new CardManaCostShard(Atom.WHITE, "W");

    /** The Constant BLUE. */
    public static final CardManaCostShard BLUE = new CardManaCostShard(Atom.BLUE, "U");

    /** The Constant BLACK. */
    public static final CardManaCostShard BLACK = new CardManaCostShard(Atom.BLACK, "B");

    /** The Constant RED. */
    public static final CardManaCostShard RED = new CardManaCostShard(Atom.RED, "R");

    /** The Constant GREEN. */
    public static final CardManaCostShard GREEN = new CardManaCostShard(Atom.GREEN, "G");

    /** The Constant PW. */
    public static final CardManaCostShard PW = new CardManaCostShard(Atom.WHITE | Atom.OR_2_LIFE, "W/P", "PW");

    /** The Constant PU. */
    public static final CardManaCostShard PU = new CardManaCostShard(Atom.BLUE | Atom.OR_2_LIFE, "U/P", "PU");

    /** The Constant PB. */
    public static final CardManaCostShard PB = new CardManaCostShard(Atom.BLACK | Atom.OR_2_LIFE, "B/P", "PB");

    /** The Constant PR. */
    public static final CardManaCostShard PR = new CardManaCostShard(Atom.RED | Atom.OR_2_LIFE, "R/P", "PR");

    /** The Constant PG. */
    public static final CardManaCostShard PG = new CardManaCostShard(Atom.GREEN | Atom.OR_2_LIFE, "G/P", "PG");

    /** The Constant WU. */
    public static final CardManaCostShard WU = new CardManaCostShard(Atom.WHITE | Atom.BLUE, "W/U", "WU");

    /** The Constant WB. */
    public static final CardManaCostShard WB = new CardManaCostShard(Atom.WHITE | Atom.BLACK, "W/B", "WB");

    /** The Constant WR. */
    public static final CardManaCostShard WR = new CardManaCostShard(Atom.WHITE | Atom.RED, "W/R", "RW");

    /** The Constant WG. */
    public static final CardManaCostShard WG = new CardManaCostShard(Atom.WHITE | Atom.GREEN, "W/G", "GW");

    /** The Constant UB. */
    public static final CardManaCostShard UB = new CardManaCostShard(Atom.BLUE | Atom.BLACK, "U/B", "UB");

    /** The Constant UR. */
    public static final CardManaCostShard UR = new CardManaCostShard(Atom.BLUE | Atom.RED, "U/R", "UR");

    /** The Constant UG. */
    public static final CardManaCostShard UG = new CardManaCostShard(Atom.BLUE | Atom.GREEN, "U/G", "GU");

    /** The Constant BR. */
    public static final CardManaCostShard BR = new CardManaCostShard(Atom.BLACK | Atom.RED, "B/R", "BR");

    /** The Constant BG. */
    public static final CardManaCostShard BG = new CardManaCostShard(Atom.BLACK | Atom.GREEN, "B/G", "BG");

    /** The Constant RG. */
    public static final CardManaCostShard RG = new CardManaCostShard(Atom.RED | Atom.GREEN, "R/G", "RG");

    /** The Constant W2. */
    public static final CardManaCostShard W2 = new CardManaCostShard(Atom.WHITE | Atom.OR_2_COLORLESS, "2/W", "2W");

    /** The Constant U2. */
    public static final CardManaCostShard U2 = new CardManaCostShard(Atom.BLUE | Atom.OR_2_COLORLESS, "2/U", "2U");

    /** The Constant B2. */
    public static final CardManaCostShard B2 = new CardManaCostShard(Atom.BLACK | Atom.OR_2_COLORLESS, "2/B", "2B");

    /** The Constant R2. */
    public static final CardManaCostShard R2 = new CardManaCostShard(Atom.RED | Atom.OR_2_COLORLESS, "2/R", "2R");

    /** The Constant G2. */
    public static final CardManaCostShard G2 = new CardManaCostShard(Atom.GREEN | Atom.OR_2_COLORLESS, "2/G", "2G");

    private static final CardManaCostShard[] ALL_POSSIBLE = new CardManaCostShard[] { X, WHITE, BLUE, BLACK, RED, GREEN,
            PW, PU, PB, PR, PG, WU, WB, WR, WG, UB, UR, UG, BR, BG, RG, W2, U2, B2, R2, G2 };

    private int getCMC() {
        if (0 != (shard & Atom.IS_X)) {
            return 0;
        }
        if (0 != (shard & Atom.OR_2_COLORLESS)) {
            return 2;
        }
        return 1;
    }

    /**
     * Returns Mana cost, adjusted slightly to make colored mana parts more
     * significant. Should only be used for comparison purposes; using this
     * method allows the sort: 2 < X 2 < 1 U < U U < UR U < X U U < X X U U
     * 
     * @return The converted cost + 0.0005* the number of colored mana in the
     *         cost + 0.00001 * the number of X's in the cost
     */
    private float getCmpCost() {
        if (0 != (shard & Atom.IS_X)) {
            return 0.0001f;
        }
        float cost = 0 != (shard & Atom.OR_2_COLORLESS) ? 2 : 1;
        // yes, these numbers are magic, slightly-magic
        if (0 != (shard & Atom.WHITE)) {
            cost += 0.0005f;
        }
        if (0 != (shard & Atom.BLUE)) {
            cost += 0.0020f;
        }
        if (0 != (shard & Atom.BLACK)) {
            cost += 0.0080f;
        }
        if (0 != (shard & Atom.RED)) {
            cost += 0.0320f;
        }
        if (0 != (shard & Atom.GREEN)) {
            cost += 0.1280f;
        }
        if (0 != (shard & Atom.OR_2_LIFE)) {
            cost += 0.00003f;
        }
        return cost;
    }

    /**
     * Gets the color mask.
     * 
     * @return the color mask
     */
    final byte getColorMask() {
        byte result = 0;
        if (0 != (shard & Atom.WHITE)) {
            result |= CardColor.WHITE;
        }
        if (0 != (shard & Atom.BLUE)) {
            result |= CardColor.BLUE;
        }
        if (0 != (shard & Atom.BLACK)) {
            result |= CardColor.BLACK;
        }
        if (0 != (shard & Atom.RED)) {
            result |= CardColor.RED;
        }
        if (0 != (shard & Atom.GREEN)) {
            result |= CardColor.GREEN;
        }
        return result;
    }

    /**
     * Value of.
     * 
     * @param atoms
     *            the atoms
     * @return the card mana cost shard
     */
    public static CardManaCostShard valueOf(final int atoms) {
        for (int i = 0; i < ALL_POSSIBLE.length; i++) {
            if (ALL_POSSIBLE[i].shard == atoms) {
                return ALL_POSSIBLE[i];
            }
        }
        throw new RuntimeException(String.format("Not fount: mana shard with profile = %x", atoms));
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public final String toString() {
        return stringValue;
    }

    /**
     * @return the cmc
     */
    public int getCmc() {
        return cmc;
    }

    /**
     * @return the cmpc
     */
    public float getCmpc() {
        return cmpc;
    }

    /**
     * @return the imageKey
     */
    public String getImageKey() {
        return imageKey;
    }
}
