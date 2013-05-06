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
import forge.game.GameState;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * The Class StaticAbility_CantBeCast.
 */
public class StaticAbilityMayLookAt {

    /**
     * TODO Write javadoc for this method.
     * 
     * @param stAb
     *            a StaticAbility
     * @param card
     *            the card
     * @param activator
     *            the player
     * @return true, if successful
     */
    public static boolean applyMayLookAtAbility(final StaticAbility stAb, final Card card, final Player player) {
        final HashMap<String, String> params = stAb.getMapParams();
        final Card hostCard = stAb.getHostCard();

        if (params.containsKey("Affected")
                && !card.isValid(params.get("Affected").split(","), hostCard.getController(), hostCard)) {
            return false;
        }

        if (params.containsKey("Player") && player != null
                && !player.isValid(params.get("Player"), hostCard.getController(), hostCard)) {
            return false;
        }
        
        if (params.containsKey("AffectedZone")) {
            ZoneType zone = card.getGame().getZoneOf(card).getZoneType();
            if (!ZoneType.listValueOf(params.get("AffectedZone")).contains(zone)) {
                return false;
            }
        }

        return true;
    }
}
