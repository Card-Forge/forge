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
import forge.Constant;
import forge.Constant.Zone;
import forge.Player;
import forge.card.spellability.SpellAbility;

/**
 * The Class StaticAbilityCantTarget.
 */
public class StaticAbilityCantTarget {

    /**
     * Apply can't target ability.
     * 
     * @param staticAbility
     *            the static ability
     * @param card
     *            the card
     * @param spellAbility
     *            the spell/ability
     * @return true, if successful
     */
    public static boolean applyCantTargetAbility(final StaticAbility staticAbility, final Card card,
            final SpellAbility spellAbility) {
        final HashMap<String, String> params = staticAbility.getMapParams();
        final Card hostCard = staticAbility.getHostCard();
        final Card source = spellAbility.getSourceCard();
        final Player activator = spellAbility.getActivatingPlayer();

        if (params.containsKey("AffectedZone")) {
            if (!card.isInZone(Zone.smartValueOf(params.get("AffectedZone")))) {
                return false;
            }
        } else { // default zone is battlefield
            if (!card.isInZone(Constant.Zone.Battlefield)) {
                return false;
            }
        }

        if (params.containsKey("Spell") && !spellAbility.isSpell()) {
            return false;
        }

        if (params.containsKey("ValidCard")
                && !card.isValid(params.get("ValidCard").split(","), hostCard.getController(), hostCard)) {
            return false;
        }

        if (params.containsKey("ValidSource")
                && !source.isValid(params.get("ValidSource").split(","), hostCard.getController(), hostCard)) {
            return false;
        }

        if (params.containsKey("Activator") && (activator != null)
                && !activator.isValid(params.get("Activator"), hostCard.getController(), hostCard)) {
            return false;
        }

        return true;
    }

}
