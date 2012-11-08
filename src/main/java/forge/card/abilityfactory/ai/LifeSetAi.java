package forge.card.abilityfactory.ai;

import java.util.Random;

import forge.Card;
import forge.Counters;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.phase.PhaseType;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.util.MyRandom;

public class LifeSetAi extends SpellAiLogic {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Random r = MyRandom.getRandom();
        // Ability_Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();
        final int myLife = ai.getLife();
        final Player opponent = ai.getOpponent();
        final int hlife = opponent.getLife();
        final String amountStr = sa.getParam("LifeAmount");

        if (!ai.canGainLife()) {
            return false;
        }

        // Don't use setLife before main 2 if possible
        if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                && !sa.hasParam("ActivationPhases")) {
            return false;
        }

        // TODO handle proper calculation of X values based on Cost and what
        // would be paid
        int amount;
        // we shouldn't have to worry too much about PayX for SetLife
        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(xPay));
            amount = xPay;
        } else {
            amount = AbilityFactory.calculateAmount(sa.getSourceCard(), amountStr, sa);
        }

        // prevent run-away activations - first time will always return true
        final boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            if (tgt.canOnlyTgtOpponent()) {
                tgt.addTarget(opponent);
                // if we can only target the human, and the Human's life
                // would
                // go up, don't play it.
                // possibly add a combo here for Magister Sphinx and
                // Higedetsu's
                // (sp?) Second Rite
                if ((amount > hlife) || !opponent.canLoseLife()) {
                    return false;
                }
            } else {
                if ((amount > myLife) && (myLife <= 10)) {
                    tgt.addTarget(ai);
                } else if (hlife > amount) {
                    tgt.addTarget(opponent);
                } else if (amount > myLife) {
                    tgt.addTarget(ai);
                } else {
                    return false;
                }
            }
        } else {
            if (sa.hasParam("Each") && sa.getParam("Defined").equals("Each")) {
                if (amount == 0) {
                    return false;
                } else if (myLife > amount) { // will decrease computer's
                                              // life
                    if ((myLife < 5) || ((myLife - amount) > (hlife - amount))) {
                        return false;
                    }
                }
            }
            if (amount < myLife) {
                return false;
            }
        }

        // if life is in danger, always activate
        if ((myLife < 3) && (amount > myLife)) {
            return true;
        }

        return ((r.nextFloat() < .6667) && chance);
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final int myLife = ai.getLife();
        final Player opponent = ai.getOpponent();
        final int hlife = opponent.getLife();
        final Card source = sa.getSourceCard();

        final String amountStr = sa.getParam("LifeAmount");

        int amount;
        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(xPay));
            amount = xPay;
        } else {
            amount = AbilityFactory.calculateAmount(sa.getSourceCard(), amountStr, sa);
        }

        if (source.getName().equals("Eternity Vessel")
                && (opponent.isCardInPlay("Vampire Hexmage") || (source.getCounters(Counters.CHARGE) == 0))) {
            return false;
        }

        // If the Target is gaining life, target self.
        // if the Target is modifying how much life is gained, this needs to
        // be
        // handled better
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            if (tgt.canOnlyTgtOpponent()) {
                tgt.addTarget(opponent);
            } else {
                if ((amount > myLife) && (myLife <= 10)) {
                    tgt.addTarget(ai);
                } else if (hlife > amount) {
                    tgt.addTarget(opponent);
                } else if (amount > myLife) {
                    tgt.addTarget(ai);
                } else {
                    return false;
                }
            }
        }

        return true;
    }

}