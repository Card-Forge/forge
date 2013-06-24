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

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.ITargetable;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityAi;
import forge.card.cost.Cost;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.TargetRestrictions;
import forge.game.Game;
import forge.game.ai.ComputerUtil;
import forge.game.ai.ComputerUtilCard;
import forge.game.ai.ComputerUtilCombat;
import forge.game.ai.ComputerUtilCost;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * <p>
 * AbilityFactory_Regenerate class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class RegenerateAi extends SpellAbilityAi {

    // Ex: A:SP$Regenerate | Cost$W | ValidTgts$ Creature | TgtPrompt$ Select target creature | SpellDescription$Regenerate
    // target creature.
    // http://www.slightlymagic.net/wiki/Forge_AbilityFactory#Regenerate

    // **************************************************************
    // ********************* Regenerate ****************************
    // **************************************************************

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Card hostCard = sa.getSourceCard();
        final Cost abCost = sa.getPayCosts();
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();

        boolean chance = false;
        if (abCost != null) {
            // AI currently disabled for these costs
            if (!ComputerUtilCost.checkLifeCost(ai, abCost, hostCard, 4, null)) {
                return false;
            }

            if (!ComputerUtilCost.checkSacrificeCost(ai, abCost, hostCard)) {
                return false;
            }

            if (!ComputerUtilCost.checkCreatureSacrificeCost(ai, abCost, hostCard)) {
                return false;
            }
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt == null) {
            // As far as I can tell these Defined Cards will only have one of
            // them
            final List<Card> list = AbilityUtils.getDefinedCards(hostCard, sa.getParam("Defined"), sa);

            if (!game.getStack().isEmpty()) {
                final List<ITargetable> objects = ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), sa);

                for (final Card c : list) {
                    if (objects.contains(c)) {
                        chance = true;
                    }
                }
            } else {
                if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                    boolean flag = false;

                    for (final Card c : list) {
                        if (c.getShield() == 0) {
                            flag |= ComputerUtilCombat.combatantWouldBeDestroyed(ai, c, combat);
                        }
                    }

                    chance = flag;
                } else { // if nothing on the stack, and it's not declare
                         // blockers. no need to regen
                    return false;
                }
            }
        } else {
            sa.resetTargets();
            // filter AIs battlefield by what I can target
            List<Card> targetables = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), tgt.getValidTgts(), ai, hostCard);
            targetables = CardLists.getTargetableCards(targetables, sa);

            if (targetables.size() == 0) {
                return false;
            }

            if (!game.getStack().isEmpty()) {
                // check stack for something on the stack will kill anything i
                // control
                final List<ITargetable> objects = ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), sa);

                final List<Card> threatenedTargets = new ArrayList<Card>();

                for (final Card c : targetables) {
                    if (objects.contains(c) && (c.getShield() == 0)) {
                        threatenedTargets.add(c);
                    }
                }

                if (!threatenedTargets.isEmpty()) {
                    // Choose "best" of the remaining to regenerate
                    sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(threatenedTargets));
                    chance = true;
                }
            } else {
                if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                    final List<Card> combatants = CardLists.filter(targetables, CardPredicates.Presets.CREATURES);
                    CardLists.sortByEvaluateCreature(combatants);

                    for (final Card c : combatants) {
                        if ((c.getShield() == 0) && ComputerUtilCombat.combatantWouldBeDestroyed(ai, c, combat)) {
                            sa.getTargets().add(c);
                            chance = true;
                            break;
                        }
                    }
                }
            }
            if (sa.getTargets().isEmpty()) {
                return false;
            }
        }

        return chance;
    } // regenerateCanPlayAI

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        boolean chance = false;

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt == null) {
            // If there's no target on the trigger, just say yes.
            chance = true;
        } else {
            chance = regenMandatoryTarget(ai, sa, mandatory);
        }

        return chance;
    }

    private static boolean regenMandatoryTarget(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final Card hostCard = sa.getSourceCard();
        final Game game = ai.getGame();
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        sa.resetTargets();
        // filter AIs battlefield by what I can target
        List<Card> targetables = game.getCardsIn(ZoneType.Battlefield);
        targetables = CardLists.getValidCards(targetables, tgt.getValidTgts(), ai, hostCard);
        targetables = CardLists.getTargetableCards(targetables, sa);
        final List<Card> compTargetables = CardLists.filterControlledBy(targetables, ai);

        if (targetables.size() == 0) {
            return false;
        }

        if (!mandatory && (compTargetables.size() == 0)) {
            return false;
        }

        if (compTargetables.size() > 0) {
            final List<Card> combatants = CardLists.filter(compTargetables, CardPredicates.Presets.CREATURES);
            CardLists.sortByEvaluateCreature(combatants);
            if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                Combat combat = game.getCombat();
                for (final Card c : combatants) {
                    if ((c.getShield() == 0) && ComputerUtilCombat.combatantWouldBeDestroyed(ai, c, combat)) {
                        sa.getTargets().add(c);
                        return true;
                    }
                }
            }

            // TODO see if something on the stack is about to kill something i
            // can target

            // choose my best X without regen
            if (CardLists.getNotType(compTargetables, "Creature").isEmpty()) {
                for (final Card c : combatants) {
                    if (c.getShield() == 0) {
                        sa.getTargets().add(c);
                        return true;
                    }
                }
                sa.getTargets().add(combatants.get(0));
                return true;
            } else {
                CardLists.sortByCmcDesc(compTargetables);
                for (final Card c : compTargetables) {
                    if (c.getShield() == 0) {
                        sa.getTargets().add(c);
                        return true;
                    }
                }
                sa.getTargets().add(compTargetables.get(0));
                return true;
            }
        }

        sa.getTargets().add(ComputerUtilCard.getCheapestPermanentAI(targetables, sa, true));
        return true;
    }

}
