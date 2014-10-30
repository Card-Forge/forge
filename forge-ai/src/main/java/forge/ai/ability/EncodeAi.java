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

import com.google.common.base.Predicate;

import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;

import java.util.List;

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
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
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
    public Card chooseSingleCard(final Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer) {
        Card choice = null;
//        final String logic = sa.getParam("AILogic");
//        if (logic == null) {
        final List<Card> attackers = CardLists.filter(options, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return ComputerUtilCombat.canAttackNextTurn(c);
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
