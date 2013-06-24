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
package forge.card.ability.ai;

import java.util.List;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.SpellAbility;
import forge.game.ai.ComputerUtilCard;
import forge.game.phase.CombatUtil;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;

/**
 * <p>
 * AbilityFactoryBond class.
 * </p>
 * 
 * @author Forge
 * @version $Id: AbilityFactoryBond.java 15090 2012-04-07 12:50:31Z Max mtg $
 */
public final class EncodeAi extends SpellAbilityAi {
    /**
     * <p>
     * bondCanPlayAI.
     * </p>
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     * 
     * @return a boolean.
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        return true;
    }
    

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        return true;
    }
    

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        return true;
    }
    
    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#chooseSingleCard(forge.game.player.Player, forge.card.spellability.SpellAbility, java.util.List, boolean)
     */
    @Override
    public Card chooseSingleCard(final Player ai, SpellAbility sa, List<Card> options, boolean isOptional) {
        Card choice = null;
//        final String logic = sa.getParam("AILogic");
//        if (logic == null) {
        final List<Card> attackers = CardLists.filter(options, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return CombatUtil.canAttackNextTurn(c);
            }
        });
        final List<Card> unblockables = CardLists.filter(attackers, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return !CombatUtil.canBeBlocked(c, ai.getOpponent());
            }
        });
        if (!unblockables.isEmpty()) {
            choice = ComputerUtilCard.getBestAI(unblockables);
        } else if (!attackers.isEmpty()) {
            choice = ComputerUtilCard.getBestAI(attackers);
        } else {
            choice = ComputerUtilCard.getBestAI(options);
        }
//        }
        return choice;
    }
}
