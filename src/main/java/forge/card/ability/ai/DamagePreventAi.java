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
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class DamagePreventAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Card hostCard = sa.getSourceCard();
        final Game game = ai.getGame();
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

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt == null) {
            // As far as I can tell these Defined Cards will only have one of
            // them
            final List<ITargetable> objects = AbilityUtils.getDefinedObjects(sa.getSourceCard(), sa.getParam("Defined"), sa);

            // react to threats on the stack
            if (!game.getStack().isEmpty()) {
                final List<ITargetable> threatenedObjects = ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), sa);
                for (final Object o : objects) {
                    if (threatenedObjects.contains(o)) {
                        chance = true;
                    }
                }
            } else {
                PhaseHandler handler = game.getPhaseHandler();
                if (handler.is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                    boolean flag = false;
                    for (final Object o : objects) {
                        if (o instanceof Card) {
                            final Card c = (Card) o;
                            flag |= ComputerUtilCombat.combatantWouldBeDestroyed(ai, c);
                        } else if (o instanceof Player) {
                            // Don't need to worry about Combat Damage during AI's turn
                            final Player p = (Player) o;
                            if (!handler.isPlayerTurn(p)) {
                                flag |= (p == ai && ((ComputerUtilCombat.wouldLoseLife(ai, game.getCombat()) && sa
                                        .isAbility()) || ComputerUtilCombat.lifeInDanger(ai, game.getCombat())));
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
        else if (!game.getStack().isEmpty()) {
            sa.resetTargets();
            // check stack for something on the stack will kill anything i
            // control
            final ArrayList<Object> objects = new ArrayList<Object>();
            // AbilityFactory.predictThreatenedObjects(af);

            if (objects.contains(ai)) {
                sa.getTargets().add(ai);
            }

            final List<Card> threatenedTargets = new ArrayList<Card>();
            // filter AIs battlefield by what I can target
            List<Card> targetables = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), tgt.getValidTgts(), ai, hostCard);
            targetables = CardLists.getTargetableCards(targetables, sa);

            for (final Card c : targetables) {
                if (objects.contains(c)) {
                    threatenedTargets.add(c);
                }
            }

            if (!threatenedTargets.isEmpty()) {
                // Choose "best" of the remaining to save
                sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(threatenedTargets));
                chance = true;
            }

        } // Protect combatants
        else if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            if (sa.canTarget(ai) && ComputerUtilCombat.wouldLoseLife(ai, game.getCombat())
                    && (ComputerUtilCombat.lifeInDanger(ai, game.getCombat()) || sa.isAbility())
                    && game.getPhaseHandler().isPlayerTurn(ai.getOpponent())) {
                sa.getTargets().add(ai);
                chance = true;
            } else {
                // filter AIs battlefield by what I can target
                List<Card> targetables = ai.getCardsIn(ZoneType.Battlefield);
                targetables = CardLists.getValidCards(targetables, tgt.getValidTgts(), ai, hostCard);
                targetables = CardLists.getTargetableCards(targetables, sa);

                if (targetables.isEmpty()) {
                    return false;
                }
                final List<Card> combatants = CardLists.filter(targetables, CardPredicates.Presets.CREATURES);
                CardLists.sortByEvaluateCreature(combatants);

                for (final Card c : combatants) {
                    if (ComputerUtilCombat.combatantWouldBeDestroyed(ai, c)) {
                        sa.getTargets().add(c);
                        chance = true;
                        break;
                    }
                }
            }
        }

        return chance;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        boolean chance = false;
        final TargetRestrictions tgt = sa.getTargetRestrictions();
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
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        sa.resetTargets();
        // filter AIs battlefield by what I can target
        final Game game = ai.getGame();
        List<Card> targetables = game.getCardsIn(ZoneType.Battlefield);
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
            if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                for (final Card c : combatants) {
                    if (ComputerUtilCombat.combatantWouldBeDestroyed(ai, c)) {
                        sa.getTargets().add(c);
                        return true;
                    }
                }
            }

            // TODO see if something on the stack is about to kill something I
            // can target

            sa.getTargets().add(combatants.get(0));
            return true;
        }

        sa.getTargets().add(ComputerUtilCard.getCheapestPermanentAI(targetables, sa, true));
        return true;
    }

}
