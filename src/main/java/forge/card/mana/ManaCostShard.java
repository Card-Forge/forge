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

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.util.BinaryUtil;

/**
 * The Class CardManaCostShard.
 */
public class ManaCostShard {

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
    protected ManaCostShard(final int value, final String sValue) {
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
    protected ManaCostShard(final int value, final String sValue, final String imgKey) {
        this.shard = value;
        this.cmc = this.getCMC();
        this.cmpc = this.getCmpCost();
        this.stringValue = sValue;
        this.imageKey = imgKey;
    }

    /** A bitmask to represent any mana symbol as an integer. */
    public abstract static class Atom {
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

    public static final int COLORS_SUPERPOSITION = Atom.WHITE | Atom.BLUE | Atom.BLACK | Atom.RED | Atom.GREEN;

    /*
     * Why boxed values are here? They is meant to be constant and require no
     * further boxing if added into an arraylist or something else, with generic
     * parameters that would require Object's descendant.
     * 
     * Choosing between "let's have some boxing" and "let's have some unboxing",
     * I choose the latter, because memory for boxed objects will be taken from
     * heap, while unboxed values will lay on stack, which is faster
     */

    public static final ManaCostShard COLORLESS = new ManaCostShard(Atom.COLORLESS, "1");

    /** The Constant X. */
    public static final ManaCostShard X = new ManaCostShard(Atom.IS_X, "X");

    /** The Constant S. */
    public static final ManaCostShard S = new ManaCostShard(Atom.IS_SNOW, "S");

    /** The Constant WHITE. */
    public static final ManaCostShard WHITE = new ManaCostShard(Atom.WHITE, "W");

    /** The Constant BLUE. */
    public static final ManaCostShard BLUE = new ManaCostShard(Atom.BLUE, "U");

    /** The Constant BLACK. */
    public static final ManaCostShard BLACK = new ManaCostShard(Atom.BLACK, "B");

    /** The Constant RED. */
    public static final ManaCostShard RED = new ManaCostShard(Atom.RED, "R");

    /** The Constant GREEN. */
    public static final ManaCostShard GREEN = new ManaCostShard(Atom.GREEN, "G");

    /** The Constant PW. */
    public static final ManaCostShard PW = new ManaCostShard(Atom.WHITE | Atom.OR_2_LIFE, "W/P", "PW");

    /** The Constant PU. */
    public static final ManaCostShard PU = new ManaCostShard(Atom.BLUE | Atom.OR_2_LIFE, "U/P", "PU");

    /** The Constant PB. */
    public static final ManaCostShard PB = new ManaCostShard(Atom.BLACK | Atom.OR_2_LIFE, "B/P", "PB");

    /** The Constant PR. */
    public static final ManaCostShard PR = new ManaCostShard(Atom.RED | Atom.OR_2_LIFE, "R/P", "PR");

    /** The Constant PG. */
    public static final ManaCostShard PG = new ManaCostShard(Atom.GREEN | Atom.OR_2_LIFE, "G/P", "PG");

    /** The Constant WU. */
    public static final ManaCostShard WU = new ManaCostShard(Atom.WHITE | Atom.BLUE, "W/U", "WU");

    /** The Constant WB. */
    public static final ManaCostShard WB = new ManaCostShard(Atom.WHITE | Atom.BLACK, "W/B", "WB");

    /** The Constant WR. */
    public static final ManaCostShard WR = new ManaCostShard(Atom.WHITE | Atom.RED, "W/R", "RW");

    /** The Constant WG. */
    public static final ManaCostShard WG = new ManaCostShard(Atom.WHITE | Atom.GREEN, "W/G", "GW");

    /** The Constant UB. */
    public static final ManaCostShard UB = new ManaCostShard(Atom.BLUE | Atom.BLACK, "U/B", "UB");

    /** The Constant UR. */
    public static final ManaCostShard UR = new ManaCostShard(Atom.BLUE | Atom.RED, "U/R", "UR");

    /** The Constant UG. */
    public static final ManaCostShard UG = new ManaCostShard(Atom.BLUE | Atom.GREEN, "U/G", "GU");

    /** The Constant BR. */
    public static final ManaCostShard BR = new ManaCostShard(Atom.BLACK | Atom.RED, "B/R", "BR");

    /** The Constant BG. */
    public static final ManaCostShard BG = new ManaCostShard(Atom.BLACK | Atom.GREEN, "B/G", "BG");

    /** The Constant RG. */
    public static final ManaCostShard RG = new ManaCostShard(Atom.RED | Atom.GREEN, "R/G", "RG");

    /** The Constant W2. */
    public static final ManaCostShard W2 = new ManaCostShard(Atom.WHITE | Atom.OR_2_COLORLESS, "2/W", "2W");

    /** The Constant U2. */
    public static final ManaCostShard U2 = new ManaCostShard(Atom.BLUE | Atom.OR_2_COLORLESS, "2/U", "2U");

    /** The Constant B2. */
    public static final ManaCostShard B2 = new ManaCostShard(Atom.BLACK | Atom.OR_2_COLORLESS, "2/B", "2B");

    /** The Constant R2. */
    public static final ManaCostShard R2 = new ManaCostShard(Atom.RED | Atom.OR_2_COLORLESS, "2/R", "2R");

    /** The Constant G2. */
    public static final ManaCostShard G2 = new ManaCostShard(Atom.GREEN | Atom.OR_2_COLORLESS, "2/G", "2G");

    private static final ManaCostShard[] ALL_POSSIBLE = new ManaCostShard[] { ManaCostShard.X, ManaCostShard.COLORLESS,
            ManaCostShard.WHITE, ManaCostShard.BLUE, ManaCostShard.BLACK, ManaCostShard.RED,
            ManaCostShard.GREEN, ManaCostShard.PW, ManaCostShard.PU, ManaCostShard.PB,
            ManaCostShard.PR, ManaCostShard.PG, ManaCostShard.WU, ManaCostShard.WB,
            ManaCostShard.WR, ManaCostShard.WG, ManaCostShard.UB, ManaCostShard.UR,
            ManaCostShard.UG, ManaCostShard.BR, ManaCostShard.BG, ManaCostShard.RG,
            ManaCostShard.W2, ManaCostShard.U2, ManaCostShard.B2, ManaCostShard.R2,
            ManaCostShard.G2, ManaCostShard.S };

    private int getCMC() {
        if (0 != (this.shard & Atom.IS_X)) {
            return 0;
        }
        if (0 != (this.shard & Atom.OR_2_COLORLESS)) {
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
        if (0 != (this.shard & Atom.IS_X)) {
            return 0.0001f;
        }
        float cost = 0 != (this.shard & Atom.OR_2_COLORLESS) ? 2 : 1;
        // yes, these numbers are magic, slightly-magic
        if (0 != (this.shard & Atom.WHITE)) {
            cost += 0.0005f;
        }
        if (0 != (this.shard & Atom.BLUE)) {
            cost += 0.0020f;
        }
        if (0 != (this.shard & Atom.BLACK)) {
            cost += 0.0080f;
        }
        if (0 != (this.shard & Atom.RED)) {
            cost += 0.0320f;
        }
        if (0 != (this.shard & Atom.GREEN)) {
            cost += 0.1280f;
        }
        if (0 != (this.shard & Atom.OR_2_LIFE)) {
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
        byte result = 0;
        if (0 != (this.shard & Atom.WHITE)) {
            result |= MagicColor.WHITE;
        }
        if (0 != (this.shard & Atom.BLUE)) {
            result |= MagicColor.BLUE;
        }
        if (0 != (this.shard & Atom.BLACK)) {
            result |= MagicColor.BLACK;
        }
        if (0 != (this.shard & Atom.RED)) {
            result |= MagicColor.RED;
        }
        if (0 != (this.shard & Atom.GREEN)) {
            result |= MagicColor.GREEN;
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
    public static ManaCostShard valueOf(final int atoms) {
        for (final ManaCostShard element : ManaCostShard.ALL_POSSIBLE) {
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
                case 'W': atoms |= Atom.WHITE;          break;
                case 'U': atoms |= Atom.BLUE;           break;
                case 'B': atoms |= Atom.BLACK;          break;
                case 'R': atoms |= Atom.RED;            break;
                case 'G': atoms |= Atom.GREEN;          break;
                case 'P': atoms |= Atom.OR_2_LIFE;      break;
                case 'S': atoms |= Atom.IS_SNOW;        break;
                case 'X': atoms |= Atom.IS_X;           break;
                case '2': atoms |= Atom.OR_2_COLORLESS; break;
                default:
                    if (c <= '9' && c >= '0') {
                        atoms |= Atom.COLORLESS;
                    }
                    break;
            }
        }
        // for cases when unparsed equals '2' or unparsed is like '12' or '20'
        if (atoms == Atom.OR_2_COLORLESS || atoms == (Atom.OR_2_COLORLESS | Atom.COLORLESS)) {
            atoms = Atom.COLORLESS;
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

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public boolean isPhyrexian() {
        return (this.shard & Atom.OR_2_LIFE) != 0;
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public boolean isSnow() {
        return (this.shard & Atom.IS_SNOW) != 0;
    }

    public boolean isMonoColor() {
        int colormask = this.shard & COLORS_SUPERPOSITION;
        return BinaryUtil.bitCount(colormask) == 1;

    }

    public boolean isOr2Colorless() {
        return (this.shard & Atom.OR_2_COLORLESS) != 0;
    }
    /**
     * TODO: Can pay for this shard with unlimited mana of given color combination?
     * @param color
     * @return
     */
    public boolean canBePaidWithAvaliable(ColorSet color) {
        // can pay with life?
        if (this.isPhyrexian()) {
            return true;
        }
        // can pay with any color?
        if (this.isOr2Colorless()) {
            return true;
        }
        // either colored part is empty, or there are same colors in shard and mana source
        return (COLORS_SUPERPOSITION & this.shard) == 0 || (color.getColor() & this.shard) > 0;
    }

    public boolean canBePaidWithManaOfColor(byte colorCode) {
        return this.isOr2Colorless() || (COLORS_SUPERPOSITION & this.shard) == 0 || (colorCode & this.shard) > 0;
    }
}
