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
    private String mode;

    /** The hs time stamp. */
    private int hsTimeStamp;

    /** The Amount. */
    private int amount;

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
        this.setMode(m);
        this.setAmount(a);
        this.setHsTimeStamp(ts);
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
     * Sets the amount.
     * 
     * @param amount0
     *            the amount to set
     */
    public void setAmount(final int amount0) {
        this.amount = amount0;
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
     * Sets the mode.
     * 
     * @param mode0
     *            the mode to set
     */
    public void setMode(final String mode0) {
        this.mode = mode0;
    }

    /**
     * Gets the hs time stamp.
     * 
     * @return the hsTimeStamp
     */
    public int getHsTimeStamp() {
        return this.hsTimeStamp;
    }

    /**
     * Sets the hs time stamp.
     * 
     * @param hsTimeStamp0
     *            the hsTimeStamp to set
     */
    public void setHsTimeStamp(final int hsTimeStamp0) {
        this.hsTimeStamp = hsTimeStamp0;
    }
}
