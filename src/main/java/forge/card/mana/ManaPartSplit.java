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
package forge.card.mana;

import forge.gui.input.InputPayManaCostUtil;

//handles mana costs like 2/R or 2/B
//for cards like Flame Javelin (Shadowmoor)
/**
 * <p>
 * Mana_PartSplit class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ManaPartSplit extends ManaPart {
    private ManaPart manaPart = null;
    private String originalCost = "";

    /**
     * <p>
     * Constructor for Mana_PartSplit.
     * </p>
     * 
     * @param manaCost
     *            a {@link java.lang.String} object.
     */
    public ManaPartSplit(final String manaCost) {
        // is mana cost like "2/R"
        if (manaCost.length() != 3) {
            throw new RuntimeException("Mana_PartSplit : constructor() error, bad mana cost parameter - " + manaCost);
        }

        this.originalCost = manaCost;
    }

    /**
     * <p>
     * isFirstTime.
     * </p>
     * 
     * @return a boolean.
     */
    private boolean isFirstTime() {
        return this.manaPart == null;
    }

    /**
     * <p>
     * setup.
     * </p>
     * 
     * @param manaToPay
     *            a {@link java.lang.String} object.
     */
    private void setup(final String manaToPay) {
        // get R out of "2/R"
        final String color = this.originalCost.substring(2, 3);

        // is manaToPay the one color we want or do we
        // treat it like colorless?
        // if originalCost is 2/R and is color W (treated like colorless)
        // or R? if W use Mana_PartColorless, if R use Mana_PartColor
        // does manaToPay contain color?
        if (0 <= manaToPay.indexOf(color)) {
            this.manaPart = new ManaPartColor(color);
        } else {
            // get 2 out of "2/R"
            this.manaPart = new ManaPartColorless(this.originalCost.substring(0, 1));
        }
    } // setup()

    /** {@inheritDoc} */
    @Override
    public final void reduce(final String mana) {
        if (this.isFirstTime()) {
            this.setup(mana);
        }

        this.manaPart.reduce(mana);
    }

    /** {@inheritDoc} */
    @Override
    public final void reduce(final Mana mana) {
        if (this.isFirstTime()) {
            this.setup(InputPayManaCostUtil.getShortColorString(mana.getColor()));
        }

        this.manaPart.reduce(mana);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isNeeded(final String mana) {
        if (this.isFirstTime()) {
            // always true because any mana can pay the colorless part of 2/G
            return true;
        }

        return this.manaPart.isNeeded(mana);
    } // isNeeded()

    /** {@inheritDoc} */
    @Override
    public final boolean isNeeded(final Mana mana) {
        if (this.isFirstTime()) {
            // always true because any mana can pay the colorless part of 2/G
            return true;
        }

        return this.manaPart.isNeeded(mana);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isColor(final String mana) {
        // ManaPart method
        final String mp = this.toString();
        return mp.indexOf(mana) != -1;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isColor(final Mana mana) {
        final String color = InputPayManaCostUtil.getShortColorString(mana.getColor());
        final String mp = this.toString();
        return mp.indexOf(color) != -1;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isEasierToPay(final ManaPart mp) {
        if (mp instanceof ManaPartColorless) {
            return false;
        }
        if (!this.isFirstTime()) {
            return true;
        }
        return this.toString().length() >= mp.toString().length();
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        if (this.isFirstTime()) {
            return this.originalCost;
        }

        return this.manaPart.toString();
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isPaid() {
        if (this.isFirstTime()) {
            return false;
        }

        return this.manaPart.isPaid();
    }

    /** {@inheritDoc} */
    @Override
    public final int getConvertedManaCost() {
        // grab the colorless portion of the split cost (usually 2, but possibly
        // more later)
        return Integer.parseInt(this.originalCost.substring(0, 1));
    }
}
