/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.card.mana;

import forge.util.BinaryUtil;

/**
 * The Class CardManaCostShard.
 */
public enum ManaCostShard {
    // declaration order matters! Place the shards that offer least ways to be paid for first

    /* Pure colors */
    WHITE(ManaAtom.WHITE, "W"),
    BLUE(ManaAtom.BLUE, "U"),
    BLACK(ManaAtom.BLACK, "B"),
    RED(ManaAtom.RED, "R"),
    GREEN(ManaAtom.GREEN, "G"),
    COLORLESS(ManaAtom.COLORLESS, "C"),

    /* Hybrid */
    WU(ManaAtom.WHITE | ManaAtom.BLUE, "W/U", "WU"),
    WB(ManaAtom.WHITE | ManaAtom.BLACK, "W/B", "WB"),
    UB(ManaAtom.BLUE | ManaAtom.BLACK, "U/B", "UB"),
    UR(ManaAtom.BLUE | ManaAtom.RED, "U/R", "UR"),
    BR(ManaAtom.BLACK | ManaAtom.RED, "B/R", "BR"),
    BG(ManaAtom.BLACK | ManaAtom.GREEN, "B/G", "BG"),
    RW(ManaAtom.RED | ManaAtom.WHITE, "R/W", "RW"),
    RG(ManaAtom.RED | ManaAtom.GREEN, "R/G", "RG"),
    GW(ManaAtom.GREEN | ManaAtom.WHITE, "G/W", "GW"),
    GU(ManaAtom.GREEN | ManaAtom.BLUE, "G/U", "GU"),

    /* Or 2 generic */
    W2(ManaAtom.WHITE | ManaAtom.OR_2_GENERIC, "2/W", "2W"),
    U2(ManaAtom.BLUE | ManaAtom.OR_2_GENERIC, "2/U", "2U"),
    B2(ManaAtom.BLACK | ManaAtom.OR_2_GENERIC, "2/B", "2B"),
    R2(ManaAtom.RED | ManaAtom.OR_2_GENERIC, "2/R", "2R"),
    G2(ManaAtom.GREEN | ManaAtom.OR_2_GENERIC, "2/G", "2G"),

    // Snow and colorless
    S(ManaAtom.IS_SNOW, "S"),
    GENERIC(ManaAtom.GENERIC, "1"),


    /* Phyrexian */
    PW(ManaAtom.WHITE | ManaAtom.OR_2_LIFE, "P/W", "PW"),
    PU(ManaAtom.BLUE | ManaAtom.OR_2_LIFE, "P/U", "PU"),
    PB(ManaAtom.BLACK | ManaAtom.OR_2_LIFE, "P/B", "PB"),
    PR(ManaAtom.RED | ManaAtom.OR_2_LIFE, "P/R", "PR"),
    PG(ManaAtom.GREEN | ManaAtom.OR_2_LIFE, "P/G", "PG"),
    PBG(ManaAtom.BLACK | ManaAtom.GREEN | ManaAtom.OR_2_LIFE, "P/B/G", "PBG"),
    PBR(ManaAtom.BLACK | ManaAtom.RED | ManaAtom.OR_2_LIFE, "P/B/R", "PBR"),
    PGU(ManaAtom.GREEN | ManaAtom.BLUE | ManaAtom.OR_2_LIFE, "P/G/U", "PGU"),
    PGW(ManaAtom.GREEN | ManaAtom.WHITE | ManaAtom.OR_2_LIFE, "P/G/W", "PGW"),
    PRG(ManaAtom.RED | ManaAtom.GREEN | ManaAtom.OR_2_LIFE, "P/R/G", "PRG"),
    PRW(ManaAtom.RED | ManaAtom.WHITE | ManaAtom.OR_2_LIFE, "P/R/W", "PRW"),
    PUB(ManaAtom.BLUE | ManaAtom.BLACK | ManaAtom.OR_2_LIFE, "P/U/B", "PUB"),
    PUR(ManaAtom.BLUE | ManaAtom.RED | ManaAtom.OR_2_LIFE, "P/U/R", "PUR"),
    PWB(ManaAtom.WHITE | ManaAtom.BLACK | ManaAtom.OR_2_LIFE, "P/W/B", "PWB"),
    PWU(ManaAtom.WHITE | ManaAtom.BLUE | ManaAtom.OR_2_LIFE, "P/W/U", "PWU"),

    X(ManaAtom.IS_X, "X"),

