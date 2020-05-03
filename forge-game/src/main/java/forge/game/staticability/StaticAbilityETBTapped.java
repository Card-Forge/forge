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
package forge.game.staticability;

import forge.game.CardTraitBase;
import forge.game.card.Card;


/**
 * The Class StaticAbility_CantBeCast.
 */
public class StaticAbilityETBTapped {

    /**
     * TODO Write javadoc for this method.
     * 
     * @param stAb
     *            a StaticAbility
     * @param card
     *            the card
     * @return true, if successful
     */
    public static boolean applyETBTappedAbility(final StaticAbility stAb, final Card card) {
        final Card hostCard = stAb.getHostCard();

        if (stAb.hasParam("ValidCard")) {
            if (!CardTraitBase.matchesValid(card, stAb.getParam("ValidCard").split(","), hostCard)) {
                return false;
            }
        }

        return true;
    }

}
