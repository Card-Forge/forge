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

import java.util.List;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The Class StaticAbility_CantBeCast.
 */
public class StaticAbilityCantBeCast {

    static String CantBeCast = "CantBeCast";
    static String CantBeActivated = "CantBeActivated";
    static String CantPlayLand = "CantPlayLand";

    public static boolean cantBeCastAbility(final SpellAbility spell, final Card card, final Player activator) {
        card.setCastSA(spell);

        final Game game = activator.getGame();
        final CardCollection allp = new CardCollection(game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES));
        allp.add(card);
        for (final Card ca : allp) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.getParam("Mode").equals(CantBeCast) || stAb.isSuppressed() || !stAb.checkConditions()) {
                    continue;
                }
                if (applyCantBeCastAbility(stAb, spell, card, activator)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean cantBeActivatedAbility(final SpellAbility spell, final Card card, final Player activator) {
        if (spell.isTrigger()) {
            return false;
        }
        final Game game = activator.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.getParam("Mode").equals(CantBeActivated) || stAb.isSuppressed() || !stAb.checkConditions()) {
                    continue;
                }
                if (applyCantBeActivatedAbility(stAb, spell, card, activator)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean cantPlayLandAbility(final SpellAbility spell, final Card card, final Player activator) {
        final Game game = activator.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.getParam("Mode").equals(CantPlayLand) || stAb.isSuppressed() || !stAb.checkConditions()) {
                    continue;
                }
                if (applyCantPlayLandAbility(stAb, card, activator)) {
                    return true;
                }
            }
        }
        return false;
    }

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
    public static boolean applyCantBeCastAbility(final StaticAbility stAb, final SpellAbility spell, final Card card, final Player activator) {
        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }

        if (!stAb.matchesValidParam("Caster", activator)) {
            return false;
        }

        if (stAb.hasParam("OnlySorcerySpeed") && (activator != null) && activator.canCastSorcery()) {
            return false;
        }

        if (stAb.hasParam("Origin")) {
            List<ZoneType> src = ZoneType.listValueOf(stAb.getParam("Origin"));
            if (!src.contains(card.getCastFrom())) {
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
            List<Card> thisTurnCast = CardUtil.getThisTurnCast(valid, card, stAb);
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
    public static boolean applyCantBeActivatedAbility(final StaticAbility stAb, final SpellAbility spellAbility, final Card card, final Player activator) {
        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }

        if (!stAb.matchesValidParam("ValidSA", spellAbility)) {
            return false;
        }

        if (stAb.hasParam("AffectedZone") && !card.isInZone(ZoneType.smartValueOf(stAb.getParam("AffectedZone")))) {
            return false;
        }

        if (!stAb.matchesValidParam("Activator", activator)) {
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
        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }

        if (stAb.hasParam("Origin")) {
            List<ZoneType> src = ZoneType.listValueOf(stAb.getParam("Origin"));

            if (!src.contains(card.getLastKnownZone().getZoneType())) {
                return false;
            }
        }

        if (!stAb.matchesValidParam("Player", player)) {
            return false;
        }

        return true;
    }

}
