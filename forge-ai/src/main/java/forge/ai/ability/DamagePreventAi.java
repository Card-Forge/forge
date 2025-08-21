package forge.ai.ability;

import forge.ai.*;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.card.*;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetChoices;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.List;

public class DamagePreventAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        final Card hostCard = sa.getHostCard();
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        boolean chance = false;

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt == null) {
            // As far as I can tell these Defined Cards will only have one of them
            final List<GameObject> objects = AbilityUtils.getDefinedObjects(hostCard, sa.getParam("Defined"), sa);

            // react to threats on the stack
            if (!game.getStack().isEmpty()) {
                final List<GameObject> threatenedObjects = ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), sa);
                for (final Object o : objects) {
                    if (threatenedObjects.contains(o)) {
                        chance = true;
                        break;
                    }
                }
            } else {
                PhaseHandler handler = game.getPhaseHandler();
                if (handler.is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                    boolean flag = false;
                    for (final Object o : objects) {
                        if (o instanceof Card) {
                            flag = flag || ComputerUtilCombat.combatantWouldBeDestroyed(ai, (Card) o, combat);
                        } else if (o instanceof Player) {
                            // Don't need to worry about Combat Damage during AI's turn
                            final Player p = (Player) o;
                            if (!handler.isPlayerTurn(p)) {
                                flag = flag || (p == ai && ((ComputerUtilCombat.wouldLoseLife(ai, combat) && sa
                                        .isAbility()) || ComputerUtilCombat.lifeInDanger(ai, combat)));
                            }
                        }
                    }

                    chance = flag;
                } else { // if nothing on the stack, and it's not declare
                         // blockers. no need to prevent
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            }
        } // non-targeted

        // react to threats on the stack
        else if (!game.getStack().isEmpty()) {
            sa.resetTargets();
        	final TargetChoices tcs = sa.getTargets();
            // check stack for something on the stack will kill anything i control
            final List<GameObject> objects = ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), sa);

            if (objects.contains(ai) && sa.canTarget(ai)) {
            	tcs.add(ai);
                chance = true;
            }
            final List<Card> threatenedTargets = new ArrayList<>();
            // filter AIs battlefield by what I can target
            List<Card> targetables = CardLists.getTargetableCards(ai.getCardsIn(ZoneType.Battlefield), sa);

            for (final Card c : targetables) {
                if (objects.contains(c)) {
                    threatenedTargets.add(c);
                }
            }

            if (!threatenedTargets.isEmpty()) {
                // Choose "best" of the remaining to save
            	tcs.add(ComputerUtilCard.getBestCreatureAI(threatenedTargets));
                chance = true;
            }

        } // Protect combatants
        else if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            sa.resetTargets();
        	final TargetChoices tcs = sa.getTargets();
            if (sa.canTarget(ai) && ComputerUtilCombat.wouldLoseLife(ai, combat)
                    && (ComputerUtilCombat.lifeInDanger(ai, combat) || sa.isAbility() || sa.isTrigger())
                    // check if any of the incoming dmg is even preventable:
                    && (ComputerUtilCombat.sumDamageIfUnblocked(combat.getAttackers(), ai, true) > ai.getPreventNextDamageTotalShields())
                    && game.getPhaseHandler().getPlayerTurn().isOpponentOf(ai)) {
            	tcs.add(ai);
                chance = true;
            } else {
                // filter AIs battlefield by what I can target
                CardCollectionView targetables = ai.getCardsIn(ZoneType.Battlefield);
                targetables = CardLists.getValidCards(targetables, tgt.getValidTgts(), ai, hostCard, sa);
                targetables = CardLists.getTargetableCards(targetables, sa);

                if (targetables.isEmpty()) {
                    return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
                }
                final CardCollection combatants = CardLists.filter(targetables, CardPredicates.CREATURES);
                ComputerUtilCard.sortByEvaluateCreature(combatants);

                for (final Card c : combatants) {
                    if (ComputerUtilCombat.combatantWouldBeDestroyed(ai, c, combat) && sa.canAddMoreTarget()) {
                    	tcs.add(c);
                        chance = true;
                    }
                }
            }
        }
        if (sa.usesTargeting() && sa.isDividedAsYouChoose() && !sa.getTargets().isEmpty()) {
            sa.addDividedAllocation(sa.getTargets().get(0), AbilityUtils.calculateAmount(hostCard, sa.getParam("Amount"), sa));
        }

        if (chance) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        } else {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        boolean chance = false;
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt == null) {
            // If there's no target on the trigger, just say yes.
            chance = true;
        } else {
            chance = preventDamageMandatoryTarget(ai, sa, mandatory);
        }

        if (chance) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        } else {
            return new AiAbilityDecision(0, AiPlayDecision.StopRunawayActivations);
        }
    }

    /**
     * <p>
     * preventDamageMandatoryTarget.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean preventDamageMandatoryTarget(final Player ai, final SpellAbility sa, final boolean mandatory) {
        sa.resetTargets();
        // filter AIs battlefield by what I can target
        final Game game = ai.getGame();
        CardCollectionView targetables = game.getCardsIn(ZoneType.Battlefield);
        targetables = CardLists.getTargetableCards(targetables, sa);
        final List<Card> compTargetables = CardLists.filterControlledBy(targetables, ai);
        Card target = null;

        if (targetables.isEmpty()) {
            return false;
        }

        if (!mandatory && compTargetables.isEmpty()) {
            return false;
        }

        if (!compTargetables.isEmpty()) {
            final CardCollection combatants = CardLists.filter(compTargetables, CardPredicates.CREATURES);
            ComputerUtilCard.sortByEvaluateCreature(combatants);
            if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                Combat combat = game.getCombat();
                for (final Card c : combatants) {
                    if (ComputerUtilCombat.combatantWouldBeDestroyed(ai, c, combat)) {
                        target = c;
                        break;
                    }
                }
            }
            if (target == null) {
                target = combatants.get(0);
            }
        } else {
            target = ComputerUtilCard.getCheapestPermanentAI(targetables, sa, true);
        }
        sa.getTargets().add(target);
        if (sa.isDividedAsYouChoose()) {
            sa.addDividedAllocation(target, AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Amount"), sa));
        }
        return true;
    }

}
