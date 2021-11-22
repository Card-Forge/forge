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

import java.util.List;
import java.util.Map;

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

    /*
     * (non-Javadoc)
     * 
     * @see forge.ai.SpellAbilityAi#confirmAction(forge.game.player.Player,
     * forge.game.spellability.SpellAbility,
     * forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        // only try to encode if there is a creature it can be used on
        return chooseCard(player, player.getCreaturesInPlay(), true) != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.ai.SpellAbilityAi#chooseSingleCard(forge.game.player.Player,
     * forge.game.spellability.SpellAbility, java.lang.Iterable, boolean,
     * forge.game.player.Player)
     */
    @Override
    public Card chooseSingleCard(final Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        return chooseCard(ai, options, isOptional);
    }

    private Card chooseCard(final Player ai, Iterable<Card> list, boolean isOptional) {
        Card choice = null;
        // final String logic = sa.getParam("AILogic");
        // if (logic == null) {
        final List<Card> attackers = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return ComputerUtilCombat.canAttackNextTurn(c);
            }
        });
        final List<Card> unblockables = CardLists.filter(attackers, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                boolean canAttackOpponent = false;
                for (Player opp : ai.getOpponents()) {
                    if (CombatUtil.canAttack(c, opp) && !CombatUtil.canBeBlocked(c, opp)) {
                        canAttackOpponent = true;
                        break;
                    }
                }
                return canAttackOpponent;
            }
        });
        if (!unblockables.isEmpty()) {
            choice = ComputerUtilCard.getBestAI(unblockables);
        } else if (!attackers.isEmpty()) {
            choice = ComputerUtilCard.getBestAI(attackers);
        } else if (!isOptional) {
            choice = ComputerUtilCard.getBestAI(list);
        }
        // }
        return choice;
    }
}
