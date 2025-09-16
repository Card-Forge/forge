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

    // needs to be before other static
    private static final Color[][] colorOrderLookup = new Color[MagicColor.ALL_COLORS + 1][];
    static {
        byte COLORLESS = MagicColor.COLORLESS;
        byte WHITE = MagicColor.WHITE;
        byte BLUE = MagicColor.BLUE;
        byte BLACK = MagicColor.BLACK;
        byte RED = MagicColor.RED;
        byte GREEN = MagicColor.GREEN;
        Color C = Color.COLORLESS;
        Color W = Color.WHITE;
        Color U = Color.BLUE;
        Color B = Color.BLACK;
        Color R = Color.RED;
        Color G = Color.GREEN;

        //colorless
        colorOrderLookup[COLORLESS] = new Color[] { C };

        //mono-color
        colorOrderLookup[WHITE] = new Color[] { W };
        colorOrderLookup[BLUE] = new Color[] { U };
        colorOrderLookup[BLACK] = new Color[] { B };
        colorOrderLookup[RED] = new Color[] { R };
        colorOrderLookup[GREEN] = new Color[] { G };

        //two-color
        colorOrderLookup[WHITE | BLUE] = new Color[] { W, U };
        colorOrderLookup[WHITE | BLACK] = new Color[] { W, B };
        colorOrderLookup[BLUE | BLACK] = new Color[] { U, B };
        colorOrderLookup[BLUE | RED] = new Color[] { U, R };
        colorOrderLookup[BLACK | RED] = new Color[] { B, R };
        colorOrderLookup[BLACK | GREEN] = new Color[] { B, G };
        colorOrderLookup[RED | GREEN] = new Color[] { R, G };
        colorOrderLookup[RED | WHITE] = new Color[] { R, W };
        colorOrderLookup[GREEN | WHITE] = new Color[] { G, W };
        colorOrderLookup[GREEN | BLUE] = new Color[] { G, U };

        //three-color
        colorOrderLookup[WHITE | BLUE | BLACK] = new Color[] { W, U, B };
        colorOrderLookup[WHITE | BLACK | GREEN] = new Color[] { W, B, G };
        colorOrderLookup[BLUE | BLACK | RED] = new Color[] { U, B, R };
        colorOrderLookup[BLUE | RED | WHITE] = new Color[] { U, R, W };
        colorOrderLookup[BLACK | RED | GREEN] = new Color[] { B, R, G };
        colorOrderLookup[BLACK | GREEN | BLUE] = new Color[] { B, G, U };
        colorOrderLookup[RED | GREEN | WHITE] = new Color[] { R, G, W };
        colorOrderLookup[RED | WHITE | BLACK] = new Color[] { R, W, B };
        colorOrderLookup[GREEN | WHITE | BLUE] = new Color[] { G, W, U };
        colorOrderLookup[GREEN | BLUE | RED] = new Color[] { G, U, R };

        //four-color
        colorOrderLookup[WHITE | BLUE | BLACK | RED] = new Color[] { W, U, B, R };
        colorOrderLookup[BLUE | BLACK | RED | GREEN] = new Color[] { U, B, R, G };
        colorOrderLookup[BLACK | RED | GREEN | WHITE] = new Color[] { B, R, G, W };
        colorOrderLookup[RED | GREEN | WHITE | BLUE] = new Color[] { R, G, W, U };
        colorOrderLookup[GREEN | WHITE | BLUE | BLACK] = new Color[] { G, W, U, B };

        //five-color
        colorOrderLookup[WHITE | BLUE | BLACK | RED | GREEN] = new Color[] { W, U, B, R, G };
    }

    private final byte myColor;
    private final float orderWeight;
    private final Set<Color> enumSet;
    private final String desc;

    private static final ColorSet[] cache = new ColorSet[32];

    public static final ColorSet ALL_COLORS = fromMask(MagicColor.ALL_COLORS);
    public static final ColorSet NO_COLORS = fromMask(MagicColor.COLORLESS);

    private ColorSet(final byte mask) {
        this.myColor = mask;
        this.orderWeight = this.getOrderWeight();
        if (this.isColorless()) {
            enumSet = EnumSet.of(Color.COLORLESS);
        } else {
            List<Color> list = new ArrayList<>();
            for (Color c : Color.values()) {
                if (hasAnyColor(c.getColorMask())) {
                    list.add(c);
                }
            }
            enumSet = EnumSet.copyOf(list);
        }
        final Color[] orderedShards = getOrderedColors();
        desc = Arrays.stream(orderedShards).map(Color::getShortName).collect(Collectors.joining());
    }

    public static ColorSet fromMask(final int mask) {
        final int mask32 = mask & MagicColor.ALL_COLORS;
        if (cache[mask32] == null) {
            cache[mask32] = new ColorSet((byte) mask32);
        }
        return cache[mask32];
    }

    public static ColorSet fromEnums(final MagicColor.Color... colors) {
        byte mask = 0;
        for (MagicColor.Color e : colors) {
            mask |= e.getColorMask();
        }
        return fromMask(mask);
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
        return desc;
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
        return EnumSet.copyOf(enumSet);
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
    public Color[] getOrderedColors() {
        return colorOrderLookup[myColor];
    }
}
