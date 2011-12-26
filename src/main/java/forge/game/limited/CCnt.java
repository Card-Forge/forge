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
package forge.game.limited;

/**
 * <p>
 * CCnt class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
class CCnt {

    /** The Color. */
    private String color;

    /** The Count. */
    private int count;

    /**
     * <p>
     * Constructor for CCnt.
     * </p>
     * 
     * @param clr
     *            a {@link java.lang.String} object.
     * @param cnt
     *            a int.
     */
    /**
     * <p>
     * deckColors class.
     * </p>
     * 
     * @param clr
     *            a {@link java.lang.String} object.
     * @param cnt
     *            a int.
     */
    public CCnt(final String clr, final int cnt) {
        this.setColor(clr);
        this.setCount(cnt);
    }

    /**
     * Gets the color.
     * 
     * @return the color
     */
    public String getColor() {
        return this.color;
    }

    /**
     * Sets the color.
     * 
     * @param color0
     *            the color to set
     */
    public void setColor(final String color0) {
        this.color = color0;
    }

    /**
     * Gets the count.
     * 
     * @return the count
     */
    public int getCount() {
        return this.count;
    }

    /**
     * Sets the count.
     * 
     * @param count0
     *            the count to set
     */
    public void setCount(final int count0) {
        this.count = count0;
    }
}
