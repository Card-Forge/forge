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

import java.util.ArrayList;
import java.util.EnumSet;

import forge.card.mana.ManaCost;

/**
 * <p>
 * Card_Color class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardColor {
    // takes care of individual card color, for global color change effects use
    // AllZone.getGameInfo().getColorChanges()
    private final EnumSet<Color> col;
    private final boolean additional;

    /**
     * <p>
     * Getter for the field <code>additional</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getAdditional() {
        return this.additional;
    }

    private Card effectingCard = null;
    private long stamp = 0;

    /**
     * <p>
     * Getter for the field <code>stamp</code>.
     * </p>
     * 
     * @return a long.
     */
    public final long getStamp() {
        return this.stamp;
    }

    /**
     * Constant <code>timeStamp=0</code>.
     */
    private static long timeStamp = 0;

    /**
     * <p>
     * getTimestamp.
     * </p>
     * 
     * @return a long.
     */
    public static long getTimestamp() {
        return CardColor.timeStamp;
    }

    /**
     * <p>
     * Constructor for Card_Color.
     * </p>
     * 
     * @param mc
     *            a {@link forge.card.mana.ManaCost} object.
     * @param c
     *            a {@link forge.Card} object.
     * @param addToColors
     *            a boolean.
     * @param baseColor
     *            a boolean.
     */
    CardColor(final ManaCost mc, final Card c, final boolean addToColors, final boolean baseColor) {
        this.additional = addToColors;
        this.col = Color.convertManaCostToColor(mc);
        this.effectingCard = c;
        if (baseColor) {
            this.stamp = 0;
        } else {
            this.stamp = CardColor.timeStamp;
        }
    }

    /**
     * <p>
     * Constructor for Card_Color.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public CardColor(final Card c) {
        this.col = Color.colorless();
        this.additional = false;
        this.stamp = 0;
        this.effectingCard = c;
    }

    /**
     * <p>
     * addToCardColor.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    final boolean addToCardColor(final String s) {
        final Color c = Color.convertFromString(s);
        if (!this.col.contains(c)) {
            this.col.add(c);
            return true;
        }
        return false;
    }

    /**
     * <p>
     * fixColorless.
     * </p>
     */
    final void fixColorless() {
        if ((this.col.size() > 1) && this.col.contains(Color.Colorless)) {
            this.col.remove(Color.Colorless);
        }
    }

    /**
     * <p>
     * increaseTimestamp.
     * </p>
     */
    static void increaseTimestamp() {
        CardColor.timeStamp++;
    }

    /**
     * <p>
     * equals.
     * </p>
     * 
     * @param cost
     *            a {@link java.lang.String} object.
     * @param c
     *            a {@link forge.Card} object.
     * @param addToColors
     *            a boolean.
     * @param time
     *            a long.
     * @return a boolean.
     */
    public final boolean equals(final String cost, final Card c, final boolean addToColors, final long time) {
        return (this.effectingCard == c) && (addToColors == this.additional) && (this.stamp == time);
    }

    /**
     * <p>
     * toStringArray.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<String> toStringArray() {
        final ArrayList<String> list = new ArrayList<String>();
        for (final Color c : this.col) {
            list.add(c.toString());
        }
        return list;
    }
}
