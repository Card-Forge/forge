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
 * HandSizeOp class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class HandSizeOp {

    /** The Mode. */
    private final String mode;

    /** The hs time stamp. */
    private final int hsTimeStamp;

    /** The Amount. */
    private final int amount;

    /**
     * <p>
     * Constructor for HandSizeOp.
     * </p>
     * 
     * @param m
     *            a {@link java.lang.String} object.
     * @param a
     *            a int.
     * @param ts
     *            a int.
     */
    public HandSizeOp(final String m, final int a, final int ts) {
        mode = m;
        amount = a;
        hsTimeStamp = ts;
    }

    /**
     * <p>
     * toString.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final String toString() {
        return "Mode(" + this.getMode() + ") Amount(" + this.getAmount() + ") Timestamp(" + this.getHsTimeStamp() + ")";
    }

    /**
     * Gets the amount.
     * 
     * @return the amount
     */
    public int getAmount() {
        return this.amount;
    }

    /**
     * Gets the mode.
     * 
     * @return the mode
     */
    public String getMode() {
        return this.mode;
    }

    /**
     * Gets the hs time stamp.
     * 
     * @return the hsTimeStamp
     */
    public int getHsTimeStamp() {
        return this.hsTimeStamp;
    }
}
