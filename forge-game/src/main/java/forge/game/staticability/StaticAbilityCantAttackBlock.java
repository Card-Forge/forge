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

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * The Class StaticAbility_CantBeCast.
 */
public class StaticAbilityCantAttackBlock {

    public static boolean cantAttack(final Card attacker, final GameEntity defender) {
        // Keywords
        // replace with Static Ability if able
        if (attacker.hasKeyword("CARDNAME can't attack.") || attacker.hasKeyword("CARDNAME can't attack or block.")) {
            return true;
        }

        if (attacker.isDetained()) {
            return true;
        }

        for (final Card ca : attacker.getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.CantAttack)) {
                    continue;
                }

                if (applyCantAttackAbility(stAb, attacker, defender)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * TODO Write javadoc for this method.
     *
     * @param stAb a StaticAbility
     * @param card the card
     * @return a Cost
     */
    public static boolean applyCantAttackAbility(final StaticAbility stAb, final Card card, final GameEntity target) {
        final Card hostCard = stAb.getHostCard();
        final Game game = hostCard.getGame();

        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }
        if (stAb.getIgnoreEffectCards().contains(card)) {
            return false;
        }

        if (!stAb.matchesValidParam("Target", target)) {
            return false;
        }

        // check for "can attack as if didn't have defender" static
        if (stAb.isKeyword(Keyword.DEFENDER) && canAttackDefender(card, target)) {
            return false;
        }

        final Player defender;
        if (target instanceof Player) {
            defender = (Player) target;
        } else {
            Card c = (Card) target;
            if (c.isBattle()) {
                defender = c.getProtectingPlayer();
            } else {
                defender = c.getController();
            }
        }

        if (stAb.hasParam("DefenderNotNearestToYouInChosenDirection")) {
            if (hostCard.getChosenDirection() == null) {
                return false;
            }
            if (target instanceof Card && ((Card) target).isBattle()) {
                return false;
            }
            Player next = card.getController();
            while (!next.isOpponentOf(card.getController())) {
                next = game.getNextPlayerAfter(next, hostCard.getChosenDirection());
            }
            if (defender.equals(next)) {
                return false;
            }
        }
        if (stAb.hasParam("UnlessDefender")) {
            final String type = stAb.getParam("UnlessDefender");
            if (defender.hasProperty(type, hostCard.getController(), hostCard, stAb)) {
                return false;
            }
        }

        return true;
    }

    public static boolean canAttackDefender(final Card card, final GameEntity target) {
        // CanAttack static abilities
        for (final Card ca : card.getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.CanAttackDefender)) {
                    continue;
                }

                if (applyCanAttackDefenderAbility(stAb, card, target)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean applyCanAttackDefenderAbility(final StaticAbility stAb, final Card card, final GameEntity target) {
        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }

        if (!stAb.matchesValidParam("ValidAttacked", target)) {
            return false;
        }

        return true;
    }

