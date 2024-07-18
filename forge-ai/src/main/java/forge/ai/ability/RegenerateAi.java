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

import java.util.ArrayList;
import java.util.List;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
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

    @Override
    protected boolean checkApiLogic(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        final Card hostCard = sa.getHostCard();
        boolean chance = false;

        if (sa.usesTargeting()) {
            sa.resetTargets();
            // filter AIs battlefield by what I can target
            List<Card> targetables = CardLists.getTargetableCards(ai.getCardsIn(ZoneType.Battlefield), sa);

            if (targetables.isEmpty()) {
                return false;
            }

            if (!game.getStack().isEmpty()) {
                // check stack for something on the stack will kill anything i control
                final List<GameObject> objects = ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), sa, true);

                final List<Card> threatenedTargets = new ArrayList<>();

                for (final Card c : targetables) {
                    if (objects.contains(c) && !ComputerUtil.canRegenerate(ai, c)) {
                        threatenedTargets.add(c);
                    }
                }

                if (!threatenedTargets.isEmpty()) {
                    // Choose "best" of the remaining to regenerate
                    sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(threatenedTargets));
                    chance = true;
                }
            } else if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                final CardCollection combatants = CardLists.filter(targetables, CardPredicates.Presets.CREATURES);
                ComputerUtilCard.sortByEvaluateCreature(combatants);

                for (final Card c : combatants) {
                    if (c.getShieldCount() == 0 && ComputerUtilCombat.combatantWouldBeDestroyed(ai, c, combat)) {
                        sa.getTargets().add(c);
                        chance = true;
                        break;
                    }
                }
            }
            if (sa.getTargets().isEmpty()) {
                return false;
            }
        } else {
            final List<Card> list = AbilityUtils.getDefinedCards(hostCard, sa.getParam("Defined"), sa);
            if (list.isEmpty()) {
                return false;
            }
            // when regenerating more than one is possible try for slightly more value
            int numToSave = Math.min(2, list.size());
            int saved = 0;

            if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                for (final Card c : list) {
                    if (c.getShieldCount() == 0 && ComputerUtil.predictCreatureWillDieThisTurn(ai, c, sa)) {
                        saved++;
                    }
                    if (saved == numToSave) {
                        break;
                    }
                }
            } else {
                final List<GameObject> objects = ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), sa, true);
                objects.retainAll(list);
                saved = objects.size();
            }
            // if nothing on the stack, and it's not declare blockers no need to regen
            chance = saved >= numToSave;
        }

        return chance;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        boolean chance = false;

        if (sa.usesTargeting()) {
            chance = regenMandatoryTarget(ai, sa, mandatory);
        } else {
            // If there's no target on the trigger, just say yes.
            chance = true;
        }

        return chance;
    }

    private static boolean regenMandatoryTarget(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final Game game = ai.getGame();
        sa.resetTargets();
        // filter AIs battlefield by what I can target
        CardCollectionView targetables = CardLists.getTargetableCards(game.getCardsIn(ZoneType.Battlefield), sa);
        final List<Card> compTargetables = CardLists.filterControlledBy(targetables, ai);

        if (targetables.isEmpty()) {
            return false;
        }

        if (!mandatory && compTargetables.isEmpty()) {
            return false;
        }

        if (compTargetables.size() > 0) {
            final CardCollection combatants = CardLists.filter(compTargetables, CardPredicates.Presets.CREATURES);
            ComputerUtilCard.sortByEvaluateCreature(combatants);
            if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                Combat combat = game.getCombat();
                for (final Card c : combatants) {
                    if (c.getShieldCount() == 0 && ComputerUtilCombat.combatantWouldBeDestroyed(ai, c, combat)) {
                        sa.getTargets().add(c);
                        return true;
                    }
                }
            }

            // TODO see if something on the stack is about to kill something i can target

            // choose my best X without regen
            if (CardLists.getNotType(compTargetables, "Creature").isEmpty()) {
                for (final Card c : combatants) {
                    if (c.getShieldCount() == 0) {
                        sa.getTargets().add(c);
                        return true;
                    }
                }
                sa.getTargets().add(combatants.get(0));
                return true;
            } else {
                CardLists.sortByCmcDesc(compTargetables);
                for (final Card c : compTargetables) {
                    if (c.getShieldCount() == 0) {
                        sa.getTargets().add(c);
                        return true;
                    }
                }
                sa.getTargets().add(compTargetables.get(0));
                return true;
            }
        }

        sa.getTargets().add(ComputerUtilCard.getCheapestPermanentAI(targetables, sa, false));
        return true;
    }

}
