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
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;

/**
 * <p>
 * CardFactoryEquipment class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
class CardFactoryEquipment {

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
    public static void buildCard(final Card card, final String cardName) {
        // TODO: Move Living Weapon to main Keyword generator, and delete this file
        if (card.hasKeyword("Living Weapon")) {
            card.removeIntrinsicKeyword("Living Weapon");

            final StringBuilder sbTrig = new StringBuilder();
            sbTrig.append("Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ");
            sbTrig.append("ValidCard$ Card.Self | Execute$ TrigGerm | TriggerDescription$ ");
            sbTrig.append("Living Weapon (When this Equipment enters the battlefield, ");
            sbTrig.append("put a 0/0 black Germ creature token onto the battlefield, then attach this to it.)");

            final StringBuilder sbGerm = new StringBuilder();
            sbGerm.append("DB$ Token | TokenAmount$ 1 | TokenName$ Germ | TokenTypes$ Creature,Germ | RememberTokens$ True | ");
            sbGerm.append("TokenOwner$ You | TokenColors$ Black | TokenPower$ 0 | TokenToughness$ 0 | TokenImage$ B 0 0 Germ | SubAbility$ DBGermAttach");

            final StringBuilder sbAttach = new StringBuilder();
            sbAttach.append("DB$ Attach | Defined$ Remembered | SubAbility$ DBGermClear");

            final StringBuilder sbClear = new StringBuilder();
            sbClear.append("DB$ Cleanup | ClearRemembered$ True");

            card.setSVar("TrigGerm", sbGerm.toString());
            card.setSVar("DBGermAttach", sbAttach.toString());
            card.setSVar("DBGermClear", sbClear.toString());

            final Trigger etbTrigger = TriggerHandler.parseTrigger(sbTrig.toString(), card, true);
            card.addTrigger(etbTrigger);
        }
    }
}
