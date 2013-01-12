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

import forge.card.mana.ManaCostBeingPaid;

/**
 * class ColorChanger. TODO Write javadoc for this type.
 * 
 */
public class ColorChanger {
    private final ArrayList<CardColor> globalColorChanges = new ArrayList<CardColor>();

    /**
     * <p>
     * addColorChanges.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     * @param c
     *            a {@link forge.Card} object.
     * @param addToColors
     *            a boolean.
     * @param bIncrease
     *            a boolean.
     * @return a long.
     */
    public final long addColorChanges(final String s, final Card c, final boolean addToColors, final boolean bIncrease) {
        if (bIncrease) {
            CardColor.increaseTimestamp();
        }
        this.globalColorChanges.add(new CardColor(new ManaCostBeingPaid(s), c, addToColors, false));
        return CardColor.getTimestamp();
    }

    /**
     * <p>
     * removeColorChanges.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     * @param c
     *            a {@link forge.Card} object.
     * @param addTo
     *            a boolean.
     * @param timestamp
     *            a long.
     */
    public final void removeColorChanges(final String s, final Card c, final boolean addTo, final long timestamp) {
        CardColor removeCol = null;
        for (final CardColor cc : this.globalColorChanges) {
            if (cc.equals(s, c, addTo, timestamp)) {
                removeCol = cc;
            }
        }

        if (removeCol != null) {
            this.globalColorChanges.remove(removeCol);
        }
    }

    /**
     * <p>
     * reset = clearColorChanges.
     * </p>
     */
    public final void reset() {
        this.clearColorChanges();
    }

    /**
     * <p>
     * clearColorChanges.
     * </p>
     */
    public final void clearColorChanges() {
        // clear the global color changes at end of each game
        this.globalColorChanges.clear();
    }

    /**
     * <p>
     * getColorChanges.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<CardColor> getColorChanges() {
        return this.globalColorChanges;
    }
}
