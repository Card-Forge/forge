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

import forge.card.ColorSet;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;

/**
 * <p>
 * Card_Color class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardColor  {
    private static long timeStamp = 0;
    public static long getTimestamp() { return CardColor.timeStamp; }
    static void increaseTimestamp() { CardColor.timeStamp++; }

    // takes care of individual card color, for global color change effects use
    // AllZone.getGameInfo().getColorChanges()
    private final byte colorMask;
    public final byte getColorMask() { return colorMask; }

    private final boolean additional;
    public final boolean isAdditional() {
        return this.additional;
    }

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
     * <p>
     * Constructor for Card_Color.
     * </p>
     * 
     * @param mc
     *            a {@link forge.card.mana.ManaCostBeingPaid} object.
     * @param c
     *            a {@link forge.Card} object.
     * @param addToColors
     *            a boolean.
     * @param baseColor
     *            a boolean.
     */
    CardColor(final String colors, final boolean addToColors, final boolean baseColor) {
        this.additional = addToColors;
        ManaCost mc = new ManaCost(new ManaCostParser(colors));
        this.colorMask = mc.getColorProfile();
        if (baseColor) {
            this.stamp = 0;
        } else {
            this.stamp = CardColor.timeStamp;
        }
    }

    public CardColor(byte mask) {
        this.colorMask = mask;
        this.additional = false;
        this.stamp = 0;
    }

    
    public CardColor() {
        this((byte)0);
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
        return (addToColors == this.additional) && (this.stamp == time);
    }

    public final ColorSet toColorSet() {
        return ColorSet.fromMask(colorMask);
    }
}
