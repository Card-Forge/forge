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
 * Mana_PartPhyrexian class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ManaPartPhyrexian extends ManaPart {
    private final ManaPartColor wrappedColor;
    private final String color;

    /**
     * <p>
     * Constructor for Mana_PartPhyrexian.
     * </p>
     * 
     * @param manaCostToPay
     *            a {@link java.lang.String} object.
     */
    public ManaPartPhyrexian(final String manaCostToPay) {
        this.wrappedColor = new ManaPartColor(manaCostToPay.substring(1));
        this.color = manaCostToPay.substring(1);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isEasierToPay(final ManaPart part) {
        return true;
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
        return this.wrappedColor.toString().equals("") ? "" : "P" + this.wrappedColor.toString();
    }

    /**
     * <p>
     * isPaid.
     * </p>
     * 
     * @return a boolean.
     */
    @Override
    public final boolean isPaid() {
        return this.wrappedColor.isPaid();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * isColor.
     * </p>
     * 
     * @param mana
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    @Override
    public final boolean isColor(final String mana) {
        return this.wrappedColor.isColor(mana);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isColor(final Mana mana) {
        return this.wrappedColor.isColor(mana);
    }

    /**
     * <p>
     * isNeeded.
     * </p>
     * 
     * @param mana
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    @Override
    public final boolean isNeeded(final String mana) {
        return this.wrappedColor.isNeeded(mana);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isNeeded(final Mana mana) {
        return this.wrappedColor.isNeeded(mana);
    }

    /** {@inheritDoc} */
    @Override
    public final void reduce(final String mana) {
        this.wrappedColor.reduce(mana);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * reduce.
     * </p>
     * 
     * @param mana
     *            a {@link forge.card.mana.Mana} object.
     */
    @Override
    public final void reduce(final Mana mana) {
        this.wrappedColor.reduce(mana);
    }

    /**
     * <p>
     * getConvertedManaCost.
     * </p>
     * 
     * @return a int.
     */
    @Override
    public final int getConvertedManaCost() {
        return this.wrappedColor.getConvertedManaCost();
    }

    /**
     * <p>
     * payLife.
     * </p>
     */
    public final void payLife() {
        this.wrappedColor.reduce(this.color);
    }
}
