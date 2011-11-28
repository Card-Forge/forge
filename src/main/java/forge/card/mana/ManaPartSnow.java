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

/**
 * <p>
 * Mana_PartSnow class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ManaPartSnow extends ManaPart {

    private boolean isPaid = false;

    /** {@inheritDoc} */
    @Override
    public final boolean isNeeded(final String mana) {
        return !this.isPaid && mana.equals("S");
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isNeeded(final Mana mana) {
        return !this.isPaid && mana.isSnow();
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isColor(final String mana) {
        // ManaPart method
        return mana.indexOf("S") != -1;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isColor(final Mana mana) {
        return mana.isSnow();
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isPaid() {
        return this.isPaid;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isEasierToPay(final ManaPart mp) {
        if (mp instanceof ManaPartColorless) {
            return false;
        }
        return this.toString().length() >= mp.toString().length();
    }

    /** {@inheritDoc} */
    @Override
    public final void reduce(final String mana) {
        if (!mana.equals("S")) {
            throw new RuntimeException("Mana_PartSnow: reduce() error, " + mana + " is not snow mana");
        }
        this.isPaid = true;
    }

    /** {@inheritDoc} */
    @Override
    public final void reduce(final Mana mana) {
        if (!mana.isSnow()) {
            throw new RuntimeException("Mana_PartSnow: reduce() error, " + mana + " is not snow mana");
        }
        this.isPaid = true;
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        return (this.isPaid ? "" : "S");
    }

    /** {@inheritDoc} */
    @Override
    public final int getConvertedManaCost() {
        return 1;
    }

}
