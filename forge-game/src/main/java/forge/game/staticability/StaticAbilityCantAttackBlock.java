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
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import java.util.Map;

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
        final Map<String, String> params = stAb.getMapParams();
        final Card hostCard = stAb.getHostCard();

        if (params.containsKey("ValidCard")
                && !card.isValid(params.get("ValidCard").split(","), hostCard.getController(), hostCard, null)) {
            return false;
        }

        if (params.containsKey("Target")
                && !target.isValid(params.get("Target").split(","), hostCard.getController(), hostCard, null)) {
            return false;
        }

        final Player defender = target instanceof Card ? ((Card) target).getController() : (Player) target;

        if (params.containsKey("UnlessDefenderControls")) {
            String type = params.get("UnlessDefenderControls");
            CardCollectionView list = defender.getCardsIn(ZoneType.Battlefield);
            if (Iterables.any(list, CardPredicates.restriction(type.split(","), hostCard.getController(), hostCard, null))) {
                return false;
            }
        }
        if (params.containsKey("IfDefenderControls")) {
            String type = params.get("IfDefenderControls");
            CardCollectionView list = defender.getCardsIn(ZoneType.Battlefield);
            if (!Iterables.any(list, CardPredicates.restriction(type.split(","), hostCard.getController(), hostCard, null))) {
                return false;
            }
        }
        if (params.containsKey("DefenderNotNearestToYouInChosenDirection")
                && hostCard.getChosenDirection() != null
                && defender.equals(hostCard.getGame().getNextPlayerAfter(card.getController(), hostCard.getChosenDirection()))) {
            return false;
        }
        if (params.containsKey("UnlessDefender")) {
        	final String type = params.get("UnlessDefender");
        	if (defender.hasProperty(type, hostCard.getController(), hostCard, null)) {
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
     * @param card
     *            the card
     * @return a Cost
     */
    public static Cost getAttackCost(final StaticAbility stAb, final Card card, final GameEntity target) {
        final Map<String, String> params = stAb.getMapParams();
        final Card hostCard = stAb.getHostCard();

        if (params.containsKey("ValidCard")
                && !card.isValid(params.get("ValidCard").split(","), hostCard.getController(), hostCard, null)) {
            return null;
        }

        if (params.containsKey("Target")
                && !target.isValid(params.get("Target").split(","), hostCard.getController(), hostCard, null)) {
            return null;
        }
        String costString = params.get("Cost");
        if ("X".equals(costString)) {
            costString = Integer.toString(CardFactoryUtil.xCount(hostCard, hostCard.getSVar("X")));
        } else if ("Y".equals(costString)) {
            costString = Integer.toString(CardFactoryUtil.xCount(hostCard, hostCard.getSVar("Y")));
        } else if (params.containsKey("References")) {
            costString = Integer.toString(CardFactoryUtil.xCount(hostCard, hostCard.getSVar(params.get("References"))));
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
        final Map<String, String> params = stAb.getMapParams();
        final Card hostCard = stAb.getHostCard();

        if (params.containsKey("ValidCard")
                && !blocker.isValid(params.get("ValidCard").split(","), hostCard.getController(), hostCard, null)) {
            return null;
        }
        
        if (params.containsKey("Attacker") && attacker != null
                && !attacker.isValid(params.get("Attacker").split(","), hostCard.getController(), hostCard, null)) {
            return null;
        }
        String costString = params.get("Cost");
        if ("X".equals(costString)) {
            costString = Integer.toString(CardFactoryUtil.xCount(hostCard, hostCard.getSVar("X")));
        } else if ("Y".equals(costString)) {
            costString = Integer.toString(CardFactoryUtil.xCount(hostCard, hostCard.getSVar("Y")));
        } else if (params.containsKey("References")) {
            costString = Integer.toString(CardFactoryUtil.xCount(hostCard, hostCard.getSVar(params.get("References"))));
        }

        final Cost cost = new Cost(costString, true);

        return cost;
    }

}
