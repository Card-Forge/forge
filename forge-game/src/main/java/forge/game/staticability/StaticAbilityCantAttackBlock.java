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

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Iterables;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardPredicates;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * The Class StaticAbility_CantBeCast.
 */
public class StaticAbilityCantAttackBlock {

    public static String MinMaxBlockerMode = "MinMaxBlocker";

    /**
     * TODO Write javadoc for this method.
     * 
     * @param stAb
     *            a StaticAbility
     * @param card
     *            the card
     * @return a Cost
     */
    public static boolean applyCantAttackAbility(final StaticAbility stAb, final Card card, final GameEntity target) {
        final Card hostCard = stAb.getHostCard();

        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }

        if (!stAb.matchesValidParam("Target", target)) {
            return false;
        }

        if (stAb.hasParam("DefenderKeyword")) {
            if (card.hasKeyword("CARDNAME can attack as though it didn't have defender.")) {
                return false;
            }
        }

        final Player defender = target instanceof Card ? ((Card) target).getController() : (Player) target;

        if (stAb.hasParam("UnlessDefenderControls")) {
            String type = stAb.getParam("UnlessDefenderControls");
            CardCollectionView list = defender.getCardsIn(ZoneType.Battlefield);
            if (Iterables.any(list, CardPredicates.restriction(type.split(","), hostCard.getController(), hostCard, stAb))) {
                return false;
            }
        }
        if (stAb.hasParam("IfDefenderControls")) {
            String type = stAb.getParam("IfDefenderControls");
            CardCollectionView list = defender.getCardsIn(ZoneType.Battlefield);
            if (!Iterables.any(list, CardPredicates.restriction(type.split(","), hostCard.getController(), hostCard, stAb))) {
                return false;
            }
        }
        if (stAb.hasParam("DefenderNotNearestToYouInChosenDirection")
                && hostCard.getChosenDirection() != null
                && defender.equals(hostCard.getGame().getNextPlayerAfter(card.getController(), hostCard.getChosenDirection()))) {
            return false;
        }
        if (stAb.hasParam("UnlessDefender")) {
            final String type = stAb.getParam("UnlessDefender");
            if (defender.hasProperty(type, hostCard.getController(), hostCard, stAb)) {
                return false;
            }
        }

        return true;
    }

    /**
     * returns true if attacker can't be blocked by blocker
     * @param stAb
     * @param attacker
     * @param blocker
     * @return boolean
     */
    public static boolean applyCantBlockByAbility(final StaticAbility stAb, final Card attacker, final Card blocker) {
        final Card host = stAb.getHostCard();
        if (!stAb.matchesValidParam("ValidAttacker", attacker)) {
            return false;
        }
        if (stAb.hasParam("ValidBlocker")) {
            boolean stillblock = true;
            for (final String v : stAb.getParam("ValidBlocker").split(",")) {
                if (blocker.isValid(v, host.getController(), host, stAb)) {
                    stillblock = false;
                    //Dragon Hunter check
                    if (v.contains("withoutReach") && blocker.hasStartOfKeyword("IfReach")) {
                        for (KeywordInterface inst : blocker.getKeywords()) {
                            String k = inst.getOriginal();
                            if (k.startsWith("IfReach")) {
                                String[] n = k.split(":");
                                if (attacker.getType().hasCreatureType(n[1])) {
                                    stillblock = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (!stillblock) {
                        break;
                    }
                }
            }
            if (stillblock) {
                return false;
            }
        }
        // relative valid relative to each other
        if (!stAb.matchesValidParam("ValidAttackerRelative", attacker, blocker)) {
            return false;
        }
        if (!stAb.matchesValidParam("ValidBlockerRelative", blocker, attacker)) {
            return false;
        }
        if (blocker != null) {
            if (!stAb.matchesValidParam("ValidDefender", blocker.getController())) {
                return false;
            }
        }
        return true;
    }

    /**
     * TODO Write javadoc for this method.
     * 
     * @param stAb
     *            a StaticAbility
     * @param attacker
     *            the card
     * @return a Cost
     */
    public static Cost getAttackCost(final StaticAbility stAb, final Card attacker, final GameEntity target) {
        final Card hostCard = stAb.getHostCard();

        if (!stAb.matchesValidParam("ValidCard", attacker)) {
            return null;
        }

        if (!stAb.matchesValidParam("Target", target)) {
            return null;
        }
        String costString = stAb.getParam("Cost");
        if (stAb.hasSVar(costString)) {
            boolean remember = stAb.hasParam("RememberingAttacker");
            if (remember) {
                hostCard.addRemembered(attacker);
            }
            costString = Integer.toString(AbilityUtils.calculateAmount(hostCard, stAb.getSVar(costString), stAb));
            if (remember) {
                hostCard.removeRemembered(attacker);
            }
        }

        return new Cost(costString, true);
    }

    /**
     * TODO Write javadoc for this method.
     * 
     * @param stAb
     *            a StaticAbility
     * @param blocker
     *            the card
     * @return a Cost
     */
    public static Cost getBlockCost(final StaticAbility stAb, final Card blocker, final GameEntity attacker) {
        final Card hostCard = stAb.getHostCard();

        if (!stAb.matchesValidParam("ValidCard", blocker)) {
            return null;
        }

        if (!stAb.matchesValidParam("Attacker", attacker)) {
            return null;
        }
        String costString = stAb.getParam("Cost");
        if (stAb.hasSVar(costString)) {
            costString = Integer.toString(AbilityUtils.calculateAmount(hostCard, costString, stAb));
        }

        return new Cost(costString, true);
    }

    public static boolean applyCanAttackHasteAbility(final StaticAbility stAb, final Card card, final GameEntity target) {
        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }

        if (!stAb.matchesValidParam("ValidTarget", target)) {
            return false;
        }

        final Player defender = target instanceof Card ? ((Card) target).getController() : (Player) target;
        if (!stAb.matchesValidParam("ValidDefender", defender)) {
            return false;
        }
        return true;
    }

    public static Pair<Integer, Integer> getMinMaxBlocker(final Card attacker, final Player defender) {
        MutablePair<Integer, Integer> result = MutablePair.of(1, Integer.MAX_VALUE);

        // Menace keyword
        if (attacker.hasKeyword(Keyword.MENACE)) {
            result.setLeft(2);
        }

        final Game game = attacker.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.getParam("Mode").equals(MinMaxBlockerMode) || stAb.isSuppressed() || !stAb.checkConditions()) {
                    continue;
                }
                applyMinMaxBlockerAbility(stAb, attacker, defender, result);
            }
        }
        if (attacker.hasKeyword("CARDNAME can't be blocked unless all creatures defending player controls block it.")) {
            if (defender != null) {
                result.setLeft(defender.getCreaturesInPlay().size());
            }
        }
        return result;
    }

    public static void applyMinMaxBlockerAbility(final StaticAbility stAb, final Card attacker, final Player defender, MutablePair<Integer, Integer> result) {
        if (!stAb.matchesValidParam("ValidCard", attacker)) {
            return;
        }

        if (stAb.hasParam("Min")) {
            result.setLeft(AbilityUtils.calculateAmount(stAb.getHostCard(), stAb.getParam("Min"), stAb));
        }

        if (stAb.hasParam("Max")) {
            result.setRight(AbilityUtils.calculateAmount(stAb.getHostCard(), stAb.getParam("Max"), stAb));
        }
    }
}