    // Colored only X, each color can be used to pay for this only once (for Emblazoned Golem)
    COLORED_X(ManaAtom.WHITE | ManaAtom.BLUE | ManaAtom.BLACK | ManaAtom.RED | ManaAtom.GREEN | ManaAtom.IS_X, "1");

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
    ManaCostShard(final int value, final String sValue) {
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
    ManaCostShard(final int value, final String sValue, final String imgKey) {
        this.shard = value;
        this.cmc = this.getCMC();
        this.cmpc = this.getCmpCost();
        this.stringValue = "{" + sValue + "}";
        this.imageKey = imgKey;
    }

    public static final int COLORS_SUPERPOSITION = ManaAtom.WHITE | ManaAtom.BLUE | ManaAtom.BLACK | ManaAtom.RED | ManaAtom.GREEN;

    private int getCMC() {
        if (0 != (this.shard & ManaAtom.IS_X)) {
            return 0;
        }
        if (0 != (this.shard & ManaAtom.OR_2_GENERIC)) {
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
        if (0 != (this.shard & ManaAtom.IS_X)) {
            return 0.0001f;
        }
        float cost = 0 != (this.shard & ManaAtom.OR_2_GENERIC) ? 2 : 1;
        // yes, these numbers are magic, slightly-magic
        if (0 != (this.shard & ManaAtom.WHITE)) {
            cost += 0.0005f;
        }
        if (0 != (this.shard & ManaAtom.BLUE)) {
            cost += 0.0020f;
        }
        if (0 != (this.shard & ManaAtom.BLACK)) {
            cost += 0.0080f;
        }
        if (0 != (this.shard & ManaAtom.RED)) {
            cost += 0.0320f;
        }
        if (0 != (this.shard & ManaAtom.GREEN)) {
            cost += 0.1280f;
        }
        if (0 != (this.shard & ManaAtom.OR_2_LIFE)) {
            cost += 0.00003f;
        }
        return cost;
    }

    /**
     * Gets the color mask.
     * 
     * @return the color mask
     */
    public final byte getColorMask() {
        return (byte)(this.shard & COLORS_SUPERPOSITION);
    }

    /**
     * Value of.
     * 
     * @param atoms
     *            the atoms
     * @return the card mana cost shard
     */
    public static ManaCostShard valueOf(final int atoms) {
        if ( atoms == 0 ) return ManaCostShard.GENERIC;
        for (final ManaCostShard element : ManaCostShard.values()) {
            if (element.shard == atoms) {
                return element;
            }
        }
        return null; // will consider anything else plain colorless;

        //throw new RuntimeException(String.format("Not found: mana shard with profile = %x", atoms));
    }

    public static ManaCostShard parseNonGeneric(final String unparsed) {
        int atoms = 0;
        for (int iChar = 0; iChar < unparsed.length(); iChar++) {
            char c = unparsed.charAt(iChar);
            switch (c) {
                case 'W': atoms |= ManaAtom.WHITE;          break;
                case 'U': atoms |= ManaAtom.BLUE;           break;
                case 'B': atoms |= ManaAtom.BLACK;          break;
                case 'R': atoms |= ManaAtom.RED;            break;
                case 'G': atoms |= ManaAtom.GREEN;          break;
                case 'P': atoms |= ManaAtom.OR_2_LIFE;      break;
                case 'S': atoms |= ManaAtom.IS_SNOW;        break;
                case 'X': atoms |= ManaAtom.IS_X;           break;
                case 'C': atoms |= ManaAtom.COLORLESS;      break;
                case '2': atoms |= ManaAtom.OR_2_GENERIC;   break;
                default:
                    if (c <= '9' && c >= '0') {
                        atoms |= ManaAtom.GENERIC;
                    }
                    break;
            }
        }
        // for cases when unparsed equals '2' or unparsed is like '12' or '20'
        if (atoms == ManaAtom.OR_2_GENERIC || atoms == (ManaAtom.OR_2_GENERIC | ManaAtom.GENERIC)) {
            atoms = ManaAtom.GENERIC;
        }
        return ManaCostShard.valueOf(atoms);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public final String toString() {
        return this.stringValue;
    }

    /**
     * Gets the cmc.
     * 
     * @return the cmc
     */
    public int getCmc() {
        return this.cmc;
    }

    /**
     * Gets the cmpc.
     * 
     * @return the cmpc
     */
    public float getCmpc() {
        return this.cmpc;
    }

    /**
     * Gets the image key.
     * 
     * @return the imageKey
     */
    public String getImageKey() {
        return this.imageKey;
    }

    public boolean isWhite() {
        return isOfKind(ManaAtom.WHITE);
    }
    public boolean isBlue() {
        return isOfKind(ManaAtom.BLUE);
    }
    public boolean isBlack() {
        return isOfKind(ManaAtom.BLACK);
    }
    public boolean isRed() {
        return isOfKind(ManaAtom.RED);
    }
    public boolean isGreen() {
        return isOfKind(ManaAtom.GREEN);
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public boolean isPhyrexian() {
        return isOfKind(ManaAtom.OR_2_LIFE);
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public boolean isSnow() {
        return isOfKind(ManaAtom.IS_SNOW);
    }

    public boolean isMonoColor() {
        return BinaryUtil.bitCount(this.shard & COLORS_SUPERPOSITION) == 1;
    }
    
    public boolean isHybrid() {
        return BinaryUtil.bitCount(this.shard & COLORS_SUPERPOSITION) == 2;
    }

    public boolean isGeneric() {
    	return isOfKind(ManaAtom.GENERIC)|| isOfKind(ManaAtom.IS_X) || this.isSnow() || this.isOr2Generic();
    }
    public boolean isOr2Generic() {
        return isOfKind(ManaAtom.OR_2_GENERIC);
    }
    
    public boolean isColor(byte colorCode) {
        return (colorCode & this.shard) > 0;
    }

    public boolean canBePaidWithManaOfColor(byte colorCode) {
        return this.isOr2Generic() || ((COLORS_SUPERPOSITION | ManaAtom.COLORLESS) & this.shard) == 0 ||
                this.isColor(colorCode);
    }
    
    public boolean isOfKind(int atom) {
        return (this.shard & atom) != 0;
    }
    
    public int getShard() {
        return this.shard;
    }
}
