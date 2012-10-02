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

import forge.card.CardColor;

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

    /**
     * Convert this to CardColor.
     * 
     * @return equivalent CardColor
     */
    public CardColor getCardColors() {
        return CardColor.fromNames(color1, color2);
    }

    /**
     * toString.
     * 
     * @return description.
     */
    @Override
    public String toString() {
        return color1 + '/' + color2;
    }

}
