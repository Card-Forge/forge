package forge.card;

public class CardManaCostShard {

    private final int shard;
    public final int cmc;
    public final float cmpc;
    private final String stringValue;
    protected CardManaCostShard(int value, String sValue) { 
        shard = value;
        cmc = getCMC();
        cmpc = getCmpCost();
        stringValue = sValue;
    }

    public interface Atom {
        //int COLORLESS = 1 << 0;
        int WHITE = 1 << 1;
        int BLUE = 1 << 2;
        int BLACK = 1 << 3;
        int RED = 1 << 4;
        int GREEN = 1 << 5;

        int IS_X = 1 << 8;
        int OR_2_COLORLESS = 1 << 9;
        int OR_2_LIFE = 1 << 10;

    }

    /* Why boxed values are here?
     *  They is meant to be constant and require no further boxing if added into an arraylist or something else,
     *  with generic parameters that would require Object's descendant.
     *
     *  Choosing between "let's have some boxing" and "let's have some unboxing",
     *  I choose the latter,
     *  because memory for boxed objects will be taken from heap,
     *  while unboxed values will lay on stack, which is faster
     */
    public static final CardManaCostShard X = new CardManaCostShard(Atom.IS_X, "X");

    public static final CardManaCostShard WHITE = new CardManaCostShard(Atom.WHITE, "W");
    public static final CardManaCostShard BLUE = new CardManaCostShard(Atom.BLUE, "U");
    public static final CardManaCostShard BLACK = new CardManaCostShard(Atom.BLACK, "B");
    public static final CardManaCostShard RED = new CardManaCostShard(Atom.RED, "R");
    public static final CardManaCostShard GREEN = new CardManaCostShard(Atom.GREEN, "G");

    public static final CardManaCostShard PW = new CardManaCostShard(Atom.WHITE | Atom.OR_2_LIFE, "W/P");
    public static final CardManaCostShard PU = new CardManaCostShard(Atom.BLUE | Atom.OR_2_LIFE, "U/P");
    public static final CardManaCostShard PB = new CardManaCostShard(Atom.BLACK | Atom.OR_2_LIFE, "B/P");
    public static final CardManaCostShard PR = new CardManaCostShard(Atom.RED | Atom.OR_2_LIFE, "R/P");
    public static final CardManaCostShard PG = new CardManaCostShard(Atom.GREEN | Atom.OR_2_LIFE, "G/P");

    public static final CardManaCostShard WU = new CardManaCostShard(Atom.WHITE | Atom.BLUE, "W/U");
    public static final CardManaCostShard WB = new CardManaCostShard(Atom.WHITE | Atom.BLACK, "W/B");
    public static final CardManaCostShard WR = new CardManaCostShard(Atom.WHITE | Atom.RED, "W/R");
    public static final CardManaCostShard WG = new CardManaCostShard(Atom.WHITE | Atom.GREEN, "W/G");
    public static final CardManaCostShard UB = new CardManaCostShard(Atom.BLUE | Atom.BLACK, "U/B");
    public static final CardManaCostShard UR = new CardManaCostShard(Atom.BLUE | Atom.RED, "U/R");
    public static final CardManaCostShard UG = new CardManaCostShard(Atom.BLUE | Atom.GREEN, "U/G");
    public static final CardManaCostShard BR = new CardManaCostShard(Atom.BLACK | Atom.RED, "B/R");
    public static final CardManaCostShard BG = new CardManaCostShard(Atom.BLACK | Atom.GREEN, "B/G");
    public static final CardManaCostShard RG = new CardManaCostShard(Atom.RED | Atom.GREEN, "R/G");

    public static final CardManaCostShard W2 = new CardManaCostShard(Atom.WHITE | Atom.OR_2_COLORLESS, "2/W");
    public static final CardManaCostShard U2 = new CardManaCostShard(Atom.BLUE | Atom.OR_2_COLORLESS, "2/U");
    public static final CardManaCostShard B2 = new CardManaCostShard(Atom.BLACK | Atom.OR_2_COLORLESS, "2/B");
    public static final CardManaCostShard R2 = new CardManaCostShard(Atom.RED | Atom.OR_2_COLORLESS, "2/R");
    public static final CardManaCostShard G2 = new CardManaCostShard(Atom.GREEN | Atom.OR_2_COLORLESS, "2/G");

    private static final CardManaCostShard[] allPossible = new CardManaCostShard[] {
        X, WHITE, BLUE, BLACK, RED, GREEN,
        PW, PU, PB, PR, PG,
        WU, WB, WR, WG, UB, UR, UG, BR, BG, RG,
        W2, U2, B2, R2, G2
    };

    private int getCMC() {
        if (0 != (shard & Atom.IS_X)) { return 0; }
        if (0 != (shard & Atom.OR_2_COLORLESS)) { return 2; }
        return 1;
    }

    /**
     * Returns Mana cost, adjusted slightly to make colored mana parts more significant.
     * Should only be used for comparison purposes; using this method allows the sort:
     * 2 < X 2 < 1 U < U U < UR U < X U U < X X U U
     *
     * @return The converted cost + 0.0005* the number of colored mana in the cost + 0.00001 *
     *         the number of X's in the cost
     */
    private float getCmpCost() {
        if (0 != (shard & Atom.IS_X)) { return 0.0001f; }
        float cost = 0 != (shard & Atom.OR_2_COLORLESS) ? 2 : 1;
        // yes, these numbers are magic, slightly-magic
        if (0 != (shard & Atom.WHITE)) { cost += 0.0005f; }
        if (0 != (shard & Atom.BLUE)) { cost += 0.0020f;  }
        if (0 != (shard & Atom.BLACK)) { cost += 0.0080f;  }
        if (0 != (shard & Atom.RED)) { cost += 0.0320f; }
        if (0 != (shard & Atom.GREEN)) { cost += 0.1280f; }
        return cost;
    }

    final byte getColorMask() {
        byte result = 0;
        if (0 != (shard & Atom.WHITE)) { result |= CardColor.WHITE; }
        if (0 != (shard & Atom.BLUE)) { result |= CardColor.BLUE; }
        if (0 != (shard & Atom.BLACK)) { result |= CardColor.BLACK; }
        if (0 != (shard & Atom.RED)) { result |= CardColor.RED; }
        if (0 != (shard & Atom.GREEN)) { result |= CardColor.GREEN; }
        return result;
    }

    public static CardManaCostShard valueOf(final int atoms) {
        for (int i = 0; i < allPossible.length; i++) {
            if (allPossible[i].shard == atoms) { return allPossible[i]; }
        }
        throw new RuntimeException(String.format("Not fount: mana shard with profile = %x", atoms));
    }
    
    @Override
    public String toString() { return stringValue; }
}
