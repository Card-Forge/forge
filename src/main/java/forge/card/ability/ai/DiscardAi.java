package forge.card.ability.ai;

import java.util.List;
import java.util.Random;

import forge.Card;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityAi;
import forge.card.cost.Cost;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.ai.ComputerUtil;
import forge.game.ai.ComputerUtilCost;
import forge.game.ai.ComputerUtilMana;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class DiscardAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();
        final Cost abCost = sa.getPayCosts();

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!ComputerUtilCost.checkSacrificeCost(ai, abCost, source)) {
                return false;
            }

            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }

            if (!ComputerUtilCost.checkDiscardCost(ai, abCost, source)) {
                return false;
            }

            if (!ComputerUtilCost.checkRemoveCounterCost(abCost, source)) {
                return false;
            }

        }

        final boolean humanHasHand = ai.getOpponent().getCardsIn(ZoneType.Hand).size() > 0;

        if (tgt != null) {
            if (!discardTargetAI(ai, sa)) {
                return false;
            }
        } else {
            // TODO: Add appropriate restrictions
            final List<Player> players = AbilityUtils.getDefinedPlayers(sa.getSourceCard(),
                    sa.getParam("Defined"), sa);

            if (players.size() == 1) {
                if (players.get(0) == ai) {
                    // the ai should only be using something like this if he has
                    // few cards in hand,
                    // cards like this better have a good drawback to be in the
                    // AIs deck
                } else {
                    // defined to the human, so that's fine as long the human
                    // has cards
                    if (!humanHasHand) {
                        return false;
                    }
                }
            } else {
                // Both players discard, any restrictions?
            }
        }

        if (sa.hasParam("NumCards")) {
           if (sa.getParam("NumCards").equals("X") && source.getSVar("X").equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                final int cardsToDiscard = Math.min(ComputerUtilMana.determineLeftoverMana(sa, ai), ai.getOpponent()
                        .getCardsIn(ZoneType.Hand).size());
                if (cardsToDiscard < 1) {
                    return false;
                }
                source.setSVar("PayX", Integer.toString(cardsToDiscard));
            } else {
                if (AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("NumCards"), sa) < 1) {
                    return false;
                }
            }
        }

        // TODO: Implement support for Discard AI for cards with AnyNumber set to true.

        // Don't use draw abilities before main 2 if possible
        if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                && !sa.hasParam("ActivationPhases")) {
            return false;
        }

        // Don't tap creatures that may be able to block
        if (ComputerUtil.waitForBlocking(sa) && !sa.hasParam("ActivationPhases")) {
            return false;
        }

        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= Math.pow(0.9, sa.getActivationsThisTurn());

        // some other variables here, like handsize vs. maxHandSize

        return randomReturn;
    } // discardCanPlayAI()

    private boolean discardTargetAI(final Player ai, final SpellAbility sa) {
        final Target tgt = sa.getTarget();
        Player opp = ai.getOpponent();
        if (opp.getCardsIn(ZoneType.Hand).isEmpty()) {
            return false;
        }
        if (tgt != null) {
            if (sa.canTarget(opp)) {
                tgt.addTarget(opp);
                return true;
            }
        }
        return false;
    } // discardTargetAI()



    @Override
    protected boolean doTriggerAINoCost(AIPlayer ai, SpellAbility sa, boolean mandatory) {
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            Player opp = ai.getOpponent();
            if (!discardTargetAI(ai, sa)) {
                if (mandatory && sa.canTarget(opp)) {
                    tgt.addTarget(opp);
                } else if (mandatory && sa.canTarget(ai)) {
                    tgt.addTarget(ai);
                } else {
                    return false;
                }
            }
        } else {
            if ("X".equals(sa.getParam("RevealNumber")) && sa.getSourceCard().getSVar("X").equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                final int cardsToDiscard = Math.min(ComputerUtilMana.determineLeftoverMana(sa, ai), ai.getOpponent()
                        .getCardsIn(ZoneType.Hand).size());
                sa.getSourceCard().setSVar("PayX", Integer.toString(cardsToDiscard));
            }
        }

        return true;
    } // discardTrigger()

    @Override
    public boolean chkAIDrawback(SpellAbility sa, AIPlayer ai) {
        // Drawback AI improvements
        // if parent draws cards, make sure cards in hand + cards drawn > 0
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            return discardTargetAI(ai, sa);
        }
        // TODO: check for some extra things
        return true;
    } // discardCheckDrawbackAI()
}
