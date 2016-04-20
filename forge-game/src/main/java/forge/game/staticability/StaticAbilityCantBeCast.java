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

import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;
import java.util.Map;

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
        final Map<String, String> params = stAb.getMapParams();
        final Card hostCard = stAb.getHostCard();

        if (params.containsKey("ValidCard")
                && !card.isValid(params.get("ValidCard").split(","), hostCard.getController(), hostCard, null)) {
            return false;
        }

        if (params.containsKey("Caster") && (activator != null)
                && !activator.isValid(params.get("Caster"), hostCard.getController(), hostCard, null)) {
            return false;
        }

        if (params.containsKey("OnlySorcerySpeed") && (activator != null) && activator.canCastSorcery()) {
            return false;
        }

        if (params.containsKey("Origin")) {
            List<ZoneType> src = ZoneType.listValueOf(params.get("Origin"));
            if (!src.contains(activator.getGame().getZoneOf(card).getZoneType())) {
                return false;
            }
        }

        if (params.containsKey("NumLimitEachTurn") && activator != null) {
            int limit = Integer.parseInt(params.get("NumLimitEachTurn"));
            String valid = params.containsKey("ValidCard") ? params.get("ValidCard") : "Card";
            List<Card> thisTurnCast = CardLists.getValidCards(card.getGame().getStack().getSpellsCastThisTurn(),
                    valid, card.getController(), card);
            if (CardLists.filterControlledBy(thisTurnCast, activator).size() < limit) {
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
        final Map<String, String> params = staticAbility.getMapParams();
        final Card hostCard = staticAbility.getHostCard();
        final Player activator = spellAbility.getActivatingPlayer();

        if (params.containsKey("ValidCard")
                && !card.isValid(params.get("ValidCard").split(","), hostCard.getController(), hostCard, null)) {
            return false;
        }

        if (params.containsKey("AffectedZone") && !card.isInZone(ZoneType.smartValueOf(params.get("AffectedZone")))) {
            return false;
        }

        if (params.containsKey("Activator") && (activator != null)
                && !activator.isValid(params.get("Activator"), hostCard.getController(), hostCard, spellAbility)) {
            return false;
        }

        if (params.containsKey("NonMana") && (spellAbility.isManaAbility())) {
            return false;
        }

        if (params.containsKey("TapAbility") && !(spellAbility.getPayCosts().hasTapCost())) {
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
        final Map<String, String> params = stAb.getMapParams();
        final Card hostCard = stAb.getHostCard();

        if (params.containsKey("ValidCard")
                && (card == null || !card.isValid(params.get("ValidCard").split(","), hostCard.getController(), hostCard, null))) {
            return false;
        }

        if (params.containsKey("Player") && (player != null)
                && !player.isValid(params.get("Player"), hostCard.getController(), hostCard, null)) {
            return false;
        }

        return true;
    }

}
