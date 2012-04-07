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
package forge.card.cardfactory;

import forge.Card;
import forge.Command;
import forge.Counters;

/**
 * <p>
 * CardFactory_Planeswalkers class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardFactoryPlaneswalkers {

    /**
     * <p>
     * getCard.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param cardName
     *            a {@link java.lang.String} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getCard(final Card card, final String cardName) {
        // All Planeswalkers set their loyality in the beginning
        if (card.getBaseLoyalty() > 0) {
            Command cmd = CardFactoryUtil.entersBattleFieldWithCounters(card, Counters.LOYALTY, card.getBaseLoyalty());
            card.addComesIntoPlayCommand(cmd);
        }

        return card;
    }

} // end class CardFactoryPlaneswalkers
