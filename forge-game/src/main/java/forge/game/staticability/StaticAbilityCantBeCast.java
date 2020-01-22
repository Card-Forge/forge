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
import forge.game.card.CardUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;

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
        final Card hostCard = stAb.getHostCard();

        if (stAb.hasParam("ValidCard")
                && !card.isValid(stAb.getParam("ValidCard").split(","), hostCard.getController(), hostCard, null)) {
            return false;
        }

        if (stAb.hasParam("Caster") && (activator != null)
                && !activator.isValid(stAb.getParam("Caster"), hostCard.getController(), hostCard, null)) {
            return false;
        }

        if (stAb.hasParam("OnlySorcerySpeed") && (activator != null) && activator.canCastSorcery()) {
            return false;
        }

        if (stAb.hasParam("Origin")) {
            List<ZoneType> src = ZoneType.listValueOf(stAb.getParam("Origin"));
            if (!src.contains(activator.getGame().getZoneOf(card).getZoneType())) {
                return false;
            }
        }

        if (stAb.hasParam("NonCasterTurn") && (activator != null)
                && activator.getGame().getPhaseHandler().isPlayerTurn(activator)) {
            return false;
        }

        if (stAb.hasParam("cmcGT") && (activator != null)
                && (card.getCMC() <= CardLists.getType(activator.getCardsIn(ZoneType.Battlefield),
                        stAb.getParam("cmcGT")).size())) {
            return false;
        }

        if (stAb.hasParam("NumLimitEachTurn") && activator != null) {
            int limit = Integer.parseInt(stAb.getParam("NumLimitEachTurn"));
            String valid = stAb.hasParam("ValidCard") ? stAb.getParam("ValidCard") : "Card";
            List<Card> thisTurnCast = CardUtil.getThisTurnCast(valid, card);
            if (CardLists.filterControlledBy(thisTurnCast, activator).size() < limit) {
                return false;
            }
        }

        return true;
    }

    /**
     * Applies Cant Be Activated ability.
     * 
     * @param stAb
     *            a StaticAbility
     * @param card
     *            the card
     * @param spellAbility
     *            a SpellAbility
     * @return true, if successful
     */
    public static boolean applyCantBeActivatedAbility(final StaticAbility stAb, final Card card,
            final SpellAbility spellAbility) {
        final Card hostCard = stAb.getHostCard();
        final Player activator = spellAbility.getActivatingPlayer();

        if (stAb.hasParam("ValidCard")
                && !card.isValid(stAb.getParam("ValidCard").split(","), hostCard.getController(), hostCard, null)) {
            return false;
        }

        if (stAb.hasParam("ValidSA")
                && !spellAbility.isValid(stAb.getParam("ValidSA").split(","), hostCard.getController(), hostCard, null)) {
            return false;
        }


        if (stAb.hasParam("AffectedZone") && !card.isInZone(ZoneType.smartValueOf(stAb.getParam("AffectedZone")))) {
            return false;
        }

        if (stAb.hasParam("Activator") && (activator != null)
                && !activator.isValid(stAb.getParam("Activator"), hostCard.getController(), hostCard, spellAbility)) {
            return false;
        }

        // TODO refactor this ones using ValidSA above
        if (stAb.hasParam("NonMana") && (spellAbility.isManaAbility())) {
            return false;
        }

        if (stAb.hasParam("NonLoyalty") && spellAbility.isPwAbility()) {
            return false;
        }

        if (stAb.hasParam("Loyalty") && !spellAbility.isPwAbility()) {
            return false;
        }

        if (stAb.hasParam("TapAbility") && !(spellAbility.getPayCosts().hasTapCost())) {
            return false;
        }

        if (stAb.hasParam("NonActivatorTurn") && (activator != null)
                && activator.getGame().getPhaseHandler().isPlayerTurn(activator)) {
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
        final Card hostCard = stAb.getHostCard();

        if (stAb.hasParam("ValidCard")
                && (card == null || !card.isValid(stAb.getParam("ValidCard").split(","), hostCard.getController(), hostCard, null))) {
            return false;
        }

        if (stAb.hasParam("Origin")) {
            List<ZoneType> src = ZoneType.listValueOf(stAb.getParam("Origin"));

            if (!src.contains(card.getZone().getZoneType())) {
                return false;
            }
        }

        if (stAb.hasParam("Player") && (player != null)
                && !player.isValid(stAb.getParam("Player"), hostCard.getController(), hostCard, null)) {
            return false;
        }

        return true;
    }

}
