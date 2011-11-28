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

/**
 * <p>
 * Mana_PartColor class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ManaPartColor extends ManaPart {
    private String manaCost;

    // String manaCostToPay is either "G" or "GW" NOT "3 G"
    // ManaPartColor only needs 1 mana in order to be paid
    // GW means it will accept either G or W like Selesnya Guildmage
    /**
     * <p>
     * Constructor for Mana_PartColor.
     * </p>
     * 
     * @param manaCostToPay
     *            a {@link java.lang.String} object.
     */
    public ManaPartColor(final String manaCostToPay) {
        final char[] c = manaCostToPay.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if ((i != 0) || (c[i] != ' ')) {
                ManaPart.checkSingleMana("" + c[i]);
            }
        }

        this.manaCost = manaCostToPay;
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        return this.manaCost;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isNeeded(final String mana) {
        // ManaPart method
        ManaPart.checkSingleMana(mana);

        return !this.isPaid() && this.isColor(mana);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isNeeded(final Mana mana) {
        return (!this.isPaid() && this.isColor(mana));
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isColor(final String mana) {
        // ManaPart method
        ManaPart.checkSingleMana(mana);

        return this.manaCost.indexOf(mana) != -1;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isColor(final Mana mana) {
        final String color = InputPayManaCostUtil.getShortColorString(mana.getColor());

        return this.manaCost.indexOf(color) != -1;
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
        // if mana is needed, then this mana cost is all paid up
        if (!this.isNeeded(mana)) {
            throw new RuntimeException("Mana_PartColor : reduce() error, argument mana not needed, mana - " + mana
                    + ", toString() - " + this.toString());
        }

        this.manaCost = "";
    }

    /** {@inheritDoc} */
    @Override
    public final void reduce(final Mana mana) {
        // if mana is needed, then this mana cost is all paid up
        if (!this.isNeeded(mana)) {
            throw new RuntimeException("Mana_PartColor : reduce() error, argument mana not needed, mana - " + mana
                    + ", toString() - " + this.toString());
        }

        this.manaCost = "";
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isPaid() {
        return this.manaCost.length() == 0;
    }

    /** {@inheritDoc} */
    @Override
    public final int getConvertedManaCost() {
        return 1;
    }
}
