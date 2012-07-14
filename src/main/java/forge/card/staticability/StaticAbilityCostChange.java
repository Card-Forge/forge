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
package forge.card.staticability;

import java.util.HashMap;

import forge.Card;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.mana.ManaCost;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

/**
 * The Class StaticAbility_CantBeCast.
 */
public class StaticAbilityCostChange {

    /**
     * Applies applyRaiseCostAbility ability.
     * 
     * @param staticAbility
     *            a StaticAbility
     * @param sa
     *            the SpellAbility
     * @param originalCost
     *            a ManaCost
     */
    public static ManaCost applyRaiseCostAbility(final StaticAbility staticAbility, final SpellAbility sa
            , final ManaCost originalCost) {
        final HashMap<String, String> params = staticAbility.getMapParams();
        final Card hostCard = staticAbility.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Card card = sa.getSourceCard();

        if (params.containsKey("ValidCard")
                && !card.isValid(params.get("ValidCard").split(","), hostCard.getController(), hostCard)) {
            return originalCost;
        }

        if (params.containsKey("Activator") && ((activator == null)
                || !activator.isValid(params.get("Activator"), hostCard.getController(), hostCard))) {
            return originalCost;
        }

        if (params.containsKey("Type") && params.get("Type").equals("Spell") && !sa.isSpell()) {
            return originalCost;
        }
        if (params.containsKey("Type") && params.get("Type").equals("Ability") && !sa.isAbility()) {
            return originalCost;
        }

        //modify the cost here
        return originalCost;
    }

    /**
     * Applies applyReduceCostAbility ability.
     * 
     * @param staticAbility
     *            a StaticAbility
     * @param sa
     *            the SpellAbility
     * @param originalCost
     *            a ManaCost
     */
    public static ManaCost applyReduceCostAbility(final StaticAbility staticAbility, final SpellAbility sa
            , final ManaCost originalCost) {
        final HashMap<String, String> params = staticAbility.getMapParams();
        final Card hostCard = staticAbility.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Card card = sa.getSourceCard();
        final String amount = params.get("Amount");
        final ManaCost manaCost = new ManaCost(originalCost.toString());

        if (params.containsKey("ValidCard")
                && !card.isValid(params.get("ValidCard").split(","), hostCard.getController(), hostCard)) {
            return originalCost;
        }
        if (params.containsKey("Activator") && ((activator == null)
                || !activator.isValid(params.get("Activator"), hostCard.getController(), hostCard))) {
            return originalCost;
        }
        if (params.containsKey("Type") && params.get("Type").equals("Spell") && !sa.isSpell()) {
            return originalCost;
        }
        if (params.containsKey("Type") && params.get("Type").equals("Ability") && !sa.isAbility()) {
            return originalCost;
        }

        if (!"WUGRB".contains(amount)) {
            int value = 0;
            if ("X".equals(amount)) {
                value = CardFactoryUtil.xCount(card, card.getSVar("X"));
            } else {
                value = Integer.valueOf(amount);
            }

            manaCost.decreaseColorlessMana(value);
        }

        return manaCost;
    }
}
