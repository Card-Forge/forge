package forge.card.ability.ai;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAiLogic;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.ai.ComputerUtil;
import forge.game.ai.ComputerUtilCombat;
import forge.game.ai.ComputerUtilCost;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class DamagePreventAi extends SpellAiLogic {

    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        final Card hostCard = sa.getSourceCard();
        boolean chance = false;

        final Cost cost = sa.getPayCosts();

        // temporarily disabled until better AI
        if (!ComputerUtilCost.checkLifeCost(ai, cost, hostCard, 4, null)) {
            return false;
        }

        if (!ComputerUtilCost.checkDiscardCost(ai, cost, hostCard)) {
            return false;
        }

        if (!ComputerUtilCost.checkSacrificeCost(ai, cost, hostCard)) {
            return false;
        }

        if (!ComputerUtilCost.checkRemoveCounterCost(cost, hostCard)) {
            return false;
        }

        final Target tgt = sa.getTarget();
        if (tgt == null) {
            // As far as I can tell these Defined Cards will only have one of
            // them
            final ArrayList<Object> objects = AbilityUtils.getDefinedObjects(sa.getSourceCard(),
                    sa.getParam("Defined"), sa);

            // react to threats on the stack
            if (Singletons.getModel().getGame().getStack().size() > 0) {
                final ArrayList<Object> threatenedObjects = ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), sa);
                for (final Object o : objects) {
                    if (threatenedObjects.contains(o)) {
                        chance = true;
                    }
                }
            } else {
                PhaseHandler handler = Singletons.getModel().getGame().getPhaseHandler();
                if (handler.is(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
                    boolean flag = false;
                    for (final Object o : objects) {
                        if (o instanceof Card) {
                            final Card c = (Card) o;
                            flag |= ComputerUtilCombat.combatantWouldBeDestroyed(ai, c);
                        } else if (o instanceof Player) {
                            // Don't need to worry about Combat Damage during AI's turn
                            final Player p = (Player) o;
                            if (!handler.isPlayerTurn(p)) {
                                flag |= (p == ai && ((ComputerUtilCombat.wouldLoseLife(ai, Singletons.getModel().getGame().getCombat()) && sa
                                        .isAbility()) || ComputerUtilCombat.lifeInDanger(ai, Singletons.getModel().getGame().getCombat())));
                            }
                        }
                    }

                    chance = flag;
                } else { // if nothing on the stack, and it's not declare
                         // blockers. no need to prevent
                    return false;
                }
            }
        } // targeted

        // react to threats on the stack
        else if (Singletons.getModel().getGame().getStack().size() > 0) {
            tgt.resetTargets();
            // check stack for something on the stack will kill anything i
            // control
            final ArrayList<Object> objects = new ArrayList<Object>();
            // AbilityFactory.predictThreatenedObjects(af);

            if (objects.contains(ai)) {
                tgt.addTarget(ai);
            }

            final List<Card> threatenedTargets = new ArrayList<Card>();
            // filter AIs battlefield by what I can target
            List<Card> targetables = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), tgt.getValidTgts(), ai, hostCard);

            for (final Card c : targetables) {
                if (objects.contains(c)) {
                    threatenedTargets.add(c);
                }
            }

            if (!threatenedTargets.isEmpty()) {
                // Choose "best" of the remaining to save
                tgt.addTarget(CardFactoryUtil.getBestCreatureAI(threatenedTargets));
                chance = true;
            }

        } // Protect combatants
        else if (Singletons.getModel().getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
            if (sa.canTarget(ai) && ComputerUtilCombat.wouldLoseLife(ai, Singletons.getModel().getGame().getCombat())
                    && (ComputerUtilCombat.lifeInDanger(ai, Singletons.getModel().getGame().getCombat()) || sa.isAbility())
                    && Singletons.getModel().getGame().getPhaseHandler().isPlayerTurn(ai.getOpponent())) {
                tgt.addTarget(ai);
                chance = true;
            } else {
                // filter AIs battlefield by what I can target
                List<Card> targetables = ai.getCardsIn(ZoneType.Battlefield);
                targetables = CardLists.getValidCards(targetables, tgt.getValidTgts(), ai, hostCard);

                if (targetables.size() == 0) {
                    return false;
                }
                final List<Card> combatants = CardLists.filter(targetables, CardPredicates.Presets.CREATURES);
                CardLists.sortByEvaluateCreature(combatants);

                for (final Card c : combatants) {
                    if (ComputerUtilCombat.combatantWouldBeDestroyed(ai, c)) {
                        tgt.addTarget(c);
                        chance = true;
                        break;
                    }
                }
            }
        }

        return chance;
    }

    @Override
    protected boolean doTriggerAINoCost(AIPlayer ai, SpellAbility sa, boolean mandatory) {
        boolean chance = false;
        final Target tgt = sa.getTarget();
        if (tgt == null) {
            // If there's no target on the trigger, just say yes.
            chance = true;
        } else {
            chance = preventDamageMandatoryTarget(ai, sa, mandatory);
        }

        return chance;
    }

    /**
     * <p>
     * preventDamageMandatoryTarget.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean preventDamageMandatoryTarget(final Player ai, final SpellAbility sa,
            final boolean mandatory) {
        final Target tgt = sa.getTarget();
        tgt.resetTargets();
        // filter AIs battlefield by what I can target
        List<Card> targetables = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        targetables = CardLists.getValidCards(targetables, tgt.getValidTgts(), ai, sa.getSourceCard());
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
                    if (ComputerUtilCombat.combatantWouldBeDestroyed(ai, c)) {
                        tgt.addTarget(c);
                        return true;
                    }
                }
            }

            // TODO see if something on the stack is about to kill something I
            // can target

            tgt.addTarget(combatants.get(0));
            return true;
        }

        tgt.addTarget(CardFactoryUtil.getCheapestPermanentAI(targetables, sa, true));
        return true;
    }

}
