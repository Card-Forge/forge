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
import java.util.HashMap;
import java.util.List;

import forge.Card;

import forge.CardLists;
import forge.CardPredicates;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.phase.CombatUtil;
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
public class RegenerateAi extends SpellAiLogic {

    // Ex: A:SP$Regenerate | Cost$W | Tgt$TgtC | SpellDescription$Regenerate
    // target creature.
    // http://www.slightlymagic.net/wiki/Forge_AbilityFactory#Regenerate

    // **************************************************************
    // ********************* Regenerate ****************************
    // **************************************************************

    /**
     * <p>
     * regenerateCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    @Override
    public boolean canPlayAI(Player ai, java.util.Map<String,String> params, SpellAbility sa) {
        final Card hostCard = sa.getAbilityFactory().getHostCard();
        boolean chance = false;
        final Cost abCost = sa.getAbilityFactory().getAbCost();
        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkLifeCost(ai, abCost, hostCard, 4, null)) {
                return false;
            }

            if (!CostUtil.checkSacrificeCost(ai, abCost, hostCard)) {
                return false;
            }

            if (!CostUtil.checkCreatureSacrificeCost(ai, abCost, hostCard)) {
                return false;
            }
        }

        final Target tgt = sa.getTarget();
        if (tgt == null) {
            // As far as I can tell these Defined Cards will only have one of
            // them
            final ArrayList<Card> list = AbilityFactory.getDefinedCards(hostCard, params.get("Defined"), sa);

            if (Singletons.getModel().getGame().getStack().size() > 0) {
                final ArrayList<Object> objects = AbilityFactory.predictThreatenedObjects(sa.getActivatingPlayer(),sa.getAbilityFactory());

                for (final Card c : list) {
                    if (objects.contains(c)) {
                        chance = true;
                    }
                }
            } else {
                if (Singletons.getModel().getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
                    boolean flag = false;

                    for (final Card c : list) {
                        if (c.getShield() == 0) {
                            flag |= CombatUtil.combatantWouldBeDestroyed(c);
                        }
                    }

                    chance = flag;
                } else { // if nothing on the stack, and it's not declare
                         // blockers. no need to regen
                    return false;
                }
            }
        } else {
            tgt.resetTargets();
            // filter AIs battlefield by what I can target
            List<Card> targetables = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), tgt.getValidTgts(), ai, hostCard);
            targetables = CardLists.getTargetableCards(targetables, sa);

            if (targetables.size() == 0) {
                return false;
            }

            if (Singletons.getModel().getGame().getStack().size() > 0) {
                // check stack for something on the stack will kill anything i
                // control
                final ArrayList<Object> objects = AbilityFactory.predictThreatenedObjects(sa.getActivatingPlayer(), sa.getAbilityFactory());

                final List<Card> threatenedTargets = new ArrayList<Card>();

                for (final Card c : targetables) {
                    if (objects.contains(c) && (c.getShield() == 0)) {
                        threatenedTargets.add(c);
                    }
                }

                if (!threatenedTargets.isEmpty()) {
                    // Choose "best" of the remaining to regenerate
                    tgt.addTarget(CardFactoryUtil.getBestCreatureAI(threatenedTargets));
                    chance = true;
                }
            } else {
                if (Singletons.getModel().getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
                    final List<Card> combatants = CardLists.filter(targetables, CardPredicates.Presets.CREATURES);
                    CardLists.sortByEvaluateCreature(combatants);

                    for (final Card c : combatants) {
                        if ((c.getShield() == 0) && CombatUtil.combatantWouldBeDestroyed(c)) {
                            tgt.addTarget(c);
                            chance = true;
                            break;
                        }
                    }
                }
            }
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    } // regenerateCanPlayAI

    /**
     * <p>
     * doTriggerAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    
    @Override
    public boolean doTriggerAINoCost(Player ai, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
        boolean chance = false;

        final Target tgt = sa.getTarget();
        if (tgt == null) {
            // If there's no target on the trigger, just say yes.
            chance = true;
        } else {
            chance = regenMandatoryTarget(ai, sa, mandatory);
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.doTrigger(mandatory);
        }

        return chance;
    }

    @Override
    public boolean chkAIDrawback(java.util.Map<String,String> params, SpellAbility sa, Player aiPlayer) {
        return true;
    }

    /**
     * <p>
     * regenMandatoryTarget.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean regenMandatoryTarget(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final Card hostCard = sa.getAbilityFactory().getHostCard();
        final Target tgt = sa.getTarget();
        tgt.resetTargets();
        // filter AIs battlefield by what I can target
        List<Card> targetables = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
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
            if (Singletons.getModel().getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
                for (final Card c : combatants) {
                    if ((c.getShield() == 0) && CombatUtil.combatantWouldBeDestroyed(c)) {
                        tgt.addTarget(c);
                        return true;
                    }
                }
            }

            // TODO see if something on the stack is about to kill something i
            // can target

            // choose my best X without regen
            if (CardLists.getNotType(compTargetables, "Creature").size() == 0) {
                for (final Card c : combatants) {
                    if (c.getShield() == 0) {
                        tgt.addTarget(c);
                        return true;
                    }
                }
                tgt.addTarget(combatants.get(0));
                return true;
            } else {
                CardLists.sortByMostExpensive(compTargetables);
                for (final Card c : compTargetables) {
                    if (c.getShield() == 0) {
                        tgt.addTarget(c);
                        return true;
                    }
                }
                tgt.addTarget(compTargetables.get(0));
                return true;
            }
        }

        tgt.addTarget(CardFactoryUtil.getCheapestPermanentAI(targetables, sa, true));
        return true;
    }

    /**
     * <p>
     * regenerateStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
}
