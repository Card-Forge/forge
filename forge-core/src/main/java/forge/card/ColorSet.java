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

import forge.card.MagicColor.Color;
import forge.card.mana.ManaCost;
import forge.util.BinaryUtil;

import java.io.Serializable;
import java.util.*;
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
public enum ColorSet implements Iterable<Color>, Serializable {

    C(Color.COLORLESS),
    W(Color.WHITE),
    U(Color.BLUE),
    WU(Color.WHITE, Color.BLUE),
    B(Color.BLACK),
    WB(Color.WHITE, Color.BLACK),
    UB(Color.BLUE, Color.BLACK),
    WUB(Color.WHITE, Color.BLUE, Color.BLACK),
    R(Color.RED),
    RW(Color.RED, Color.WHITE),
    UR(Color.BLUE, Color.RED),
    URW(Color.BLUE, Color.RED, Color.WHITE),
    BR(Color.BLACK, Color.RED),
    RWB(Color.RED, Color.WHITE, Color.BLACK),
    UBR(Color.BLUE, Color.BLACK, Color.RED),
    WUBR(Color.WHITE, Color.BLUE, Color.BLACK, Color.RED),
    G(Color.GREEN),
    GW(Color.GREEN, Color.WHITE),
    GU(Color.GREEN, Color.BLUE),
    GWU(Color.GREEN, Color.WHITE, Color.BLUE),
    BG(Color.BLACK, Color.GREEN),
    WBG(Color.WHITE, Color.BLACK, Color.GREEN),
    BGU(Color.BLACK, Color.GREEN, Color.BLUE),
    GWUB(Color.GREEN, Color.WHITE, Color.BLUE, Color.BLACK),
    RG(Color.RED, Color.GREEN),
    RGW(Color.RED, Color.GREEN, Color.WHITE),
    GUR(Color.GREEN, Color.BLUE, Color.RED),
    RGWU(Color.RED, Color.GREEN, Color.WHITE, Color.BLUE),
    BRG(Color.BLACK, Color.RED, Color.GREEN),
    BRGW(Color.BLACK, Color.RED, Color.GREEN, Color.WHITE),
    UBRG(Color.BLUE, Color.BLACK, Color.RED, Color.GREEN),
    WUBRG(Color.WHITE, Color.BLUE, Color.BLACK, Color.RED, Color.GREEN)
    ;

    private static final long serialVersionUID = 794691267379929080L;
    // needs to be before other static

    private final Collection<Color> orderedShards;
    private final float orderWeight;

    private ColorSet(final Color... ordered) {
        this.orderedShards = Arrays.asList(ordered);
        this.orderWeight = this.calcOrderWeight();
    }

    public static ColorSet fromMask(final int mask) {
        final int mask32 = mask & MagicColor.ALL_COLORS;
        return values()[mask32];
    }

    public static ColorSet fromEnums(final Color... colors) {
        byte mask = 0;
        for (Color e : colors) {
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

    public static ColorSet combine(final ColorSet... colors) {
        byte mask = 0;
        for (ColorSet c : colors) {
            mask |= c.getColor();
        }
        return fromMask(mask);
    }

    /**
     * Checks for any color.
     *
     * @param colormask
     *            the colormask
     * @return true, if successful
     */
    public boolean hasAnyColor(final int colormask) {
        return (this.ordinal() & colormask) != 0;
    }
    public boolean hasAnyColor(final Color c) {
        return this.orderedShards.contains(c);
    }

    /**
     * Checks for all colors.
     *
     * @param colormask
     *            the colormask
     * @return true, if successful
     */
    public boolean hasAllColors(final int colormask) {
        return (this.ordinal() & colormask) == colormask;
    }

    /** this has exactly the colors defined by operand.  */
    public boolean hasExactlyColor(final int colormask) {
        return this.ordinal() == colormask;
    }

    /** this has no other colors except defined by operand.  */
    public boolean hasNoColorsExcept(final ColorSet other) {
        return hasNoColorsExcept(other.getColor());
    }

    /** this has no other colors except defined by operand.  */
    public boolean hasNoColorsExcept(final int colormask) {
        return (this.ordinal() & ~colormask) == 0;
    }

    /** This returns the colors that colormask contains that are not in color */
    public ColorSet getMissingColors(final byte colormask) {
        return fromMask(this.ordinal() & ~colormask);
    }

    /** Operand has no other colors except defined by this. */
    public boolean containsAllColorsFrom(final int colorProfile) {
        return (~this.ordinal() & colorProfile) == 0;
    }

    /**
     * Count colors.
     *
     * @return the int
     */
    public int countColors() {
        return BinaryUtil.bitCount(this.ordinal());
    } // bit count

    // order has to be: W U B R G multi colorless - same as cards numbering
    // through a set
    /**
     * Gets the order weight.
     *
     * @return the order weight
     */
    private float calcOrderWeight() {
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
    public float getOrderWeight()
    {
        return orderWeight;
    }

    /**
     * Checks if is colorless.
     *
     * @return true, if is colorless
     */
    public boolean isColorless() {
        return this == C;
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
        return this == WUBRG;
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
        return color == this.ordinal();
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
        byte mask = (byte)this.ordinal();
        mask ^= MagicColor.ALL_COLORS;
        return fromMask(mask);
    }

    public byte getColor() {
        return (byte)ordinal();
    }

    /**
     * Shares color with.
     *
     * @param ccOther the cc other
     * @return true, if successful
     */
    public boolean sharesColorWith(final ColorSet ccOther) {
        return (this.ordinal() & ccOther.ordinal()) != 0;
    }

    public ColorSet getSharedColors(final ColorSet ccOther) {
        return fromMask(getColor() & ccOther.getColor());
    }

    public ColorSet getOffColors(final ColorSet ccOther) {
        return fromMask(~this.ordinal() & ccOther.ordinal());
    }

    public Set<Color> toEnumSet() {
        return EnumSet.copyOf(orderedShards);
    }

    //@Override
    public Iterator<Color> iterator() {
        return this.orderedShards.iterator();
    }

    public Stream<Color> stream() {
        return this.orderedShards.stream();
    }

    //Get array of mana cost shards for color set in the proper order
    public Collection<Color> getOrderedColors() {
        return orderedShards;
    }
}
