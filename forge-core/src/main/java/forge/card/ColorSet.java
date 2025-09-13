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
package forge.card;

import com.google.common.collect.UnmodifiableIterator;
import forge.card.MagicColor.Color;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.util.BinaryUtil;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>CardColor class.</p>
 * <p>Represents a set of any number of colors out of 5 possible in the game</p>
 * <p><i>This class is immutable, do not generate any setters here</i></p>
 *
 * @author Max mtg
 * @version $Id: CardColor.java 9708 2011-08-09 19:34:12Z jendave $
 *
 *
 */
public final class ColorSet implements Comparable<ColorSet>, Iterable<Byte>, Serializable {
    private static final long serialVersionUID = 794691267379929080L;

    private final byte myColor;
    private final float orderWeight;

    private static final ColorSet[] cache = new ColorSet[32];

    public static final ColorSet ALL_COLORS = fromMask(MagicColor.ALL_COLORS);
    public static final ColorSet NO_COLORS = fromMask(MagicColor.COLORLESS);

    private ColorSet(final byte mask) {
        this.myColor = mask;
        this.orderWeight = this.getOrderWeight();
    }

    public static ColorSet fromMask(final int mask) {
        final int mask32 = mask & MagicColor.ALL_COLORS;
        if (cache[mask32] == null) {
            cache[mask32] = new ColorSet((byte) mask32);
        }
        return cache[mask32];
    }

    public static ColorSet fromNames(final String... colors) {
        byte mask = 0;
        for (final String s : colors) {
            mask |= MagicColor.fromName(s);
        }
        return fromMask(mask);
    }

    public static ColorSet fromNames(final Iterable<String> colors) {
        byte mask = 0;
        for (final String s : colors) {
            mask |= MagicColor.fromName(s);
        }
        return fromMask(mask);
    }

    public static ColorSet fromNames(final char[] colors) {
        byte mask = 0;
        for (final char s : colors) {
            mask |= MagicColor.fromName(s);
        }
        return fromMask(mask);
    }

    public static ColorSet fromManaCost(final ManaCost mana) {
        return fromMask(mana.getColorProfile());
    }

    /**
     * Checks for any color.
     *
     * @param colormask
     *            the colormask
     * @return true, if successful
     */
    public boolean hasAnyColor(final int colormask) {
        return (this.myColor & colormask) != 0;
    }

    /**
     * Checks for all colors.
     *
     * @param colormask
     *            the colormask
     * @return true, if successful
     */
    public boolean hasAllColors(final int colormask) {
        return (this.myColor & colormask) == colormask;
    }

    /** this has exactly the colors defined by operand.  */
    public boolean hasExactlyColor(final int colormask) {
        return this.myColor == colormask;
    }

    /** this has no other colors except defined by operand.  */
    public boolean hasNoColorsExcept(final ColorSet other) {
        return hasNoColorsExcept(other.getColor());
    }

    /** this has no other colors except defined by operand.  */
    public boolean hasNoColorsExcept(final int colormask) {
        return (this.myColor & ~colormask) == 0;
    }

    /** This returns the colors that colormask contains that are not in color */
    public ColorSet getMissingColors(final byte colormask) {
        return fromMask(this.myColor & ~colormask);
    }

    /** Operand has no other colors except defined by this. */
    public boolean containsAllColorsFrom(final int colorProfile) {
        return (~this.myColor & colorProfile) == 0;
    }

    /**
     * Count colors.
     *
     * @return the int
     */
    public int countColors() {
        return BinaryUtil.bitCount(this.myColor);
    } // bit count

    // order has to be: W U B R G multi colorless - same as cards numbering
    // through a set
    /**
     * Gets the order weight.
     *
     * @return the order weight
     */
    private float getOrderWeight() {
        float res = this.countColors();
        if (hasWhite()) {
            res += 0.0005f;
        }
        if (hasBlue()) {
            res += 0.0020f;
        }
        if (hasBlack()) {
            res += 0.0080f;
        }
        if (hasRed()) {
            res += 0.0320f;
        }
        if (hasGreen()) {
            res += 0.1280f;
        }
        return res;
    }

