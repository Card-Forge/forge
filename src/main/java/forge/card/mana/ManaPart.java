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
 * Abstract Mana_Part class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class ManaPart {
    /** {@inheritDoc} */
    @Override
    public abstract String toString();

    /**
     * <p>
     * reduce.
     * </p>
     * 
     * @param mana
     *            a {@link java.lang.String} object.
     */
    public abstract void reduce(String mana);

    /**
     * <p>
     * reduce.
     * </p>
     * 
     * @param mana
     *            a {@link forge.card.mana.Mana} object.
     */
    public abstract void reduce(Mana mana);

    /**
     * <p>
     * isPaid.
     * </p>
     * 
     * @return a boolean.
     */
    public abstract boolean isPaid();

    /**
     * <p>
     * isNeeded.
     * </p>
     * 
     * @param mana
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public abstract boolean isNeeded(String mana);

    /**
     * <p>
     * isNeeded.
     * </p>
     * 
     * @param mana
     *            a {@link forge.card.mana.Mana} object.
     * @return a boolean.
     */
    public abstract boolean isNeeded(Mana mana);

    /**
     * <p>
     * isColor.
     * </p>
     * 
     * @param mana
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public abstract boolean isColor(String mana);

    /**
     * <p>
     * isColor.
     * </p>
     * 
     * @param mana
     *            a {@link forge.card.mana.Mana} object.
     * @return a boolean.
     */
    public abstract boolean isColor(Mana mana);

    /**
     * <p>
     * isEasierToPay.
     * </p>
     * 
     * @param mp
     *            a {@link forge.card.mana.ManaPart} object.
     * @return a boolean.
     */
    public abstract boolean isEasierToPay(ManaPart mp);

    /**
     * <p>
     * getConvertedManaCost.
     * </p>
     * 
     * @return a int.
     */
    public abstract int getConvertedManaCost();

    /**
     * <p>
     * checkSingleMana.
     * </p>
     * 
     * @param m
     *            a {@link java.lang.String} object.
     */
    public static void checkSingleMana(final String m) {
        if (m.length() != 1) {
            throw new RuntimeException("Mana_Part : checkMana() error, argument mana is not of length 1, mana - " + m);
        }

        if (!(m.equals("G") || m.equals("U") || m.equals("W") || m.equals("B") || m.equals("R") || m.equals("1")
                || m.equals("S") || m.startsWith("P"))) {
            throw new RuntimeException("Mana_Part : checkMana() error, argument mana is invalid mana, mana - " + m);
        }
    }
}
