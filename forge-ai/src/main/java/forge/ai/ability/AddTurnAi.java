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
package forge.ai.ability;

import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * AbilityFactory_Turns class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AddTurnAi extends SpellAbilityAi {


    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final Player opp = ai.getWeakestOpponent();

        if (sa.usesTargeting()) {
            sa.resetTargets();
            if (sa.canTarget(ai)) {
                sa.getTargets().add(ai);
            } else if (mandatory) {
            	for (final Player ally : ai.getAllies()) {
                    if (sa.canTarget(ally)) {
                    	sa.getTargets().add(ally);
                    	break;
                    }
            	}
                if (!sa.getTargetRestrictions().isMinTargetsChosen(sa.getHostCard(), sa) && sa.canTarget(opp)) {
                    sa.getTargets().add(opp);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            final List<Player> tgtPlayers = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Defined"), sa);
            for (final Player p : tgtPlayers) {
                if (p.isOpponentOf(ai) && !mandatory) {
                    return false;
                }
            }
            if (!StringUtils.isNumeric(sa.getParam("NumTurns"))) {
                // TODO: improve ai for Sage of Hours
                return false;
            }
            // not sure if the AI should be playing with cards that give the
            // Human more turns.
        }
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        return doTriggerAINoCost(aiPlayer, sa, false);
    }

}