    /**
     * Checks if is colorless.
     *
     * @return true, if is colorless
     */
    public boolean isColorless() {
        return this.myColor == 0;
    }

    /**
     * Checks if is multicolor.
     *
     * @return true, if is multicolor
     */
    public boolean isMulticolor() {
        return this.countColors() > 1;
    }

    /**
     * Checks if is all colors.
     *
     * @return true, if is all colors
     */
    public boolean isAllColors() {
        return this == ALL_COLORS;
    }

    /**
     * Checks if is mono color.
     *
     * @return true, if is mono color
     */
    public boolean isMonoColor() {
        return this.countColors() == 1;
    }

    /**
     * Checks if is equal.
     *
     * @param color
     *            the color
     * @return true, if is equal
     */
    public boolean isEqual(final byte color) {
        return color == this.myColor;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final ColorSet other) {
        return Float.compare(this.orderWeight, other.orderWeight);
    }

    // Presets
    /**
     * Checks for white.
     *
     * @return true, if successful
     */
    public boolean hasWhite() {
        return this.hasAnyColor(MagicColor.WHITE);
    }

    /**
     * Checks for blue.
     *
     * @return true, if successful
     */
    public boolean hasBlue() {
        return this.hasAnyColor(MagicColor.BLUE);
    }

    /**
     * Checks for black.
     *
     * @return true, if successful
     */
    public boolean hasBlack() {
        return this.hasAnyColor(MagicColor.BLACK);
    }

    /**
     * Checks for red.
     *
     * @return true, if successful
     */
    public boolean hasRed() {
        return this.hasAnyColor(MagicColor.RED);
    }

    /**
     * Checks for green.
     *
     * @return true, if successful
     */
    public boolean hasGreen() {
        return this.hasAnyColor(MagicColor.GREEN);
    }

    public ColorSet inverse() {
        byte mask = this.myColor;
        mask ^= MagicColor.ALL_COLORS;
        return fromMask(mask);
    }

    public byte getColor() {
        return myColor;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final ManaCostShard[] orderedShards = getOrderedShards();
        return Arrays.stream(orderedShards).map(ManaCostShard::toShortString).collect(Collectors.joining());
    }

    /**
     * Shares color with.
     *
     * @param ccOther the cc other
     * @return true, if successful
     */
    public boolean sharesColorWith(final ColorSet ccOther) {
        return (this.myColor & ccOther.myColor) != 0;
    }

    public ColorSet getSharedColors(final ColorSet ccOther) {
        return fromMask(getColor() & ccOther.getColor());
    }

    public ColorSet getOffColors(final ColorSet ccOther) {
        return fromMask(~this.myColor & ccOther.myColor);
    }

    public Set<Color> toEnumSet() {
        if (isColorless()) {
            return EnumSet.of(Color.COLORLESS);
        }
        List<Color> list = new ArrayList<>();
        for (Color c : Color.values()) {
            if (hasAnyColor(c.getColormask())) {
                list.add(c);
            }
        }
        return EnumSet.copyOf(list);
    }

    @Override
    public Iterator<Byte> iterator() {
        return new ColorIterator();
    }

    private class ColorIterator extends UnmodifiableIterator<Byte> {
        int currentBit = -1;

        private int getIndexOfNextColor(){
            int nextBit = currentBit + 1;
            while (nextBit < MagicColor.NUMBER_OR_COLORS) {
                if ((myColor & MagicColor.WUBRG[nextBit]) != 0) {
                    break;
                }
                nextBit++;
            }
            return nextBit;
        }

        @Override
        public boolean hasNext() {
            return getIndexOfNextColor() < MagicColor.NUMBER_OR_COLORS;
        }

        @Override
        public Byte next() {
            currentBit = getIndexOfNextColor();
            if (currentBit >= MagicColor.NUMBER_OR_COLORS) {
                throw new NoSuchElementException();
            }

            return MagicColor.WUBRG[currentBit];
        }
    }

    public Stream<MagicColor.Color> stream() {
        return this.toEnumSet().stream();
    }

    //Get array of mana cost shards for color set in the proper order
    public ManaCostShard[] getOrderedShards() {
        return shardOrderLookup[myColor];
    }

