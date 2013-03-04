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
 * CardInSet class.
 * </p>
 * 
 * @author Forge
 * @version $Id: CardInSet.java 9708 2011-08-09 19:34:12Z jendave $
 */

public class CardInSet {
    private final CardRarity rarity;
    private final int numCopies;

    /**
     * Instantiates a new card in set.
     * 
     * @param rarity
     *            the rarity
     * @param cntCopies
     *            the cnt copies
     */
    public CardInSet(final CardRarity rarity, final int cntCopies ) {
        this.rarity = rarity;
        this.numCopies = cntCopies;
    }

    /**
     * Gets the copies count.
     * 
     * @return the copies count
     */
    public final int getCopiesCount() {
        return this.numCopies;
    }

    /**
     * Gets the rarity.
     * 
     * @return the rarity
     */
    public final CardRarity getRarity() {
        return this.rarity;
    }

}
