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

import java.util.ArrayList;

import forge.Card;
import forge.CardList;
import forge.Counters;
import forge.card.cost.Cost;
import forge.card.spellability.Ability;
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
     * shouldEquip.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int shouldEquip(final Card c) {
        final ArrayList<String> a = c.getKeyword();
        for (int i = 0; i < a.size(); i++) {

            // Keyword renamed to eqPump, was VanillaEquipment
            if (a.get(i).toString().startsWith("eqPump")) {
                return i;
            }
        }
        return -1;
    }

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

        // *************** START *********** START **************************
        if (cardName.equals("Blade of the Bloodchief")) {
            final Ability triggeredAbility = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if (card.getEquipping().size() != 0) {
                        final Card equipping = card.getEquipping().get(0);
                        if (equipping.isType("Vampire")) {
                            equipping.addCounter(Counters.P1P1, 2);
                        } else {
                            equipping.addCounter(Counters.P1P1, 1);
                        }
                    }
                }
            };

            final StringBuilder sbTrig = new StringBuilder();
            sbTrig.append("Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard | ");
            sbTrig.append("ValidCard$ Creature | TriggerZones$ Battlefield | Execute$ TrigOverride | ");
            sbTrig.append("TriggerDescription$ Whenever a creature is put into a graveyard ");
            sbTrig.append("from the battlefield, put a +1/+1 counter on equipped creature. ");
            sbTrig.append("If equipped creature is a Vampire, put two +1/+1 counters on it instead.");
            final Trigger myTrigger = TriggerHandler.parseTrigger(sbTrig.toString(), card, true);
            myTrigger.setOverridingAbility(triggeredAbility);

            card.addTrigger(myTrigger);
        } // *************** END ************ END **************************

        if (CardFactoryEquipment.shouldEquip(card) != -1) {
            final int n = CardFactoryEquipment.shouldEquip(card);
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);

                final String[] k = parse.split(":");
                String tmpCost;
                tmpCost = k[0].substring(6);
                String keywordsUnsplit = "";
                String[] extrinsicKeywords = { "none" }; // for equips with no
                                                         // keywords to add

                // final String manaCost = tmpCost.trim();
                final Cost abCost = new Cost(card, tmpCost.trim(), true);
                int power = 0;
                int tough = 0;

                final String[] ptk = k[1].split("/");
                // keywords in first cell
                if (ptk.length == 1) {
                    keywordsUnsplit = ptk[0];
                } else {
                    // parse the power/toughness boosts in first two cells
                    for (int i = 0; i < 2; i++) {
                        if (ptk[i].matches("[\\+\\-][0-9]")) {
                            ptk[i] = ptk[i].replace("+", "");
                        }
                    }

                    power = Integer.parseInt(ptk[0].trim());
                    tough = Integer.parseInt(ptk[1].trim());

                    if (ptk.length > 2) { // keywords in third cell
                        keywordsUnsplit = ptk[2];
                    }
                }

                if (keywordsUnsplit.length() > 0) // then there is at least one
                                                  // extrinsic keyword to assign
                {
                    final String[] tempKwds = keywordsUnsplit.split("&");
                    extrinsicKeywords = new String[tempKwds.length];

                    for (int i = 0; i < tempKwds.length; i++) {
                        extrinsicKeywords[i] = tempKwds[i].trim();
                    }
                }

                card.addSpellAbility(CardFactoryUtil.eqPumpEquip(card, power, tough, extrinsicKeywords, abCost));
                card.addEquipCommand(CardFactoryUtil.eqPumpOnEquip(card, power, tough, extrinsicKeywords, abCost));
                card.addUnEquipCommand(CardFactoryUtil.eqPumpUnEquip(card, power, tough, extrinsicKeywords, abCost));

            }
        } // eqPump (was VanillaEquipment)

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

        return card;
    }
}