    private static final ManaCostShard[][] shardOrderLookup = new ManaCostShard[MagicColor.ALL_COLORS + 1][];
    static {
        byte COLORLESS = MagicColor.COLORLESS;
        byte WHITE = MagicColor.WHITE;
        byte BLUE = MagicColor.BLUE;
        byte BLACK = MagicColor.BLACK;
        byte RED = MagicColor.RED;
        byte GREEN = MagicColor.GREEN;
        ManaCostShard C = ManaCostShard.COLORLESS;
        ManaCostShard W = ManaCostShard.WHITE;
        ManaCostShard U = ManaCostShard.BLUE;
        ManaCostShard B = ManaCostShard.BLACK;
        ManaCostShard R = ManaCostShard.RED;
        ManaCostShard G = ManaCostShard.GREEN;

        //colorless
        shardOrderLookup[COLORLESS] = new ManaCostShard[] { C };

        //mono-color
        shardOrderLookup[WHITE] = new ManaCostShard[] { W };
        shardOrderLookup[BLUE] = new ManaCostShard[] { U };
        shardOrderLookup[BLACK] = new ManaCostShard[] { B };
        shardOrderLookup[RED] = new ManaCostShard[] { R };
        shardOrderLookup[GREEN] = new ManaCostShard[] { G };

        //two-color
        shardOrderLookup[WHITE | BLUE] = new ManaCostShard[] { W, U };
        shardOrderLookup[WHITE | BLACK] = new ManaCostShard[] { W, B };
        shardOrderLookup[BLUE | BLACK] = new ManaCostShard[] { U, B };
        shardOrderLookup[BLUE | RED] = new ManaCostShard[] { U, R };
        shardOrderLookup[BLACK | RED] = new ManaCostShard[] { B, R };
        shardOrderLookup[BLACK | GREEN] = new ManaCostShard[] { B, G };
        shardOrderLookup[RED | GREEN] = new ManaCostShard[] { R, G };
        shardOrderLookup[RED | WHITE] = new ManaCostShard[] { R, W };
        shardOrderLookup[GREEN | WHITE] = new ManaCostShard[] { G, W };
        shardOrderLookup[GREEN | BLUE] = new ManaCostShard[] { G, U };

        //three-color
        shardOrderLookup[WHITE | BLUE | BLACK] = new ManaCostShard[] { W, U, B };
        shardOrderLookup[WHITE | BLACK | GREEN] = new ManaCostShard[] { W, B, G };
        shardOrderLookup[BLUE | BLACK | RED] = new ManaCostShard[] { U, B, R };
        shardOrderLookup[BLUE | RED | WHITE] = new ManaCostShard[] { U, R, W };
        shardOrderLookup[BLACK | RED | GREEN] = new ManaCostShard[] { B, R, G };
        shardOrderLookup[BLACK | GREEN | BLUE] = new ManaCostShard[] { B, G, U };
        shardOrderLookup[RED | GREEN | WHITE] = new ManaCostShard[] { R, G, W };
        shardOrderLookup[RED | WHITE | BLACK] = new ManaCostShard[] { R, W, B };
        shardOrderLookup[GREEN | WHITE | BLUE] = new ManaCostShard[] { G, W, U };
        shardOrderLookup[GREEN | BLUE | RED] = new ManaCostShard[] { G, U, R };

        //four-color
        shardOrderLookup[WHITE | BLUE | BLACK | RED] = new ManaCostShard[] { W, U, B, R };
        shardOrderLookup[BLUE | BLACK | RED | GREEN] = new ManaCostShard[] { U, B, R, G };
        shardOrderLookup[BLACK | RED | GREEN | WHITE] = new ManaCostShard[] { B, R, G, W };
        shardOrderLookup[RED | GREEN | WHITE | BLUE] = new ManaCostShard[] { R, G, W, U };
        shardOrderLookup[GREEN | WHITE | BLUE | BLACK] = new ManaCostShard[] { G, W, U, B };

        //five-color
        shardOrderLookup[WHITE | BLUE | BLACK | RED | GREEN] = new ManaCostShard[] { W, U, B, R, G };
    }
}
