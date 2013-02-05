package forge.card.abilityfactory.ai;

import java.util.List;
import java.util.Random;

import forge.Card;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
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

public class MillAi extends SpellAiLogic {

    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        final Card source = sa.getSourceCard();
        final Cost abCost = sa.getPayCosts();

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }

            if (!ComputerUtilCost.checkDiscardCost(ai, abCost, source)) {
                return false;
            }

            if (!ComputerUtilCost.checkSacrificeCost(ai, abCost, source)) {
                return false;
            }

            if (!ComputerUtilCost.checkRemoveCounterCost(abCost, source)) {
                return false;
            }

        }

        if (!targetAI(ai, sa, false)) {
            return false;
        }

        final Random r = MyRandom.getRandom();

        // Don't use draw abilities before main 2 if possible
        if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2) && !sa.hasParam("ActivationPhases")) {
            return false;
        }

        // Don't tap creatures that may be able to block
        if (ComputerUtil.waitForBlocking(sa)) {
            return false;
        }

        double chance = .4; // 40 percent chance of milling with instant speed
                            // stuff
        if (AbilityFactory.isSorcerySpeed(sa)) {
            chance = .667; // 66.7% chance for sorcery speed
        }

        if ((Singletons.getModel().getGame().getPhaseHandler().is(PhaseType.END_OF_TURN)
                && Singletons.getModel().getGame().getPhaseHandler().getNextTurn().equals(ai))) {
            chance = .9; // 90% for end of opponents turn
        }

        boolean randomReturn = r.nextFloat() <= Math.pow(chance, sa.getActivationsThisTurn() + 1);

        if (sa.getParam("NumCards").equals("X") && source.getSVar("X").startsWith("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int cardsToDiscard =
                    Math.min(ComputerUtilMana.determineLeftoverMana(sa, ai), ai.getOpponent().getCardsIn(ZoneType.Library).size());
            source.setSVar("PayX", Integer.toString(cardsToDiscard));
            if (cardsToDiscard <= 0) {
                return false;
            }
        }

        if (AbilityFactory.playReusable(ai, sa)) {
            randomReturn = true;
            // some other variables here, like deck size, and phase and other fun stuff
        }

        return randomReturn;
    }

    private boolean targetAI(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final Target tgt = sa.getTarget();
        Player opp = ai.getOpponent();

        if (tgt != null) {
            tgt.resetTargets();
            if (!sa.canTarget(opp)) {
                if (mandatory && sa.canTarget(ai)) {
                    tgt.addTarget(ai);
                    return true;
                }
                return false;
            }

            final int numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), sa.getParam("NumCards"), sa);

            final List<Card> pLibrary = opp.getCardsIn(ZoneType.Library);

            if (pLibrary.size() == 0) { // deck already empty, no need to mill
                if (!mandatory) {
                    return false;
                }

                tgt.addTarget(opp);
                return true;
            }

            if (numCards >= pLibrary.size()) {
                // Can Mill out Human's deck? Do it!
                tgt.addTarget(opp);
                return true;
            }

            // Obscure case when you know what your top card is so you might?
            // want to mill yourself here
            // if (AI wants to mill self)
            // tgt.addTarget(AllZone.getComputerPlayer());
            // else
            tgt.addTarget(opp);
        }
        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, AIPlayer aiPlayer) {
        return targetAI(aiPlayer, sa, true);
    }


    @Override
    protected boolean doTriggerAINoCost(AIPlayer aiPlayer, SpellAbility sa, boolean mandatory) {
        if (!targetAI(aiPlayer, sa, mandatory)) {
            return false;
        }

        final Card source = sa.getSourceCard();
        if (sa.getParam("NumCards").equals("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int cardsToDiscard = Math.min(ComputerUtilMana.determineLeftoverMana(sa, aiPlayer), aiPlayer.getOpponent()
                    .getCardsIn(ZoneType.Library).size());
            source.setSVar("PayX", Integer.toString(cardsToDiscard));
        }

        return true;
    }
}
