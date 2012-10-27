package forge.card.abilityfactory.ai;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import forge.Card;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.phase.PhaseType;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.util.MyRandom;

public class LoseLifeAi extends SpellAiLogic {

/* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public boolean canPlayAI(Player ai, Map<String, String> params, SpellAbility sa) {

    final Random r = MyRandom.getRandom();
    final Cost abCost = sa.getPayCosts();
    final Card source = sa.getSourceCard();
    boolean priority = false;

    final String amountStr = params.get("LifeAmount");

    // TODO handle proper calculation of X values based on Cost and what
    // would be paid
    int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), amountStr, sa);

    if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
        // Set PayX here to maximum value.
        amount = ComputerUtil.determineLeftoverMana(sa, ai);
        source.setSVar("PayX", Integer.toString(amount));
    }

    if (amount <= 0) {
        return false;
    }

    if (abCost != null) {
        // AI currently disabled for these costs
        if (!CostUtil.checkLifeCost(ai, abCost, source, amount, null)) {
            return false;
        }

        if (!CostUtil.checkDiscardCost(ai, abCost, source)) {
            return false;
        }

        if (!CostUtil.checkSacrificeCost(ai, abCost, source)) {
            return false;
        }

        if (!CostUtil.checkRemoveCounterCost(abCost, source)) {
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
            && !params.containsKey("ActivationPhases") && !priority) {
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
    if (AbilityFactory.playReusable(ai, sa) || priority) {
        randomReturn = true;
    }

    return (randomReturn);
}


/**
 * <p>
 * loseLifeDoTriggerAINoCost.
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
public boolean doTriggerAINoCost(final Player ai, final Map<String, String> params,
        final SpellAbility sa, final boolean mandatory) {
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
    final String amountStr = params.get("LifeAmount");
    int amount = 0;
    if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
        // Set PayX here to maximum value.
        final int xPay = ComputerUtil.determineLeftoverMana(sa, ai);
        source.setSVar("PayX", Integer.toString(xPay));
        amount = xPay;
    } else {
        amount = AbilityFactory.calculateAmount(source, amountStr, sa);
    }

    ArrayList<Player> tgtPlayers;
    if (tgt != null) {
        tgtPlayers = tgt.getTargetPlayers();
    } else {
        tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
    }

    if (!mandatory && tgtPlayers.contains(ai)) {
        // For cards like Foul Imp, ETB you lose life
        if ((amount + 3) > ai.getLife()) {
            return false;
        }
    }

    // check SubAbilities DoTrigger?
    final AbilitySub abSub = sa.getSubAbility();
    if (abSub != null) {
        return abSub.doTrigger(mandatory);
    }

    return true;
}
}