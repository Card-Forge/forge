package forge.card.abilityfactory.ai;

import java.util.List;
import java.util.Random;

import forge.Card;
import forge.Singletons;
import forge.card.abilityfactory.AbilityUtils;
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
import forge.util.MyRandom;

public class LifeLoseAi extends SpellAiLogic {

    @Override
    public boolean chkAIDrawback(SpellAbility sa, AIPlayer ai) {

        final Target tgt = sa.getTarget();
        List<Player> tgtPlayers;
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityUtils.getDefinedPlayers(sa.getSourceCard(), sa.getParam("Defined"), sa);
        }

        final Card source = sa.getSourceCard();
        final String amountStr = sa.getParam("LifeAmount");
        int amount = 0;
        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(xPay));
            amount = xPay;
        } else {
            amount = AbilityUtils.calculateAmount(source, amountStr, sa);
        }

        if (tgtPlayers.contains(ai) && amount > 0 && amount + 3 > ai.getLife()) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {

        final Random r = MyRandom.getRandom();
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();
        boolean priority = false;

        final String amountStr = sa.getParam("LifeAmount");

        // TODO handle proper calculation of X values based on Cost and what
        // would be paid
        int amount = AbilityUtils.calculateAmount(sa.getSourceCard(), amountStr, sa);

        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            amount = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(amount));
        }

        if (amount <= 0) {
            return false;
        }

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, amount, null)) {
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

        Player opp = ai.getOpponent();

        if (!opp.canLoseLife()) {
            return false;
        }

        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        if (amount >= opp.getLife()) {
            priority = true; // killing the human should be done asap
        }

        // Don't use loselife before main 2 if possible
        if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                && !sa.hasParam("ActivationPhases") && !priority) {
            return false;
        }

        // Don't tap creatures that may be able to block
        if (ComputerUtil.waitForBlocking(sa) && !priority) {
            return false;
        }

        final Target tgt = sa.getTarget();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            if (sa.canTarget(opp)) {
                sa.getTarget().addTarget(opp);
            } else {
                return false;
            }
        }

        boolean randomReturn = r.nextFloat() <= .6667;
        if (SpellAiLogic.playReusable(ai, sa) || priority) {
            randomReturn = true;
        }

        return (randomReturn);
    }

    @Override
    protected boolean doTriggerAINoCost(final AIPlayer ai, final SpellAbility sa,
    final boolean mandatory) {
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            if (sa.canTarget(ai.getOpponent())) {
                tgt.addTarget(ai.getOpponent());
            } else if (mandatory && sa.canTarget(ai)) {
                tgt.addTarget(ai);
            } else {
                return false;
            }
        }

        final Card source = sa.getSourceCard();
        final String amountStr = sa.getParam("LifeAmount");
        int amount = 0;
        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(xPay));
            amount = xPay;
        } else {
            amount = AbilityUtils.calculateAmount(source, amountStr, sa);
        }

        List<Player> tgtPlayers;
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityUtils.getDefinedPlayers(sa.getSourceCard(), sa.getParam("Defined"), sa);
        }

        if (!mandatory && tgtPlayers.contains(ai) && amount > 0 && amount + 3 > ai.getLife()) {
            // For cards like Foul Imp, ETB you lose life
            return false;
        }

        return true;
    }
}
