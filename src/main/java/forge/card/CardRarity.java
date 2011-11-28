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
package forge.card;

/**
 * <p>
 * CardRarity class.
 * </p>
 * 
 * @author Forge
 * @version $Id: CardRarity.java 9708 2011-08-09 19:34:12Z jendave $
 */

public enum CardRarity {

    /** The Basic land. */
    BasicLand("L"),

    /** The Common. */
    Common("C"),

    /** The Uncommon. */
    Uncommon("U"),

    /** The Rare. */
    Rare("R"),

    /** The Mythic rare. */
    MythicRare("M"),

    /** The Special. */
    Special("S"), // Timeshifted
    /** The Unknown. */
    Unknown("?"); // In development

    private final String strValue;

    private CardRarity(final String sValue) {
        this.strValue = sValue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return this.strValue;
    }

}
