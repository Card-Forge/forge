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

import forge.Constant;

/**
 * Created by IntelliJ IDEA. User: dhudson Date: 6/24/11 Time: 8:42 PM To change
 * this template use File | Settings | File Templates.
 */
class DeckColors {

    /** The Color1. */
    private String color1 = "none";

    /** The Color2. */
    private String color2 = "none";
    // public String Splash = "none";
    /** The Mana1. */
    private String mana1 = "";

    /** The Mana2. */
    private String mana2 = "";

    // public String ManaS = "";

    /**
     * <p>
     * Constructor for deckColors.
     * </p>
     * 
     * @param c1
     *            a {@link java.lang.String} object.
     * @param c2
     *            a {@link java.lang.String} object.
     * @param sp
     *            a {@link java.lang.String} object.
     */
    public DeckColors(final String c1, final String c2, final String sp) {
        this.setColor1(c1);
        this.setColor2(c2);
        // Splash = sp;
    }

    /**
     * <p>
     * Constructor for DeckColors.
     * </p>
     */
    public DeckColors() {

    }

    /**
     * <p>
     * ColorToMana.
     * </p>
     * 
     * @param color
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String colorToMana(final String color) {
        final String[] mana = { "W", "U", "B", "R", "G" };

        for (int i = 0; i < Constant.Color.ONLY_COLORS.length; i++) {
            if (Constant.Color.ONLY_COLORS[i].equals(color)) {
                return mana[i];
            }
        }

        return "";
    }

    /**
     * Gets the color1.
     * 
     * @return the color1
     */
    public String getColor1() {
        return this.color1;
    }

    /**
     * Sets the color1.
     * 
     * @param color1in
     *            the color1 to set
     */
    public void setColor1(final String color1in) {
        this.color1 = color1in;
    }

    /**
     * Gets the mana1.
     * 
     * @return the mana1
     */
    public String getMana1() {
        return this.mana1;
    }

    /**
     * Sets the mana1.
     * 
     * @param mana1in
     *            the mana1 to set
     */
    public void setMana1(final String mana1in) {
        this.mana1 = mana1in;
    }

    /**
     * Gets the mana2.
     * 
     * @return the mana2
     */
    public String getMana2() {
        return this.mana2;
    }

    /**
     * Sets the mana2.
     * 
     * @param mana2in
     *            the mana2 to set
     */
    public void setMana2(final String mana2in) {
        this.mana2 = mana2in;
    }

    /**
     * Gets the color2.
     * 
     * @return the color2
     */
    public String getColor2() {
        return this.color2;
    }

    /**
     * Sets the color2.
     * 
     * @param color2in
     *            the color2 to set
     */
    public void setColor2(final String color2in) {
        this.color2 = color2in;
    }

}
