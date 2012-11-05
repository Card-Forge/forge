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
package forge.card.abilityfactory.ai;

import java.util.ArrayList;
import java.util.Map;

import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.game.player.Player;

/**
 * <p>
 * AbilityFactory_Turns class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AddTurnAi extends SpellAiLogic {


    @Override
    protected boolean doTriggerAINoCost(Player ai, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
        final Player opp = ai.getOpponent();
        final Target tgt = sa.getTarget();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            if (sa.canTarget(ai)) {
                sa.getTarget().addTarget(ai);
            } else if (mandatory && sa.canTarget(opp)) {
                sa.getTarget().addTarget(opp);
            } else {
                return false;
            }
        } else {
            final ArrayList<Player> tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(),
                    params.get("Defined"), sa);
            for (final Player p : tgtPlayers) {
                if (p.isHuman() && !mandatory) {
                    return false;
                }
            }
            // not sure if the AI should be playing with cards that give the
            // Human more turns.
        }
        return true;
    }

    @Override
    public boolean chkAIDrawback(java.util.Map<String,String> params, SpellAbility sa, Player aiPlayer) {
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public boolean canPlayAI(Player aiPlayer, Map<String, String> params, SpellAbility sa) {
        return doTriggerAINoCost(aiPlayer, params, sa, false);
    }

}