    public static boolean cantBlock(final Card blocker) {
        if (blocker.isDetained()) {
            return true;
        }

        CardCollection list = new CardCollection(blocker.getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES));
        // add blocker in case of LKI
        list.add(blocker);
        for (final Card ca : list) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.CantBlock)) {
                    continue;
                }
                if (applyCantBlockAbility(stAb, blocker)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean applyCantBlockAbility(final StaticAbility stAb, final Card blocker) {
        if (!stAb.matchesValidParam("ValidCard", blocker)) {
            return false;
        }
        if (stAb.getIgnoreEffectCards().contains(blocker)) {
            return false;
        }
        return true;
    }

    public static boolean canBlockTapped(final Card card)  {
        final Game game = card.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.BlockTapped)) {
                    continue;
                }

                if (applyBlockTapped(stAb, card)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean applyBlockTapped(final StaticAbility stAb, final Card card) {
        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }
        return true;
    }

    public static boolean cantBlockBy(final Card attacker, final Card blocker) {
        CardCollection list = new CardCollection(attacker.getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES));
        // add attacker and blocker in case of LKI
        list.add(attacker);
        if (blocker != null) {
            list.add(blocker);
        }
        for (final Card ca : list) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.CantBlockBy)) {
                    continue;
                }
                if (applyCantBlockByAbility(stAb, attacker, blocker)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * returns true if attacker can't be blocked by blocker
     *
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
                if (blocker != null && blocker.isValid(v, host.getController(), host, stAb)) {
                    stillblock = false;
                    // Dragon Hunter check
                    if (v.contains("withoutReach") && canBlockIfReach(attacker, blocker)) {
                        stillblock = true;
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
        if (blocker == null || !stAb.matchesValidParam("ValidDefender", blocker.getController())) {
            return false;
        }
        if (stAb.isKeyword(Keyword.LANDWALK)) {
            if (StaticAbilityIgnoreLandwalk.ignoreLandWalk(attacker, blocker, stAb.getKeyword())) {
                return false;
            }
        }
        return true;
    }

    public static boolean canBlockIfReach(final Card attacker, final Card blocker) {
        for (final Card ca : attacker.getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.CanBlockIfReach)) {
                    continue;
                }
                if (applyCanBlockIfReachAbility(stAb, attacker, blocker)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean applyCanBlockIfReachAbility(final StaticAbility stAb, final Card attacker, final Card blocker) {
        if (!stAb.matchesValidParam("ValidAttacker", attacker)) {
            return false;
        }
        if (!stAb.matchesValidParam("ValidBlocker", blocker)) {
            return false;
        }
        return true;
    }

    /**
     * TODO Write javadoc for this method.
     *
     * @param stAb     a StaticAbility
     * @param attacker the card
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
            // keep X shards
            boolean addX = costString.startsWith("X");
            costString = Integer.toString(AbilityUtils.calculateAmount(hostCard, stAb.getSVar(costString), stAb));
            if (addX) {
                costString += " X";
            }
            if (remember) {
                hostCard.removeRemembered(attacker);
            }
        }

        Cost cost = new Cost(costString, true);

        if (stAb.hasParam("Trigger")) {
            cost.getCostParts().get(0).setTrigger(stAb.getPayingTrigSA());
        }

        return cost;
    }

    /**
     * TODO Write javadoc for this method.
     *
     * @param stAb    a StaticAbility
     * @param blocker the card
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
            boolean addX = costString.startsWith("X");
            costString = Integer.toString(AbilityUtils.calculateAmount(hostCard, stAb.getSVar(costString), stAb));
            if (addX) {
                costString += " X";
            }
        }

        return new Cost(costString, true);
    }

    public static boolean canAttackHaste(final Card attacker, final GameEntity defender) {
        final Game game = attacker.getGame();
        if (!attacker.isSick()) {
            return true;
        }
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.CanAttackIfHaste)) {
                    continue;
                }
                if (applyCanAttackHasteAbility(stAb, attacker, defender)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean applyCanAttackHasteAbility(final StaticAbility stAb, final Card card,
            final GameEntity target) {
        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }

        if (!stAb.matchesValidParam("ValidTarget", target)) {
            return false;
        }
        return true;
    }

    public static Pair<Integer, Integer> getMinMaxBlocker(final Card attacker, final Player defender) {
        MutablePair<Integer, Integer> result = MutablePair.of(1, Integer.MAX_VALUE);

        if (attacker.hasKeyword(Keyword.MENACE)) {
            result.setLeft(2);
        }

        final Game game = attacker.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.MinMaxBlocker)) {
                    continue;
                }
                applyMinMaxBlockerAbility(stAb, attacker, defender, result);
            }
        }
        return result;
    }

    public static void applyMinMaxBlockerAbility(final StaticAbility stAb, final Card attacker, final Player defender,
            MutablePair<Integer, Integer> result) {
        if (!stAb.matchesValidParam("ValidCard", attacker)) {
            return;
        }

        if (stAb.hasParam("Min")) {
            if ("All".equals(stAb.getParam("Min"))) {
                if (defender != null) {
                    result.setLeft(defender.getCreaturesInPlay().size());
                }
            } else {
                result.setLeft(AbilityUtils.calculateAmount(stAb.getHostCard(), stAb.getParam("Min"), stAb));
            }
        }

        if (stAb.hasParam("Max")) {
            result.setRight(AbilityUtils.calculateAmount(stAb.getHostCard(), stAb.getParam("Max"), stAb));
        }
    }

    public static boolean attackVigilance(final Card card)  {
        final Game game = card.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.AttackVigilance)) {
                    continue;
                }

                if (applyAttackVigilanceAbility(stAb, card)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean applyAttackVigilanceAbility(final StaticAbility stAb, final Card card) {
        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }
        return true;
    }
}
