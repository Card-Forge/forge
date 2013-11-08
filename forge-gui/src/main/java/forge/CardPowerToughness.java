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

/**
 * <p>
 * CardPowerToughness class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardPowerToughness {

    private final int power;
    private final int toughness;
    private long timeStamp = 0;

    /**
     * <p>
     * getTimestamp.
     * </p>
     * 
     * @return a long.
     */
    public final long getTimestamp() {
        return this.timeStamp;
    }

    /**
     * <p>
     * Constructor for Card_PT.
     * </p>
     * 
     * @param newPower
     *            a int.
     * @param newToughness
     *            a int.
     * @param stamp
     *            a long.
     */
    CardPowerToughness(final int newPower, final int newToughness, final long stamp) {
        this.power = newPower;
        this.toughness = newToughness;
        this.timeStamp = stamp;
    }

    /**
     * 
     * Get Power.
     * 
     * @return int
     */
    public final int getPower() {
        return this.power;
    }

    /**
     * 
     * Get Toughness.
     * 
     * @return int
     */
    public final int getToughness() {
        return this.toughness;
    }

    /**
     * <p>
     * equals.
     * </p>
     * 
     * @param newPower
     *            a int.
     * @param newToughness
     *            a int.
     * @param stamp
     *            a long.
     * @return a boolean.
     */
    public final boolean equals(final int newPower, final int newToughness, final long stamp) {
        return (this.timeStamp == stamp) && (this.power == newPower) && (this.toughness == newToughness);
    }
}
