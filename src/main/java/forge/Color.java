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
package forge;

import java.util.EnumSet;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaCostBeingPaid;

/**
 * <p>
 * Color class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public enum Color {

    /** The Colorless. */
    Colorless(0),
    /** The White. */
    White(1),
    /** The Green. */
    Green(2),
    /** The Red. */
    Red(4),
    /** The Black. */
    Black(8),
    /** The Blue. */
    Blue(16);
    
    public static final Color[] WUBRG = new Color[] { White, Blue, Black, Red, Green };  

    @SuppressWarnings("unused")
    private int flag = 0;

    /**
     * <p>
     * Constructor for Color.
     * </p>
     * 
     * @param c
     *            a int.
     */
    Color(final int c) {
        this.flag = c;
    }

    /**
     * <p>
     * Colorless.
     * </p>
     * 
     * @return a {@link java.util.EnumSet} object.
     */
    public static EnumSet<Color> colorless() {
        final EnumSet<Color> colors = EnumSet.of(Color.Colorless);
        return colors;
    }

    /**
     * <p>
     * ConvertStringsToColor.
     * </p>
     * 
     * @param s
     *            an array of {@link java.lang.String} objects.
     * @return a {@link java.util.EnumSet} object.
     */
    public static EnumSet<Color> convertStringsToColor(final String[] s) {
        final EnumSet<Color> colors = EnumSet.of(Color.Colorless);

        for (final String element : s) {
            colors.add(Color.convertFromString(element));
        }

        if (colors.size() > 1) {
            colors.remove(Color.Colorless);
        }

        return colors;
    }

    /**
     * <p>
     * ConvertFromString.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     * @return a {@link forge.Color} object.
     */
    public static Color convertFromString(final String s) {

        if (s.equals(Constant.Color.WHITE)) {
            return Color.White;
        } else if (s.equals(Constant.Color.GREEN)) {
            return Color.Green;
        } else if (s.equals(Constant.Color.RED)) {
            return Color.Red;
        } else if (s.equals(Constant.Color.BLACK)) {
            return Color.Black;
        } else if (s.equals(Constant.Color.BLUE)) {
            return Color.Blue;
        }

        return Color.Colorless;
    }

    public static EnumSet<Color> fromColorSet(ColorSet cc) {
        final EnumSet<Color> colors = EnumSet.of(Color.Colorless);
        for( int i = 0; i < MagicColor.NUMBER_OR_COLORS; i++ ) {
            if( cc.hasAnyColor(MagicColor.WUBRG[i]) )
                colors.add(Color.WUBRG[i]);
        }
        if (colors.size() > 1) {
            colors.remove(Color.Colorless);
        }
        return colors;
    }
    
    /**
     * <p>
     * ConvertManaCostToColor.
     * </p>
     * 
     * @param m
     *            a {@link forge.card.mana.ManaCostBeingPaid} object.
     * @return a {@link java.util.EnumSet} object.
     */
    public static EnumSet<Color> convertManaCostToColor(final ManaCostBeingPaid m) {
        final EnumSet<Color> colors = EnumSet.of(Color.Colorless);

        if (m.isColor("W")) {
            colors.add(Color.White);
        }
        if (m.isColor("G")) {
            colors.add(Color.Green);
        }
        if (m.isColor("R")) {
            colors.add(Color.Red);
        }
        if (m.isColor("B")) {
            colors.add(Color.Black);
        }
        if (m.isColor("U")) {
            colors.add(Color.Blue);
        }

        if (colors.size() > 1) {
            colors.remove(Color.Colorless);
        }

        return colors;
    }

    /**
     * <p>
     * toString.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String toString() {
        if (this.equals(Color.White)) {
            return Constant.Color.WHITE;
        } else if (this.equals(Color.Green)) {
            return Constant.Color.GREEN;
        } else if (this.equals(Color.Red)) {
            return Constant.Color.RED;
        } else if (this.equals(Color.Black)) {
            return Constant.Color.BLACK;
        } else if (this.equals(Color.Blue)) {
            return Constant.Color.BLUE;
        } else {
            return Constant.Color.COLORLESS;
        }
    }
}
