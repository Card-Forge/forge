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
package forge.item;

/**
 * The Class CardPrintedCharacteristics.
 */
public class CardPrintedCharacteristics {
    private String name;
    private String cardSet;

    /**
     * Gets the name.
     * 
     * @return the name
     */
    public final String getName() {
        return this.name;
    }

    /**
     * Sets the name.
     * 
     * @param name0
     *            the name to set
     */
    public final void setName(final String name0) {
        this.name = name0; // TODO Add 0 to parameter's name.
    }

    /**
     * Gets the card set.
     * 
     * @return the cardSet
     */
    public final String getCardSet() {
        return this.cardSet;
    }

    /**
     * Sets the card set.
     * 
     * @param cardSet0
     *            the cardSet to set
     */
    public final void setCardSet(final String cardSet0) {
        this.cardSet = cardSet0; // TODO: Add 0 to parameter's name.
    }
}
