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

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardPredicates;
import forge.game.cost.Cost;
import forge.game.keyword.KeywordInterface;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import com.google.common.collect.Iterables;

/**
 * The Class StaticAbility_CantBeCast.
 */
public class StaticAbilityCantAttackBlock {

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

        if (stAb.hasParam("ValidCard")
                && !card.isValid(stAb.getParam("ValidCard").split(","), hostCard.getController(), hostCard, null)) {
            return false;
        }

        if (stAb.hasParam("Target")
                && !target.isValid(stAb.getParam("Target").split(","), hostCard.getController(), hostCard, null)) {
            return false;
        }

        final Player defender = target instanceof Card ? ((Card) target).getController() : (Player) target;

        if (stAb.hasParam("UnlessDefenderControls")) {
            String type = stAb.getParam("UnlessDefenderControls");
            CardCollectionView list = defender.getCardsIn(ZoneType.Battlefield);
            if (Iterables.any(list, CardPredicates.restriction(type.split(","), hostCard.getController(), hostCard, null))) {
                return false;
            }
        }
        if (stAb.hasParam("IfDefenderControls")) {
            String type = stAb.getParam("IfDefenderControls");
            CardCollectionView list = defender.getCardsIn(ZoneType.Battlefield);
            if (!Iterables.any(list, CardPredicates.restriction(type.split(","), hostCard.getController(), hostCard, null))) {
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
            if (defender.hasProperty(type, hostCard.getController(), hostCard, null)) {
                return false;
            }
        }

        return true;
    }

    /**
     * returns true if attacker can be blocked by blocker
     * @param stAb
     * @param attacker
     * @param blocker
     * @return boolean
     */
    public static boolean applyCantBlockByAbility(final StaticAbility stAb, final Card attacker, final Card blocker) {
        final Card host = stAb.getHostCard();
        if (stAb.hasParam("ValidAttacker")) {
            if (!attacker.isValid(stAb.getParam("ValidAttacker").split(","), host.getController(), host, null)) {
                return false;
            }
        }
        if (stAb.hasParam("ValidBlocker")) {
            for (final String v : stAb.getParam("ValidBlocker").split(",")) {
                if (blocker.isValid(v, host.getController(), host, null)) {
                    boolean stillblock = false;
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
                        return true;
                    }
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
     * @return a Cost
     */
    public static Cost getAttackCost(final StaticAbility stAb, final Card card, final GameEntity target) {
        final Card hostCard = stAb.getHostCard();

        if (stAb.hasParam("ValidCard")
                && !card.isValid(stAb.getParam("ValidCard").split(","), hostCard.getController(), hostCard, null)) {
            return null;
        }

        if (stAb.hasParam("Target")
                && !target.isValid(stAb.getParam("Target").split(","), hostCard.getController(), hostCard, null)) {
            return null;
        }
        String costString = stAb.getParam("Cost");
        if ("X".equals(costString)) {
            costString = Integer.toString(CardFactoryUtil.xCount(hostCard, hostCard.getSVar("X")));
        } else if ("Y".equals(costString)) {
            costString = Integer.toString(CardFactoryUtil.xCount(hostCard, hostCard.getSVar("Y")));
        } else if (stAb.hasParam("References")) {
            costString = Integer.toString(CardFactoryUtil.xCount(hostCard, hostCard.getSVar(stAb.getParam("References"))));
        }

        final Cost cost = new Cost(costString, true);

        return cost;
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

        if (stAb.hasParam("ValidCard")
                && !blocker.isValid(stAb.getParam("ValidCard").split(","), hostCard.getController(), hostCard, null)) {
            return null;
        }
        
        if (stAb.hasParam("Attacker") && attacker != null
                && !attacker.isValid(stAb.getParam("Attacker").split(","), hostCard.getController(), hostCard, null)) {
            return null;
        }
        String costString = stAb.getParam("Cost");
        if ("X".equals(costString)) {
            costString = Integer.toString(CardFactoryUtil.xCount(hostCard, hostCard.getSVar("X")));
        } else if ("Y".equals(costString)) {
            costString = Integer.toString(CardFactoryUtil.xCount(hostCard, hostCard.getSVar("Y")));
        } else if (stAb.hasParam("References")) {
            costString = Integer.toString(CardFactoryUtil.xCount(hostCard, hostCard.getSVar(stAb.getParam("References"))));
        }

        final Cost cost = new Cost(costString, true);

        return cost;
    }

    public static boolean applyCanAttackHasteAbility(final StaticAbility stAb, final Card card, final GameEntity target) {
        final Card hostCard = stAb.getHostCard();
        if (stAb.hasParam("ValidCard")
                && !card.isValid(stAb.getParam("ValidCard").split(","), hostCard.getController(), hostCard, null)) {
            return false;
        }

        if (stAb.hasParam("ValidTarget")
                && !target.isValid(stAb.getParam("ValidTarget").split(","), hostCard.getController(), hostCard, null)) {
            return false;
        }

        final Player defender = target instanceof Card ? ((Card) target).getController() : (Player) target;
        if (stAb.hasParam("ValidDefender")
                && !defender.isValid(stAb.getParam("ValidDefender").split(","), hostCard.getController(), hostCard, null)) {
            return false;
        }
        return true;
    }
}
