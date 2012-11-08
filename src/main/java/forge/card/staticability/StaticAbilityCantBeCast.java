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
import java.util.List;

import forge.Card;
import forge.Singletons;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * The Class StaticAbility_CantBeCast.
 */
public class StaticAbilityCantBeCast {

    /**
     * TODO Write javadoc for this method.
     * 
     * @param stAb
     *            a StaticAbility
     * @param card
     *            the card
     * @param activator
     *            the activator
     * @return true, if successful
     */
    public static boolean applyCantBeCastAbility(final StaticAbility stAb, final Card card, final Player activator) {
        final HashMap<String, String> params = stAb.getMapParams();
        final Card hostCard = stAb.getHostCard();

        if (params.containsKey("ValidCard")
                && !card.isValid(params.get("ValidCard").split(","), hostCard.getController(), hostCard)) {
            return false;
        }

        if (params.containsKey("Caster") && (activator != null)
                && !activator.isValid(params.get("Caster"), hostCard.getController(), hostCard)) {
            return false;
        }

        if (params.containsKey("OnlySorcerySpeed") && (activator != null) && activator.canCastSorcery()) {
            return false;
        }

        if (params.containsKey("Origin")) {
            List<ZoneType> src = ZoneType.listValueOf(params.get("Origin"));
            if (!src.contains(Singletons.getModel().getGame().getZoneOf(card).getZoneType())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Applies Cant Be Activated ability.
     * 
     * @param staticAbility
     *            a StaticAbility
     * @param card
     *            the card
     * @param spellAbility
     *            a SpellAbility
     * @return true, if successful
     */
    public static boolean applyCantBeActivatedAbility(final StaticAbility staticAbility, final Card card,
            final SpellAbility spellAbility) {
        final HashMap<String, String> params = staticAbility.getMapParams();
        final Card hostCard = staticAbility.getHostCard();
        final Player activator = spellAbility.getActivatingPlayer();

        if (params.containsKey("ValidCard")
                && !card.isValid(params.get("ValidCard").split(","), hostCard.getController(), hostCard)) {
            return false;
        }

        if (params.containsKey("Activator") && (activator != null)
                && !activator.isValid(params.get("Activator"), hostCard.getController(), hostCard)) {
            return false;
        }

        if (params.containsKey("NonMana") && (spellAbility.isManaAbility())) {
            return false;
        }

        if (params.containsKey("TapAbility") && !(spellAbility.getPayCosts().getTap())) {
            return false;
        }

        return true;
    }

    /**
     * TODO Write javadoc for this method.
     * 
     * @param stAb
     *            a StaticAbility
     * @param card
     *            the card
     * @param player
     *            the player
     * @return true, if successful
     */
    public static boolean applyCantPlayLandAbility(final StaticAbility stAb, final Card card, final Player player) {
        final HashMap<String, String> params = stAb.getMapParams();
        final Card hostCard = stAb.getHostCard();

        if (params.containsKey("ValidCard") && (card != null)
                && !card.isValid(params.get("ValidCard").split(","), hostCard.getController(), hostCard)) {
            return false;
        }

        if (params.containsKey("Player") && (player != null)
                && !player.isValid(params.get("Player"), hostCard.getController(), hostCard)) {
            return false;
        }

        return true;
    }

}
